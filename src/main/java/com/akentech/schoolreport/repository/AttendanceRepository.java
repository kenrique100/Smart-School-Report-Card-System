package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByAttendanceDate(LocalDate date);
    List<Attendance> findByStudentId(Long studentId);
    List<Attendance> findByStudentIdAndAttendanceDate(Long studentId, LocalDate date);

    // Add these methods for statistics
    long countByStatus(String status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = :status AND a.attendanceDate = :date")
    long countByStatusAndAttendanceDate(@Param("status") String status, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = :date")
    long countByAttendanceDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = :status")
    long countByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") String status);

    // Add this missing method for dashboard statistics
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = 'PRESENT' AND a.attendanceDate = CURRENT_DATE")
    long countPresentToday();
}