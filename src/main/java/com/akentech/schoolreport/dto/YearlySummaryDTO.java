package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlySummaryDTO {
    private String academicYear;
    private int totalClasses;
    private int totalStudents;
    private int totalPassed;
    private int totalFailed;
    private double overallAverage;
    private double overallPassRate;
    private List<ClassSummaryDTO> classSummaries;
}