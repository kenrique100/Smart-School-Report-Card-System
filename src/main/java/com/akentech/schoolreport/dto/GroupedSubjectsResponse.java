package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupedSubjectsResponse {
    private boolean success;
    private Map<String, List<SubjectDTO>> groupedSubjects;
    private int compulsoryCount;
    private int departmentCount;
    private int specialtyCount;
    private int optionalCount;
    private String message;
}