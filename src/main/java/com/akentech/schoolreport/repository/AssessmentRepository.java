package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByStudentAndTerm(Student student, Integer term);

    List<Assessment> findByStudentAndSubjectAndTerm(Student student, Subject subject, Integer term);

    List<Assessment> findBySubjectAndTerm(Subject subject, Integer term);

    Optional<Assessment> findByStudentAndSubjectAndTermAndType(Student student, Subject subject, Integer term, String type);

    // Remove duplicate and use proper parameter name
    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId")
    List<Assessment> findByStudentId(@Param("studentId") Long studentId);

    List<Assessment> findByStudent(Student student);

    @Query("SELECT a FROM Assessment a WHERE a.student.classRoom.id = :classRoomId AND a.term = :term")
    List<Assessment> findByClassRoomIdAndTerm(@Param("classRoomId") Long classRoomId, @Param("term") Integer term);

    // Additional useful queries
    @Query("SELECT a FROM Assessment a WHERE a.student.id = :studentId AND a.term = :term AND a.type = :type")
    List<Assessment> findByStudentIdAndTermAndType(@Param("studentId") Long studentId,
                                                   @Param("term") Integer term,
                                                   @Param("type") String type);
}