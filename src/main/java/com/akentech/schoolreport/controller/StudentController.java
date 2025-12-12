package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.*;
import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.*;
import com.akentech.schoolreport.util.DepartmentUtil;
import com.akentech.schoolreport.util.ParameterUtils;
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
    private final AssessmentService assessmentService;
    private final GradeService gradeService;
    private final ReportService reportService;

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
            classRooms.stream()
                    .filter(cr -> cr.getCode() != null)
                    .findFirst().ifPresent(student::setClassRoom);
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
                        log.warn("An Invalid subject ID: {}", id);
                    }
                }
            }

            log.info("📋 Student {} will be enrolled in {} subjects: {}",
                    student.getFullName(), subjectIds.size(), subjectIds);

            Student savedStudent = studentService.createStudent(student, subjectIds);
            log.info("✅ Successfully created student with ID: {}", savedStudent.getStudentId());

            redirectAttributes.addFlashAttribute("success",
                    "Student created successfully! Enrolled in " + subjectIds.size() + " subjects.");
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

            // Ensure studentSubjects is never null
            model.addAttribute("studentSubjects", studentSubjects != null ? studentSubjects : new ArrayList<>());

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
                                @RequestParam(value = "subjectIds", required = false) String subjectIdsParam,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateEditModelAttributes(model, student);
            return "edit-student";
        }

        try {
            // Parse subject IDs from comma-separated string
            List<Long> subjectIds = new ArrayList<>();
            if (subjectIdsParam != null && !subjectIdsParam.trim().isEmpty()) {
                String[] ids = subjectIdsParam.split(",");
                for (String idStr : ids) {
                    try {
                        subjectIds.add(Long.parseLong(idStr.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid subject ID: {}", idStr);
                    }
                }
            }

            Student updatedStudent = studentService.updateStudent(id, student, subjectIds);
            log.info("✅ Successfully updated student with ID: {}", updatedStudent.getStudentId());

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
    public String viewStudent(@PathVariable Long id,
                              @RequestParam(value = "term", defaultValue = "1") Integer term,
                              Model model) {
        try {
            Student student = studentService.getStudentByIdOrThrow(id);
            List<StudentSubject> studentSubjects = studentService.getStudentSubjects(id);

            Map<String, Object> subjectsSummary = studentEnrollmentService.getStudentEnrollmentSummary(id);

            // Get assessment summary with ranks
            StudentAssessmentSummaryDTO assessmentSummary = getAssessmentSummary(id);

            // Calculate and set term rank
            Integer termRank = calculateStudentTermRank(id, student.getClassRoom().getId(), term);
            assessmentSummary.setTermRank(termRank);

            // Calculate and set yearly rank
            Integer yearlyRank = calculateStudentYearlyRank(id, student.getClassRoom().getId());
            assessmentSummary.setClassRank(yearlyRank);

            model.addAttribute("assessmentSummary", assessmentSummary);

            // Get term assessments
            List<TermAssessmentDTO> termAssessments = getTermAssessments(id, term);
            model.addAttribute("termAssessments", termAssessments);
            model.addAttribute("currentTerm", term);

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

            // Get enrolled subjects for optional display (remove if not needed)
            List<Subject> enrolledSubjects = studentEnrollmentService.getStudentEnrollments(id).stream()
                    .map(StudentSubject::getSubject)
                    .distinct()
                    .collect(Collectors.toList());
            model.addAttribute("enrolledSubjects", enrolledSubjects);

            return "view-student";
        } catch (EntityNotFoundException e) {
            log.warn("Student not found for viewing with id: {}", id);
            return "redirect:/students?error=notfound";
        } catch (Exception e) {
            log.error("Error viewing student with id: {}", id, e);
            return "redirect:/students?error=server_error";
        }
    }

    private Integer calculateStudentTermRank(Long studentId, Long classId, Integer term) {
        try {
            List<StudentTermAverageDTO> termAverages = assessmentService.getTermAveragesForClass(classId, term);

            for (int i = 0; i < termAverages.size(); i++) {
                if (termAverages.get(i).getStudent().getId().equals(studentId)) {
                    return i + 1; // Rank is 1-based
                }
            }
        } catch (Exception e) {
            log.error("Error calculating term rank for student {} term {}: {}", studentId, term, e.getMessage());
        }
        return null;
    }

    private Integer calculateStudentYearlyRank(Long studentId, Long classId) {
        try {
            List<StudentYearlyAverageDTO> yearlyAverages = assessmentService.getYearlyAveragesForClass(classId);

            for (int i = 0; i < yearlyAverages.size(); i++) {
                if (yearlyAverages.get(i).getStudent().getId().equals(studentId)) {
                    return i + 1; // Rank is 1-based
                }
            }
        } catch (Exception e) {
            log.error("Error calculating yearly rank for student {}: {}", studentId, e.getMessage());
        }
        return null;
    }

    private StudentAssessmentSummaryDTO getAssessmentSummary(Long studentId) {
        Student student = studentService.getStudentByIdOrThrow(studentId);

        // Get all assessments
        List<Assessment> assessments = assessmentService.getAssessmentsByStudent(studentId);

        // Group by term
        Map<Integer, List<Assessment>> assessmentsByTerm = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        // Calculate term averages
        Double term1Avg = assessmentService.calculateTermAverage(studentId, 1);
        Double term2Avg = assessmentService.calculateTermAverage(studentId, 2);
        Double term3Avg = assessmentService.calculateTermAverage(studentId, 3);

        // Calculate yearly average
        Double yearlyAvg = assessmentService.calculateYearlyAverage(studentId);

        // Get enrolled subjects count
        List<StudentSubject> enrolledSubjects = studentEnrollmentService.getStudentEnrollments(studentId);
        int totalSubjects = enrolledSubjects.size();

        // Check completion status for each term
        boolean term1Completed = checkTermCompletion(assessmentsByTerm.get(1), totalSubjects);
        boolean term2Completed = checkTermCompletion(assessmentsByTerm.get(2), totalSubjects);
        boolean term3Completed = checkTermCompletion(assessmentsByTerm.get(3), totalSubjects);

        // Calculate rank-related statistics
        int term1CompletedSubjects = countCompletedSubjects(assessmentsByTerm.get(1));
        int term2CompletedSubjects = countCompletedSubjects(assessmentsByTerm.get(2));
        int term3CompletedSubjects = countCompletedSubjects(assessmentsByTerm.get(3));

        return StudentAssessmentSummaryDTO.builder()
                .studentId(studentId)
                .studentName(student.getFullName())
                .totalAssessments(assessments.size())
                .term1Assessments(assessmentsByTerm.getOrDefault(1, List.of()).size())
                .term2Assessments(assessmentsByTerm.getOrDefault(2, List.of()).size())
                .term3Assessments(assessmentsByTerm.getOrDefault(3, List.of()).size())
                .term1Average(term1Avg)
                .term2Average(term2Avg)
                .term3Average(term3Avg)
                .yearlyAverage(yearlyAvg)
                .totalSubjects(totalSubjects)
                .term1CompletedSubjects(term1CompletedSubjects)
                .term2CompletedSubjects(term2CompletedSubjects)
                .term3CompletedSubjects(term3CompletedSubjects)
                .term1Completed(term1Completed)
                .term2Completed(term2Completed)
                .term3Completed(term3Completed)
                .termRank(null) // Will be set by caller
                .classRank(null) // Will be set by caller
                .build();
    }

    private List<TermAssessmentDTO> getTermAssessments(Long studentId, Integer term) {
        List<StudentSubject> enrolledSubjects = studentEnrollmentService.getStudentEnrollments(studentId);
        List<TermAssessmentDTO> termAssessments = new ArrayList<>();

        for (StudentSubject enrollment : enrolledSubjects) {
            Subject subject = enrollment.getSubject();

            // Get assessments for this subject and term
            List<Assessment> subjectAssessments = assessmentService.getAssessmentsByStudentSubjectAndTerm(
                    studentId, subject.getId(), term);

            // Group by assessment type/number
            Map<Integer, Double> scoresByAssessment = new HashMap<>();
            for (Assessment assessment : subjectAssessments) {
                scoresByAssessment.put(assessment.getType().getAssessmentNumber(), assessment.getScore());
            }

            // Calculate term average
            Double termAverage = calculateSubjectTermAverage(scoresByAssessment, term);
            String termGrade = termAverage != null ?
                    gradeService.calculateLetterGrade(termAverage, subject.getName()) : null;

            TermAssessmentDTO dto = TermAssessmentDTO.builder()
                    .subjectId(subject.getId())
                    .subjectName(subject.getName())
                    .subjectCode(subject.getSubjectCode())
                    .coefficient(subject.getCoefficient())
                    .assessment1Score(scoresByAssessment.get(1))
                    .assessment2Score(scoresByAssessment.get(2))
                    .assessment3Score(scoresByAssessment.get(3))
                    .assessment4Score(scoresByAssessment.get(4))
                    .assessment5Score(scoresByAssessment.get(5))
                    .termAverage(termAverage)
                    .termGrade(termGrade)
                    .completed(termAverage != null)
                    .build();

            termAssessments.add(dto);
        }

        // Sort by subject name
        termAssessments.sort(Comparator.comparing(TermAssessmentDTO::getSubjectName));

        return termAssessments;
    }

    private Double calculateSubjectTermAverage(Map<Integer, Double> scores, Integer term) {
        if (scores.isEmpty()) {
            return null;
        }

        return switch (term) {
            case 1 -> {
                // Term 1: Average of Assessment 1 and 2
                Double a1 = scores.get(1);
                Double a2 = scores.get(2);
                yield (a1 != null && a2 != null) ? (a1 + a2) / 2.0 : null;
            }
            case 2 -> {
                // Term 2: Average of Assessment 3 and 4
                Double a3 = scores.get(3);
                Double a4 = scores.get(4);
                yield (a3 != null && a4 != null) ? (a3 + a4) / 2.0 : null;
            }
            case 3 -> {
                // Term 3: Only Assessment 5
                yield scores.get(5);
            }
            default -> null;
        };
    }

    private boolean checkTermCompletion(List<Assessment> assessments, int totalSubjects) {
        if (assessments == null) {
            return false;
        }

        // Count unique subjects with assessments
        long assessedSubjects = assessments.stream()
                .map(a -> a.getSubject().getId())
                .distinct()
                .count();

        return assessedSubjects == totalSubjects;
    }

    private int countCompletedSubjects(List<Assessment> assessments) {
        if (assessments == null) {
            return 0;
        }

        return (int) assessments.stream()
                .map(a -> a.getSubject().getId())
                .distinct()
                .count();
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

            // Convert to DTOs
            Map<String, List<SubjectDTO>> groupedSubjectsDTO = new LinkedHashMap<>();
            groupedSubjects.forEach((key, value) -> groupedSubjectsDTO.put(key, value.stream()
                    .map(this::convertToSubjectDTO)
                    .collect(Collectors.toList())));

            GroupedSubjectsResponse response = new GroupedSubjectsResponse();
            response.setSuccess(true);
            response.setGroupedSubjects(groupedSubjectsDTO);
            response.setCompulsoryCount(groupedSubjects.getOrDefault("compulsory", List.of()).size());
            response.setDepartmentCount(groupedSubjects.getOrDefault("department", List.of()).size());
            response.setSpecialtyCount(groupedSubjects.getOrDefault("specialty", List.of()).size());
            response.setOptionalCount(groupedSubjects.getOrDefault("optional", List.of()).size());
            response.setMessage("Subjects loaded successfully");

            log.info("📊 Returning grouped subjects - Compulsory: {}, Department: {}, Specialty: {}, Optional: {}",
                    response.getCompulsoryCount(), response.getDepartmentCount(),
                    response.getSpecialtyCount(), response.getOptionalCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting grouped subjects", e);
            return buildGroupedSubjectErrorResponse(e);
        }
    }

    private SubjectDTO convertToSubjectDTO(Subject subject) {
        SubjectDTO dto = new SubjectDTO();
        dto.setId(subject.getId());
        dto.setName(subject.getName());
        dto.setSubjectCode(subject.getSubjectCode());
        dto.setCoefficient(subject.getCoefficient());
        dto.setOptional(subject.getOptional());
        dto.setSpecialty(subject.getSpecialty());
        dto.setDescription(subject.getDescription());

        if (subject.getDepartment() != null) {
            dto.setDepartmentId(subject.getDepartment().getId());
            dto.setDepartmentName(subject.getDepartment().getName());
        }

        return dto;
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

            // Determine requirements - ONLY enable specialty if the department has specialties
            boolean required = isSixthForm && (departmentCode.equals("SCI") || departmentCode.equals("ART"));

            Map<String, Object> response = new HashMap<>();
            response.put("required", required);
            response.put("allowed", hasSpecialties);
            response.put("hasSpecialties", hasSpecialties);
            response.put("specialties", specialties);
            response.put("specialtiesCount", specialties.size());

            String message;
            if (!hasSpecialties) {
                message = "This department does not have specialties";
            } else if (required) {
                message = "Specialty is required for " + classCode + " " + departmentCode;
            } else {
                message = "Specialty is optional";
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