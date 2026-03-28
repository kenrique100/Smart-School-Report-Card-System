package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSummaryDTO {
    private String className;
    private String classTeacher;
    private int classSize;
    private double classAverage;
    private double passRate;
    private int totalPassed;
    private int totalFailed;
}