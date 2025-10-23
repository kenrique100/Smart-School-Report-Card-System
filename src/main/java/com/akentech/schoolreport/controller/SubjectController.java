package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final SubjectService subjectService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String listSubjects(Model model) {
        model.addAttribute("subjects", subjectService.getAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("subject", new Subject()); // Add this for the form
        return "subjects";
    }

    @PostMapping("/add")
    public String addSubject(@ModelAttribute Subject subject) {
        subjectService.save(subject);
        return "redirect:/subjects";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Subject> subject = subjectService.getAll().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();

        if (subject.isPresent()) {
            model.addAttribute("subject", subject.get());
        } else {
            return "redirect:/subjects";
        }

        model.addAttribute("departments", departmentRepository.findAll());
        return "edit-subject";
    }

    @PostMapping("/update")
    public String updateSubject(@ModelAttribute Subject subject) {
        subjectService.save(subject);
        return "redirect:/subjects";
    }

    @GetMapping("/delete/{id}")
    public String deleteSubject(@PathVariable Long id) {
        subjectService.delete(id);
        return "redirect:/subjects";
    }
}