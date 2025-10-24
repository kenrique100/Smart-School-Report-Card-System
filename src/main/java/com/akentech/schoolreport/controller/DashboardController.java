package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.DashboardStatistics;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final NoticeRepository noticeRepository;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            DashboardStatistics stats = getDashboardStatistics();
            model.addAttribute("statistics", stats);
            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("statistics", DashboardStatistics.builder().build());
            return "dashboard";
        }
    }

    private DashboardStatistics getDashboardStatistics() {
        log.info("Loading dashboard statistics...");

        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalSubjects = subjectRepository.count();
        long totalNotices = noticeRepository.countByIsActive(true);
        long totalClasses = classRoomRepository.count();
        long totalDepartments = departmentRepository.count();

        Map<String, Long> studentsByDepartment = getStudentsByDepartment();
        Map<String, Long> studentsBySpecialty = getStudentsBySpecialty();
        Map<String, Long> studentsByClass = getStudentsByClass();
        Map<String, Long> genderDistribution = getGenderDistribution();

        DashboardStatistics stats = DashboardStatistics.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalSubjects(totalSubjects)
                .totalNotices(totalNotices)
                .totalClasses(totalClasses)
                .totalDepartments(totalDepartments)
                .totalSpecialties(studentsBySpecialty.size())
                .studentsByDepartment(studentsByDepartment)
                .studentsBySpecialty(studentsBySpecialty)
                .studentsByClass(studentsByClass)
                .genderDistribution(genderDistribution)
                .build();

        log.info("Dashboard stats successfully computed: {}", stats);
        return stats;
    }

    private Map<String, Long> getStudentsByDepartment() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getDepartment().getName(),
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getStudentsBySpecialty() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getSpecialty() != null && !s.getSpecialty().isEmpty())
                .collect(Collectors.groupingBy(Student::getSpecialty, Collectors.counting()));
    }

    private Map<String, Long> getStudentsByClass() {
        List<Student> allStudents = studentRepository.findAll();

        return allStudents.stream()
                .filter(student -> student.getClassRoom() != null && student.getClassRoom().getName() != null)
                .collect(Collectors.groupingBy(
                        student -> student.getClassRoom().getName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // ✅ sort ascending by class name
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // ✅ preserve order
                ));
    }


    private Map<String, Long> getGenderDistribution() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getGender() != null && !s.getGender().isEmpty())
                .collect(Collectors.groupingBy(Student::getGender, Collectors.counting()));
    }
}