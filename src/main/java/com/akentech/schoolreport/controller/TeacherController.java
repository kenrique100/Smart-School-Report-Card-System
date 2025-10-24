package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Teacher;
import com.akentech.schoolreport.service.TeacherService;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final TeacherService teacherService;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;

    @GetMapping
    public String listTeachers(Model model) {
        model.addAttribute("teachers", teacherService.getAllTeachers());
        return "teachers";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        Teacher teacher = new Teacher();
        // Auto-generate teacher ID for new teachers
        teacher.setTeacherId(teacherService.generateTeacherId());
        model.addAttribute("teacher", teacher);
        model.addAttribute("allSubjects", subjectRepository.findAll());
        model.addAttribute("allClassrooms", classRoomRepository.findAll());
        return "add-teacher";
    }

    @PostMapping("/add")
    public String addTeacher(@ModelAttribute Teacher teacher,
                             @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                             @RequestParam(value = "classroomIds", required = false) List<Long> classroomIds) {
        teacherService.saveTeacher(teacher, subjectIds, classroomIds);
        return "redirect:/teachers?success";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        teacherService.getTeacherById(id).ifPresent(teacher -> {
            model.addAttribute("teacher", teacher);
            model.addAttribute("allSubjects", subjectRepository.findAll());
            model.addAttribute("allClassrooms", classRoomRepository.findAll());
        });
        return "edit-teacher";
    }

    @PostMapping("/update")
    public String updateTeacher(@ModelAttribute Teacher teacher,
                                @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                                @RequestParam(value = "classroomIds", required = false) List<Long> classroomIds) {
        teacherService.updateTeacher(teacher, subjectIds, classroomIds);
        return "redirect:/teachers?updated";
    }

    @GetMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return "redirect:/teachers?deleted";
    }

    @GetMapping("/view/{id}")
    public String viewTeacher(@PathVariable Long id, Model model) {
        teacherService.getTeacherById(id).ifPresent(teacher -> {
            model.addAttribute("teacher", teacher);
        });
        return "view-teacher";
    }
}