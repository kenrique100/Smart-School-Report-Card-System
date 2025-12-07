package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.akentech.schoolreport.dto.YearlyReportDTO.getString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private Object student; // Can be Student entity or StudentDTO
    private String studentFullName;
    private String rollNumber;
    private String className;
    private String department;
    private String specialty;
    private Integer term;
    private Double termAverage;
    private String formattedAverage;
    private Integer rankInClass;
    private Integer totalStudentsInClass;
    private String remarks;
    private List<SubjectReport> subjectReports;
    private String academicYear;
    private String classTeacher;

    /**
     * Get formatted term average
     */
    public String getFormattedAverage() {
        return termAverage != null ? String.format("%.2f", termAverage) : "0.00";
    }

    /**
     * Get student's full name from student object if needed
     */
    public String getStudentFullName() {
        return getString(studentFullName, student);
    }
}