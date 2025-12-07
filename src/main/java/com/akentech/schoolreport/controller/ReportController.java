package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.YearlyReportDTO;
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
        ReportDTO report = reportService.generateReportForStudent(id, term);
        model.addAttribute("report", report);
        return "report_term";
    }

    @GetMapping("/student/yearly/{id}")
    public String studentYearlyReport(@PathVariable Long id, Model model) {
        YearlyReportDTO report = reportService.generateYearlyReportForStudent(id);
        model.addAttribute("report", report);
        return "report_yearly";
    }

    @GetMapping("/class")
    public String classReport(@RequestParam Long classId, @RequestParam(defaultValue = "1") Integer term, Model model) {
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        List<ReportDTO> reports = reportService.generateReportsForClass(classId, term);
        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);

        // Calculate statistics
        long totalStudents = reports.size();
        long totalPassed = reports.stream()
                .filter(r -> {
                    if (r.getTermAverage() == null) return false;
                    return r.getTermAverage() >= 10;
                })
                .count();

        // Calculate class average
        Double classAverage = reports.stream()
                .filter(r -> r.getTermAverage() != null)
                .mapToDouble(ReportDTO::getTermAverage)
                .average()
                .orElse(0.0);

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalPassed", totalPassed);
        model.addAttribute("totalFailed", totalStudents - totalPassed);
        model.addAttribute("classAverage", String.format("%.2f", classAverage));

        return "class_reports_term";
    }

    @GetMapping("/class/yearly")
    public String classYearlyReport(@RequestParam Long classId, Model model) {
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        List<YearlyReportDTO> reports = reportService.generateYearlyReportsForClass(classId);
        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);

        // Calculate statistics
        if (!reports.isEmpty()) {
            long totalStudents = reports.size();
            long totalPassed = reports.stream()
                    .filter(YearlyReportDTO::getPassed)
                    .count();

            // Calculate class yearly average
            Double classYearlyAverage = reports.stream()
                    .filter(r -> r.getYearlyAverage() != null)
                    .mapToDouble(YearlyReportDTO::getYearlyAverage)
                    .average()
                    .orElse(0.0);

            // Calculate average pass rate
            Double averagePassRate = reports.stream()
                    .filter(r -> r.getPassRate() != null)
                    .mapToDouble(YearlyReportDTO::getPassRate)
                    .average()
                    .orElse(0.0);

            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalPassed", totalPassed);
            model.addAttribute("totalFailed", totalStudents - totalPassed);
            model.addAttribute("classYearlyAverage", String.format("%.2f", classYearlyAverage));
            model.addAttribute("averagePassRate", String.format("%.1f%%", averagePassRate));
        }

        return "class_reports_yearly";
    }
}