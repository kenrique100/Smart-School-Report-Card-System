package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.StudentTermAverageDTO;
import com.akentech.schoolreport.dto.StudentYearlyAverageDTO;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.AssessmentType;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.AssessmentService;
import com.akentech.schoolreport.service.GradeService;
import com.akentech.schoolreport.service.StudentEnrollmentService;
import com.akentech.schoolreport.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assessments")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentService studentEnrollmentService;
    private final StudentService studentService;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final GradeService gradeService;

    @GetMapping("/entry")
    public String entryForm(Model model,
                            @RequestParam(value = "classRoomId", required = false) Long classRoomId,
                            @RequestParam(value = "studentId", required = false) Long studentId,
                            @RequestParam(value = "term", required = false, defaultValue = "1") Integer term,
                            @RequestParam(value = "assessmentNumber", required = false) Integer assessmentNumber,
                            @RequestParam(value = "departmentId", required = false) Long departmentId,
                            @RequestParam(value = "specialty", required = false) String specialty) {

        // Add all classes and departments for filtering
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());

        // Add filter values to model
        model.addAttribute("classRoomId", classRoomId);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("specialty", specialty);
        model.addAttribute("term", term);
        model.addAttribute("terms", List.of(1, 2, 3));

        // Get available assessments for the selected term
        AssessmentType[] availableAssessments = AssessmentType.getAssessmentsForTerm(term);
        List<Integer> availableAssessmentNumbers = Arrays.stream(availableAssessments)
                .map(AssessmentType::getAssessmentNumber)
                .collect(Collectors.toList());

        model.addAttribute("assessmentNumbers", availableAssessmentNumbers);

        // Set default assessment number if not provided or invalid
        if (assessmentNumber == null || !availableAssessmentNumbers.contains(assessmentNumber)) {
            assessmentNumber = availableAssessmentNumbers.get(0);
        }
        model.addAttribute("assessmentNumber", assessmentNumber);

        // Get current assessment type
        AssessmentType currentAssessmentType = null;
        try {
            currentAssessmentType = AssessmentType.fromTermAndNumber(term, assessmentNumber);
        } catch (Exception e) {
            log.warn("Invalid assessment type for term {}, number {}", term, assessmentNumber);
            // Default to first available assessment for the term
            currentAssessmentType = availableAssessments[0];
            model.addAttribute("assessmentNumber", currentAssessmentType.getAssessmentNumber());
        }
        model.addAttribute("currentAssessmentType", currentAssessmentType);
        model.addAttribute("assessmentName", currentAssessmentType != null ?
                currentAssessmentType.getDisplayName() : "Assessment");

        // Get filtered students based on class, department, specialty
        List<Student> filteredStudents;
        if (classRoomId != null || departmentId != null || (specialty != null && !specialty.isEmpty())) {
            filteredStudents = getFilteredStudents(classRoomId, departmentId, specialty);
        } else {
            filteredStudents = studentRepository.findAll();
        }

        model.addAttribute("students", filteredStudents);

        // Load subjects and existing marks if studentId is provided
        if (studentId != null) {
            try {
                Student student = studentService.getStudentByIdOrThrow(studentId);

                // Add student details to model
                model.addAttribute("selectedStudent", student);
                model.addAttribute("selectedStudentId", studentId);

                // Get subjects for the student (enrolled subjects)
                List<Subject> studentSubjects = getSubjectsForStudent(studentId);

                if (studentSubjects.isEmpty()) {
                    model.addAttribute("warningMessage", "This student is not enrolled in any subjects.");
                }

                model.addAttribute("subjects", studentSubjects);

                // Get existing assessment for each subject in the selected term and assessment number
                Map<Long, Assessment> existingAssessments = new HashMap<>();
                if (currentAssessmentType != null) {
                    for (Subject subject : studentSubjects) {
                        Optional<Assessment> existingAssessment = assessmentService.getAssessmentByStudentSubjectAndTermAndType(
                                studentId, subject.getId(), term, currentAssessmentType);
                        existingAssessment.ifPresent(assessment -> existingAssessments.put(subject.getId(), assessment));
                    }
                }

                model.addAttribute("existingAssessments", existingAssessments);

                log.info("Loaded {} subjects and {} existing assessments for student {} in term {}, assessment {}",
                        studentSubjects.size(), existingAssessments.size(), studentId, term, assessmentNumber);

            } catch (Exception e) {
                log.error("Error loading subjects for student {}", studentId, e);
                model.addAttribute("subjects", List.of());
                model.addAttribute("errorMessage", "Error loading student subjects: " + e.getMessage());
            }
        } else {
            model.addAttribute("subjects", List.of());
            model.addAttribute("selectedStudentId", null);
            model.addAttribute("existingAssessments", new HashMap<>());
        }

        return "assessments";
    }

    @PostMapping("/save-batch")
    public String saveBatch(@RequestParam Long studentId,
                            @RequestParam Integer term,
                            @RequestParam Integer assessmentNumber,
                            @RequestParam(value = "classRoomId", required = false) Long classRoomId,
                            @RequestParam(value = "departmentId", required = false) Long departmentId,
                            @RequestParam(value = "specialty", required = false) String specialty,
                            @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                            @RequestParam(value = "scores", required = false) List<Double> scores,
                            RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentByIdOrThrow(studentId);
            List<Assessment> assessments = new ArrayList<>();

            // Validate assessment type before proceeding
            AssessmentType assessmentType;
            try {
                assessmentType = AssessmentType.fromTermAndNumber(term, assessmentNumber);
            } catch (IllegalArgumentException e) {
                log.error("Invalid assessment combination: term={}, assessment={}", term, assessmentNumber);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid assessment combination. Term " + term + " only accepts: " +
                                getValidAssessmentsForTerm(term));

                // Preserve filter parameters
                redirectAttributes.addAttribute("classRoomId", classRoomId);
                redirectAttributes.addAttribute("studentId", studentId);
                redirectAttributes.addAttribute("term", term);
                redirectAttributes.addAttribute("departmentId", departmentId);
                redirectAttributes.addAttribute("specialty", specialty);

                return "redirect:/assessments/entry";
            }

            if (subjectIds != null && scores != null && subjectIds.size() == scores.size()) {
                for (int i = 0; i < subjectIds.size(); i++) {
                    Long subjectId = subjectIds.get(i);
                    Double score = scores.get(i);

                    if (score != null) {
                        if (score >= 0 && score <= 20) {
                            Subject subject = new Subject();
                            subject.setId(subjectId);

                            // Check if assessment already exists
                            Optional<Assessment> existingAssessment =
                                    assessmentService.getAssessmentByStudentSubjectAndTermAndType(
                                            studentId, subjectId, term, assessmentType);

                            Assessment assessment;
                            if (existingAssessment.isPresent()) {
                                // Update existing assessment
                                assessment = existingAssessment.get();
                                assessment.setScore(score);
                            } else {
                                // Create new assessment
                                assessment = Assessment.builder()
                                        .student(student)
                                        .subject(subject)
                                        .term(term)
                                        .type(assessmentType)
                                        .score(score)
                                        .build();
                            }
                            assessments.add(assessment);
                        } else {
                            log.warn("Invalid score {} for subject {}", score, subjectId);
                        }
                    }
                }
            }

            if (!assessments.isEmpty()) {
                assessmentService.saveAll(assessments);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Saved " + assessments.size() + " assessments successfully for " +
                                assessmentType.getDisplayName() + "!");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "No assessments to save. Please enter valid scores (0-20).");
            }

        } catch (Exception e) {
            log.error("Error saving batch assessments", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving assessments: " + e.getMessage());
        }

        // Preserve filter parameters
        redirectAttributes.addAttribute("classRoomId", classRoomId);
        redirectAttributes.addAttribute("studentId", studentId);
        redirectAttributes.addAttribute("term", term);
        redirectAttributes.addAttribute("departmentId", departmentId);
        redirectAttributes.addAttribute("specialty", specialty);

        return "redirect:/assessments/entry";
    }

    private String getValidAssessmentsForTerm(Integer term) {
        return switch (term) {
            case 1 -> "Assessments 1-2";
            case 2 -> "Assessments 3-4";
            case 3 -> "Assessment 5";
            default -> "No valid assessments";
        };
    }

    @GetMapping("/term-averages/{classId}")
    public String getTermAverages(@PathVariable Long classId,
                                  @RequestParam(defaultValue = "1") Integer term,
                                  Model model) {
        try {
            ClassRoom classRoom = classRoomRepository.findById(classId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

            List<StudentTermAverageDTO> termAverages = assessmentService.getTermAveragesForClass(classId, term);

            model.addAttribute("classRoom", classRoom);
            model.addAttribute("term", term);
            model.addAttribute("termAverages", termAverages);
            model.addAttribute("gradeService", gradeService);

            return "term-averages";
        } catch (Exception e) {
            log.error("Error getting term averages for class {} term {}", classId, term, e);
            model.addAttribute("error", "Unable to load term averages");
            return "term-averages";
        }
    }

    @GetMapping("/yearly-ranking/{classId}")
    public String getYearlyRanking(@PathVariable Long classId, Model model) {
        try {
            ClassRoom classRoom = classRoomRepository.findById(classId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

            List<StudentYearlyAverageDTO> yearlyAverages = assessmentService.getYearlyAveragesForClass(classId);

            model.addAttribute("classRoom", classRoom);
            model.addAttribute("yearlyAverages", yearlyAverages);
            model.addAttribute("gradeService", gradeService);

            return "yearly-ranking";
        } catch (Exception e) {
            log.error("Error getting yearly ranking for class {}", classId, e);
            model.addAttribute("error", "Unable to load yearly ranking");
            return "yearly-ranking";
        }
    }

    @GetMapping("/student-performance/{studentId}")
    public String getStudentPerformance(@PathVariable Long studentId,
                                        @RequestParam(required = false) Integer term,
                                        Model model) {
        try {
            Student student = studentService.getStudentByIdOrThrow(studentId);

            // Get all terms data
            Map<Integer, Map<String, Object>> termData = new HashMap<>();

            for (int t = 1; t <= 3; t++) {
                Map<String, Object> data = new HashMap<>();

                // Calculate term average
                Double termAverage = assessmentService.calculateTermAverage(studentId, t);
                data.put("average", termAverage);
                data.put("formattedAverage", String.format("%.2f", termAverage));
                data.put("status", gradeService.getPerformanceStatus(termAverage));
                data.put("passed", gradeService.isPassing(termAverage));
                data.put("remarks", gradeService.generateRemarks(termAverage));

                // Get subject scores for this term
                Map<Long, List<Double>> subjectScores = assessmentService.getStudentSubjectScoresByTerm(studentId, t);
                data.put("subjectScores", subjectScores);

                termData.put(t, data);
            }

            // Calculate yearly average
            Double yearlyAverage = assessmentService.calculateYearlyAverage(studentId);

            model.addAttribute("student", student);
            model.addAttribute("termData", termData);
            model.addAttribute("yearlyAverage", yearlyAverage);
            model.addAttribute("formattedYearlyAverage", String.format("%.2f", yearlyAverage));
            model.addAttribute("yearlyStatus", gradeService.getPerformanceStatus(yearlyAverage));
            model.addAttribute("yearlyPassed", gradeService.isPassing(yearlyAverage));

            return "student-performance";
        } catch (Exception e) {
            log.error("Error getting student performance for student {}", studentId, e);
            model.addAttribute("error", "Unable to load student performance");
            return "student-performance";
        }
    }

    @GetMapping("/edit/{id}")
    public String editAssessment(@PathVariable Long id,
                                 @RequestParam(required = false) Long classRoomId,
                                 @RequestParam(required = false) Long studentId,
                                 @RequestParam(required = false) Integer term,
                                 @RequestParam(required = false) Integer assessmentNumber,
                                 RedirectAttributes redirectAttributes) {
        try {
            Assessment assessment = assessmentService.getAssessmentById(id);

            redirectAttributes.addAttribute("classRoomId", classRoomId);
            redirectAttributes.addAttribute("studentId", assessment.getStudent().getId());
            redirectAttributes.addAttribute("term", assessment.getTerm());
            redirectAttributes.addAttribute("assessmentNumber", assessment.getType().getAssessmentNumber());

            return "redirect:/assessments/entry";
        } catch (Exception e) {
            log.error("Error editing assessment {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error editing assessment");
            return "redirect:/assessments/entry";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteAssessment(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            assessmentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Assessment deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting assessment {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting assessment");
        }

        return "redirect:/assessments/entry";
    }

    @GetMapping("/subjects/{studentId}")
    @ResponseBody
    public List<Subject> getSubjectsForStudent(@PathVariable Long studentId) {
        try {
            return studentEnrollmentService.getStudentEnrollments(studentId).stream()
                    .map(StudentSubject::getSubject)
                    .distinct()
                    .sorted((s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching subjects for student {}", studentId, e);
            return List.of();
        }
    }

    private List<Student> getFilteredStudents(Long classRoomId, Long departmentId, String specialty) {
        List<Student> allStudents = studentRepository.findAll();

        return allStudents.stream()
                .filter(student -> {
                    if (classRoomId != null &&
                            (student.getClassRoom() == null || !student.getClassRoom().getId().equals(classRoomId))) {
                        return false;
                    }
                    if (departmentId != null &&
                            (student.getDepartment() == null || !student.getDepartment().getId().equals(departmentId))) {
                        return false;
                    }
                    if (specialty != null && !specialty.isEmpty()) {
                        if (student.getSpecialty() == null) return false;
                        return student.getSpecialty().equals(specialty);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}