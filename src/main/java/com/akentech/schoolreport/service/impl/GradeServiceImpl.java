package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.dto.SubjectReport;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.service.GradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GradeServiceImpl implements GradeService {

    @Override
    public Double calculateSubjectAverage(Double a1, Double a2, Integer term) {
        // Handle null values
        double safeA1 = a1 != null ? a1 : 0.0;
        double safeA2 = a2 != null ? a2 : 0.0;

        switch (term) {
            case 1, 2:
                // Terms 1 & 2: average of 2 assessments
                int count = 0;
                double sum = 0.0;

                if (a1 != null) {
                    sum += safeA1;
                    count++;
                }
                if (a2 != null) {
                    sum += safeA2;
                    count++;
                }

                return count > 0 ? sum / count : 0.0;
            case 3:
                // Term 3: only Assessment 5
                return safeA1;
            default:
                throw new IllegalArgumentException("Invalid term: " + term);
        }
    }

    @Override
    public Double calculateSubjectAverageForTerm(List<Assessment> assessments, Integer term) {
        if (assessments == null || assessments.isEmpty()) {
            return 0.0;
        }

        return switch (term) {
            case 1, 2 -> {
                // For terms 1 & 2, average all assessments
                double sum = assessments.stream()
                        .mapToDouble(Assessment::getScore)
                        .sum();
                yield sum / assessments.size();
            }
            case 3 ->
                // For term 3, just return the first assessment score
                    assessments.stream()
                            .findFirst()
                            .map(Assessment::getScore)
                            .orElse(0.0);
            default -> {
                log.warn("Invalid term: {}", term);
                yield 0.0;
            }
        };
    }

    @Override
    public Double calculateYearlyAverage(Double term1Avg, Double term2Avg, Double term3Avg) {
        if (term1Avg == null || term2Avg == null || term3Avg == null) {
            return 0.0;
        }
        return (term1Avg + term2Avg + term3Avg) / 3.0;
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
        if (scoreOutOf20 >= 18.0) return "A";
        if (scoreOutOf20 >= 16.0) return "B";
        if (scoreOutOf20 >= 14.0) return "C";
        if (scoreOutOf20 >= 12.0) return "D";
        if (scoreOutOf20 >= 10.0) return "E";
        if (scoreOutOf20 >= 8.0) return "O";
        return "F";
    }

    private String calculateOrdinaryLevelGrade(Double scoreOutOf20) {
        if (scoreOutOf20 >= 18.0) return "A";
        if (scoreOutOf20 >= 15.0) return "B";
        if (scoreOutOf20 >= 10.0) return "C";
        if (scoreOutOf20 >= 5.0) return "D";
        return "U";
    }

    @Override
    public boolean isPassing(Double averageOutOf20) {
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
                totalWeightedScore += subject.getSubjectAverage() * subject.getCoefficient();
                totalCoefficient += subject.getCoefficient();
            }
        }

        return totalCoefficient > 0 ? totalWeightedScore / totalCoefficient : 0.0;
    }

    @Override
    public String generateRemarks(Double averageOutOf20) {
        if (averageOutOf20 == null) return "No assessment data";

        if (averageOutOf20 >= 18.0) return "Excellent performance! Keep up the good work.";
        else if (averageOutOf20 >= 15.0) return "Very good performance. Continue with the good work.";
        else if (averageOutOf20 >= 10.0) return "Good performance. Room for improvement.";
        else if (averageOutOf20 >= 5.0) return "Satisfactory. Needs to work harder.";
        else return "Needs significant improvement. Please seek additional help.";
    }

    @Override
    public String getPerformanceStatus(Double averageOutOf20) {
        if (averageOutOf20 == null) return "No Data";

        if (averageOutOf20 >= 18.0) return "Excellent";
        else if (averageOutOf20 >= 15.0) return "Very Good";
        else if (averageOutOf20 >= 10.0) return "Good";
        else if (averageOutOf20 >= 5.0) return "Satisfactory";
        else return "Needs Improvement";
    }
}