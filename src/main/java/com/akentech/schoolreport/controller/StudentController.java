package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.StudentSubject;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.SpecialtyService;
import com.akentech.schoolreport.service.StudentEnrollmentService;
import com.akentech.schoolreport.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final SpecialtyService specialtyService;
    private final StudentEnrollmentService studentEnrollmentService;

    @GetMapping
    public String listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String classRoomId, // Changed to String
            @RequestParam(required = false) String departmentId, // Changed to String
            @RequestParam(required = false) String specialty,
            Model model) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // FIXED: Handle "null" string conversion properly
            Long classRoomIdLong = null;
            if (classRoomId != null && !classRoomId.equals("null") && !classRoomId.trim().isEmpty()) {
                try {
                    classRoomIdLong = Long.parseLong(classRoomId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid classRoomId provided: {}", classRoomId);
                }
            }

            Long departmentIdLong = null;
            if (departmentId != null && !departmentId.equals("null") && !departmentId.trim().isEmpty()) {
                try {
                    departmentIdLong = Long.parseLong(departmentId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid departmentId provided: {}", departmentId);
                }
            }

            // Always show all students - remove firstName and lastName filters
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
    public String showAddForm(Model model) {
        Student student = new Student();
        model.addAttribute("student", student);
        model.addAttribute("classRooms", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());

        model.addAttribute("groupedSubjects", Map.of(
                "compulsory", List.of(),
                "department", List.of(),
                "optional", List.of()
        ));
        model.addAttribute("selectedSubjectIds", List.of());

        return "add-student";
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
            List<Long> selectedSubjectIds = studentSubjects.stream()
                    .map(ss -> ss.getSubject().getId())
                    .collect(Collectors.toList());

            Map<String, List<Subject>> groupedSubjects = studentService.getGroupedAvailableSubjects(student);

            model.addAttribute("student", student);
            model.addAttribute("classRooms", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", studentService.getAllSpecialties());
            model.addAttribute("groupedSubjects", groupedSubjects);
            model.addAttribute("selectedSubjectIds", selectedSubjectIds);
            model.addAttribute("studentSubjects", studentSubjects);

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

    @GetMapping("/check-specialty-requirements")
    @ResponseBody
    public SpecialtyService.SpecialtyRequirement checkSpecialtyRequirements(
            @RequestParam String classCode,
            @RequestParam String departmentCode) {
        return studentService.checkSpecialtyRequirement(classCode, departmentCode);
    }

    @GetMapping("/available-subjects")
    @ResponseBody
    public List<Subject> getAvailableSubjects(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {
        return studentService.getAvailableSubjects(classCode, departmentId, specialty);
    }

    @GetMapping("/grouped-subjects")
    @ResponseBody
    public Map<String, List<Subject>> getGroupedSubjects(
            @RequestParam String classCode,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String specialty) {

        Student mockStudent = Student.builder()
                .classRoom(com.akentech.schoolreport.model.ClassRoom.builder()
                        .code(com.akentech.schoolreport.model.enums.ClassLevel.fromCode(classCode))
                        .build())
                .department(com.akentech.schoolreport.model.Department.builder().id(departmentId).build())
                .specialty(specialty)
                .build();

        return studentService.getGroupedAvailableSubjects(mockStudent);
    }

    @GetMapping("/search")
    @ResponseBody
    public List<Student> searchStudents(@RequestParam String query) {
        return studentService.searchStudents(query);
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
        model.addAttribute("groupedSubjects", Map.of(
                "compulsory", List.of(),
                "department", List.of(),
                "optional", List.of()
        ));
        model.addAttribute("selectedSubjectIds", List.of());
    }

    private void populateEditModelAttributes(Model model, Student student) {
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());

        Map<String, List<Subject>> groupedSubjects = studentService.getGroupedAvailableSubjects(student);
        model.addAttribute("groupedSubjects", groupedSubjects);

        List<StudentSubject> studentSubjects = studentService.getStudentSubjects(student.getId());
        List<Long> selectedSubjectIds = studentSubjects.stream()
                .map(ss -> ss.getSubject().getId())
                .collect(Collectors.toList());
        model.addAttribute("selectedSubjectIds", selectedSubjectIds);
    }
}