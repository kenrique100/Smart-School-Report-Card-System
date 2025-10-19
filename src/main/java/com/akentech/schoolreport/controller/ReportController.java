package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;

    @GetMapping("/select")
    public String selectView(Model model) {
        model.addAttribute("classes", classRoomRepository.findAll());
        return "select_report";
    }

    @GetMapping("/student/{id}")
    public String studentReport(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer term, Model model) {
        ReportDTO dto = reportService.generateReportForStudent(id, term);
        model.addAttribute("report", dto);
        return "report_term"; // Thymeleaf template for term-based report
    }

    @GetMapping("/class")
    public String classReport(@RequestParam Long classId, @RequestParam(defaultValue = "1") Integer term, Model model) {
        // produce reports for every student in class
        ClassRoom cr = classRoomRepository.findById(classId).orElseThrow();
        List<Student> students = studentRepository.findByClassRoom(cr);
        // Build reports for each student
        List<ReportDTO> reports = students.stream()
                .map(s -> reportService.generateReportForStudent(s.getId(), term))
                .toList();
        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", cr);
        model.addAttribute("term", term);
        return "class_reports_term"; // multi-report printable template
    }
}
