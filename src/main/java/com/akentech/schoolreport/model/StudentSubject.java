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
    @JsonProperty("student")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance")
    private PerformanceLevel performance;

    @Builder.Default
    @Column(name = "is_compulsory")
    private Boolean isCompulsory = true;

    // FIX: Add performanceString property with proper null handling
    @Transient
    public String getPerformanceString() {
        return performance != null ? performance.getDisplayName() : "Not Assessed";
    }

    // FIX: Add helper method for template compatibility
    @Transient
    public String getFormattedScore() {
        return score != null ? String.format("%.2f", score) : "N/A";
    }

    @Transient
    public Boolean getCompulsory() {
        return Boolean.TRUE.equals(this.isCompulsory);
    }

    @Transient
    public void setCompulsory(Boolean compulsory) {
        this.isCompulsory = compulsory;
    }

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