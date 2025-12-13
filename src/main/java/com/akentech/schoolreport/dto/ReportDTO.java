package com.akentech.schoolreport.dto;

import com.akentech.schoolreport.model.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private Student student;
    private String studentFullName;
    private String rollNumber;
    private String className;
    private String department;
    private String specialty;
    private Integer term;
    private Double termAverage;
    private String formattedAverage;
    private Integer rankInClass;
    private Integer totalStudentsInClass;
    private String remarks;
    private List<SubjectReport> subjectReports;
    private String academicYear;
    private String action;
    private String classTeacher;

    // Added fields for pass rate and subjects passed
    private Double passRate;
    private Integer subjectsPassed;
    private Integer totalSubjects;

    /**
     * Get formatted term average
     */
    public String getFormattedAverage() {
        if (formattedAverage != null && !formattedAverage.isEmpty()) {
            return formattedAverage;
        }
        return termAverage != null ? String.format("%.2f/20", termAverage) : "0.00/20";
    }

    /**
     * Get student's full name from student object if needed
     */
    public String getStudentFullName() {
        if (studentFullName != null && !studentFullName.isEmpty()) {
            return studentFullName;
        }
        if (student != null) {
            return student.getFirstName() + " " + student.getLastName();
        }
        return "Unknown Student";
    }

    /**
     * Get academic year
     */
    public String getAcademicYear() {
        if (academicYear != null && !academicYear.isEmpty()) {
            return academicYear;
        }
        if (student != null && student.getClassRoom() != null) {
            return student.getClassRoom().getAcademicYear();
        }
        return LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1);
    }

    /**
     * Get class teacher name
     */
    public String getClassTeacher() {
        if (classTeacher != null && !classTeacher.isEmpty()) {
            return classTeacher;
        }
        if (student != null && student.getClassRoom() != null) {
            return student.getClassRoom().getClassTeacher() != null ?
                    student.getClassRoom().getClassTeacher() : "Not Assigned";
        }
        return "Not Assigned";
    }

    /**
     * Get student date of birth formatted
     */
    public String getFormattedDateOfBirth() {
        if (student != null && student.getDateOfBirth() != null) {
            return student.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "N/A";
    }

    /**
     * Get student gender
     */
    public String getStudentGender() {
        if (student != null && student.getGender() != null) {
            return student.getGender().toString();
        }
        return "N/A";
    }

    /**
     * Get student ID string
     */
    public String getStudentIdString() {
        if (student != null && student.getStudentId() != null) {
            return student.getStudentId();
        }
        return "N/A";
    }

    /**
     * Get roll number
     */
    public String getRollNumber() {
        if (rollNumber != null && !rollNumber.isEmpty()) {
            return rollNumber;
        }
        if (student != null && student.getRollNumber() != null) {
            return student.getRollNumber();
        }
        return "N/A";
    }

    /**
     * Get class name
     */
    public String getClassName() {
        if (className != null && !className.isEmpty()) {
            return className;
        }
        if (student != null && student.getClassRoom() != null) {
            return student.getClassRoom().getName();
        }
        return "N/A";
    }

    /**
     * Get department name
     */
    public String getDepartment() {
        if (department != null && !department.isEmpty()) {
            return department;
        }
        if (student != null && student.getDepartment() != null) {
            return student.getDepartment().getName();
        }
        return "N/A";
    }

    /**
     * Get specialty
     */
    public String getSpecialty() {
        if (specialty != null && !specialty.isEmpty()) {
            return specialty;
        }
        if (student != null && student.getSpecialty() != null) {
            return student.getSpecialty();
        }
        return "General";
    }

    /**
     * Get formatted pass rate
     */
    public String getFormattedPassRate() {
        return passRate != null ? String.format("%.1f%%", passRate) : "0.0%";
    }

    /**
     * Get passed status based on term average
     */
    public Boolean getPassed() {
        return termAverage != null && termAverage >= 10.0;
    }
}