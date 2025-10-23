package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Exam;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
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
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;

    @GetMapping
    public String listExams(Model model) {
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("classRooms", classRoomRepository.findAll());
        return "exams";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("exam", new Exam());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("classRooms", classRoomRepository.findAll());
        return "add-exam";
    }

    @PostMapping("/add")
    public String addExam(@ModelAttribute Exam exam) {
        examService.saveExam(exam);
        return "redirect:/exams";
    }

    @GetMapping("/delete/{id}")
    public String deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return "redirect:/exams";
    }
}