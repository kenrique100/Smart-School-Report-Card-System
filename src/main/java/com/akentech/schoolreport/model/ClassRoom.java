package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classroom")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., Form 1, Form 2, Lower Sixth

    @Column(nullable = false, unique = true, length = 10)
    private String code; // e.g., "F1", "F2", "L6"

    @Column(name = "academic_year")
    private String academicYear;

    // optional link to department (for advanced level grouping)
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "classRoom", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Student> students = new ArrayList<>();
}