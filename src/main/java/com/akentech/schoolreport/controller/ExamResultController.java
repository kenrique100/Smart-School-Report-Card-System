package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.ExamResult;
import com.akentech.schoolreport.repository.ExamRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.ExamResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/exams/results")
@RequiredArgsConstructor
@Slf4j
public class ExamResultController {

    private final ExamResultService examResultService;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public String listExamResults(Model model) {
        model.addAttribute("examResults", examResultService.getAllExamResults());
        model.addAttribute("exams", examRepository.findAll());
        model.addAttribute("students", studentRepository.findAll());
        return "exam-results";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("examResult", new ExamResult());
        model.addAttribute("exams", examRepository.findAll());
        model.addAttribute("students", studentRepository.findAll());
        return "add-exam-result";
    }

    @PostMapping("/add")
    public String addExamResult(@ModelAttribute ExamResult examResult) {
        examResultService.saveExamResult(examResult);
        return "redirect:/exams/results";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        examResultService.getExamResultById(id).ifPresent(examResult -> model.addAttribute("examResult", examResult));
        model.addAttribute("exams", examRepository.findAll());
        model.addAttribute("students", studentRepository.findAll());
        return "edit-exam-result";
    }

    @PostMapping("/update")
    public String updateExamResult(@ModelAttribute ExamResult examResult) {
        examResultService.saveExamResult(examResult);
        return "redirect:/exams/results";
    }

    @GetMapping("/delete/{id}")
    public String deleteExamResult(@PathVariable Long id) {
        examResultService.deleteExamResult(id);
        return "redirect:/exams/results";
    }
}