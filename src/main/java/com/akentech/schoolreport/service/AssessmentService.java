package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.StudentTermAverageDTO;
import com.akentech.schoolreport.dto.StudentYearlyAverageDTO;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.AssessmentType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AssessmentService {

    Optional<Assessment> getAssessmentByStudentSubjectAndTermAndType(Long studentId, Long subjectId,
                                                                     Integer term, AssessmentType type);

    List<Assessment> getAssessmentsByStudentAndTerm(Long studentId, Integer term);

    List<Assessment> getAssessmentsByStudentSubjectAndTerm(Long studentId, Long subjectId, Integer term);

    Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject,
                                                      Integer term, String type);

    Assessment save(Assessment assessment);

    List<Assessment> saveAll(List<Assessment> assessments);

    void delete(Long id);

    Assessment getAssessmentById(Long id);

    List<Assessment> getAssessmentsByStudent(Long studentId);

    List<Assessment> getAssessmentsBySubjectAndTerm(Long subjectId, Integer term);

    Double calculateTermAverage(Long studentId, Integer term);

    Double calculateYearlyAverage(Long studentId);

    List<StudentTermAverageDTO> getTermAveragesForClass(Long classId, Integer term);

    List<StudentYearlyAverageDTO> getYearlyAveragesForClass(Long classId);

    Map<Long, List<Double>> getStudentSubjectScoresByTerm(Long studentId, Integer term);
}