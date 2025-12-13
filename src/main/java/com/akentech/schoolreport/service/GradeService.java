package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.SubjectReport;
import com.akentech.schoolreport.model.Assessment;

import java.util.List;

public interface GradeService {

    Double calculateSubjectAverage(Double assessment1, Double assessment2, Integer term);

    Double calculateSubjectAverageForTerm(List<Assessment> assessments, Integer term);

    Double calculateYearlyAverage(Double term1Avg, Double term2Avg, Double term3Avg);

    String calculateLetterGrade(Double scoreOutOf20, String className);

    boolean isPassing(Double averageOutOf20);

    // Fixed: Updated method signatures to match implementation
    boolean isSubjectPassing(String letterGrade, String className);

    Double calculateWeightedTermAverage(List<SubjectReport> subjectReports);

    // Fixed: Updated method signatures to match implementation
    Double calculatePassRate(List<SubjectReport> subjectReports, String className);

    Long countPassedSubjects(List<SubjectReport> subjectReports, String className);

    String generateRemarks(Double averageOutOf20);

    String getPerformanceStatus(Double averageOutOf20);
}