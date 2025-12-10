package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Exam;
import com.akentech.schoolreport.model.ExamResult;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.service.ExamResultService;
import com.akentech.schoolreport.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/exams")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;
    private final ExamResultService examResultService;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public String listExams(Model model) {
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("exam", new Exam());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("classRooms", classRoomRepository.findAll());
        return "exams";
    }

    @PostMapping("/add")
    public String addExam(@ModelAttribute Exam exam) {
        Exam savedExam = examService.saveExam(exam);
        log.info("Exam added successfully: {}", savedExam.getName());
        return "redirect:/exams";
    }

    @GetMapping("/delete/{id}")
    public String deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return "redirect:/exams";
    }


    // Exam Results Management
    @GetMapping("/results")
    public String viewAllExamResults(Model model) {
        model.addAttribute("examResults", examResultService.getAllExamResults());
        model.addAttribute("examResult", new ExamResult()); // For add form
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("students", studentRepository.findAll());
        return "exam-results";
    }

    @GetMapping("/{examId}/results")
    public String viewExamResults(@PathVariable Long examId, Model model) {
        model.addAttribute("examResults", examResultService.getExamResultsByExam(examId));
        model.addAttribute("exam", examService.getExamById(examId).orElseThrow());
        model.addAttribute("examResult", new ExamResult());
        model.addAttribute("students", studentRepository.findAll());
        return "exam-results";
    }

    @PostMapping("/results/save")
    public String saveExamResult(@ModelAttribute ExamResult examResult) {
        examResultService.saveExamResult(examResult);
        return "redirect:/exams/results"; // Redirect to all results page
    }

    // Optional: Add delete result method
    @GetMapping("/results/delete/{id}")
    public String deleteExamResult(@PathVariable Long id) {
        examResultService.deleteExamResult(id);
        return "redirect:/exams/results";
    }
}