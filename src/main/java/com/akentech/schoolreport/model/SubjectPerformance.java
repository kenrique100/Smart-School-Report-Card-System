package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.PerformanceLevel;
import lombok.Data;

@Data
public class SubjectPerformance {
    private Double score;
    private PerformanceLevel performance;
}