package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.AverageRecord;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AverageRecordRepository extends JpaRepository<AverageRecord, Long> {

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student IN :students AND ar.term = :term")
    List<AverageRecord> findByStudentInAndTerm(@Param("students") List<Student> students,
                                               @Param("term") Integer term);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student = :student AND ar.term = :term")
    Optional<AverageRecord> findByStudentAndTerm(@Param("student") Student student,
                                                 @Param("term") Integer term);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.term = :term")
    List<AverageRecord> findByTerm(@Param("term") Integer term);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student.id = :studentId")
    List<AverageRecord> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student = :student")
    List<AverageRecord> findByStudent(@Param("student") Student student);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student.classRoom.id = :classRoomId AND ar.term = :term")
    List<AverageRecord> findByClassRoomIdAndTerm(@Param("classRoomId") Long classRoomId,
                                                 @Param("term") Integer term);

    @Query("SELECT ar FROM AverageRecord ar WHERE ar.student.classRoom.id = :classRoomId ORDER BY ar.average DESC")
    List<AverageRecord> findByClassRoomIdOrderByAverageDesc(@Param("classRoomId") Long classRoomId);

    @Query("SELECT COUNT(ar) FROM AverageRecord ar WHERE ar.student = :student")
    long countByStudent(@Param("student") Student student);

    @Modifying
    @Query("DELETE FROM AverageRecord ar WHERE ar.student = :student")
    void deleteByStudent(@Param("student") Student student);

    @Modifying
    @Query("DELETE FROM AverageRecord ar WHERE ar.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}