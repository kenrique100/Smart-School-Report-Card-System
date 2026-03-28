package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.dto.SubjectReport;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.enums.AssessmentType;
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
                // Terms 1 & 2: weighted average (50% Assessment1, 50% Assessment2)
                if (a1 != null && a2 != null) {
                    return (safeA1 * 0.5) + (safeA2 * 0.5);
                } else if (a1 != null) {
                    return safeA1;
                } else if (a2 != null) {
                    return safeA2;
                } else {
                    return 0.0;
                }
            case 3:
                // Term 3: only Assessment 5 (Final Exam)
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
            case 1 -> {
                // Term 1: Assessment1 (50%) + Assessment2 (50%)
                Double a1 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_1);
                Double a2 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_2);
                yield calculateSubjectAverage(a1, a2, term);
            }
            case 2 -> {
                // Term 2: Assessment3 (50%) + Assessment4 (50%)
                Double a3 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_3);
                Double a4 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_4);
                yield calculateSubjectAverage(a3, a4, term);
            }
            case 3 -> {
                // Term 3: Assessment5 (100%) - Final Exam
                Double exam = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_5);
                yield calculateSubjectAverage(exam, null, term);
            }
            default -> {
                log.warn("Invalid term: {}", term);
                yield 0.0;
            }
        };
    }

    private Double extractAssessmentScore(List<Assessment> assessments, AssessmentType type) {
        return assessments.stream()
                .filter(a -> a.getType() == type)
                .findFirst()
                .map(Assessment::getScore)
                .orElse(null);
    }

    @Override
    public Double calculateYearlyAverage(Double term1Avg, Double term2Avg, Double term3Avg) {
        if (term1Avg == null || term2Avg == null || term3Avg == null) {
            return 0.0;
        }
        // Yearly average = (Term1 + Term2 + Term3) / 3
        return (term1Avg + term2Avg + term3Avg) / 3.0;
    }

    @Override
    public String calculateLetterGrade(Double scoreOutOf20, String className) {
        if (scoreOutOf20 == null || scoreOutOf20 < 0) {
            boolean isAdvancedLevel = isAdvancedLevelClass(className);
            return isAdvancedLevel ? "F" : "U";
        }

        boolean isAdvancedLevel = isAdvancedLevelClass(className);

        if (isAdvancedLevel) {
            return calculateAdvancedLevelGrade(scoreOutOf20);
        } else {
            return calculateOrdinaryLevelGrade(scoreOutOf20);
        }
    }

    private boolean isAdvancedLevelClass(String className) {
        if (className == null) return false;
        String lowerClassName = className.toLowerCase();
        return lowerClassName.contains("sixth") ||
                lowerClassName.contains("upper") ||
                lowerClassName.contains("lower") ||
                lowerClassName.contains("advanced") ||
                lowerClassName.contains("a level") ||
                lowerClassName.contains("as level") ||
                lowerClassName.contains("higher");
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
    public boolean isSubjectPassing(String letterGrade, String className) {
        if (letterGrade == null || className == null) return false;

        boolean isAdvancedLevel = isAdvancedLevelClass(className);

        if (isAdvancedLevel) {
            // Advanced level: A, B, C, D, E are passing grades
            return letterGrade.matches("[ABCDE]");
        } else {
            // Ordinary level: A, B, C are passing grades
            return letterGrade.matches("[ABC]");
        }
    }

    @Override
    public Double calculateWeightedTermAverage(List<SubjectReport> subjectReports) {
        if (subjectReports == null || subjectReports.isEmpty()) {
            return 0.0;
        }

        log.debug("Calculating weighted term average for {} subjects", subjectReports.size());

        double totalWeightedScore = 0.0;
        int totalCoefficient = 0;
        int validSubjects = 0;

        for (SubjectReport subject : subjectReports) {
            // Skip subjects without data
            if (!subject.hasData()) {
                log.debug("Skipping subject {} - no assessment data", subject.getSubjectName());
                continue;
            }

            Double subjectAverage = subject.getSubjectAverage();
            Integer coefficient = subject.getCoefficient();

            log.debug("Subject: {} - Average: {}, Coefficient: {}",
                    subject.getSubjectName(), subjectAverage, coefficient);

            if (subjectAverage != null) {
                // Use coefficient if available, otherwise default to 1
                int coeff = coefficient != null ? coefficient : 1;
                totalWeightedScore += subjectAverage * coeff;
                totalCoefficient += coeff;
                validSubjects++;

                log.debug("Added: {} * {} = {} (Total so far: {}, Coeff: {})",
                        subjectAverage, coeff, subjectAverage * coeff,
                        totalWeightedScore, totalCoefficient);
            } else {
                log.debug("Subject {} has null average, skipping", subject.getSubjectName());
            }
        }

        if (totalCoefficient == 0) {
            log.warn("Total coefficient is 0 for {} subjects ({} valid)",
                    subjectReports.size(), validSubjects);
            return 0.0;
        }

        double average = totalWeightedScore / totalCoefficient;
        log.debug("Final calculation: {} / {} = {}", totalWeightedScore, totalCoefficient, average);

        return average;
    }


    @Override
    public Double calculatePassRate(List<SubjectReport> subjectReports, String className) {
        if (subjectReports == null || subjectReports.isEmpty()) {
            return 0.0;
        }

        long totalSubjects = subjectReports.stream()
                .filter(SubjectReport::hasData)  // Only count subjects with data
                .count();

        if (totalSubjects == 0) {
            return 0.0;
        }

        long passedSubjects = subjectReports.stream()
                .filter(SubjectReport::hasData)  // Only consider subjects with data
                .filter(s -> s.getLetterGrade() != null && isSubjectPassing(s.getLetterGrade(), className))
                .count();

        return (passedSubjects * 100.0) / totalSubjects;
    }

    @Override
    public Long countPassedSubjects(List<SubjectReport> subjectReports, String className) {
        if (subjectReports == null || subjectReports.isEmpty()) {
            return 0L;
        }

        return subjectReports.stream()
                .filter(SubjectReport::hasData)  // Only consider subjects with data
                .filter(s -> s.getLetterGrade() != null && isSubjectPassing(s.getLetterGrade(), className))
                .count();
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