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

    private String specialty;

    @Column(name = "subject_code", unique = true, nullable = false)
    private String subjectCode;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_optional")
    private Boolean optional = false;

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
}