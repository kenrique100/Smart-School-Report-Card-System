package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.StudentSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSubjectRepository extends JpaRepository<StudentSubject, Long> {
    List<StudentSubject> findByStudentId(Long studentId);

    @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.subject.id = :subjectId")
    List<StudentSubject> findByStudentAndSubject(@Param("studentId") Long studentId,
                                                 @Param("subjectId") Long subjectId);

    @Modifying
    @Query("DELETE FROM StudentSubject ss WHERE ss.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}