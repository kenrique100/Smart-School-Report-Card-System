package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Subject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDTO {
    private Long id;
    private String name;
    private String subjectCode;
    private Integer coefficient;
    private Boolean optional;
    private String specialty;
    private String description;
    private Long departmentId;
    private String departmentName;

    public static SubjectDTO fromEntity(Subject subject) {
        if (subject == null) {
            return null;
        }

        SubjectDTO dto = new SubjectDTO();
        dto.setId(subject.getId());
        dto.setName(subject.getName());
        dto.setSubjectCode(subject.getSubjectCode());
        dto.setCoefficient(subject.getCoefficient());
        dto.setOptional(subject.getOptional());
        dto.setSpecialty(subject.getSpecialty());
        dto.setDescription(subject.getDescription());

        if (subject.getDepartment() != null) {
            dto.setDepartmentId(subject.getDepartment().getId());
            dto.setDepartmentName(subject.getDepartment().getName());
        }

        return dto;
    }
}