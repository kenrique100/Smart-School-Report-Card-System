package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"roll_number", "classroom_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", unique = true, nullable = false)
    private String studentId;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "roll_number", nullable = false)
    private String rollNumber;

    @NotNull(message = "Class is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "specialty")
    private String specialty;

    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    private String address;

    @Column(name = "academic_year_start")
    private Integer academicYearStart;

    @Column(name = "academic_year_end")
    private Integer academicYearEnd;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Assessment> assessments = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ExamResult> examResults = new ArrayList<>();

}