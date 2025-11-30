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

    @Column(nullable = false)
    private Integer coefficient;

    @ManyToOne(fetch = FetchType.LAZY)
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
}
