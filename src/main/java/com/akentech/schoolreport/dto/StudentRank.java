package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Student;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentRank {
    private Student student;
    private Double average;
    private Integer rank;
    private String remarks;
}
