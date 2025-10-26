package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.DepartmentCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "department")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // UPDATED: Use enum instead of String
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private DepartmentCode code;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private List<Student> students = new ArrayList<>();
}