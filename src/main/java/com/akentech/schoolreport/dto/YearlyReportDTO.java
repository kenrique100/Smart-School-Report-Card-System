package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyReportDTO {
    private Object student;
    private String studentFullName;
    private String rollNumber;
    private String className;
    private String department;
    private String specialty;
    private Integer academicYear;
    private Double yearlyAverage;
    private String formattedYearlyAverage;
    private Double passRate;
    private String formattedPassRate;
    private Integer yearlyRank;
    private Integer term1Rank;
    private Integer term2Rank;
    private Integer term3Rank;
    private String remarks;
    private Boolean passed;
    private String overallGrade;
    private Integer totalStudentsInClass;
    private Integer totalPassed;
    private Integer totalFailed;
    private Integer subjectsPassed;
    private Integer totalSubjects;
    private List<YearlySubjectReport> subjectReports;
    private List<TermReportSummary> termSummaries;

    public String getStudentFullName() {
        return getString(studentFullName, student);
    }

    static String getString(String studentFullName, Object student) {
        if (studentFullName != null && !studentFullName.isEmpty()) {
            return studentFullName;
        }
        if (student != null) {
            try {
                // Try to extract from Student object
                java.lang.reflect.Method getFirstName = student.getClass().getMethod("getFirstName");
                java.lang.reflect.Method getLastName = student.getClass().getMethod("getLastName");
                Object firstName = getFirstName.invoke(student);
                Object lastName = getLastName.invoke(student);
                return firstName + " " + lastName;
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public String getFormattedYearlyAverage() {
        return yearlyAverage != null ? String.format("%.2f", yearlyAverage) : "0.00";
    }

    public String getFormattedPassRate() {
        return passRate != null ? String.format("%.1f%%", passRate) : "0.0%";
    }
}