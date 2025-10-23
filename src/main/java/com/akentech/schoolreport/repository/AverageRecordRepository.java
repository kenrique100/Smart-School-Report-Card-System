package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.AverageRecord;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AverageRecordRepository extends JpaRepository<AverageRecord, Long> {
    List<AverageRecord> findByStudentInAndTerm(List<Student> students, Integer term);
    Optional<AverageRecord> findByStudentAndTerm(Student student, Integer term);
    List<AverageRecord> findByTerm(Integer term);

    // Remove duplicate and use proper parameter name
    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student.id = :studentId")
    List<AverageRecord> findByStudentId(@Param("studentId") Long studentId);

    List<AverageRecord> findByStudent(Student student);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student.classRoom.id = :classRoomId AND ar.term = :term")
    List<AverageRecord> findByClassRoomIdAndTerm(@Param("classRoomId") Long classRoomId, @Param("term") Integer term);

    // Additional useful queries
    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student.classRoom.id = :classRoomId ORDER BY ar.average DESC")
    List<AverageRecord> findByClassRoomIdOrderByAverageDesc(@Param("classRoomId") Long classRoomId);
}