package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final SubjectService subjectService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String listSubjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String specialty,
            Model model) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Subject> subjectPage = subjectService.getSubjectsByFilters(name, departmentId, specialty, pageable);

            model.addAttribute("subjects", subjectPage.getContent());
            model.addAttribute("currentPage", subjectPage.getNumber());
            model.addAttribute("totalPages", subjectPage.getTotalPages());
            model.addAttribute("totalItems", subjectPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("groupedSubjects", subjectService.getSubjectsGroupedByDepartmentAndSpecialty());
            model.addAttribute("subject", new Subject());

            // Add filter values for form persistence
            model.addAttribute("nameFilter", name);
            model.addAttribute("departmentIdFilter", departmentId);
            model.addAttribute("specialtyFilter", specialty);

            return "subjects";
        } catch (Exception e) {
            log.error("Error loading subjects list", e);
            model.addAttribute("error", "Unable to load subjects");
            return "subjects";
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("subject", new Subject());
        model.addAttribute("departments", departmentRepository.findAll());
        return "add-subject";
    }

    @PostMapping("/add")
    public String addSubject(@ModelAttribute Subject subject) {
        try {
            subjectService.createSubject(subject);
            return "redirect:/subjects?success";
        } catch (Exception e) {
            log.error("Error adding subject", e);
            return "redirect:/subjects?error=add_failed";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Subject subject = subjectService.getSubjectById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Subject", id));

            model.addAttribute("subject", subject);
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", subjectService.getSpecialtiesByDepartment(subject.getDepartment().getId()));
            return "edit-subject";
        } catch (EntityNotFoundException e) {
            log.warn("Subject not found with id: {}", id);
            return "redirect:/subjects?error=notfound";
        } catch (Exception e) {
            log.error("Error loading subject edit form for id: {}", id, e);
            return "redirect:/subjects?error=server_error";
        }
    }

    @PostMapping("/update")
    public String updateSubject(@ModelAttribute Subject subject) {
        try {
            subjectService.updateSubject(subject.getId(), subject);
            return "redirect:/subjects?updated";
        } catch (EntityNotFoundException e) {
            log.warn("Subject not found for update with id: {}", subject.getId());
            return "redirect:/subjects?error=notfound";
        } catch (Exception e) {
            log.error("Error updating subject with id: {}", subject.getId(), e);
            return "redirect:/subjects?error=update_failed";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSubject(@PathVariable Long id) {
        try {
            subjectService.deleteSubject(id);
            return "redirect:/subjects?deleted";
        } catch (EntityNotFoundException e) {
            log.warn("Subject not found for deletion with id: {}", id);
            return "redirect:/subjects?error=notfound";
        } catch (Exception e) {
            log.error("Error deleting subject with id: {}", id, e);
            return "redirect:/subjects?error=delete_failed";
        }
    }

    // AJAX endpoint to get specialties by department
    @GetMapping("/specialties/{departmentId}")
    @ResponseBody
    public List<String> getSpecialtiesByDepartment(@PathVariable Long departmentId) {
        return subjectService.getSpecialtiesByDepartment(departmentId);
    }
}