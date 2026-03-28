package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.PerformanceLevel;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.repository.StudentSubjectRepository;
import com.akentech.schoolreport.service.GradeService;
import com.akentech.schoolreport.service.StudentPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPerformanceServiceImpl implements StudentPerformanceService {

    private final AssessmentRepository assessmentRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final GradeService gradeService;

    @Override
    @Transactional
    public void updateStudentSubjectScores(Long studentId, Integer term) {
        try {
            List<Assessment> assessments = assessmentRepository.findByStudentIdAndTerm(studentId, term);

            Map<Long, List<Assessment>> assessmentsBySubject = assessments.stream()
                    .collect(Collectors.groupingBy(a -> a.getSubject().getId()));

            for (Map.Entry<Long, List<Assessment>> entry : assessmentsBySubject.entrySet()) {
                Long subjectId = entry.getKey();
                List<Assessment> subjectAssessments = entry.getValue();

                Optional<StudentSubject> studentSubjectOpt =
                        studentSubjectRepository.findByStudentIdAndSubjectId(studentId, subjectId);

                if (studentSubjectOpt.isPresent()) {
                    StudentSubject studentSubject = studentSubjectOpt.get();

                    // Use the correct method from GradeService
                    Double subjectAverage = gradeService.calculateSubjectAverageForTerm(subjectAssessments, term);

                    SubjectPerformance performance = new SubjectPerformance();
                    performance.setScore(subjectAverage);
                    performance.setPerformance(determinePerformanceLevel(subjectAverage));

                    studentSubject.setScore(performance.getScore());
                    studentSubject.setPerformance(performance.getPerformance());
                    studentSubjectRepository.save(studentSubject);

                    log.info("Updated subject {} for student {}: Score={}, Performance={}",
                            subjectId, studentId, performance.getScore(), performance.getPerformance());
                }
            }

            log.info("Updated subject scores for student {} in term {}", studentId, term);

        } catch (Exception e) {
            log.error("Error updating student subject scores for student {} term {}", studentId, term, e);
            throw new RuntimeException("Failed to update student subject scores", e);
        }
    }

    @Override
    public SubjectPerformance calculateSubjectPerformance(List<Assessment> assessments, Integer term) {
        SubjectPerformance performance = new SubjectPerformance();

        // Use the correct method from GradeService
        Double subjectAverage = gradeService.calculateSubjectAverageForTerm(assessments, term);
        performance.setScore(subjectAverage);
        performance.setPerformance(determinePerformanceLevel(subjectAverage));

        return performance;
    }

    private PerformanceLevel determinePerformanceLevel(Double average) {
        if (average == null) return PerformanceLevel.FAIL;

        if (average >= 18.0) {
            return PerformanceLevel.EXCELLENT;
        } else if (average >= 16.0) {
            return PerformanceLevel.VERY_GOOD;
        } else if (average >= 14.0) {
            return PerformanceLevel.GOOD;
        } else if (average >= 12.0) {
            return PerformanceLevel.FAIR;
        } else if (average >= 10.0) {
            return PerformanceLevel.AVERAGE;
        } else {
            return PerformanceLevel.FAIL;
        }
    }

    @Override
    @Transactional
    public void updateAllStudentScores(Integer term) {
        List<Long> studentIds = assessmentRepository.findDistinctStudentIds();

        for (Long studentId : studentIds) {
            try {
                updateStudentSubjectScores(studentId, term);
            } catch (Exception e) {
                log.error("Failed to update scores for student {}", studentId, e);
            }
        }

        log.info("Updated scores for {} students in term {}", studentIds.size(), term);
    }
}