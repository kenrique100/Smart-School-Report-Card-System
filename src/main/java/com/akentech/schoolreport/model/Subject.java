package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subject")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Coefficient / weight for subject when computing weighted average
     */
    @Column(nullable = false)
    private Integer coefficient = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "specialty")
    private String specialty; // Can be null for general subjects

    @Column(name = "subject_code", unique = true)
    private String subjectCode; // Auto-generated code

    @Column(name = "description", length = 500)
    private String description;
}