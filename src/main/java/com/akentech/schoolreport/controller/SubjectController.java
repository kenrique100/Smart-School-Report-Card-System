package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        model.addAttribute("groupedSubjects", subjectService.getSubjectsGroupedByDepartmentAndSpecialty());
        model.addAttribute("subject", new Subject());
        return "subjects";
    }

    // Add GET mapping for the add form
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("subject", new Subject());
        model.addAttribute("departments", departmentRepository.findAll());
        return "add-subject";
    }

    @PostMapping("/add")
    public String addSubject(@ModelAttribute Subject subject) {
        subjectService.save(subject);
        return "redirect:/subjects?success";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Subject> subject = subjectService.getById(id);

        if (subject.isPresent()) {
            model.addAttribute("subject", subject.get());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", subjectService.getAllSpecialties());
            return "edit-subject";
        } else {
            return "redirect:/subjects?error=notfound";
        }
    }

    @PostMapping("/update")
    public String updateSubject(@ModelAttribute Subject subject) {
        subjectService.save(subject);
        return "redirect:/subjects?updated";
    }

    @GetMapping("/delete/{id}")
    public String deleteSubject(@PathVariable Long id) {
        subjectService.delete(id);
        return "redirect:/subjects?deleted";
    }

    // AJAX endpoint to get specialties by department
    @GetMapping("/specialties/{departmentId}")
    @ResponseBody
    public List<String> getSpecialtiesByDepartment(@PathVariable Long departmentId) {
        return subjectService.getSpecialtiesByDepartment(departmentId);
    }
}