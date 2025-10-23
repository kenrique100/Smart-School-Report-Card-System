package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String listStudents(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("classes", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("student", new Student());
        return "students";
    }

    @PostMapping("/add")
    public String addStudent(@Valid @ModelAttribute("student") Student student,
                             BindingResult result,
                             Model model) {
        log.info("Adding student: {}", student);

        if (result.hasErrors()) {
            log.warn("Validation errors: {}", result.getAllErrors());
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            return "students";
        }

        try {
            studentService.saveStudent(student);
            log.info("Student added successfully: {}", student.getStudentId());
        } catch (Exception e) {
            log.error("Error adding student: {}", e.getMessage());
            model.addAttribute("error", "Failed to add student: " + e.getMessage());
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            return "students";
        }

        return "redirect:/students?success";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        log.info("Editing student with ID: {}", id);

        try {
            Student student = studentService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid student id: " + id));
            model.addAttribute("student", student);
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            return "edit-student";
        } catch (Exception e) {
            log.error("Error loading student for edit: {}", e.getMessage());
            return "redirect:/students?error=Student not found";
        }
    }

    @PostMapping("/update")
    public String updateStudent(@Valid @ModelAttribute("student") Student student,
                                BindingResult result,
                                Model model) {
        log.info("Updating student: {}", student.getId());

        if (result.hasErrors()) {
            log.warn("Validation errors in update: {}", result.getAllErrors());
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            return "edit-student";
        }

        try {
            studentService.saveStudent(student);
            log.info("Student updated successfully: {}", student.getStudentId());
        } catch (Exception e) {
            log.error("Error updating student: {}", e.getMessage());
            model.addAttribute("error", "Failed to update student: " + e.getMessage());
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            return "edit-student";
        }

        return "redirect:/students?success";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable("id") Long id) {
        log.info("Deleting student with ID: {}", id);

        try {
            studentService.deleteStudent(id);
            log.info("Student deleted successfully: {}", id);
            return "redirect:/students?success";
        } catch (Exception e) {
            log.error("Error deleting student: {}", e.getMessage());
            return "redirect:/students?error=Failed to delete student";
        }
    }
}