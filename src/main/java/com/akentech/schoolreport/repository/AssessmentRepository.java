package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    // Add the missing method
    @Query("SELECT a FROM Assessment a WHERE a.student.id IN :studentIds AND a.term = :term")
    List<Assessment> findByStudentIdInAndTerm(@Param("studentIds") List<Long> studentIds,
                                              @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id IN :studentIds AND a.term = :term " +
            "AND a.academicYearStart = :academicYearStart AND a.academicYearEnd = :academicYearEnd")
    List<Assessment> findByStudentIdInAndTermAndAcademicYear(
            @Param("studentIds") List<Long> studentIds,
            @Param("term") Integer term,
            @Param("academicYearStart") Integer academicYearStart,
            @Param("academicYearEnd") Integer academicYearEnd);

    @Query("SELECT a FROM Assessment a " +
            "JOIN FETCH a.subject " +
            "WHERE a.student.id = :studentId " +
            "ORDER BY a.term, a.type")
    List<Assessment> findAllByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Assessment a WHERE a.student = :student AND a.subject = :subject " +
            "AND a.term = :term AND a.type = :type")
    Optional<Assessment> findByStudentAndSubjectAndTermAndType(@Param("student") Student student,
                                                               @Param("subject") Subject subject,
                                                               @Param("term") Integer term,
                                                               @Param("type") AssessmentType type);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.subject.id = :subjectId " +
            "AND a.term = :term AND a.type = :type")
    Optional<Assessment> findByStudentIdAndSubjectIdAndTermAndType(@Param("studentId") Long studentId,
                                                                   @Param("subjectId") Long subjectId,
                                                                   @Param("term") Integer term,
                                                                   @Param("type") AssessmentType type);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.subject.id = :subjectId AND a.term = :term")
    List<Assessment> findByStudentIdAndSubjectIdAndTerm(@Param("studentId") Long studentId,
                                                        @Param("subjectId") Long subjectId,
                                                        @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.term = :term ORDER BY a.subject.name ASC")
    List<Assessment> findByStudentIdAndTermOrderBySubjectNameAsc(@Param("studentId") Long studentId,
                                                                 @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.term = :term")
    List<Assessment> findByStudentIdAndTerm(@Param("studentId") Long studentId,
                                            @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId")
    List<Assessment> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Assessment a WHERE a.subject.id = :subjectId AND a.term = :term ORDER BY a.score DESC")
    List<Assessment> findBySubjectIdAndTermOrderByScoreDesc(@Param("subjectId") Long subjectId,
                                                            @Param("term") Integer term);

    @Query("SELECT DISTINCT a.term FROM Assessment a WHERE a.student.id = :studentId ORDER BY a.term")
    List<Integer> findDistinctTermByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Assessment a WHERE a.student = :student")
    List<Assessment> findByStudent(@Param("student") Student student);

    @Query("SELECT COUNT(a) FROM Assessment a WHERE a.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM Assessment a WHERE a.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM Assessment a WHERE a.student = :student")
    void deleteByStudent(@Param("student") Student student);

    @Query("SELECT a FROM Assessment a WHERE a.subject.id = :subjectId")
    List<Assessment> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT a FROM Assessment a WHERE a.term = :term")
    List<Assessment> findByTerm(@Param("term") Integer term);

    @Query("SELECT DISTINCT a.student.id FROM Assessment a WHERE a.subject.id = :subjectId")
    List<Long> findStudentIdsBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT a FROM Assessment a WHERE a.type = :type AND a.term = :term")
    List<Assessment> findByTypeAndTerm(@Param("type") AssessmentType type,
                                       @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.classRoom.id = :classId AND a.term = :term")
    List<Assessment> findByClassIdAndTerm(@Param("classId") Long classId,
                                          @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.term = :term AND a.academicYearStart = :startYear AND a.academicYearEnd = :endYear")
    List<Assessment> findByStudentIdAndTermAndAcademicYear(
            @Param("studentId") Long studentId,
            @Param("term") Integer term,
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear
    );

    @Query("SELECT a FROM Assessment a " +
            "JOIN FETCH a.subject " +
            "WHERE a.student.id = :studentId " +
            "AND a.term = :term " +
            "ORDER BY a.subject.name, a.type")
    List<Assessment> findByStudentIdAndTermWithSubject(
            @Param("studentId") Long studentId,
            @Param("term") Integer term);

    @Query("SELECT a.term, COUNT(a) FROM Assessment a WHERE a.student.id = :studentId GROUP BY a.term")
    List<Object[]> countAssessmentsByTermForStudent(@Param("studentId") Long studentId);

    @Query("SELECT DISTINCT a.student.id FROM Assessment a")
    List<Long> findDistinctStudentIds();

    // Add more batch query methods for performance optimization
    @Query("SELECT a FROM Assessment a WHERE a.student.id IN :studentIds")
    List<Assessment> findByStudentIdIn(@Param("studentIds") List<Long> studentIds);

    @Query("SELECT a FROM Assessment a WHERE a.student.id IN :studentIds AND a.term = :term " +
            "ORDER BY a.student.id, a.subject.name")
    List<Assessment> findByStudentIdInAndTermOrderByStudentId(@Param("studentIds") List<Long> studentIds,
                                                              @Param("term") Integer term);

}