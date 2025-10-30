package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.StudentSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSubjectRepository extends JpaRepository<StudentSubject, Long> {
    List<StudentSubject> findByStudentId(Long studentId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.subject.id = :subjectId")
    List<StudentSubject> findByStudentAndSubject(@Param("studentId") Long studentId,
                                                 @Param("subjectId") Long subjectId);

    @Modifying
    @Query("DELETE FROM StudentSubject ss WHERE ss.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT ss FROM StudentSubject ss JOIN FETCH ss.subject WHERE ss.student.id = :studentId")
    List<StudentSubject> findByStudentIdWithSubject(@Param("studentId") Long studentId);

    // ADD THIS CRITICAL METHOD - Check if student is already enrolled in subject
    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.subject.id = :subjectId")
    Optional<StudentSubject> findByStudentIdAndSubjectId(@Param("studentId") Long studentId,
                                                         @Param("subjectId") Long subjectId);
}