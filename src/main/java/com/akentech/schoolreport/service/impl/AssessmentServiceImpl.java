package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.dto.StudentAssessmentSummaryDTO;
import com.akentech.schoolreport.dto.StudentTermAverageDTO;
import com.akentech.schoolreport.dto.StudentYearlyAverageDTO;
import com.akentech.schoolreport.dto.TermAssessmentDTO;
import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.StudentSubject;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.AssessmentType;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.StudentSubjectRepository;
import com.akentech.schoolreport.service.AssessmentService;
import com.akentech.schoolreport.service.GradeService;
import com.akentech.schoolreport.service.StudentPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final StudentRepository studentRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final StudentPerformanceService studentPerformanceService;
    private final GradeService gradeService;

    @Override
    @Transactional(readOnly = true)
    public Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject,
                                                             Integer term, String type) {
        try {
            AssessmentType assessmentType = AssessmentType.valueOf(type.toUpperCase());
            return assessmentRepository.findByStudentAndSubjectAndTermAndType(student, subject, term, assessmentType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid assessment type: {}", type);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assessment> getAssessmentByStudentSubjectAndTermAndType(Long studentId, Long subjectId,
                                                                            Integer term, AssessmentType type) {
        return assessmentRepository.findByStudentIdAndSubjectIdAndTermAndType(studentId, subjectId, term, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsByStudentAndTerm(Long studentId, Integer term) {
        return assessmentRepository.findByStudentIdAndTerm(studentId, term);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsByStudentSubjectAndTerm(Long studentId, Long subjectId, Integer term) {
        return assessmentRepository.findByStudentIdAndSubjectIdAndTerm(studentId, subjectId, term);
    }

    @Override
    @Transactional
    public Assessment save(Assessment assessment) {
        validateAssessment(assessment);

        Optional<Assessment> existing = assessmentRepository.findByStudentAndSubjectAndTermAndType(
                assessment.getStudent(), assessment.getSubject(), assessment.getTerm(), assessment.getType());

        if (existing.isPresent() && !existing.get().getId().equals(assessment.getId())) {
            throw new BusinessRuleException("Assessment of type '" + assessment.getType() +
                    "' already exists for this student, subject, and term");
        }

        Assessment saved = assessmentRepository.save(assessment);
        log.info("Saved assessment: student={} {} subject={} term={} type={} score={}/20",
                saved.getStudent().getFirstName(),
                saved.getStudent().getLastName(),
                saved.getSubject().getName(),
                saved.getTerm(),
                saved.getType(),
                saved.getScore());

        try {
            studentPerformanceService.updateStudentSubjectScores(
                    saved.getStudent().getId(), saved.getTerm());
        } catch (Exception e) {
            log.error("Failed to update student scores after assessment save", e);
        }

        return saved;
    }

    @Override
    @Transactional
    public List<Assessment> saveAll(List<Assessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return List.of();
        }

        for (Assessment assessment : assessments) {
            validateAssessment(assessment);
        }

        List<Assessment> saved = assessmentRepository.saveAll(assessments);
        log.info("Saved {} assessments", saved.size());

        if (!saved.isEmpty()) {
            Long studentId = saved.getFirst().getStudent().getId();
            Integer term = saved.getFirst().getTerm();
            try {
                studentPerformanceService.updateStudentSubjectScores(studentId, term);
            } catch (Exception e) {
                log.error("Failed to update student scores after batch save", e);
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!assessmentRepository.existsById(id)) {
            throw new IllegalArgumentException("Assessment not found with id: " + id);
        }
        assessmentRepository.deleteById(id);
        log.info("Deleted assessment id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsByStudent(Long studentId) {
        return assessmentRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsBySubjectAndTerm(Long subjectId, Integer term) {
        return assessmentRepository.findBySubjectIdAndTermOrderByScoreDesc(subjectId, term);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateTermAverage(Long studentId, Integer term) {
        List<Subject> studentSubjects = getStudentSubjects(studentId);

        if (studentSubjects.isEmpty()) {
            return 0.0;
        }

        double totalWeightedScore = 0.0;
        int totalCoefficient = 0;

        for (Subject subject : studentSubjects) {
            List<Assessment> subjectAssessments = getAssessmentsByStudentSubjectAndTerm(
                    studentId, subject.getId(), term);

            if (!subjectAssessments.isEmpty()) {
                double subjectAverage = gradeService.calculateSubjectAverageForTerm(subjectAssessments, term);
                totalWeightedScore += subjectAverage * subject.getCoefficient();
                totalCoefficient += subject.getCoefficient();
            }
        }

        return totalCoefficient > 0 ? totalWeightedScore / totalCoefficient : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateYearlyAverage(Long studentId) {
        double term1Avg = calculateTermAverage(studentId, 1);
        double term2Avg = calculateTermAverage(studentId, 2);
        double term3Avg = calculateTermAverage(studentId, 3);

        return gradeService.calculateYearlyAverage(term1Avg, term2Avg, term3Avg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentTermAverageDTO> getTermAveragesForClass(Long classId, Integer term) {
        List<Student> students = studentRepository.findByClassRoomId(classId);
        List<StudentTermAverageDTO> termAverages = new ArrayList<>();

        for (Student student : students) {
            Double average = calculateTermAverage(student.getId(), term);
            termAverages.add(new StudentTermAverageDTO(student, average, null));
        }

        termAverages.sort(Comparator.comparing(StudentTermAverageDTO::getAverage).reversed());

        int rank = 1;
        for (StudentTermAverageDTO dto : termAverages) {
            dto.setRank(rank++);
        }

        return termAverages;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentYearlyAverageDTO> getYearlyAveragesForClass(Long classId) {
        List<Student> students = studentRepository.findByClassRoomId(classId);
        List<StudentYearlyAverageDTO> yearlyAverages = new ArrayList<>();

        for (Student student : students) {
            Double yearlyAverage = calculateYearlyAverage(student.getId());
            yearlyAverages.add(new StudentYearlyAverageDTO(student, yearlyAverage, null));
        }

        yearlyAverages.sort(Comparator.comparing(StudentYearlyAverageDTO::getYearlyAverage).reversed());

        int rank = 1;
        for (StudentYearlyAverageDTO dto : yearlyAverages) {
            dto.setRank(rank++);
        }

        return yearlyAverages;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Double>> getStudentSubjectScoresByTerm(Long studentId, Integer term) {
        List<Subject> subjects = getStudentSubjects(studentId);
        Map<Long, List<Double>> subjectScores = new HashMap<>();

        for (Subject subject : subjects) {
            List<Assessment> assessments = getAssessmentsByStudentSubjectAndTerm(studentId, subject.getId(), term);
            List<Double> scores = assessments.stream()
                    .map(Assessment::getScore)
                    .collect(Collectors.toList());
            subjectScores.put(subject.getId(), scores);
        }

        return subjectScores;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAssessmentSummaryDTO getAssessmentSummary(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        List<Assessment> assessments = getAssessmentsByStudent(studentId);

        Map<Integer, List<Assessment>> assessmentsByTerm = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        Double term1Avg = calculateTermAverage(studentId, 1);
        Double term2Avg = calculateTermAverage(studentId, 2);
        Double term3Avg = calculateTermAverage(studentId, 3);
        Double yearlyAvg = calculateYearlyAverage(studentId);

        List<StudentSubject> enrolledSubjects = studentSubjectRepository.findByStudentId(studentId);
        int totalSubjects = enrolledSubjects.size();

        boolean term1Completed = checkTermCompletion(assessmentsByTerm.get(1), totalSubjects);
        boolean term2Completed = checkTermCompletion(assessmentsByTerm.get(2), totalSubjects);
        boolean term3Completed = checkTermCompletion(assessmentsByTerm.get(3), totalSubjects);

        return StudentAssessmentSummaryDTO.builder()
                .studentId(studentId)
                .studentName(student.getFullName())
                .totalAssessments(assessments.size())
                .term1Assessments(assessmentsByTerm.getOrDefault(1, List.of()).size())
                .term2Assessments(assessmentsByTerm.getOrDefault(2, List.of()).size())
                .term3Assessments(assessmentsByTerm.getOrDefault(3, List.of()).size())
                .term1Average(term1Avg)
                .term2Average(term2Avg)
                .term3Average(term3Avg)
                .yearlyAverage(yearlyAvg)
                .totalSubjects(totalSubjects)
                .term1CompletedSubjects(countCompletedSubjects(assessmentsByTerm.get(1)))
                .term2CompletedSubjects(countCompletedSubjects(assessmentsByTerm.get(2)))
                .term3CompletedSubjects(countCompletedSubjects(assessmentsByTerm.get(3)))
                .term1Completed(term1Completed)
                .term2Completed(term2Completed)
                .term3Completed(term3Completed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermAssessmentDTO> getTermAssessments(Long studentId, Integer term) {
        List<StudentSubject> enrolledSubjects = studentSubjectRepository.findByStudentId(studentId);
        List<TermAssessmentDTO> termAssessments = new ArrayList<>();

        for (StudentSubject enrollment : enrolledSubjects) {
            Subject subject = enrollment.getSubject();

            List<Assessment> subjectAssessments = getAssessmentsByStudentSubjectAndTerm(
                    studentId, subject.getId(), term);

            Map<Integer, Double> scores = new HashMap<>();
            for (Assessment assessment : subjectAssessments) {
                scores.put(assessment.getType().getAssessmentNumber(), assessment.getScore());
            }

            Double termAverage = calculateSubjectTermAverage(scores, term);
            String termGrade = termAverage != null ?
                    gradeService.calculateLetterGrade(termAverage, subject.getName()) : null;

            TermAssessmentDTO dto = TermAssessmentDTO.builder()
                    .subjectId(subject.getId())
                    .subjectName(subject.getName())
                    .subjectCode(subject.getSubjectCode())
                    .coefficient(subject.getCoefficient())
                    .assessment1Score(scores.get(1))
                    .assessment2Score(scores.get(2))
                    .assessment3Score(scores.get(3))
                    .assessment4Score(scores.get(4))
                    .assessment5Score(scores.get(5))
                    .termAverage(termAverage)
                    .termGrade(termGrade)
                    .completed(termAverage != null)
                    .build();

            termAssessments.add(dto);
        }

        // Sort by subject name
        termAssessments.sort(Comparator.comparing(TermAssessmentDTO::getSubjectName));

        return termAssessments;
    }

    private Double calculateSubjectTermAverage(Map<Integer, Double> scores, Integer term) {
        if (scores.isEmpty()) {
            return null;
        }

        return switch (term) {
            case 1 -> {
                Double a1 = scores.get(1);
                Double a2 = scores.get(2);
                if (a1 != null && a2 != null) {
                    yield (a1 + a2) / 2.0;
                } else if (a1 != null) {
                    yield a1;
                } else yield a2;
            }
            case 2 -> {
                Double a3 = scores.get(3);
                Double a4 = scores.get(4);
                if (a3 != null && a4 != null) {
                    yield (a3 + a4) / 2.0;
                } else if (a3 != null) {
                    yield a3;
                } else yield a4;
            }
            case 3 -> scores.get(5);
            default -> null;
        };
    }

    private boolean checkTermCompletion(List<Assessment> assessments, int totalSubjects) {
        if (assessments == null) {
            return false;
        }

        long assessedSubjects = assessments.stream()
                .map(a -> a.getSubject().getId())
                .distinct()
                .count();

        return assessedSubjects == totalSubjects;
    }

    private int countCompletedSubjects(List<Assessment> assessments) {
        if (assessments == null) {
            return 0;
        }

        return (int) assessments.stream()
                .map(a -> a.getSubject().getId())
                .distinct()
                .count();
    }

    private List<Subject> getStudentSubjects(Long studentId) {
        return studentSubjectRepository.findByStudentId(studentId).stream()
                .map(StudentSubject::getSubject)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateAssessment(Assessment assessment) {
        if (assessment == null) {
            throw new BusinessRuleException("Assessment cannot be null");
        }

        if (assessment.getStudent() == null || assessment.getStudent().getId() == null) {
            throw new BusinessRuleException("Student is required");
        }

        if (assessment.getSubject() == null || assessment.getSubject().getId() == null) {
            throw new BusinessRuleException("Subject is required");
        }

        if (assessment.getTerm() == null || assessment.getTerm() < 1 || assessment.getTerm() > 3) {
            throw new BusinessRuleException("Term must be between 1 and 3");
        }

        if (assessment.getType() == null) {
            throw new BusinessRuleException("Assessment type is required");
        }

        if (assessment.getScore() == null) {
            throw new BusinessRuleException("Score is required");
        }

        if (assessment.getScore() < 0 || assessment.getScore() > 20) {
            throw new BusinessRuleException("Score must be between 0 and 20 (out of 20 marks)");
        }

        AssessmentType type = assessment.getType();
        if (!type.getTerm().equals(assessment.getTerm())) {
            throw new BusinessRuleException("Assessment type " + type + " is not valid for term " + assessment.getTerm());
        }
    }
}