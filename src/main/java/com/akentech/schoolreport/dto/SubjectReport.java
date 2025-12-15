package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectReport {
    private String subjectName;
    private Integer coefficient;
    private Double assessment1;
    private Double assessment2;
    private Double subjectAverage;
    private String letterGrade;
    private String className;
    private Boolean hasData;

    // Fixed: Add proper getPassed() method that uses className
    public Boolean getPassed() {
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

    // Get proper assessment names based on term
    public Double getAssessmentForTerm(Integer term, int assessmentNumber) {
        if (term == 1) {
            return assessmentNumber == 1 ? assessment1 : assessment2;
        } else if (term == 2) {
            return assessmentNumber == 1 ? assessment1 : assessment2; // Actually assessment3 and assessment4
        } else if (term == 3) {
            return assessmentNumber == 1 ? assessment1 : null; // Only exam in term 3
        }
        return null;
    }

    // Helper method to get total score based on term
    public Double getTotalScore(Integer term) {
        if (term == 3) {
            return assessment1; // Exam score for term 3
        } else {
            if (assessment1 == null && assessment2 == null) return null;
            double a1 = assessment1 != null ? assessment1 * 0.5 : 0;
            double a2 = assessment2 != null ? assessment2 * 0.5 : 0;
            return a1 + a2;
        }
    }

    public boolean hasData() {
        return hasData != null && hasData;
    }
}