package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/assessments")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    @GetMapping("/entry")
    public String entryForm(Model model) {
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("assessment", new Assessment());
        return "assessments";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Assessment assessment) {
        assessmentService.save(assessment);
        return "redirect:/assessments/entry?success";
    }
}