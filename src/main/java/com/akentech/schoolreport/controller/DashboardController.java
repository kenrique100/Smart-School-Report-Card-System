package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.DashboardStatistics;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.Gender;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.service.StudentService;
import com.akentech.schoolreport.service.SubjectService;
import com.akentech.schoolreport.service.TeacherService;
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
    private final NoticeRepository noticeRepository;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final StudentService studentService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            DashboardStatistics stats = getDashboardStatistics();
            List<ClassRoom> classes = classRoomRepository.findAll();

            model.addAttribute("statistics", stats);
            model.addAttribute("classes", classes);

            log.info("Dashboard loaded successfully with {} classes", classes.size());
            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("statistics", DashboardStatistics.builder().build());
            model.addAttribute("classes", List.of());
            model.addAttribute("error", "Unable to load dashboard statistics");
            return "dashboard";
        }
    }

    private DashboardStatistics getDashboardStatistics() {
        log.info("Loading dashboard statistics...");

        long totalStudents = studentService.getStudentCount();
        long totalTeachers = teacherService.getTeacherCount();
        long totalSubjects = subjectService.getSubjectCount();
        long totalNotices = noticeRepository.countByIsActive(true);
        long totalClasses = classRoomRepository.count();
        long totalDepartments = departmentRepository.count();

        Map<String, Long> studentsByDepartment = getStudentsByDepartment();
        Map<String, Long> studentsBySpecialty = getStudentsBySpecialty();
        Map<String, Long> studentsByClass = getStudentsByClass();
        Map<String, Long> genderDistribution = getGenderDistribution();

        return DashboardStatistics.builder()
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
    }

    private Map<String, Long> getStudentsByDepartment() {
        Map<String, Long> departmentStats = studentRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getDepartment().getName(),
                        Collectors.counting()
                ));

        List<String> allDepartments = departmentRepository.findAll().stream()
                .map(Department::getName)
                .toList();

        Map<String, Long> result = new LinkedHashMap<>();
        for (String dept : allDepartments) {
            result.put(dept, departmentStats.getOrDefault(dept, 0L));
        }

        return result;
    }

    private Map<String, Long> getStudentsBySpecialty() {
        Map<String, Long> specialtyStats = studentRepository.findAll().stream()
                .filter(s -> s.getSpecialty() != null && !s.getSpecialty().isEmpty())
                .collect(Collectors.groupingBy(Student::getSpecialty, Collectors.counting()));

        return specialtyStats.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Long> getStudentsByClass() {
        List<Student> allStudents = studentRepository.findAll();

        Map<String, Long> classCounts = allStudents.stream()
                .filter(student -> student.getClassRoom() != null && student.getClassRoom().getName() != null)
                .collect(Collectors.groupingBy(
                        student -> student.getClassRoom().getName(),
                        Collectors.counting()
                ));

        List<String> allClassNames = classRoomRepository.findAll().stream()
                .map(ClassRoom::getName)
                .toList();

        List<String> sortedClassNames = allClassNames.stream()
                .sorted((c1, c2) -> {
                    if (c1.contains("Form") && c2.contains("Form")) {
                        return extractFormNumber(c1) - extractFormNumber(c2);
                    } else if (c1.contains("Form") && c2.contains("Sixth")) {
                        return -1;
                    } else if (c1.contains("Sixth") && c2.contains("Form")) {
                        return 1;
                    } else if (c1.contains("Sixth") && c2.contains("Sixth")) {
                        if (c1.contains("Lower") && c2.contains("Upper")) return -1;
                        if (c1.contains("Upper") && c2.contains("Lower")) return 1;
                        return c1.compareTo(c2);
                    }
                    return c1.compareTo(c2);
                })
                .toList();

        Map<String, Long> result = new LinkedHashMap<>();
        for (String className : sortedClassNames) {
            result.put(className, classCounts.getOrDefault(className, 0L));
        }

        return result;
    }

    private int extractFormNumber(String className) {
        try {
            if (className.contains("Form")) {
                String numberPart = className.replaceAll("[^0-9]", "");
                return numberPart.isEmpty() ? 0 : Integer.parseInt(numberPart);
            }
        } catch (NumberFormatException e) {
            log.warn("Could not extract form number from: {}", className);
        }
        return 0;
    }

    private Map<String, Long> getGenderDistribution() {
        Map<String, Long> genderStats = studentRepository.findAll().stream()
                .filter(s -> s.getGender() != null)
                .collect(Collectors.groupingBy(
                        student -> student.getGender().name(),
                        Collectors.counting()
                ));

        Map<String, Long> result = new LinkedHashMap<>();
        for (Gender gender : Gender.values()) {
            result.put(gender.name(), genderStats.getOrDefault(gender.name(), 0L));
        }

        return result;
    }

    // Add helper method to check if classroom is form level
    public boolean isFormLevel(ClassRoom classRoom) {
        return classRoom != null && classRoom.getCode() != null && classRoom.getCode().isFormLevel();
    }

    // Add helper method to check if classroom is sixth form
    public boolean isSixthForm(ClassRoom classRoom) {
        return classRoom != null && classRoom.getCode() != null &&
                (classRoom.getCode() == ClassLevel.LOWER_SIXTH || classRoom.getCode() == ClassLevel.UPPER_SIXTH);
    }
}