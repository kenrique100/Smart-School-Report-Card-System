package com.akentech.schoolreport.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStatistics {
    private long totalStudents;
    private long totalTeachers;
    private long totalSubjects;
    private long totalNotices;
    private long totalClasses;
    private long totalDepartments;
    private long totalSpecialties;


    // Distribution maps
    private Map<String, Long> studentsByDepartment;
    private Map<String, Long> studentsBySpecialty;
    private Map<String, Long> studentsByClass;
    private Map<String, Long> genderDistribution;
}