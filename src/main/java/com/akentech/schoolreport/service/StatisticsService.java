package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.DashboardStatistics;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.enums.Gender;
import com.akentech.schoolreport.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;

    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> stats = new LinkedHashMap<>();

        try {
            stats.put("totalStudents", studentRepository.count());
            stats.put("totalTeachers", teacherRepository.count());
            stats.put("totalSubjects", subjectRepository.count());
            stats.put("totalClasses", classRoomRepository.count());
            stats.put("totalDepartments", departmentRepository.count());
            stats.put("totalSpecialties", studentRepository.countDistinctSpecialties());

            log.info("Dashboard statistics loaded successfully: {}", stats);

        } catch (Exception e) {
            log.error("Error loading dashboard statistics", e);
            // Fallback defaults
            stats.put("totalStudents", 0L);
            stats.put("totalTeachers", 0L);
            stats.put("totalSubjects", 0L);
            stats.put("totalClasses", 0L);
            stats.put("totalDepartments", 0L);
            stats.put("totalSpecialties", 0L);
        }

        return stats;
    }

    public DashboardStatistics getCompleteDashboardStatistics() {
        Map<String, Long> basicStats = getDashboardStatistics();

        return DashboardStatistics.builder()
                .totalStudents(basicStats.get("totalStudents"))
                .totalTeachers(basicStats.get("totalTeachers"))
                .totalSubjects(basicStats.get("totalSubjects"))
                .totalClasses(basicStats.get("totalClasses"))
                .totalDepartments(basicStats.get("totalDepartments"))
                .totalSpecialties(basicStats.get("totalSpecialties"))
                .studentsByDepartment(getStudentsByDepartment())
                .studentsBySpecialty(getStudentsBySpecialty())
                .studentsByClass(getStudentsByClass())
                .genderDistribution(getGenderDistribution())
                .build();
    }

    public Map<String, Long> getStudentsByDepartment() {
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

    public Map<String, Long> getStudentsBySpecialty() {
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

    public Map<String, Long> getStudentsByClass() {
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

    public Map<String, Long> getGenderDistribution() {
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
}