package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.PerformanceLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_subject")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
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
}