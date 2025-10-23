package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ExamResult;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudent(Student student);
    List<ExamResult> findByStudentAndTerm(Student student, Integer term);
    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);
    List<ExamResult> findByExamId(Long examId);
    List<ExamResult> findByStudentId(Long studentId);
    List<ExamResult> findByStudentIdAndExamId(Long studentId, Long examId);

}