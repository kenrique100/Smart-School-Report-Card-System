package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Student;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
     * List of subject reports with detailed performance data
     */
    private List<SubjectReport> subjectReports;

    // Additional fields for enhanced reporting
    private String academicYear;
    private Integer totalStudentsInClass;
    private String classTeacher;

    /**
     * Get student's full name for templates
     */
    public String getStudentFullName() {
        return student != null ? student.getFirstName() + " " + student.getLastName() : "";
    }

    /**
     * Get formatted term average
     */
    public String getFormattedAverage() {
        return termAverage != null ? String.format("%.2f", termAverage) : "0.00";
    }
}