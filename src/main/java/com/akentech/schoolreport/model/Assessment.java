package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 1,2,3 -> term number
     */
    @Column(nullable = false)
    private Integer term;

    /**
     * Assessment type: "Assessment1", "Assessment2", "Assessment3", "Assessment4", "Exam"
     */
    @Column(nullable = false)
    private String type;

    /**
     * Score in the assessment. Convention: all scores out of 20.
     */
    @Column(nullable = false)
    private Double score;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

}