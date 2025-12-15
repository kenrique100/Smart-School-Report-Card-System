package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.AssessmentType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment",
        indexes = {
                @Index(name = "idx_student_term", columnList = "student_id, term"),
                @Index(name = "idx_subject_term", columnList = "subject_id, term")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer term;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssessmentType type; // Changed to enum

    @Column(nullable = false)
    private Double score; // Score out of 20

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonBackReference("student-assessments")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "academic_year_start")
    private Integer academicYearStart;

    @Column(name = "academic_year_end")
    private Integer academicYearEnd;

    // Or use a single string field:
    @Column(name = "academic_year")
    private String academicYear;

    // Helper method to get assessment number
    public Integer getAssessmentNumber() {
        return type != null ? type.getAssessmentNumber() : null;
    }

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}