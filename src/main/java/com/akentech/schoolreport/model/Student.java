package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.Gender;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "student", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"roll_number", "classroom_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @ManyToOne(fetch = FetchType.EAGER) // CHANGED: EAGER fetch to prevent LazyInitializationException
    @JoinColumn(name = "classroom_id", nullable = false)
    @JsonProperty("classRoom")
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.EAGER) // CHANGED: EAGER fetch to prevent LazyInitializationException
    @JoinColumn(name = "department_id")
    @JsonProperty("department")
    private Department department;

    @Column(name = "specialty")
    private String specialty;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

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

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @JsonManagedReference("student-subjects")
    private List<StudentSubject> studentSubjects = new ArrayList<>();

    // NEW: Helper method for gender display
    @Transient
    public String getGenderDisplay() {
        if (this.gender == null) {
            return "Not Set";
        }
        return this.gender == Gender.MALE ? "Male" : "Female";
    }

    // NEW: Helper method for gender CSS classes
    @Transient
    public String getGenderCssClass() {
        if (this.gender == null) {
            return "bg-gray-100 text-gray-800";
        }
        return this.gender == Gender.MALE ? "bg-blue-100 text-blue-800" : "bg-pink-100 text-pink-800";
    }

    // NEW: Helper method to get department CSS class
    @Transient
    public String getDepartmentCssClass() {
        if (this.department == null || this.department.getCode() == null) {
            return "bg-gray-100 text-gray-800";
        }

        return switch (this.department.getCode()) {
            case SCI -> "bg-green-100 text-green-800";
            case ART -> "bg-yellow-100 text-yellow-800";
            case COM -> "bg-blue-100 text-blue-800";
            case TEC -> "bg-purple-100 text-purple-800";
            case HE -> "bg-pink-100 text-pink-800";
            default -> "bg-gray-100 text-gray-800";
        };
    }

    // NEW: Helper method to get department name safely
    @Transient
    public String getDepartmentName() {
        return this.department != null ? this.department.getName() : "General";
    }

    // NEW: Helper method to get classroom name safely
    @Transient
    public String getClassroomName() {
        return this.classRoom != null ? this.classRoom.getName() : "Not assigned";
    }

    // Helper method for subject management
    public void addSubject(Subject subject, boolean isCompulsory) {
        StudentSubject enrollment = StudentSubject.builder()
                .student(this)
                .subject(subject)
                .isCompulsory(isCompulsory)
                .build();
        this.studentSubjects.add(enrollment);
    }

    public void removeSubject(Subject subject) {
        this.studentSubjects.removeIf(enrollment -> enrollment.getSubject().equals(subject));
    }

    @Transient
    public List<Subject> getSubjects() {
        return this.studentSubjects.stream()
                .map(StudentSubject::getSubject)
                .collect(Collectors.toList());
    }

    @Transient
    public List<Subject> getAvailableSubjects() {
        // This will be populated by the service layer
        return new ArrayList<>();
    }

    @Transient
    public List<Long> getSelectedSubjectIds() {
        if (this.studentSubjects == null) return new ArrayList<>();
        return this.studentSubjects.stream()
                .map(ss -> ss.getSubject().getId())
                .collect(Collectors.toList());
    }

    public void updateSubjects(List<Subject> subjects) {
        if (this.studentSubjects == null) {
            this.studentSubjects = new ArrayList<>();
        } else {
            this.studentSubjects.clear();
        }

        if (subjects != null) {
            subjects.forEach(subject -> {
                boolean isCompulsory = isCompulsorySubject(subject);
                StudentSubject studentSubject = StudentSubject.builder()
                        .student(this)
                        .subject(subject)
                        .isCompulsory(isCompulsory)
                        .build();
                this.studentSubjects.add(studentSubject);
            });
        }
    }

    private boolean isCompulsorySubject(Subject subject) {
        // Define compulsory subjects for Forms 1-5
        if (this.classRoom != null && this.classRoom.getCode() != null) {
            ClassLevel classLevel = this.classRoom.getCode();
            if (classLevel.isFormLevel()) {
                List<String> compulsoryNames = Arrays.asList("Mathematics", "English Language", "French Language");
                return compulsoryNames.contains(subject.getName());
            }
        }
        return false;
    }

    @Transient
    public ClassLevel getClassLevel() {
        return this.classRoom != null ? this.classRoom.getCode() : null;
    }

    @Transient
    public boolean isSubjectForLevel(Subject subject) {
        if (subject == null || classRoom == null) return false;

        boolean isAdvancedSubject = subject.getName().startsWith("A-");
        boolean isOrdinarySubject = subject.getName().startsWith("O-") || !isAdvancedSubject;

        ClassLevel level = getClassLevel();
        if (level == null) return false;

        return (level.isFormLevel() && isOrdinarySubject) ||
                (level.isSixthForm() && isAdvancedSubject);
    }

    // Helper method to check if student is in form level
    @Transient
    public boolean isFormLevel() {
        return this.classRoom != null && this.classRoom.getCode() != null && this.classRoom.getCode().isFormLevel();
    }

    // Helper method to check if student is in sixth form
    @Transient
    public boolean isSixthForm() {
        return this.classRoom != null && this.classRoom.getCode() != null &&
                (this.classRoom.getCode() == ClassLevel.LOWER_SIXTH || this.classRoom.getCode() == ClassLevel.UPPER_SIXTH);
    }

    // Helper method to get full name
    @Transient
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    // Helper method to get academic year
    @Transient
    public String getAcademicYear() {
        if (this.academicYearStart != null && this.academicYearEnd != null) {
            return this.academicYearStart + "-" + this.academicYearEnd;
        }
        return "N/A";
    }

    // NEW: Helper method to get age
    @Transient
    public Integer getAge() {
        if (this.dateOfBirth == null) return null;
        return Period.between(this.dateOfBirth, LocalDate.now()).getYears();
    }

    // NEW: Helper method to get formatted date of birth
    @Transient
    public String getFormattedDateOfBirth() {
        return this.dateOfBirth != null ? this.dateOfBirth.toString() : "N/A";
    }
}