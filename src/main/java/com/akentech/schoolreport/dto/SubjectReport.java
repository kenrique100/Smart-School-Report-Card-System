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
    private Double exam;
    private Double subjectAverage;
    private String letterGrade;

    // Helper methods
    public Boolean getPassed() {
        if (letterGrade == null) return false;
        return letterGrade.matches("[ABC]");
    }
}