package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.SubjectReport;

import java.util.List;

public interface GradeService {
    Double calculateSubjectAverage(Double assessment1, Double assessment2, Double exam, Integer term);
    String calculateLetterGrade(Double scoreOutOf20, String className);
    boolean isPassing(Double averageOutOf20);
    Double calculateWeightedTermAverage(List<SubjectReport> subjectReports);
    String generateRemarks(Double averageOutOf20);
}