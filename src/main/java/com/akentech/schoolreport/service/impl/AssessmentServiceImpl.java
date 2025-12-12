package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.dto.StudentTermAverageDTO;
import com.akentech.schoolreport.dto.StudentYearlyAverageDTO;
import com.akentech.schoolreport.exception.BusinessRuleException;
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

    // Add this method to handle String type parameter conversion
    @Override
    @Transactional(readOnly = true)
    public Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject,
                                                             Integer term, String type) {
        try {
            // Convert String to AssessmentType
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

        // Check for duplicate assessment
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

        // Update student subject scores after saving assessment
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

        // Update scores for the first student in the batch
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
        // Get all subjects for the student
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

        // Calculate yearly average (average of all terms)
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

        // Rank students by average (descending)
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

        // Rank students by yearly average (descending)
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

        // Ensure all scores are on 20 marks scale
        if (assessment.getScore() < 0 || assessment.getScore() > 20) {
            throw new BusinessRuleException("Score must be between 0 and 20 (out of 20 marks)");
        }

        // Validate term and assessment type consistency
        AssessmentType type = assessment.getType();
        if (!type.getTerm().equals(assessment.getTerm())) {
            throw new BusinessRuleException("Assessment type " + type + " is not valid for term " + assessment.getTerm());
        }
    }
}