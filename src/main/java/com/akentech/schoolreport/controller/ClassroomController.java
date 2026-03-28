package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/classroom")
@RequiredArgsConstructor
@Slf4j
public class ClassroomController {

    private final ClassRoomRepository classRoomRepository;
    private final StudentService studentService;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/students/{id}")
    public String getClassroomStudents(@PathVariable Long id,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size,
                                       @RequestParam(defaultValue = "firstName") String sortBy,
                                       @RequestParam(defaultValue = "asc") String sortDir,
                                       @RequestParam(required = false) String departmentId,
                                       @RequestParam(required = false) String specialty,
                                       Model model) {
        try {
            ClassRoom classroom = classRoomRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("ClassRoom", id));

            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // FIXED: Use safe parsing method
            Long departmentIdLong = safeParseLong(departmentId);

            Page<Student> studentPage = studentService.getStudentsByFilters(
                    null, null, id, departmentIdLong, specialty, pageable);

            List<Student> students = studentPage.getContent();

            Map<String, Long> genderStats = calculateGenderStatistics(students);
            long maleCount = genderStats.getOrDefault("MALE", 0L);
            long femaleCount = genderStats.getOrDefault("FEMALE", 0L);
            long totalStudents = students.size();

            model.addAttribute("classroom", classroom);
            model.addAttribute("students", students);
            model.addAttribute("allClasses", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", studentService.getAllSpecialties());

            model.addAttribute("currentPage", studentPage.getNumber());
            model.addAttribute("totalPages", studentPage.getTotalPages());
            model.addAttribute("totalItems", studentPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            model.addAttribute("departmentIdFilter", departmentIdLong);
            model.addAttribute("specialtyFilter", specialty);

            model.addAttribute("maleCount", maleCount);
            model.addAttribute("femaleCount", femaleCount);
            model.addAttribute("totalCount", totalStudents);
            model.addAttribute("withDepartmentCount", students.stream()
                    .filter(s -> s.getDepartment() != null)
                    .count());

            log.info("Loaded {} students for classroom: {} (Male: {}, Female: {})",
                    students.size(), classroom.getName(), maleCount, femaleCount);
            return "classroom-students";

        } catch (EntityNotFoundException e) {
            log.warn("Classroom not found with id: {}", id);
            return "redirect:/dashboard?error=classroom_not_found";
        } catch (Exception e) {
            log.error("Error loading classroom students for id: {}", id, e);
            return "redirect:/dashboard?error=server_error";
        }
    }

    // FIXED: Helper method to safely parse Long
    private Long safeParseLong(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long from value: {}", value);
            return null;
        }
    }

    @GetMapping
    public String listClassrooms(Model model) {
        try {
            List<ClassRoom> classrooms = classRoomRepository.findAll();
            long totalStudents = studentService.getStudentCount();

            Map<Long, Map<String, Long>> classroomStats = classrooms.stream()
                    .collect(Collectors.toMap(
                            ClassRoom::getId,
                            classroom -> {
                                List<Student> classStudents = studentService.getStudentsByClass(classroom);
                                return calculateGenderStatistics(classStudents);
                            }
                    ));

            model.addAttribute("classrooms", classrooms);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("classroomStats", classroomStats);

            log.info("Loaded {} classrooms with {} total students", classrooms.size(), totalStudents);
            return "classrooms";
        } catch (Exception e) {
            log.error("Error loading classrooms", e);
            model.addAttribute("error", "Unable to load classrooms");
            model.addAttribute("classrooms", List.of());
            model.addAttribute("totalStudents", 0L);
            model.addAttribute("classroomStats", Map.of());
            return "classrooms";
        }
    }

    private Map<String, Long> calculateGenderStatistics(List<Student> students) {
        return students.stream()
                .filter(student -> student.getGender() != null)
                .collect(Collectors.groupingBy(
                        student -> student.getGender().name(),
                        Collectors.counting()
                ));
    }
}