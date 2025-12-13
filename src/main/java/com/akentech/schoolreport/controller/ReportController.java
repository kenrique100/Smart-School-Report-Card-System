package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.YearlyReportDTO;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final ClassRoomRepository classRoomRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/select")
    public String selectView(Model model) {
        model.addAttribute("classes", classRoomRepository.findAll());
        return "select_report";
    }

    // ====== STUDENT REPORT SELECTION FLOW ======

    @GetMapping("/student/select")
    public String selectStudentReportForm(Model model) {
        model.addAttribute("classes", classRoomRepository.findAll());
        return "select_student_report_form";
    }

    @PostMapping("/student/select")
    public String processStudentSelection(
            @RequestParam Long classId,
            @RequestParam Integer term,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("classId", classId);
        redirectAttributes.addAttribute("term", term);
        return "redirect:/reports/student/list";
    }

    @GetMapping("/student/list")
    public String listStudentsInClass(
            @RequestParam Long classId,
            @RequestParam Integer term,
            Model model) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        List<Student> students = studentRepository.findByClassRoomId(classId);

        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);
        model.addAttribute("students", students);

        return "select_student";
    }

    // ====== GENERATE REPORTS ======

    @GetMapping("/student/report")
    public String generateStudentReport(
            @RequestParam Long classId,
            @RequestParam Integer term,
            @RequestParam(required = false) Long studentId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            if (studentId == null) {
                redirectAttributes.addAttribute("classId", classId);
                redirectAttributes.addAttribute("term", term);
                return "redirect:/reports/student/list";
            }

            ReportDTO report = reportService.generateReportForStudent(studentId, term);
            model.addAttribute("report", report);
            return "report_term";

        } catch (Exception e) {
            log.error("Error generating report for student {} in term {}: {}",
                    studentId, term, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Error generating report: " + e.getMessage());
            redirectAttributes.addAttribute("classId", classId);
            redirectAttributes.addAttribute("term", term);
            return "redirect:/reports/student/list";
        }
    }

    @GetMapping("/student/yearly/report")
    public String generateStudentYearlyReport(
            @RequestParam Long studentId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            YearlyReportDTO report = reportService.generateYearlyReportForStudent(studentId);
            model.addAttribute("report", report);
            return "report_yearly";

        } catch (Exception e) {
            log.error("Error generating yearly report for student {}: {}",
                    studentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Error generating yearly report: " + e.getMessage());
            return "redirect:/reports/select";
        }
    }

    // ====== CLASS REPORTS ======

    @GetMapping("/class")
    public String classReport(@RequestParam Long classId,
                              @RequestParam(defaultValue = "1") Integer term,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(defaultValue = "rankInClass") String sortBy,
                              @RequestParam(defaultValue = "asc") String sortDir,
                              Model model) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReportDTO> reportPage = reportService.generatePaginatedReportsForClass(classId, term, pageable);
        List<ReportDTO> reports = reportPage.getContent();

        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);

        // Pagination attributes
        model.addAttribute("currentPage", reportPage.getNumber());
        model.addAttribute("totalPages", reportPage.getTotalPages());
        model.addAttribute("totalItems", reportPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "class_reports_term";
    }

    @GetMapping("/class/yearly")
    public String classYearlyReport(@RequestParam Long classId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "yearlyRank") String sortBy,
                                    @RequestParam(defaultValue = "asc") String sortDir,
                                    Model model) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<YearlyReportDTO> reportPage = reportService.generatePaginatedYearlyReportsForClass(classId, pageable);
        List<YearlyReportDTO> reports = reportPage.getContent();

        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);

        // Pagination attributes
        model.addAttribute("currentPage", reportPage.getNumber());
        model.addAttribute("totalPages", reportPage.getTotalPages());
        model.addAttribute("totalItems", reportPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "class_reports_yearly";
    }
}