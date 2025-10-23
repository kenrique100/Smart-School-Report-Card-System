package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTerm(Integer term);
    List<Exam> findByClassRoomId(Long classRoomId);
}