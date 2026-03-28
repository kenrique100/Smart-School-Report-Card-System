package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SubjectDTO {
    private Long id;
    private String name;
    private String subjectCode;
    private Integer coefficient;
    private Boolean optional;
    private String specialty;
    private String description;
    private Long departmentId;
    private String departmentName;

    public static SubjectDTO fromEntity(com.akentech.schoolreport.model.Subject subject) {
        return SubjectDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .subjectCode(subject.getSubjectCode())
                .coefficient(subject.getCoefficient())
                .optional(subject.getOptional())
                .specialty(subject.getSpecialty())
                .description(subject.getDescription())
                .departmentId(subject.getDepartment() != null ? subject.getDepartment().getId() : null)
                .departmentName(subject.getDepartment() != null ? subject.getDepartment().getName() : null)
                .build();
    }
}