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
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;

    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> stats = new HashMap<>();

        try {
            stats.put("totalStudents", studentRepository.count());
            stats.put("totalTeachers", teacherRepository.count());
            stats.put("totalSubjects", subjectRepository.count());
            stats.put("totalNotices", noticeRepository.countByIsActive(true));
            stats.put("totalClasses", classRoomRepository.count());
            stats.put("totalDepartments", departmentRepository.count());
            stats.put("totalSpecialties", studentRepository.count());

            log.info("Dashboard statistics loaded successfully: {}", stats);

        } catch (Exception e) {
            log.error("Error loading dashboard statistics", e);
            // Fallback defaults
            stats.put("totalStudents", 0L);
            stats.put("totalTeachers", 0L);
            stats.put("totalSubjects", 0L);
            stats.put("totalNotices", 0L);
            stats.put("totalClasses", 0L);
            stats.put("totalDepartments", 0L);
            stats.put("totalSpecialties", 0L);
        }

        return stats;
    }
}