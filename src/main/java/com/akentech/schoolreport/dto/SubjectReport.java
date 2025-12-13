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

    // Helper methods
    public Boolean getPassed() {
        if (letterGrade == null) return false;
        return letterGrade.matches("[ABC]");
    }

    // Add getter for assessment3 if needed (you can reuse assessment1 with different context)
    public Double getAssessment3() {
        return assessment1; // For term 2, assessment1 represents assessment3
    }

    public Double getAssessment4() {
        return assessment2; // For term 2, assessment2 represents assessment4
    }

    // For exam in term 3
    public Double getExam() {
        return assessment1; // For term 3, assessment1 represents exam
    }
}