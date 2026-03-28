package com.akentech.schoolreport.model;

import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "department")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(nullable = false, unique = true)
    @JsonProperty("name")
    private String name;

    // UPDATED: Use enum instead of String
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    @JsonProperty("code")
    private DepartmentCode code;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private List<Student> students = new ArrayList<>();
}