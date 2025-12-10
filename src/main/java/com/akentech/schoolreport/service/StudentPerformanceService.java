package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.SubjectPerformance;

import java.util.List;

public interface StudentPerformanceService {
    void updateStudentSubjectScores(Long studentId, Integer term);
    SubjectPerformance calculateSubjectPerformance(List<Assessment> assessments, Integer term);
    void updateAllStudentScores(Integer term);
}