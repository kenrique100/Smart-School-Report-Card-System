package com.akentech.schoolreport.service;

import com.akentech.schoolreport.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final NoticeRepository noticeRepository;
    private final AttendanceRepository attendanceRepository;

    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> stats = new HashMap<>();

        try {
            stats.put("totalStudents", studentRepository.count());
            stats.put("totalTeachers", teacherRepository.count());
            stats.put("totalSubjects", subjectRepository.count());
            stats.put("totalNotices", noticeRepository.countByIsActiveTrue());

            // Get today's present count with proper error handling
            try {
                long presentToday = attendanceRepository.countPresentToday();
                stats.put("presentToday", presentToday);
            } catch (Exception e) {
                log.warn("Error counting today's attendance, setting to 0", e);
                stats.put("presentToday", 0L);
            }

            log.info("Dashboard statistics loaded: {}", stats);
        } catch (Exception e) {
            log.error("Error loading dashboard statistics", e);
            // Set default values
            stats.put("totalStudents", 0L);
            stats.put("totalTeachers", 0L);
            stats.put("totalSubjects", 0L);
            stats.put("totalNotices", 0L);
            stats.put("presentToday", 0L);
        }

        return stats;
    }
}