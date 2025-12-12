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

    Double calculateWeightedTermAverage(List<SubjectReport> subjectReports);

    String generateRemarks(Double averageOutOf20);

    String getPerformanceStatus(Double averageOutOf20);
}
