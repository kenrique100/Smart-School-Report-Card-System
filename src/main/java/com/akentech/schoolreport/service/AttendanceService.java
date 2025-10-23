package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Attendance;
import com.akentech.schoolreport.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDate(date);
    }

    public List<Attendance> getAttendanceByStudent(Long studentId) {
        // Implementation would fetch by student ID
        return attendanceRepository.findAll(); // Simplified
    }

    @Transactional
    public Attendance saveAttendance(Attendance attendance) {
        if (attendance.getAttendanceDate() == null) {
            attendance.setAttendanceDate(LocalDate.now());
        }

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Saved attendance for student: {} on {}",
                saved.getStudent().getFirstName(), saved.getAttendanceDate());
        return saved;
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
        log.info("Deleted attendance id: {}", id);
    }

    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }
}