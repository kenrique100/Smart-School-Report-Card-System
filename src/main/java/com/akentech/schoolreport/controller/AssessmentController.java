package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.StudentSubject;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.AssessmentService;
import com.akentech.schoolreport.service.StudentEnrollmentService;
import com.akentech.schoolreport.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/entry")
    public String entryForm(Model model,
                            @RequestParam(value = "studentId", required = false) Long studentId,
                            @RequestParam(value = "term", required = false, defaultValue = "1") Integer term,
                            @RequestParam(value = "classRoomId", required = false) Long classRoomId,
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

        // Get filtered students based on class, department, specialty
        List<Student> filteredStudents;
        if (classRoomId != null || departmentId != null || (specialty != null && !specialty.isEmpty())) {
            filteredStudents = getFilteredStudents(classRoomId, departmentId, specialty);
        } else {
            filteredStudents = studentRepository.findAll();
        }

        model.addAttribute("students", filteredStudents);
        model.addAttribute("assessment", new Assessment());
        model.addAttribute("term", term);
        model.addAttribute("terms", List.of(1, 2, 3));

        // Load subjects if studentId is provided
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

                // Get existing assessments for each subject in the selected term
                List<Assessment> existingAssessments = new ArrayList<>();
                for (Subject subject : studentSubjects) {
                    List<Assessment> subjectAssessments = assessmentService.getAssessmentsByStudentSubjectAndTerm(
                            studentId, subject.getId(), term);
                    existingAssessments.addAll(subjectAssessments);
                }

                model.addAttribute("existingAssessments", existingAssessments);
                model.addAttribute("assessmentTypes", List.of("Assessment1", "Assessment2", "Exam"));

                log.info("Loaded {} subjects and {} existing assessments for student {} in term {}",
                        studentSubjects.size(), existingAssessments.size(), studentId, term);

            } catch (Exception e) {
                log.error("Error loading subjects for student {}", studentId, e);
                model.addAttribute("subjects", List.of());
                model.addAttribute("errorMessage", "Error loading student subjects: " + e.getMessage());
            }
        } else {
            model.addAttribute("subjects", List.of());
            model.addAttribute("selectedStudentId", null);
            model.addAttribute("existingAssessments", List.of());
        }

        return "assessments";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Assessment assessment,
                       @RequestParam(value = "studentId", required = false) Long studentId,
                       @RequestParam(value = "term", required = false) Integer term,
                       @RequestParam(value = "classRoomId", required = false) Long classRoomId,
                       @RequestParam(value = "departmentId", required = false) Long departmentId,
                       @RequestParam(value = "specialty", required = false) String specialty,
                       RedirectAttributes redirectAttributes) {
        try {
            // Validate the assessment
            if (assessment.getStudent() == null || assessment.getStudent().getId() == null) {
                throw new IllegalArgumentException("Student is required");
            }
            if (assessment.getSubject() == null || assessment.getSubject().getId() == null) {
                throw new IllegalArgumentException("Subject is required");
            }
            if (assessment.getTerm() == null) {
                throw new IllegalArgumentException("Term is required");
            }
            if (assessment.getType() == null || assessment.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Assessment type is required");
            }
            if (assessment.getScore() == null) {
                throw new IllegalArgumentException("Score is required");
            }

            assessmentService.save(assessment);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Assessment saved successfully! Score: " + assessment.getScore() + "/20");

        } catch (Exception e) {
            log.error("Error saving assessment", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving assessment: " + e.getMessage());
        }

        // Preserve filter parameters in redirect
        redirectAttributes.addAttribute("studentId", assessment.getStudent().getId());
        redirectAttributes.addAttribute("term", assessment.getTerm());
        if (classRoomId != null) redirectAttributes.addAttribute("classRoomId", classRoomId);
        if (departmentId != null) redirectAttributes.addAttribute("departmentId", departmentId);
        if (specialty != null && !specialty.isEmpty()) redirectAttributes.addAttribute("specialty", specialty);

        return "redirect:/assessments/entry";
    }

    @PostMapping("/save-batch")
    public String saveBatch(@RequestParam Long studentId,
                            @RequestParam Integer term,
                            @RequestParam String assessmentType,
                            @RequestParam Map<String, String> allParams,
                            RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentByIdOrThrow(studentId);
            List<Assessment> assessments = new ArrayList<>();

            // Extract subjectIds and scores from parameters
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();

                if (key.startsWith("subjectIds[")) {
                    // Extract index from key: subjectIds[0], subjectIds[1], etc.
                    String indexStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));
                    int index = Integer.parseInt(indexStr);

                    Long subjectId = Long.parseLong(entry.getValue());
                    String scoreKey = "scores[" + index + "]";
                    String scoreValue = allParams.get(scoreKey);

                    if (scoreValue != null && !scoreValue.trim().isEmpty()) {
                        double score = Double.parseDouble(scoreValue);

                        if (score >= 0 && score <= 20) {
                            Subject subject = new Subject();
                            subject.setId(subjectId);

                            Assessment assessment = Assessment.builder()
                                    .student(student)
                                    .subject(subject)
                                    .term(term)
                                    .type(assessmentType)
                                    .score(score)
                                    .build();
                            assessments.add(assessment);
                        }
                    }
                }
            }

            if (!assessments.isEmpty()) {
                assessmentService.saveAll(assessments);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Saved " + assessments.size() + " assessments successfully!");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "No assessments to save. Please enter scores.");
            }

        } catch (Exception e) {
            log.error("Error saving batch assessments", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving assessments: " + e.getMessage());
        }

        // Preserve filter parameters
        redirectAttributes.addAttribute("studentId", studentId);
        redirectAttributes.addAttribute("term", term);

        // Add back any filter parameters
        if (allParams.containsKey("classRoomId") && allParams.get("classRoomId") != null) {
            redirectAttributes.addAttribute("classRoomId", allParams.get("classRoomId"));
        }
        if (allParams.containsKey("departmentId") && allParams.get("departmentId") != null) {
            redirectAttributes.addAttribute("departmentId", allParams.get("departmentId"));
        }
        if (allParams.containsKey("specialty") && allParams.get("specialty") != null && !allParams.get("specialty").isEmpty()) {
            redirectAttributes.addAttribute("specialty", allParams.get("specialty"));
        }

        return "redirect:/assessments/entry";
    }

    // AJAX endpoint to get subjects for a specific student
    @GetMapping("/subjects/{studentId}")
    @ResponseBody
    public List<Subject> getSubjectsForStudent(@PathVariable Long studentId) {
        try {
            // Get student's enrolled subjects using the enrollment service
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

    // Helper method to get filtered students
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