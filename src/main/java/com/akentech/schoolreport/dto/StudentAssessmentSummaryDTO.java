package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssessmentSummaryDTO {
    private Long studentId;
    private String studentName;
    private Integer totalAssessments;
    private Integer term1Assessments;
    private Integer term2Assessments;
    private Integer term3Assessments;
    private Double term1Average;
    private Double term2Average;
    private Double term3Average;
    private Double yearlyAverage;
    private Integer termRank;
    private Integer classRank;
    private Integer totalSubjects;
    private Integer term1CompletedSubjects;
    private Integer term2CompletedSubjects;
    private Integer term3CompletedSubjects;
    private Boolean term1Completed;
    private Boolean term2Completed;
    private Boolean term3Completed;
    private String academicYear;
}