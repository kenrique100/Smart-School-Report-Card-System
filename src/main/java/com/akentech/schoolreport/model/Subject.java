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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Coefficient / weight for subject when computing weighted average
     */
    @Column(nullable = false)
    private Integer coefficient = 1;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
}
