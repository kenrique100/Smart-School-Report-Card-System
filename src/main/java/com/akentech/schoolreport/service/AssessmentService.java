package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.StudentAssessmentSummaryDTO;
import com.akentech.schoolreport.dto.StudentTermAverageDTO;
import com.akentech.schoolreport.dto.StudentYearlyAverageDTO;
import com.akentech.schoolreport.dto.TermAssessmentDTO;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.AssessmentType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AssessmentService {
    // Existing methods...
    Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject, Integer term, String type);
    Optional<Assessment> getAssessmentByStudentSubjectAndTermAndType(Long studentId, Long subjectId, Integer term, AssessmentType type);
    List<Assessment> getAssessmentsByStudentAndTerm(Long studentId, Integer term);
    List<Assessment> getAssessmentsByStudentSubjectAndTerm(Long studentId, Long subjectId, Integer term);
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

    // New methods for student assessment summary
    StudentAssessmentSummaryDTO getAssessmentSummary(Long studentId);
    List<TermAssessmentDTO> getTermAssessments(Long studentId, Integer term);

    // New methods with academic year support
    Double calculateTermAverageWithAcademicYear(Long studentId, Integer term,
                                                Integer academicYearStart, Integer academicYearEnd);
    Double calculateYearlyAverageWithAcademicYear(Long studentId, Integer academicYearStart,
                                                  Integer academicYearEnd);
    List<StudentTermAverageDTO> getTermAveragesForClassWithAcademicYear(Long classId, Integer term,
                                                                        Integer academicYearStart,
                                                                        Integer academicYearEnd);
    List<StudentYearlyAverageDTO> getYearlyAveragesForClassWithAcademicYear(Long classId,
                                                                            Integer academicYearStart,
                                                                            Integer academicYearEnd);
    Map<Long, List<Double>> getStudentSubjectScoresByTermAndAcademicYear(Long studentId, Integer term,
                                                                         Integer academicYearStart,
                                                                         Integer academicYearEnd);
    StudentAssessmentSummaryDTO getAssessmentSummaryWithAcademicYear(Long studentId,
                                                                     Integer academicYearStart,
                                                                     Integer academicYearEnd);
    List<TermAssessmentDTO> getTermAssessmentsWithAcademicYear(Long studentId, Integer term,
                                                               Integer academicYearStart,
                                                               Integer academicYearEnd);

    // Batch academic year fixing
    void fixAcademicYearsForStudent(Long studentId, Integer academicYearStart, Integer academicYearEnd);
    void fixAllAcademicYears(Integer academicYearStart, Integer academicYearEnd);

    // Grouping methods
    Map<Integer, List<Assessment>> getAssessmentsByStudentGroupedByTerm(Long studentId);
    Map<Integer, Map<Long, List<Assessment>>> getAssessmentsByStudentGroupedByTermAndSubject(Long studentId);

}