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

    @ManyToOne(fetch = FetchType.EAGER) // FIXED: Changed from LAZY to EAGER
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

    // NEW: Helper method to get department name safely
    @Transient
    public String getDepartmentName() {
        return this.department != null ? this.department.getName() : "General";
    }

    // NEW: Helper method to check if subject is advanced level
    @Transient
    public boolean isAdvancedLevel() {
        return this.name != null && this.name.startsWith("A-");
    }

    // NEW: Helper method to check if subject is ordinary level
    @Transient
    public boolean isOrdinaryLevel() {
        return this.name != null && (this.name.startsWith("O-") || !this.isAdvancedLevel());
    }
}