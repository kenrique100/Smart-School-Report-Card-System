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
        // Set academic year if missing
        assessment = setAcademicYearIfMissing(assessment);

        validateAssessment(assessment);

        Optional<Assessment> existing = assessmentRepository.findByStudentAndSubjectAndTermAndType(
                assessment.getStudent(), assessment.getSubject(), assessment.getTerm(), assessment.getType());

        if (existing.isPresent() && !existing.get().getId().equals(assessment.getId())) {
            throw new BusinessRuleException("Assessment of type '" + assessment.getType() +
                    "' already exists for this student, subject, and term");
        }

        Assessment saved = assessmentRepository.save(assessment);
        log.info("Saved assessment: student={} {} subject={} term={} type={} score={}/20 academicYear={}-{}",
                saved.getStudent().getFirstName(),
                saved.getStudent().getLastName(),
                saved.getSubject().getName(),
                saved.getTerm(),
                saved.getType(),
                saved.getScore(),
                saved.getAcademicYearStart(),
                saved.getAcademicYearEnd());

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

        // Set academic years for all assessments
        List<Assessment> validatedAssessments = new ArrayList<>();
        for (Assessment assessment : assessments) {
            Assessment validated = setAcademicYearIfMissing(assessment);
            validateAssessment(validated);
            validatedAssessments.add(validated);
        }

        List<Assessment> saved = assessmentRepository.saveAll(validatedAssessments);
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
        return calculateTermAverageWithAcademicYear(studentId, term, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateTermAverageWithAcademicYear(Long studentId, Integer term,
                                                       Integer academicYearStart, Integer academicYearEnd) {
        List<Subject> studentSubjects = getStudentSubjects(studentId);

        if (studentSubjects.isEmpty()) {
            return 0.0;
        }

        double totalWeightedScore = 0.0;
        int totalCoefficient = 0;

        for (Subject subject : studentSubjects) {
            List<Assessment> subjectAssessments;

            if (academicYearStart != null && academicYearEnd != null) {
                // Get assessments filtered by academic year
                subjectAssessments = getAssessmentsByStudentSubjectTermAndAcademicYear(
                        studentId, subject.getId(), term, academicYearStart, academicYearEnd);
            } else {
                // Get all assessments for the term
                subjectAssessments = getAssessmentsByStudentSubjectAndTerm(studentId, subject.getId(), term);
            }

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
        return calculateYearlyAverageWithAcademicYear(studentId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateYearlyAverageWithAcademicYear(Long studentId, Integer academicYearStart,
                                                         Integer academicYearEnd) {
        double term1Avg = calculateTermAverageWithAcademicYear(studentId, 1, academicYearStart, academicYearEnd);
        double term2Avg = calculateTermAverageWithAcademicYear(studentId, 2, academicYearStart, academicYearEnd);
        double term3Avg = calculateTermAverageWithAcademicYear(studentId, 3, academicYearStart, academicYearEnd);

        return gradeService.calculateYearlyAverage(term1Avg, term2Avg, term3Avg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentTermAverageDTO> getTermAveragesForClass(Long classId, Integer term) {
        return getTermAveragesForClassWithAcademicYear(classId, term, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentTermAverageDTO> getTermAveragesForClassWithAcademicYear(Long classId, Integer term,
                                                                               Integer academicYearStart,
                                                                               Integer academicYearEnd) {
        List<Student> students = studentRepository.findByClassRoomId(classId);
        List<StudentTermAverageDTO> termAverages = new ArrayList<>();

        for (Student student : students) {
            Double average;
            if (academicYearStart != null && academicYearEnd != null) {
                average = calculateTermAverageWithAcademicYear(student.getId(), term,
                        academicYearStart, academicYearEnd);
            } else {
                average = calculateTermAverage(student.getId(), term);
            }
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
        return getYearlyAveragesForClassWithAcademicYear(classId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentYearlyAverageDTO> getYearlyAveragesForClassWithAcademicYear(Long classId,
                                                                                   Integer academicYearStart,
                                                                                   Integer academicYearEnd) {
        List<Student> students = studentRepository.findByClassRoomId(classId);
        List<StudentYearlyAverageDTO> yearlyAverages = new ArrayList<>();

        for (Student student : students) {
            Double yearlyAverage;
            if (academicYearStart != null && academicYearEnd != null) {
                yearlyAverage = calculateYearlyAverageWithAcademicYear(student.getId(),
                        academicYearStart, academicYearEnd);
            } else {
                yearlyAverage = calculateYearlyAverage(student.getId());
            }
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
        return getStudentSubjectScoresByTermAndAcademicYear(studentId, term, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Double>> getStudentSubjectScoresByTermAndAcademicYear(Long studentId, Integer term,
                                                                                Integer academicYearStart,
                                                                                Integer academicYearEnd) {
        List<Subject> subjects = getStudentSubjects(studentId);
        Map<Long, List<Double>> subjectScores = new HashMap<>();

        for (Subject subject : subjects) {
            List<Assessment> assessments;
            if (academicYearStart != null && academicYearEnd != null) {
                assessments = getAssessmentsByStudentSubjectTermAndAcademicYear(
                        studentId, subject.getId(), term, academicYearStart, academicYearEnd);
            } else {
                assessments = getAssessmentsByStudentSubjectAndTerm(studentId, subject.getId(), term);
            }

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
        return getAssessmentSummaryWithAcademicYear(studentId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAssessmentSummaryDTO getAssessmentSummaryWithAcademicYear(Long studentId,
                                                                            Integer academicYearStart,
                                                                            Integer academicYearEnd) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        List<Assessment> assessments;
        if (academicYearStart != null && academicYearEnd != null) {
            assessments = getAssessmentsByStudentAndAcademicYearFromRepo(studentId, academicYearStart, academicYearEnd);
        } else {
            assessments = getAssessmentsByStudent(studentId);
        }

        Map<Integer, List<Assessment>> assessmentsByTerm = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        Double term1Avg = calculateTermAverageWithAcademicYear(studentId, 1, academicYearStart, academicYearEnd);
        Double term2Avg = calculateTermAverageWithAcademicYear(studentId, 2, academicYearStart, academicYearEnd);
        Double term3Avg = calculateTermAverageWithAcademicYear(studentId, 3, academicYearStart, academicYearEnd);
        Double yearlyAvg = calculateYearlyAverageWithAcademicYear(studentId, academicYearStart, academicYearEnd);

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
                .academicYear(academicYearStart != null && academicYearEnd != null ?
                        academicYearStart + "-" + academicYearEnd : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermAssessmentDTO> getTermAssessments(Long studentId, Integer term) {
        return getTermAssessmentsWithAcademicYear(studentId, term, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermAssessmentDTO> getTermAssessmentsWithAcademicYear(Long studentId, Integer term,
                                                                      Integer academicYearStart,
                                                                      Integer academicYearEnd) {
        List<StudentSubject> enrolledSubjects = studentSubjectRepository.findByStudentId(studentId);
        List<TermAssessmentDTO> termAssessments = new ArrayList<>();

        for (StudentSubject enrollment : enrolledSubjects) {
            Subject subject = enrollment.getSubject();

            List<Assessment> subjectAssessments;
            if (academicYearStart != null && academicYearEnd != null) {
                subjectAssessments = getAssessmentsByStudentSubjectTermAndAcademicYear(
                        studentId, subject.getId(), term, academicYearStart, academicYearEnd);
            } else {
                subjectAssessments = getAssessmentsByStudentSubjectAndTerm(studentId, subject.getId(), term);
            }

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
                    .academicYear(academicYearStart != null && academicYearEnd != null ?
                            academicYearStart + "-" + academicYearEnd : null)
                    .build();

            termAssessments.add(dto);
        }

        // Sort by subject name
        termAssessments.sort(Comparator.comparing(TermAssessmentDTO::getSubjectName));

        return termAssessments;
    }

    // ========== HELPER METHODS ==========

    private Assessment setAcademicYearIfMissing(Assessment assessment) {
        if (assessment == null) {
            return null;
        }

        // If academic year is already set, return as is
        if (assessment.getAcademicYearStart() != null && assessment.getAcademicYearEnd() != null) {
            if (assessment.getAcademicYear() == null) {
                assessment.setAcademicYear(
                        assessment.getAcademicYearStart() + "-" + assessment.getAcademicYearEnd()
                );
            }
            return assessment;
        }

        // Try to get academic year from student
        if (assessment.getStudent() != null && assessment.getStudent().getId() != null) {
            Student student = studentRepository.findById(assessment.getStudent().getId()).orElse(null);
            if (student != null && student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
                assessment.setAcademicYearStart(student.getAcademicYearStart());
                assessment.setAcademicYearEnd(student.getAcademicYearEnd());
                assessment.setAcademicYear(
                        student.getAcademicYearStart() + "-" + student.getAcademicYearEnd()
                );
                log.debug("Set academic year {}-{} for assessment from student {}",
                        student.getAcademicYearStart(), student.getAcademicYearEnd(),
                        student.getStudentId());
            } else if (student != null) {
                log.warn("Student {} has no academic year set, assessment will not have academic year",
                        student.getStudentId());
            }
        } else {
            log.warn("Assessment has no student reference, cannot set academic year");
        }

        return assessment;
    }

    // Renamed to avoid conflict with interface method
    private List<Assessment> getAssessmentsByStudentAndAcademicYearFromRepo(Long studentId,
                                                                            Integer academicYearStart,
                                                                            Integer academicYearEnd) {
        List<Assessment> allAssessments = assessmentRepository.findByStudentId(studentId);
        return allAssessments.stream()
                .filter(a -> a.getAcademicYearStart() != null && a.getAcademicYearEnd() != null &&
                        a.getAcademicYearStart().equals(academicYearStart) &&
                        a.getAcademicYearEnd().equals(academicYearEnd))
                .collect(Collectors.toList());
    }

    private List<Assessment> getAssessmentsByStudentSubjectTermAndAcademicYear(Long studentId, Long subjectId,
                                                                               Integer term,
                                                                               Integer academicYearStart,
                                                                               Integer academicYearEnd) {
        List<Assessment> allAssessments = assessmentRepository.findByStudentIdAndSubjectIdAndTerm(studentId, subjectId, term);
        return allAssessments.stream()
                .filter(a -> a.getAcademicYearStart() != null && a.getAcademicYearEnd() != null &&
                        a.getAcademicYearStart().equals(academicYearStart) &&
                        a.getAcademicYearEnd().equals(academicYearEnd))
                .collect(Collectors.toList());
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

        // Academic year validation - warn but don't fail if not set
        if (assessment.getAcademicYearStart() == null || assessment.getAcademicYearEnd() == null) {
            log.warn("Assessment for student {} subject {} term {} type {} has no academic year set",
                    assessment.getStudent().getId(), assessment.getSubject().getName(),
                    assessment.getTerm(), assessment.getType());
        } else if (assessment.getAcademicYearStart() >= assessment.getAcademicYearEnd()) {
            throw new BusinessRuleException("Academic year start must be before academic year end");
        }
    }

    // ========== NEW METHODS FOR ACADEMIC YEAR SUPPORT ==========

    @Override
    @Transactional
    public void fixAcademicYearsForStudent(Long studentId, Integer academicYearStart, Integer academicYearEnd) {
        List<Assessment> assessments = assessmentRepository.findByStudentId(studentId);
        int fixedCount = 0;

        for (Assessment assessment : assessments) {
            if (assessment.getAcademicYearStart() == null || assessment.getAcademicYearEnd() == null) {
                assessment.setAcademicYearStart(academicYearStart);
                assessment.setAcademicYearEnd(academicYearEnd);
                assessment.setAcademicYear(academicYearStart + "-" + academicYearEnd);
                assessmentRepository.save(assessment);
                fixedCount++;
                log.info("Fixed academic year for assessment {}: student {}, subject {}, term {}, type {}",
                        assessment.getId(), studentId, assessment.getSubject().getName(),
                        assessment.getTerm(), assessment.getType());
            }
        }

        log.info("Fixed academic years for {} assessments for student {}", fixedCount, studentId);
    }

    @Override
    @Transactional
    public void fixAllAcademicYears(Integer academicYearStart, Integer academicYearEnd) {
        List<Assessment> assessments = assessmentRepository.findAll();
        int fixedCount = 0;

        for (Assessment assessment : assessments) {
            if (assessment.getAcademicYearStart() == null || assessment.getAcademicYearEnd() == null) {
                assessment.setAcademicYearStart(academicYearStart);
                assessment.setAcademicYearEnd(academicYearEnd);
                assessment.setAcademicYear(academicYearStart + "-" + academicYearEnd);
                assessmentRepository.save(assessment);
                fixedCount++;
            }
        }

        log.info("Fixed academic years for {} assessments", fixedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<Assessment>> getAssessmentsByStudentGroupedByTerm(Long studentId) {
        List<Assessment> assessments = assessmentRepository.findByStudentId(studentId);
        return assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<Long, List<Assessment>>> getAssessmentsByStudentGroupedByTermAndSubject(Long studentId) {
        List<Assessment> assessments = assessmentRepository.findByStudentId(studentId);

        Map<Integer, Map<Long, List<Assessment>>> result = new HashMap<>();
        for (Assessment assessment : assessments) {
            result.computeIfAbsent(assessment.getTerm(), k -> new HashMap<>())
                    .computeIfAbsent(assessment.getSubject().getId(), k -> new ArrayList<>())
                    .add(assessment);
        }

        return result;
    }
}