package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.PerformanceLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_subject")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    @JsonProperty("student")  // Explicitly allow serialization of student
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private Double score;

    // UPDATED: Use enum instead of String
    @Enumerated(EnumType.STRING)
    @Column(name = "performance")
    private PerformanceLevel performance;

    @Builder.Default
    private Boolean isCompulsory = false;

    // Add helper method to avoid circular reference issues
    @Transient
    public Long getStudentId() {
        return student != null ? student.getId() : null;
    }

    @Transient
    public String getStudentName() {
        return student != null ? student.getFullName() : null;
    }

    @Transient
    public String getSubjectName() {
        return subject != null ? subject.getName() : null;
    }
}