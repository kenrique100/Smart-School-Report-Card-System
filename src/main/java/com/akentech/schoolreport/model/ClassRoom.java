package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

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

    // optional link to department (for advanced level grouping)
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
}
