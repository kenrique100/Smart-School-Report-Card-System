package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final SubjectService subjectService;
    private final DepartmentRepository departmentRepository;
    private final ClassRoomRepository classRoomRepository;

    @GetMapping
    public String listSubjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String specialty,
            Model model) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // FIXED: Use safe parsing
            Long departmentIdLong = safeParseLong(departmentId);

            Page<Subject> subjectPage = subjectService.getSubjectsByFilters(name, departmentIdLong, specialty, pageable);

            // Calculate statistics
            List<Subject> allSubjects = subjectService.getAllSubjects();
            long totalSubjects = allSubjects.size();

            // Count Ordinary Level subjects (Forms 1-5)
            long ordinarySubjects = allSubjects.stream()
                    .filter(s -> {
                        if (s.getClassRoom() == null || s.getClassRoom().getCode() == null) return false;
                        ClassLevel level = s.getClassRoom().getCode();
                        return level == ClassLevel.FORM_1 || level == ClassLevel.FORM_2 ||
                                level == ClassLevel.FORM_3 || level == ClassLevel.FORM_4 ||
                                level == ClassLevel.FORM_5;
                    })
                    .count();

            // Count Advanced Level subjects (Sixth Form)
            long advancedSubjects = allSubjects.stream()
                    .filter(s -> {
                        if (s.getClassRoom() == null || s.getClassRoom().getCode() == null) return false;
                        ClassLevel level = s.getClassRoom().getCode();
                        return level == ClassLevel.LOWER_SIXTH || level == ClassLevel.UPPER_SIXTH;
                    })
                    .count();

            // Count optional subjects
            long optionalSubjects = allSubjects.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getOptional()))
                    .count();

            // Get all specialties for filter dropdown
            List<String> allSpecialties = allSubjects.stream()
                    .map(Subject::getSpecialty)
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .toList();

            // Add statistics to model
            model.addAttribute("totalSubjects", totalSubjects);
            model.addAttribute("ordinarySubjects", ordinarySubjects);
            model.addAttribute("advancedSubjects", advancedSubjects);
            model.addAttribute("optionalSubjects", optionalSubjects);
            model.addAttribute("allSpecialties", allSpecialties);

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

            model.addAttribute("nameFilter", name);
            model.addAttribute("departmentIdFilter", departmentIdLong);
            model.addAttribute("specialtyFilter", specialty);

            log.info("Subject statistics - Total: {}, Ordinary: {}, Advanced: {}, Optional: {}",
                    totalSubjects, ordinarySubjects, advancedSubjects, optionalSubjects);

            return "subjects";
        } catch (Exception e) {
            log.error("Error loading subjects list", e);
            model.addAttribute("error", "Unable to load subjects");

            // Set default values for statistics on error
            model.addAttribute("totalSubjects", 0);
            model.addAttribute("ordinarySubjects", 0);
            model.addAttribute("advancedSubjects", 0);
            model.addAttribute("optionalSubjects", 0);
            model.addAttribute("allSpecialties", List.of());

            return "subjects";
        }
    }

    // FIXED: Helper method to safely parse Long
    private Long safeParseLong(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long from value: {}", value);
            return null;
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("subject", new Subject());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("classRooms", classRoomRepository.findAll());
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
            model.addAttribute("classRooms", classRoomRepository.findAll());
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

    // NEW: Statistics endpoint
    @GetMapping("/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = subjectService.getSubjectStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting subject statistics", e);
            return ResponseEntity.badRequest().build();
        }
    }
}