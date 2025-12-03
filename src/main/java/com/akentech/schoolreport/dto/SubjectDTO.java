package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Subject;
import lombok.Data;

@Data
public class SubjectDTO {

    private Long id;
    private String name;
    private String subjectCode;
    private Integer coefficient;
    private String departmentName;
    private String specialty;
    private Boolean optional;
    private String description;

    public static SubjectDTO fromEntity(Subject subject) {
        SubjectDTO dto = new SubjectDTO();

        dto.setId(subject.getId());
        dto.setName(subject.getName());
        dto.setSubjectCode(subject.getSubjectCode());
        dto.setCoefficient(subject.getCoefficient());
        dto.setDepartmentName(subject.getDepartment() != null
                ? subject.getDepartment().getName()
                : "General");
        dto.setSpecialty(subject.getSpecialty());
        dto.setOptional(subject.getOptional());
        dto.setDescription(subject.getDescription());

        return dto;
    }
}
