package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Student;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Report data returned to controller/Thymeleaf for rendering per-term report.
 */
@Data
@Builder
public class ReportDTO {
    private Student student;
    private Integer term;
    private Double termAverage;
    private Integer rankInClass;
    private String remarks;
    /**
     * Subject -> SubjectReport (map)
     */
    private List<SubjectReport> subjectReports;
}
