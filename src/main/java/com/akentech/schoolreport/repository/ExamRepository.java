package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findAllByOrderByExamDateDesc();

    @Query("SELECT e FROM Exam e WHERE e.classRoom.id = :classRoomId ORDER BY e.examDate DESC")
    List<Exam> findByClassRoomIdOrderByExamDateDesc(@Param("classRoomId") Long classRoomId);

    List<Exam> findByExamDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.classRoom.id = :classRoomId")
    long countByClassRoomId(@Param("classRoomId") Long classRoomId);

    @Query("SELECT e FROM Exam e WHERE e.name LIKE %:name%")
    List<Exam> findByNameContaining(@Param("name") String name);
}