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
public class ClassTermReportDTO {
    private Object classRoom;
    private Integer term;
    private List<StudentTermReportDTO> reports;
    private Double classAverage;
}