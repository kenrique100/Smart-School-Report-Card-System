package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
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
import java.time.format.DateTimeFormatter;
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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", nullable = false)
    @JsonProperty("classRoom")
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.EAGER)
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
    @Column(unique = true, nullable = true)
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

    @PrePersist
    @PreUpdate
    public void sanitizeFields() {
        if (this.email != null && this.email.trim().isEmpty()) {
            this.email = null;
        }

        if (this.rollNumber != null) {
            this.rollNumber = this.rollNumber.trim();
        }

        if (this.firstName != null) {
            this.firstName = this.firstName.trim();
        }
        if (this.lastName != null) {
            this.lastName = this.lastName.trim();
        }
    }

    @Transient
    public String getFormattedDateOfBirth() {
        if (this.dateOfBirth == null) {
            return "Not Set";
        }
        return this.dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Transient
    public String getDateOfBirthISO() {
        if (this.dateOfBirth == null) {
            return "";
        }
        return this.dateOfBirth.toString();
    }

    @Transient
    public String getSanitizedEmail() {
        if (this.email == null || this.email.trim().isEmpty()) {
            return null;
        }
        return this.email.trim();
    }

    @Transient
    public boolean hasValidEmail() {
        return this.email != null && !this.email.trim().isEmpty();
    }

    @Transient
    public String getGenderDisplay() {
        if (this.gender == null) {
            return "Not Set";
        }
        return this.gender == Gender.MALE ? "Male" : "Female";
    }

    @Transient
    public String getGenderCssClass() {
        if (this.gender == null) {
            return "bg-gray-100 text-gray-800";
        }
        return this.gender == Gender.MALE ? "bg-blue-100 text-blue-800" : "bg-pink-100 text-pink-800";
    }

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

    @Transient
    public String getDepartmentName() {
        return this.department != null ? this.department.getName() : "General";
    }

    @Transient
    public String getClassroomName() {
        return this.classRoom != null ? this.classRoom.getName() : "Not assigned";
    }

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
        if (this.classRoom != null && this.classRoom.getCode() != null) {
            ClassLevel classLevel = this.classRoom.getCode();
            if (classLevel.isFormLevel()) {
                List<String> compulsoryNames = Arrays.asList("O-Mathematics", "O-English Language", "O-French Language");
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
        if (subject == null || classRoom == null || classRoom.getCode() == null) return false;

        ClassLevel level = getClassLevel();
        String subjectName = subject.getName();

        if (subjectName == null) return false;

        boolean isAdvancedSubject = subjectName.startsWith("A-");
        boolean isOrdinarySubject = subjectName.startsWith("O-");

        if (level.isSixthForm()) {
            return isAdvancedSubject;
        } else {
            return isOrdinarySubject ||
                    (!isAdvancedSubject &&
                            subject.getDepartment() != null &&
                            subject.getDepartment().getCode() == DepartmentCode.GEN);
        }
    }

    @Transient
    public boolean isFormLevel() {
        return this.classRoom != null && this.classRoom.getCode() != null && this.classRoom.getCode().isFormLevel();
    }

    @Transient
    public boolean isSixthForm() {
        return this.classRoom != null && this.classRoom.getCode() != null &&
                (this.classRoom.getCode() == ClassLevel.LOWER_SIXTH || this.classRoom.getCode() == ClassLevel.UPPER_SIXTH);
    }

    @Transient
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    @Transient
    public String getAcademicYear() {
        if (this.academicYearStart != null && this.academicYearEnd != null) {
            return this.academicYearStart + "-" + this.academicYearEnd;
        }
        return "Not Set";
    }

    @Transient
    public Integer getAge() {
        if (this.dateOfBirth == null) {
            return null;
        }
        return java.time.Period.between(this.dateOfBirth, LocalDate.now()).getYears();
    }

    @Transient
    public String getDisplayEmail() {
        if (this.email == null || this.email.trim().isEmpty()) {
            return "Not Set";
        }
        return this.email;
    }

    @Transient
    public String getFormattedAcademicYear() {
        if (this.academicYearStart != null && this.academicYearEnd != null) {
            return this.academicYearStart + "/" + this.academicYearEnd;
        }
        return "Not Set";
    }

    @Transient
    public String getAgeWithLabel() {
        Integer age = getAge();
        if (age == null) {
            return "Age: Not Set";
        }
        return "Age: " + age + " years";
    }

    public void setEmail(String email) {
        if (email != null && email.trim().isEmpty()) {
            this.email = null;
        } else {
            this.email = email;
        }
    }

}