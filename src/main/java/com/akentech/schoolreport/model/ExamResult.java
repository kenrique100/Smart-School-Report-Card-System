package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private Double marks;

    private String grade;

    @Column(name = "term")
    private Integer term;
}