package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.term = :term ORDER BY a.subject.name ASC")
    List<Assessment> findByStudentIdAndTermOrderBySubjectNameAsc(@Param("studentId") Long studentId,
                                                                 @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.subject.id = :subjectId AND a.term = :term")
    List<Assessment> findByStudentIdAndSubjectIdAndTerm(@Param("studentId") Long studentId,
                                                        @Param("subjectId") Long subjectId,
                                                        @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student = :student AND a.subject = :subject AND a.term = :term AND a.type = :type")
    Optional<Assessment> findByStudentAndSubjectAndTermAndType(@Param("student") Student student,
                                                               @Param("subject") Subject subject,
                                                               @Param("term") Integer term,
                                                               @Param("type") String type);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId")
    List<Assessment> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Assessment a WHERE a.subject.id = :subjectId AND a.term = :term ORDER BY a.score DESC")
    List<Assessment> findBySubjectIdAndTermOrderByScoreDesc(@Param("subjectId") Long subjectId,
                                                            @Param("term") Integer term);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.term = :term")
    List<Assessment> findByStudentIdAndTerm(@Param("studentId") Long studentId,
                                            @Param("term") Integer term);

    @Query("SELECT DISTINCT a.term FROM Assessment a WHERE a.student.id = :studentId ORDER BY a.term")
    List<Integer> findDistinctTermByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Assessment a WHERE a.student = :student")
    List<Assessment> findByStudent(@Param("student") Student student);

    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId ORDER BY a.term, a.subject.name")
    List<Assessment> findAllByStudentId(@Param("studentId") Long studentId);

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
}