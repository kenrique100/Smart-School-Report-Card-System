package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Exam;

import java.util.List;
import java.util.Optional;

public interface ExamService {
    List<Exam> getAllExams();
    Optional<Exam> getExamById(Long id);
    Exam saveExam(Exam exam);
    void deleteExam(Long id);
}