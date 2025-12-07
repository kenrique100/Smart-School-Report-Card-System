package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermReportSummary {
    private Integer term;
    private Double termAverage;
    private String formattedAverage;
    private Integer rankInClass;
    private String remarks;
    private Boolean passed;
}