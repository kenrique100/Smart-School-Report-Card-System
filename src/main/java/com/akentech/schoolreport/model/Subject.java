package com.akentech.schoolreport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subject")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer coefficient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id")
    private ClassRoom classRoom;

    private String specialty;

    @Column(name = "subject_code", unique = true, nullable = false)
    private String subjectCode;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_optional")
    private Boolean optional = false;

    @PrePersist
    protected void onCreate() {
        if (coefficient == null) {
            coefficient = 1; // Default to 1 if not set
        }
    }

    // FIXED: Add getter for department name
    @Transient
    public String getDepartmentName() {
        return department != null ? department.getName() : "General";
    }

    // FIXED: Add getter for department code
    @Transient
    public String getDepartmentCode() {
        return department != null && department.getCode() != null ? department.getCode().name() : "GEN";
    }

    // ADD: Getter for classroom name
    @Transient
    public String getClassName() {
        return classRoom != null ? classRoom.getName() : "Not assigned";
    }

    // ADD: Getter for classroom code
    @Transient
    public String getClassCode() {
        return classRoom != null && classRoom.getCode() != null ? classRoom.getCode().name() : null;
    }

    // FIXED: Add helper method to check if subject is compulsory
    @Transient
    public boolean isCompulsory() {
        return !optional;
    }

    // FIXED: Add helper method to get full name with code
    @Transient
    public String getFullNameWithCode() {
        return this.name + " (" + this.subjectCode + ")";
    }

    // FIXED: Add helper method for display
    @Transient
    public String getDisplayName() {
        return this.name + " - " + this.subjectCode;
    }

    // NEW: Helper method to get subject type (optional/compulsory)
    @Transient
    public String getSubjectType() {
        return optional ? "Optional" : "Compulsory";
    }

    // NEW: Helper method to check if subject has a specialty
    @Transient
    public boolean hasSpecialty() {
        return specialty != null && !specialty.trim().isEmpty();
    }

    // NEW: Helper method for summary display
    @Transient
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(name);
        summary.append(" (Code: ").append(subjectCode).append(")");
        summary.append(" - Coeff: ").append(coefficient);

        if (hasSpecialty()) {
            summary.append(" - Specialty: ").append(specialty);
        }

        if (optional) {
            summary.append(" [Optional]");
        }

        return summary.toString();
    }
}