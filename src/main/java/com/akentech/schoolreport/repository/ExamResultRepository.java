package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    @Query("SELECT er FROM ExamResult er WHERE er.student.id = :studentId AND er.exam.id = :examId")
    List<ExamResult> findByStudentIdAndExamId(@Param("studentId") Long studentId,
                                              @Param("examId") Long examId);

    @Modifying
    @Query("DELETE FROM ExamResult er WHERE er.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM ExamResult er WHERE er.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);

    @Query("SELECT COUNT(er) FROM ExamResult er WHERE er.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
}