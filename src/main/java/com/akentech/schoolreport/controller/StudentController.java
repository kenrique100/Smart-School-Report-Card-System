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
        model.addAttribute("specialties", studentService.getAllSpecialties());
        model.addAttribute("student", new Student());
        return "students";
    }

    @PostMapping("/save")
    public String saveStudent(@Valid @ModelAttribute("student") Student student,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", studentService.getAllSpecialties());
            return "students";
        }

        studentService.saveStudent(student);
        return "redirect:/students?success";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Student student = studentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student ID: " + id));
        model.addAttribute("student", student);
        model.addAttribute("classRooms", classRoomRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("specialties", studentService.getAllSpecialties());
        return "edit-student";
    }

    @PostMapping("/update/{id}")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("student") Student student,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("classes", classRoomRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("specialties", studentService.getAllSpecialties());
            return "edit-student";
        }

        student.setId(id);
        studentService.saveStudent(student);
        return "redirect:/students?updated";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return "redirect:/students?deleted";
    }

    @GetMapping("/view/{id}")
    public String viewStudent(@PathVariable Long id, Model model) {
        Student student = studentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student ID: " + id));
        model.addAttribute("student", student);
        return "view-student";
    }

    // AJAX endpoint to check specialty requirements
    @GetMapping("/check-specialty-requirements")
    @ResponseBody
    public StudentService.SpecialtyRequirement checkSpecialtyRequirements(
            @RequestParam String classCode,
            @RequestParam String departmentCode) {
        return studentService.checkSpecialtyRequirement(classCode, departmentCode);
    }
}