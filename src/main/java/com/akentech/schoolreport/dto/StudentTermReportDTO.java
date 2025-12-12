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
public class StudentTermReportDTO {
    private Object student; // You might want to create a StudentDTO instead
    private String studentFullName;
    private Integer term;
    private String academicYear;
    private Double termAverage;
    private String formattedAverage;
    private Integer rankInClass;
    private Integer totalStudentsInClass;
    private String remarks;
    private List<SubjectReport> subjectReports;
}