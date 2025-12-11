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
            // Get all assessments for the student in the term
            List<Assessment> assessments = assessmentRepository.findByStudentIdAndTerm(studentId, term);

            // Group assessments by subject
            Map<Long, List<Assessment>> assessmentsBySubject = assessments.stream()
                    .collect(Collectors.groupingBy(a -> a.getSubject().getId()));

            // Update each subject's performance
            for (Map.Entry<Long, List<Assessment>> entry : assessmentsBySubject.entrySet()) {
                Long subjectId = entry.getKey();
                List<Assessment> subjectAssessments = entry.getValue();

                // Find student subject record
                Optional<StudentSubject> studentSubjectOpt = studentSubjectRepository.findByStudentIdAndSubjectId(studentId, subjectId);

                if (studentSubjectOpt.isPresent()) {
                    StudentSubject studentSubject = studentSubjectOpt.get();

                    // Calculate subject performance
                    SubjectPerformance performance = calculateSubjectPerformance(subjectAssessments, term);

                    // Update student subject record
                    studentSubject.setScore(performance.getScore());
                    studentSubject.setPerformance(performance.getPerformance());

                    studentSubjectRepository.save(studentSubject);

                    log.info("Updated subject {} for student {}: Score={}, Performance={}",
                            subjectId, studentId, performance.getScore(),
                            performance.getPerformance());
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

        // Separate assessments by type
        Double assessment1 = null;
        Double assessment2 = null;
        Double exam = null;

        for (Assessment assessment : assessments) {
            switch (assessment.getType()) {
                case "Assessment1" -> assessment1 = assessment.getScore();
                case "Assessment2" -> assessment2 = assessment.getScore();
                case "Exam" -> exam = assessment.getScore();
            }
        }

        // Calculate subject average (on 20 marks scale)
        Double subjectAverage = gradeService.calculateSubjectAverage(assessment1, assessment2, exam, term);
        performance.setScore(subjectAverage);

        // Determine performance level based on score (out of 20)
        if (subjectAverage >= 18.0) {
            performance.setPerformance(PerformanceLevel.EXCELLENT);
        } else if (subjectAverage >= 16.0) {
            performance.setPerformance(PerformanceLevel.VERY_GOOD);
        } else if (subjectAverage >= 14.0) {
            performance.setPerformance(PerformanceLevel.GOOD);
        } else if (subjectAverage >= 12.0) {
            performance.setPerformance(PerformanceLevel.FAIR);
        } else if (subjectAverage >= 10.0) {
            performance.setPerformance(PerformanceLevel.AVERAGE);
        } else {
            performance.setPerformance(PerformanceLevel.FAIL);
        }

        return performance;
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