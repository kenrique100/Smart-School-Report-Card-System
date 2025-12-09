package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.util.DepartmentUtil;
import com.akentech.schoolreport.util.ParameterUtils;
import com.akentech.schoolreport.dto.GroupedSubjectsResponse;
import com.akentech.schoolreport.dto.SubjectDTO;
import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
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

import java.util.*;
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
    private final SubjectService subjectService;

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

            Long classRoomIdLong = ParameterUtils.safeParseLong(classRoomId);
            Long departmentIdLong = ParameterUtils.safeParseLong(departmentId);
            String cleanedSpecialty = ParameterUtils.cleanString(specialty);

            Page<Student> studentPage = studentService.getStudentsByFilters(
                    null, null, classRoomIdLong, departmentIdLong, cleanedSpecialty, pageable);

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

            model.addAttribute("classRoomIdFilter", classRoomIdLong);
            model.addAttribute("departmentIdFilter", departmentIdLong);
            model.addAttribute("specialtyFilter", cleanedSpecialty);

            return "students";
        } catch (Exception e) {
            log.error("Error loading students list", e);
            model.addAttribute("error", "Unable to load students");
            populateModelAttributes(model);
            return "students";
        }
    }

    @GetMapping("/sort")
    public String sortStudents(
            @RequestParam String sortBy,
            @RequestParam(required = false) String currentSortDir,
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer currentSize,
            @RequestParam(required = false) String classRoomId,
            @RequestParam(required = false) String departmentId,
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

        if (classRoomId != null && !classRoomId.equals("null")) {
            redirectAttributes.addAttribute("classRoomId", classRoomId);
        }
        if (departmentId != null && !departmentId.equals("null")) {
            redirectAttributes.addAttribute("departmentId", departmentId);
        }
        if (specialty != null && !specialty.equals("null")) {
            redirectAttributes.addAttribute("specialty", specialty);
        }

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

        ClassRoom defaultClass = findDefaultClass(classRooms, classLevel);
        Department defaultDepartment = findDefaultDepartment(departments, departmentCode);

        // Make sure default class has a valid code
        if (defaultClass != null && defaultClass.getCode() == null) {
            log.warn("Default class has null code: {}", defaultClass.getName());
            // Find another class with valid code
            defaultClass = classRooms.stream()
                    .filter(cr -> cr.getCode() != null)
                    .findFirst()
                    .orElse(null);
        }

        if (defaultClass != null) {
            student.setClassRoom(defaultClass);
        } else if (!classRooms.isEmpty()) {
            // Find first class with valid code
            ClassRoom firstValidClass = classRooms.stream()
                    .filter(cr -> cr.getCode() != null)
                    .findFirst()
                    .orElse(null);
            if (firstValidClass != null) {
                student.setClassRoom(firstValidClass);
            }
        }

        if (defaultDepartment != null) {
            student.setDepartment(defaultDepartment);
        } else if (!departments.isEmpty()) {
            student.setDepartment(departments.getFirst());
        }

        if (specialty != null && !specialty.trim().isEmpty() && !"null".equals(specialty)) {
            student.setSpecialty(specialty);
        }

        Map<String, List<Subject>> groupedSubjects = new HashMap<>();
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null && student.getDepartment() != null) {
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
                student.getClassRoom() != null ? student.getClassRoom().getName() : "None",
                student.getDepartment() != null ? student.getDepartment().getName() : "None",
                student.getSpecialty(),
                groupedSubjects.getOrDefault("compulsory", List.of()).size(),
                groupedSubjects.getOrDefault("department", List.of()).size(),
                groupedSubjects.getOrDefault("specialty", List.of()).size(),
                groupedSubjects.getOrDefault("optional", List.of()).size());

        return "add-student";
    }

    private ClassRoom findDefaultClass(List<ClassRoom> classRooms, String classLevel) {
        if (classLevel != null && !classLevel.equals("null")) {
            return classRooms.stream()
                    .filter(cr -> cr.getCode().name().equalsIgnoreCase(classLevel))
                    .findFirst()
                    .orElse(null);
        }
        return classRooms.stream()
                .filter(cr -> cr.getCode() == ClassLevel.LOWER_SIXTH)
                .findFirst()
                .orElse(null);
    }

    private Department findDefaultDepartment(List<Department> departments, String departmentCode) {
        if (departmentCode != null && !departmentCode.equals("null")) {
            return departments.stream()
                    .filter(d -> d.getCode().name().equalsIgnoreCase(departmentCode))
                    .findFirst()
                    .orElse(null);
        }
        return departments.stream()
                .filter(d -> d.getCode() == DepartmentCode.SCI)
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/save")
    public String saveStudent(@Valid @ModelAttribute("student") Student student,
                              BindingResult result,
                              @RequestParam(value = "subjectIds", required = false) String subjectIdsParam,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateAddModelAttributes(model, student);
            return "add-student";
        }

        try {
            List<Long> subjectIds = new ArrayList<>();
            if (subjectIdsParam != null && !subjectIdsParam.trim().isEmpty()) {
                String[] ids = subjectIdsParam.split(",");
                for (String id : ids) {
                    try {
                        subjectIds.add(Long.parseLong(id.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid subject ID: {}", id);
                    }
                }
            }

            Student savedStudent = studentService.createStudent(student, subjectIds);
            log.info("Successfully created student with ID: {}", savedStudent.getStudentId());

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
            Student updatedStudent = studentService.updateStudent(id, student, subjectIds);
            log.info("Successfully updated student with ID: {}", updatedStudent.getStudentId());

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

            Map<String, Object> subjectsSummary = studentEnrollmentService.getStudentEnrollmentSummary(id);

            Map<String, List<Subject>> groupedSubjects = studentService.getGroupedSubjectsForStudent(id);

            log.info("📊 Loading student {} {} (ID: {}) with {} subjects",
                    student.getFirstName(), student.getLastName(),
                    student.getStudentId(), studentSubjects.size());

            log.info("📊 Student class: {}, department: {}, specialty: {}",
                    student.getClassRoom() != null ? student.getClassRoom().getName() : "None",
                    student.getDepartment() != null ? student.getDepartment().getName() : "None",
                    student.getSpecialty() != null ? student.getSpecialty() : "None");

            model.addAttribute("student", student);
            model.addAttribute("subjectsSummary", subjectsSummary);
            model.addAttribute("studentSubjects", studentSubjects);
            model.addAttribute("groupedSubjects", groupedSubjects);

            return "view-student";
        } catch (EntityNotFoundException e) {
            log.warn("Student not found for viewing with id: {}", id);
            return "redirect:/students?error=notfound";
        } catch (Exception e) {
            log.error("Error viewing student with id: {}", id, e);
            return "redirect:/students?error=server_error";
        }
    }

    @GetMapping("/grouped-subjects")
    @ResponseBody
    public ResponseEntity<GroupedSubjectsResponse> getGroupedSubjects(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {

        try {
            log.info("Fetching grouped subjects for class: {}, department: {}, specialty: {}",
                    classCode, departmentId, specialty);

            Map<String, List<Subject>> groupedSubjects =
                    subjectService.getGroupedSubjectsForEnrollment(classCode, departmentId, specialty);

            Map<String, List<SubjectDTO>> groupedSubjectsDTO = convertToDTO(groupedSubjects);

            GroupedSubjectsResponse response = new GroupedSubjectsResponse();
            response.setSuccess(true);
            response.setGroupedSubjects(groupedSubjectsDTO);
            response.setCompulsoryCount(groupedSubjects.getOrDefault("compulsory", List.of()).size());
            response.setDepartmentCount(groupedSubjects.getOrDefault("department", List.of()).size());
            response.setSpecialtyCount(groupedSubjects.getOrDefault("specialty", List.of()).size());
            response.setOptionalCount(groupedSubjects.getOrDefault("optional", List.of()).size());
            response.setMessage("Subjects loaded successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting grouped subjects", e);
            return buildGroupedSubjectErrorResponse(e);
        }
    }

    private Map<String, List<SubjectDTO>> convertToDTO(Map<String, List<Subject>> groupedSubjects) {
        return groupedSubjects.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(SubjectDTO::fromEntity)
                                .collect(Collectors.toList())
                ));
    }

    private ResponseEntity<GroupedSubjectsResponse> buildGroupedSubjectErrorResponse(Exception e) {
        GroupedSubjectsResponse errorResponse = new GroupedSubjectsResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage("Failed to load subjects: " + e.getMessage());

        errorResponse.setGroupedSubjects(Map.of(
                "compulsory", List.of(),
                "department", List.of(),
                "specialty", List.of(),
                "optional", List.of()
        ));

        errorResponse.setCompulsoryCount(0);
        errorResponse.setDepartmentCount(0);
        errorResponse.setSpecialtyCount(0);
        errorResponse.setOptionalCount(0);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @GetMapping("/check-specialty-requirements")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkSpecialtyRequirements(
            @RequestParam String classCode,
            @RequestParam String departmentCode) {
        try {
            log.info("Checking specialty requirements for class: {}, department: {}", classCode, departmentCode);

            // Use DepartmentUtil to get specialties
            List<String> specialties = DepartmentUtil.getSpecialtiesForDepartment(departmentCode);
            boolean hasSpecialties = DepartmentUtil.hasSpecialties(departmentCode);

            // Check if sixth form
            boolean isSixthForm = classCode.equals("LOWER_SIXTH") || classCode.equals("UPPER_SIXTH");

            // Determine requirements
            boolean required = isSixthForm && (departmentCode.equals("SCI") || departmentCode.equals("ART"));
            boolean allowed = !classCode.equals("FORM_1") && !classCode.equals("FORM_2") &&
                    !classCode.equals("FORM_3") && hasSpecialties;

            Map<String, Object> response = new HashMap<>();
            response.put("required", required);
            response.put("allowed", allowed);
            response.put("hasSpecialties", hasSpecialties);
            response.put("specialties", specialties);
            response.put("specialtiesCount", specialties.size());

            String message;
            if (!hasSpecialties) {
                message = "This department does not have specialties";
            } else if (required) {
                message = "Specialty is required for " + classCode + " " + departmentCode;
            } else if (allowed) {
                message = "Specialty is optional";
            } else {
                message = "Specialty is not allowed for this class";
            }
            response.put("message", message);

            log.info("Specialty requirement check completed: {}", message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking specialty requirements for class: {}, department: {}", classCode, departmentCode, e);
            return ResponseEntity.badRequest().build();
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

    @GetMapping("/{studentId}/available-subjects")
    @ResponseBody
    public ResponseEntity<List<SubjectDTO>> getAvailableSubjectsForEnrollment(@PathVariable Long studentId) {
        try {
            List<Subject> availableSubjects = studentEnrollmentService.getAvailableSubjectsForEnrollment(studentId);
            List<SubjectDTO> subjectDTOs = availableSubjects.stream()
                    .map(SubjectDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(subjectDTOs);
        } catch (Exception e) {
            log.error("Error getting available subjects for student: {}", studentId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{studentId}/enroll-subjects")
    @ResponseBody
    public ResponseEntity<?> enrollStudentInSubjects(
            @PathVariable Long studentId,
            @RequestBody List<Long> subjectIds) {
        try {
            studentEnrollmentService.enrollStudentInAdditionalSubjects(studentId, subjectIds);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            log.warn("Student not found for enrollment: {}", studentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error enrolling student {} in subjects: {}", studentId, subjectIds, e);
            return ResponseEntity.badRequest().body("Error enrolling in subjects: " + e.getMessage());
        }
    }

    @DeleteMapping("/{studentId}/subjects/{subjectId}/unenroll")
    @ResponseBody
    public ResponseEntity<?> unenrollStudentFromSubject(
            @PathVariable Long studentId,
            @PathVariable Long subjectId) {
        try {
            studentEnrollmentService.removeStudentFromSubject(studentId, subjectId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error unenrolling student {} from subject {}", studentId, subjectId, e);
            return ResponseEntity.badRequest().body("Error removing subject enrollment");
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
            List<Subject> allSubjects = subjectService.getAllSubjects();
            debugInfo.put("totalSubjectsInSystem", allSubjects.size());

            List<Subject> filteredSubjects = subjectService.getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
            debugInfo.put("filteredSubjectsCount", filteredSubjects.size());

            Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(classCode, departmentId, specialty);
            debugInfo.put("groupedSubjects", groupedSubjects);

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

    @GetMapping("/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudentStatistics() {
        try {
            Map<String, Object> stats = studentService.getStudentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting student statistics", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/debug-filter")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugFilter(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Subject> allSubjects = subjectService.getAllSubjects();
            response.put("totalSubjects", allSubjects.size());

            Optional<Department> department = departmentRepository.findById(departmentId);
            response.put("requestedDepartmentId", departmentId);
            response.put("requestedDepartmentName", department.map(Department::getName).orElse("Not Found"));
            response.put("requestedDepartmentCode", department.map(Department::getCode).orElse(null));

            List<Map<String, Object>> scienceSubjects = allSubjects.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(2L))
                    .map(this::buildSubjectDebugInfo)
                    .collect(Collectors.toList());

            response.put("scienceSubjectsCount", scienceSubjects.size());
            response.put("scienceSubjects", scienceSubjects);

            List<Map<String, Object>> subjectsInfo = allSubjects.stream()
                    .map(this::buildSubjectDebugInfo)
                    .collect(Collectors.toList());

            response.put("subjectsInfo", subjectsInfo);

            List<Subject> filtered = subjectService.getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
            response.put("filteredCount", filtered.size());
            response.put("filteredSubjects", filtered.stream()
                    .map(s -> s.getName() + " (Dept: " +
                            (s.getDepartment() != null ? s.getDepartment().getName() : "None") +
                            ", Specialty: " + s.getSpecialty() + ")")
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> buildSubjectDebugInfo(Subject s) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", s.getId());
        info.put("name", s.getName());
        info.put("subjectCode", s.getSubjectCode());
        info.put("departmentId", s.getDepartment() != null ? s.getDepartment().getId() : null);
        info.put("departmentName", s.getDepartment() != null ? s.getDepartment().getName() : null);
        info.put("departmentCode", s.getDepartment() != null ? s.getDepartment().getCode() : null);
        info.put("specialty", s.getSpecialty());
        info.put("optional", s.getOptional());
        return info;
    }

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

        Map<String, List<Subject>> groupedSubjects;

        // Check if student has valid class and department before calling service
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null &&
                student.getDepartment() != null) {
            groupedSubjects = studentService.getGroupedAvailableSubjects(student);
        } else {
            groupedSubjects = new HashMap<>();
            groupedSubjects.put("compulsory", new ArrayList<>());
            groupedSubjects.put("department", new ArrayList<>());
            groupedSubjects.put("specialty", new ArrayList<>());
            groupedSubjects.put("optional", new ArrayList<>());
        }

        model.addAttribute("groupedSubjects", groupedSubjects);
        model.addAttribute("selectedSubjectIds", List.of());
    }

    private void populateEditModelAttributes(Model model, Student student) {
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());

        Map<String, List<Subject>> groupedSubjects = studentService.getGroupedAvailableSubjects(student);
        model.addAttribute("groupedSubjects", groupedSubjects);

        List<Long> selectedSubjectIds = studentService.getSelectedSubjectIds(student.getId());
        model.addAttribute("selectedSubjectIds", selectedSubjectIds);
    }
}