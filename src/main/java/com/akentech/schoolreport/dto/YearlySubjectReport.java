package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlySubjectReport {
    private String subjectName;
    private Integer coefficient;
    private Double term1Average;
    private Double term2Average;
    private Double term3Average;
    private Double yearlyAverage;
    private String yearlyGrade;
    private Boolean passed;
}