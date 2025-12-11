package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.dto.SubjectReport;
import com.akentech.schoolreport.service.GradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GradeServiceImpl implements GradeService {

    @Override
    public Double calculateSubjectAverage(Double assessment1, Double assessment2, Double exam, Integer term) {
        // Handle null values
        Double safeAssessment1 = getSafeScore(assessment1);
        Double safeAssessment2 = getSafeScore(assessment2);
        Double safeExam = getSafeScore(exam);

        // All inputs are already on 20 marks scale
        switch (term) {
            case 1, 2 -> {
                // Terms 1 & 2: (Assessment1 + Assessment2) / 2
                return (safeAssessment1 + safeAssessment2) / 2;
            }
            case 3 -> {
                // Term 3: Exam only
                return safeExam;
            }
            default -> {
                log.warn("Invalid term: {}", term);
                return 0.0;
            }
        }
    }

    @Override
    public String calculateLetterGrade(Double scoreOutOf20, String className) {
        if (scoreOutOf20 == null || scoreOutOf20 < 0) return "U";

        boolean isAdvancedLevel = className != null &&
                (className.contains("Sixth") || className.contains("Upper") || className.contains("Lower"));

        if (isAdvancedLevel) {
            return calculateAdvancedLevelGrade(scoreOutOf20);
        } else {
            return calculateOrdinaryLevelGrade(scoreOutOf20);
        }
    }

    private String calculateAdvancedLevelGrade(Double scoreOutOf20) {
        // A-Level grading (out of 20)
        if (scoreOutOf20 >= 18.0) return "A";
        if (scoreOutOf20 >= 16.0) return "B";
        if (scoreOutOf20 >= 14.0) return "C";
        if (scoreOutOf20 >= 12.0) return "D";
        if (scoreOutOf20 >= 10.0) return "E";
        if (scoreOutOf20 >= 8.0) return "O"; // Complimentary
        return "F";
    }

    private String calculateOrdinaryLevelGrade(Double scoreOutOf20) {
        // Ordinary Level (out of 20)
        if (scoreOutOf20 >= 18.0) return "A";
        if (scoreOutOf20 >= 15.0) return "B";
        if (scoreOutOf20 >= 10.0) return "C";
        if (scoreOutOf20 >= 5.0) return "D";
        return "U";
    }

    @Override
    public boolean isPassing(Double averageOutOf20) {
        // FIXED: Student passes if average ≥ 10/20
        return averageOutOf20 != null && averageOutOf20 >= 10.0;
    }

    @Override
    public Double calculateWeightedTermAverage(List<SubjectReport> subjectReports) {
        if (subjectReports == null || subjectReports.isEmpty()) {
            return 0.0;
        }

        double totalWeightedScore = 0.0;
        int totalCoefficient = 0;

        for (SubjectReport subject : subjectReports) {
            if (subject.getSubjectAverage() != null) {
                // All subject averages are on 20 marks scale
                totalWeightedScore += subject.getSubjectAverage() * subject.getCoefficient();
                totalCoefficient += subject.getCoefficient();
            }
        }

        return totalCoefficient > 0 ? totalWeightedScore / totalCoefficient : 0.0;
    }

    @Override
    public String generateRemarks(Double averageOutOf20) {
        if (averageOutOf20 == null) return "No assessment data";

        // All averages are on 20 marks scale
        if (averageOutOf20 >= 18.0) return "Excellent performance! Keep up the good work.";
        else if (averageOutOf20 >= 15.0) return "Very good performance. Continue with the good work.";
        else if (averageOutOf20 >= 10.0) return "Good performance. Room for improvement.";
        else if (averageOutOf20 >= 5.0) return "Satisfactory. Needs to work harder.";
        else return "Needs significant improvement. Please seek additional help.";
    }

    private Double getSafeScore(Double score) {
        return score != null ? score : 0.0;
    }
}