package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.SpecialtyService;
import com.akentech.schoolreport.service.StudentEnrollmentService;
import com.akentech.schoolreport.service.StudentService;
import com.akentech.schoolreport.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentEnrollmentService studentEnrollmentService;
    private final StudentRepository studentRepository;
    private final SubjectService subjectService;
    private final SpecialtyService specialtyService;

    @GetMapping
    public String listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String classRoomId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String specialty,
            Model model) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Handle filter parameter conversion
            Long classRoomIdLong = parseLongParameter(classRoomId, "classRoomId");
            Long departmentIdLong = parseLongParameter(departmentId, "departmentId");

            Page<Student> studentPage = studentService.getStudentsByFilters(
                    null, null, classRoomIdLong, departmentIdLong, specialty, pageable);

            model.addAttribute("students", studentPage.getContent());
            model.addAttribute("currentPage", studentPage.getNumber());
            model.addAttribute("totalPages", studentPage.getTotalPages());
            model.addAttribute("totalItems", studentPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", studentService.getAllSpecialties());
            model.addAttribute("student", new Student());

            // Add filter values for form persistence
            model.addAttribute("classRoomIdFilter", classRoomIdLong);
            model.addAttribute("departmentIdFilter", departmentIdLong);
            model.addAttribute("specialtyFilter", specialty);

            return "students";
        } catch (Exception e) {
            log.error("Error loading students list", e);
            model.addAttribute("error", "Unable to load students");
            populateModelAttributes(model);
            return "students";
        }
    }

    private Long parseLongParameter(String paramValue, String paramName) {
        if (paramValue != null && !paramValue.equals("null") && !paramValue.trim().isEmpty()) {
            try {
                return Long.parseLong(paramValue);
            } catch (NumberFormatException e) {
                log.warn("Invalid {} provided: {}", paramName, paramValue);
            }
        }
        return null;
    }

    @GetMapping("/sort")
    public String sortStudents(
            @RequestParam String sortBy,
            @RequestParam(required = false) String currentSortDir,
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer currentSize,
            @RequestParam(required = false) Long classRoomId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String specialty,
            RedirectAttributes redirectAttributes) {

        String sortDir = "asc";
        if (sortBy.equals(currentSortDir)) {
            sortDir = currentSortDir.equals("asc") ? "desc" : "asc";
        }

        redirectAttributes.addAttribute("sortBy", sortBy);
        redirectAttributes.addAttribute("sortDir", sortDir);
        redirectAttributes.addAttribute("page", currentPage != null ? currentPage : 0);
        redirectAttributes.addAttribute("size", currentSize != null ? currentSize : 100);

        if (classRoomId != null) redirectAttributes.addAttribute("classRoomId", classRoomId);
        if (departmentId != null) redirectAttributes.addAttribute("departmentId", departmentId);
        if (specialty != null) redirectAttributes.addAttribute("specialty", specialty);

        return "redirect:/students";
    }

    @GetMapping("/add")
    public String showAddForm(
            @RequestParam(required = false) String classLevel,
            @RequestParam(required = false) String departmentCode,
            @RequestParam(required = false) String specialty,
            Model model) {

        Student student = new Student();

        List<ClassRoom> classRooms = classRoomRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        // Enhanced default selection logic
        ClassRoom defaultClass = findDefaultClass(classRooms, classLevel);
        Department defaultDepartment = findDefaultDepartment(departments, departmentCode);

        if (defaultClass != null) {
            student.setClassRoom(defaultClass);
        } else if (!classRooms.isEmpty()) {
            student.setClassRoom(classRooms.getFirst());
        }

        if (defaultDepartment != null) {
            student.setDepartment(defaultDepartment);
        } else if (!departments.isEmpty()) {
            student.setDepartment(departments.getFirst());
        }

        // Set specialty if provided
        if (specialty != null && !specialty.trim().isEmpty()) {
            student.setSpecialty(specialty);
        }

        // Get grouped subjects based on default selection
        Map<String, List<Subject>> groupedSubjects = new HashMap<>();
        if (student.getClassRoom() != null && student.getDepartment() != null) {
            String classCode = student.getClassRoom().getCode().name();
            Long departmentId = student.getDepartment().getId();
            String studentSpecialty = student.getSpecialty();
            groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(classCode, departmentId, studentSpecialty);
        } else {
            groupedSubjects.put("compulsory", new ArrayList<>());
            groupedSubjects.put("department", new ArrayList<>());
            groupedSubjects.put("specialty", new ArrayList<>());
            groupedSubjects.put("optional", new ArrayList<>());
        }

        List<Long> selectedSubjectIds = new ArrayList<>();

        model.addAttribute("student", student);
        model.addAttribute("classRooms", classRooms);
        model.addAttribute("departments", departments);
        model.addAttribute("specialties", studentService.getAllSpecialties());
        model.addAttribute("groupedSubjects", groupedSubjects);
        model.addAttribute("selectedSubjectIds", selectedSubjectIds);

        log.info("Add form initialized for class: {}, department: {}, specialty: {} with {} compulsory, {} department, {} specialty, {} optional subjects",
                student.getClassRoom() != null ? student.getClassRoom().getCode() : "None",
                student.getDepartment() != null ? student.getDepartment().getName() : "None",
                student.getSpecialty(),
                groupedSubjects.getOrDefault("compulsory", List.of()).size(),
                groupedSubjects.getOrDefault("department", List.of()).size(),
                groupedSubjects.getOrDefault("specialty", List.of()).size(),
                groupedSubjects.getOrDefault("optional", List.of()).size());

        return "add-student";
    }

    // Helper methods for default selection
    private ClassRoom findDefaultClass(List<ClassRoom> classRooms, String classLevel) {
        if (classLevel != null) {
            return classRooms.stream()
                    .filter(cr -> cr.getCode().name().equalsIgnoreCase(classLevel))
                    .findFirst()
                    .orElse(null);
        }
        // Default to LOWER_SIXTH if available
        return classRooms.stream()
                .filter(cr -> cr.getCode() == ClassLevel.LOWER_SIXTH)
                .findFirst()
                .orElse(null);
    }

    private Department findDefaultDepartment(List<Department> departments, String departmentCode) {
        if (departmentCode != null) {
            return departments.stream()
                    .filter(d -> d.getCode().name().equalsIgnoreCase(departmentCode))
                    .findFirst()
                    .orElse(null);
        }
        // Default to Sciences if available
        return departments.stream()
                .filter(d -> d.getCode() == DepartmentCode.SCI)
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/save")
    public String saveStudent(@Valid @ModelAttribute("student") Student student,
                              BindingResult result,
                              @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateAddModelAttributes(model, student);
            return "add-student";
        }

        try {
            studentService.createStudent(student, subjectIds);
            redirectAttributes.addFlashAttribute("success", "Student created successfully!");
            return "redirect:/students?success";
        } catch (BusinessRuleException | DataIntegrityException e) {
            log.warn("Business error saving student: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            populateAddModelAttributes(model, student);
            return "add-student";
        } catch (Exception e) {
            log.error("Error saving student", e);
            model.addAttribute("error", "Error saving student: " + e.getMessage());
            populateAddModelAttributes(model, student);
            return "add-student";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Student student = studentService.getStudentByIdOrThrow(id);
            List<StudentSubject> studentSubjects = studentService.getStudentSubjects(id);
            List<Long> selectedSubjectIds = studentService.getSelectedSubjectIds(id);

            // Get grouped available subjects for the student's current configuration
            Map<String, List<Subject>> groupedSubjects = studentService.getGroupedAvailableSubjects(student);

            model.addAttribute("student", student);
            model.addAttribute("classRooms", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", studentService.getAllSpecialties());
            model.addAttribute("groupedSubjects", groupedSubjects);
            model.addAttribute("selectedSubjectIds", selectedSubjectIds);
            model.addAttribute("studentSubjects", studentSubjects);

            log.info("Edit form loaded for student {} with {} compulsory, {} department, {} optional subjects",
                    student.getStudentId(),
                    groupedSubjects.getOrDefault("compulsory", List.of()).size(),
                    groupedSubjects.getOrDefault("department", List.of()).size(),
                    groupedSubjects.getOrDefault("optional", List.of()).size());

            return "edit-student";
        } catch (EntityNotFoundException e) {
            log.warn("Student not found with id: {}", id);
            return "redirect:/students?error=notfound";
        } catch (Exception e) {
            log.error("Error loading student edit form for id: {}", id, e);
            return "redirect:/students?error=server_error";
        }
    }

    @PostMapping("/update/{id}")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("student") Student student,
                                BindingResult result,
                                @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateEditModelAttributes(model, student);
            return "edit-student";
        }

        try {
            studentService.updateStudent(id, student, subjectIds);
            redirectAttributes.addFlashAttribute("success", "Student updated successfully!");
            return "redirect:/students?updated";
        } catch (BusinessRuleException | DataIntegrityException e) {
            log.warn("Business error updating student: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            populateEditModelAttributes(model, student);
            return "edit-student";
        } catch (Exception e) {
            log.error("Error updating student with id: {}", id, e);
            model.addAttribute("error", "Error updating student: " + e.getMessage());
            populateEditModelAttributes(model, student);
            return "edit-student";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentByIdOrThrow(id);
            studentService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("success",
                    "Student " + student.getFullName() + " deleted successfully!");
            return "redirect:/students?deleted";
        } catch (EntityNotFoundException e) {
            log.warn("Student not found for deletion with id: {}", id);
            return "redirect:/students?error=notfound";
        } catch (Exception e) {
            log.error("Error deleting student with id: {}", id, e);
            return "redirect:/students?error=deletefailed";
        }
    }

    @GetMapping("/view/{id}")
    public String viewStudent(@PathVariable Long id, Model model) {
        try {
            Student student = studentService.getStudentByIdOrThrow(id);
            List<StudentSubject> studentSubjects = studentService.getStudentSubjects(id);

            // Use the enhanced enrollment summary
            Map<String, Object> subjectsSummary = studentEnrollmentService.getStudentEnrollmentSummary(id);

            // Get available subjects for this student (for reference)
            List<Subject> availableSubjects = studentService.getAvailableSubjectsForStudentView(id);

            // Get grouped subjects for display context
            Map<String, List<Subject>> groupedSubjects = studentService.getGroupedSubjectsForStudent(id);

            model.addAttribute("student", student);
            model.addAttribute("subjectsSummary", subjectsSummary);
            model.addAttribute("studentSubjects", studentSubjects);
            model.addAttribute("availableSubjects", availableSubjects);
            model.addAttribute("groupedSubjects", groupedSubjects);

            log.debug("Loaded student {} with {} subjects", student.getFullName(), studentSubjects.size());
            return "view-student";
        } catch (EntityNotFoundException e) {
            log.warn("Student not found for viewing with id: {}", id);
            return "redirect:/students?error=notfound";
        } catch (Exception e) {
            log.error("Error viewing student with id: {}", id, e);
            return "redirect:/students?error=server_error";
        }
    }

    @GetMapping("/available-subjects")
    @ResponseBody
    public ResponseEntity<List<Subject>> getAvailableSubjects(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {
        try {
            List<Subject> subjects = studentService.getFilteredSubjectsForStudent(classCode, departmentId, specialty);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("Error getting available subjects for class: {}, department: {}, specialty: {}",
                    classCode, departmentId, specialty, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/grouped-subjects")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGroupedSubjects(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {

        try {
            log.info("Fetching grouped subjects for class: {}, department: {}, specialty: {}",
                    classCode, departmentId, specialty);

            Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(classCode, departmentId, specialty);

            log.info("Found {} compulsory, {} department, {} specialty, {} optional subjects",
                    groupedSubjects.getOrDefault("compulsory", List.of()).size(),
                    groupedSubjects.getOrDefault("department", List.of()).size(),
                    groupedSubjects.getOrDefault("specialty", List.of()).size(),
                    groupedSubjects.getOrDefault("optional", List.of()).size());

            // Return as a proper response object
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("groupedSubjects", groupedSubjects);
            response.put("compulsoryCount", groupedSubjects.getOrDefault("compulsory", List.of()).size());
            response.put("departmentCount", groupedSubjects.getOrDefault("department", List.of()).size());
            response.put("specialtyCount", groupedSubjects.getOrDefault("specialty", List.of()).size());
            response.put("optionalCount", groupedSubjects.getOrDefault("optional", List.of()).size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting grouped subjects for class: {}, department: {}, specialty: {}",
                    classCode, departmentId, specialty, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to load subjects: " + e.getMessage());
            errorResponse.put("groupedSubjects", Map.of(
                    "compulsory", List.of(),
                    "department", List.of(),
                    "specialty", List.of(),
                    "optional", List.of()
            ));

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<List<Student>> searchStudents(@RequestParam String query) {
        try {
            List<Student> students = studentService.searchStudents(query);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error searching students with query: {}", query, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{studentId}/subjects/{subjectId}/score")
    @ResponseBody
    public ResponseEntity<?> updateSubjectScore(
            @PathVariable Long studentId,
            @PathVariable Long subjectId,
            @RequestParam Double score) {
        try {
            studentEnrollmentService.updateStudentScore(studentId, subjectId, score);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            log.warn("Student or subject not found for score update: studentId={}, subjectId={}",
                    studentId, subjectId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating score for student: {}, subject: {}", studentId, subjectId, e);
            return ResponseEntity.badRequest().body("Error updating score");
        }
    }

    @GetMapping("/debug-subjects")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugSubjects(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {

        Map<String, Object> debugInfo = new HashMap<>();

        try {
            // Get all subjects
            List<Subject> allSubjects = subjectService.getAllSubjects();
            debugInfo.put("totalSubjectsInSystem", allSubjects.size());

            // Get filtered subjects
            List<Subject> filteredSubjects = subjectService.getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
            debugInfo.put("filteredSubjectsCount", filteredSubjects.size());

            // Get grouped subjects
            Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(classCode, departmentId, specialty);
            debugInfo.put("groupedSubjects", groupedSubjects);

            // Log subject details
            List<Map<String, Object>> subjectDetails = allSubjects.stream()
                    .map(subject -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("id", subject.getId());
                        details.put("name", subject.getName());
                        details.put("subjectCode", subject.getSubjectCode());
                        details.put("department", subject.getDepartment() != null ?
                                subject.getDepartment().getName() + " (" + subject.getDepartment().getCode() + ")" : "None");
                        details.put("specialty", subject.getSpecialty());
                        details.put("optional", subject.getOptional());
                        details.put("coefficient", subject.getCoefficient());
                        return details;
                    })
                    .collect(Collectors.toList());

            debugInfo.put("allSubjects", subjectDetails);

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(debugInfo);
        }
    }

    @GetMapping("/debug-subjects-s7")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugS7Subjects() {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            // Test LOWER_SIXTH + SCI + S7 combination
            String classCode = "LOWER_SIXTH";
            Long sciDepartmentId = departmentRepository.findByCode(DepartmentCode.SCI)
                    .map(Department::getId)
                    .orElse(1L); // Fallback to ID 1

            List<Subject> allSubjects = subjectService.getAllSubjects();
            List<Subject> filteredSubjects = subjectService.getSubjectsByClassDepartmentAndSpecialty(
                    classCode, sciDepartmentId, "S7");

            Map<String, List<Subject>> grouped = subjectService.getGroupedSubjectsForEnrollment(
                    classCode, sciDepartmentId, "S7");

            debugInfo.put("classCode", classCode);
            debugInfo.put("departmentId", sciDepartmentId);
            debugInfo.put("specialty", "S7");
            debugInfo.put("totalSubjects", allSubjects.size());
            debugInfo.put("filteredSubjects", filteredSubjects.size());
            debugInfo.put("groupedSubjects", grouped);

            // List all subjects with details
            List<Map<String, Object>> subjectDetails = allSubjects.stream()
                    .map(subject -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("id", subject.getId());
                        details.put("name", subject.getName());
                        details.put("subjectCode", subject.getSubjectCode());
                        details.put("department", subject.getDepartment() != null ?
                                subject.getDepartment().getName() + " (" + subject.getDepartment().getCode() + ")" : "None");
                        details.put("specialty", subject.getSpecialty());
                        details.put("optional", subject.getOptional());
                        details.put("coefficient", subject.getCoefficient());
                        return details;
                    })
                    .collect(Collectors.toList());

            debugInfo.put("allSubjects", subjectDetails);

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(debugInfo);
        }
    }

    // Helper methods

    private void populateModelAttributes(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());
        model.addAttribute("student", new Student());
    }

    private void populateAddModelAttributes(Model model, Student student) {
        model.addAttribute("student", student);
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());

        // Get grouped subjects based on current student configuration
        Map<String, List<Subject>> groupedSubjects = studentService.getGroupedAvailableSubjects(student);
        model.addAttribute("groupedSubjects", groupedSubjects);

        model.addAttribute("selectedSubjectIds", List.of());
    }

    private void populateEditModelAttributes(Model model, Student student) {
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());

        // Get grouped subjects based on current student configuration
        Map<String, List<Subject>> groupedSubjects = studentService.getGroupedAvailableSubjects(student);
        model.addAttribute("groupedSubjects", groupedSubjects);

        // Get current subject selections
        List<Long> selectedSubjectIds = studentService.getSelectedSubjectIds(student.getId());
        model.addAttribute("selectedSubjectIds", selectedSubjectIds);
    }

    @GetMapping("/check-specialty-requirements")
    @ResponseBody
    public ResponseEntity<SpecialtyService.SpecialtyRequirement> checkSpecialtyRequirements(
            @RequestParam String classCode,
            @RequestParam String departmentCode) {
        try {
            log.info("Checking specialty requirements for class: {}, department: {}", classCode, departmentCode);
            SpecialtyService.SpecialtyRequirement requirement = specialtyService.checkSpecialtyRequirement(classCode, departmentCode);
            return ResponseEntity.ok(requirement);
        } catch (Exception e) {
            log.error("Error checking specialty requirements for class: {}, department: {}", classCode, departmentCode, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudentStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", studentService.getStudentCount());

            // Get counts by class
            Map<String, Long> classCounts = new HashMap<>();
            List<ClassRoom> classes = classRoomRepository.findAll();
            for (ClassRoom cls : classes) {
                long count = studentRepository.countByClassRoom(cls);
                classCounts.put(cls.getName(), count);
            }
            stats.put("classDistribution", classCounts);

            // Get counts by department
            Map<String, Long> deptCounts = new HashMap<>();
            List<Department> departments = departmentRepository.findAll();
            for (Department dept : departments) {
                long count = studentRepository.findAll().stream()
                        .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(dept.getId()))
                        .count();
                deptCounts.put(dept.getName(), count);
            }
            stats.put("departmentDistribution", deptCounts);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting student statistics", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/debug-filtering")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugFiltering(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {

        Map<String, Object> debugInfo = new HashMap<>();

        try {
            List<Subject> allSubjects = subjectService.getAllSubjects();
            List<Subject> filteredSubjects = subjectService.getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);

            debugInfo.put("totalSubjects", allSubjects.size());
            debugInfo.put("filteredSubjects", filteredSubjects.size());
            debugInfo.put("classCode", classCode);
            debugInfo.put("departmentId", departmentId);
            debugInfo.put("specialty", specialty);

            // List all subjects with their properties
            List<Map<String, Object>> subjectDetails = allSubjects.stream()
                    .map(subject -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("id", subject.getId());
                        details.put("name", subject.getName());
                        details.put("subjectCode", subject.getSubjectCode());
                        details.put("department", subject.getDepartment() != null ?
                                subject.getDepartment().getName() + " (" + subject.getDepartment().getCode() + ")" : "None");
                        details.put("specialty", subject.getSpecialty());
                        details.put("optional", subject.getOptional());
                        details.put("coefficient", subject.getCoefficient());
                        return details;
                    })
                    .collect(Collectors.toList());

            debugInfo.put("allSubjects", subjectDetails);

            // List filtered subjects
            List<Map<String, Object>> filteredDetails = filteredSubjects.stream()
                    .map(subject -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("id", subject.getId());
                        details.put("name", subject.getName());
                        details.put("subjectCode", subject.getSubjectCode());
                        details.put("department", subject.getDepartment() != null ?
                                subject.getDepartment().getName() : "None");
                        return details;
                    })
                    .collect(Collectors.toList());

            debugInfo.put("filteredSubjectsDetails", filteredDetails);

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(debugInfo);
        }
    }
}