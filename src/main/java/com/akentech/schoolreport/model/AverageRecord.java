package com.akentech.schoolreport.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "average_record", indexes = {
        @Index(name = "idx_avg_student_term", columnList = "student_id, term")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AverageRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer term;

    private Double average; // average over 20 for the term (weighted)

    private Integer rankInClass;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}