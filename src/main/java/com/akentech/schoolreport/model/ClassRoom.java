package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.ClassLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(nullable = false)
    @JsonProperty("name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    @JsonProperty("code")
    private ClassLevel code;

    @Column(name = "academic_year")
    @JsonProperty("academicYear")
    private String academicYear;

    @Column(name = "class_teacher")
    private String classTeacher; // ADD THIS FIELD

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

    // ADD THIS METHOD
    public String getClassTeacher() {
        return classTeacher != null ? classTeacher : "Not Assigned";
    }

    @Override
    public String toString() {
        return name;
    }
}