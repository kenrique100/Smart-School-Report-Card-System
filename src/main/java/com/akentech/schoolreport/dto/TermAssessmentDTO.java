package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermAssessmentDTO {
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private Integer coefficient;
    private Double assessment1Score;
    private Double assessment2Score;
    private Double assessment3Score;
    private Double assessment4Score;
    private Double assessment5Score;
    private Double termAverage;
    private String termGrade;
    private Boolean completed;
}