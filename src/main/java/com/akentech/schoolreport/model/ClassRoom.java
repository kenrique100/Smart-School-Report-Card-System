package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.ClassLevel;
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
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private ClassLevel code;

    @Column(name = "academic_year")
    private String academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "classRoom", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private List<Student> students = new ArrayList<>();

    // Helper method to get student count
    @Transient
    public int getStudentCount() {
        return students != null ? students.size() : 0;
    }

    // Helper method for display
    @Transient
    public String getDisplayInfo() {
        return name + " (" + code.getDisplayName() + ") - " + academicYear;
    }

    @Override
    public String toString() {
        return name;
    }
}