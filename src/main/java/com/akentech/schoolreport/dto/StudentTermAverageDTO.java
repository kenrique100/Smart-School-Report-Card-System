package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Student;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentTermAverageDTO {
    private Student student;
    private Double average;
    private Integer rank;
}