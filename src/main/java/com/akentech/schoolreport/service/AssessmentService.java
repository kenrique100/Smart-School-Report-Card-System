package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;

import java.util.List;
import java.util.Optional;

public interface AssessmentService {
    List<Assessment> findByStudentAndTermOrderBySubjectNameAsc(Student student, Integer term);
    List<Assessment> getAssessmentsByStudentSubjectAndTerm(Long studentId, Long subjectId, Integer term);
    Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject, Integer term, String type);
    Assessment save(Assessment assessment);
    List<Assessment> saveAll(List<Assessment> assessments);
    void delete(Long id);
    Assessment getAssessmentById(Long id);
    List<Assessment> getAssessmentsByStudent(Long studentId);
    List<Assessment> getAssessmentsBySubjectAndTerm(Long subjectId, Integer term);
}