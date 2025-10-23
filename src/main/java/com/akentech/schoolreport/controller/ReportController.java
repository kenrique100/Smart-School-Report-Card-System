package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.repository.ClassRoomRepository;
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
        return "report_term";
    }

    @GetMapping("/class")
    public String classReport(@RequestParam Long classId, @RequestParam(defaultValue = "1") Integer term, Model model) {
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        List<ReportDTO> reports = reportService.generateReportsForClass(classId, term);
        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);
        return "class_reports_term";
    }
}