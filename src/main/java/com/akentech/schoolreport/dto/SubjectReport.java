package com.akentech.schoolreport.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectReport {
    private Long subjectId;
    private String subjectName;
    private Integer coefficient;
    private Double assessment1; // null if absent
    private Double assessment2; // null if absent
    private Double exam;        // null if absent (for term 3 or later)
    private Double subjectAverage; // average for the term (over 20)
    private String letterGrade;
}
