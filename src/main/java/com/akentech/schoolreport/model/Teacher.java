package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teacher")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", unique = true, nullable = false)
    private String teacherId; // Format: TC1000020000

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    private String email;

    private String address;

    private String contact;

    @Column(length = 1000)
    private String skills; // Comma-separated skills: "Science,Mathematics,History,Geography"

    // Many-to-Many relationship with Subject
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<Subject> subjects = new ArrayList<>();

    // Many-to-Many relationship with ClassRoom
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_classrooms",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "classroom_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<ClassRoom> classrooms = new ArrayList<>();
}