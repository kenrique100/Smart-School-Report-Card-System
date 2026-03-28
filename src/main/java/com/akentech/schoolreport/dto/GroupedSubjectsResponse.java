package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GroupedSubjectsResponse {
    private boolean success;
    private String message;
    private Map<String, List<SubjectDTO>> groupedSubjects;
    private int compulsoryCount;
    private int departmentCount;
    private int specialtyCount;
    private int optionalCount;
}