package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.YearlyReportDTO;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.repository.ClassRoomRepository;
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
    public String studentReport(@PathVariable Long id,
                                @RequestParam(defaultValue = "1") Integer term,
                                Model model) {
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
    public String classReport(@RequestParam Long classId,
                              @RequestParam(defaultValue = "1") Integer term,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(defaultValue = "rankInClass") String sortBy,
                              @RequestParam(defaultValue = "asc") String sortDir,
                              Model model) {
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        // Create pageable for pagination
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get paginated reports
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

        // Calculate statistics
        long totalStudents = reportPage.getTotalElements();
        long totalPassed = reports.stream()
                .filter(r -> {
                    if (r.getTermAverage() == null) return false;
                    return r.getTermAverage() >= 10;
                })
                .count();

        Double classAverage = reports.stream()
                .filter(r -> r.getTermAverage() != null)
                .mapToDouble(ReportDTO::getTermAverage)
                .average()
                .orElse(0.0);

        // Calculate performance distribution
        long excellentCount = reports.stream()
                .filter(r -> r.getTermAverage() != null && r.getTermAverage() >= 18)
                .count();
        long veryGoodCount = reports.stream()
                .filter(r -> r.getTermAverage() != null && r.getTermAverage() >= 15 && r.getTermAverage() < 18)
                .count();
        long goodCount = reports.stream()
                .filter(r -> r.getTermAverage() != null && r.getTermAverage() >= 10 && r.getTermAverage() < 15)
                .count();
        long needsImprovementCount = reports.stream()
                .filter(r -> r.getTermAverage() != null && r.getTermAverage() < 10)
                .count();

        long positiveRemarksCount = reports.stream()
                .filter(r -> r.getRemarks() != null &&
                        (r.getRemarks().contains("Excellent") || r.getRemarks().contains("Very good")))
                .count();

        long top3Count = reports.stream()
                .filter(r -> r.getRankInClass() != null && r.getRankInClass() <= 3)
                .count();

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalPassed", totalPassed);
        model.addAttribute("totalFailed", totalStudents - totalPassed);
        model.addAttribute("classAverage", String.format("%.2f", classAverage));
        model.addAttribute("excellentCount", excellentCount);
        model.addAttribute("veryGoodCount", veryGoodCount);
        model.addAttribute("goodCount", goodCount);
        model.addAttribute("needsImprovementCount", needsImprovementCount);
        model.addAttribute("positiveRemarksCount", positiveRemarksCount);
        model.addAttribute("top3Count", top3Count);

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

        // Create pageable for pagination
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get paginated yearly reports
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

        // Calculate statistics
        if (!reports.isEmpty()) {
            long totalStudents = reportPage.getTotalElements();
            long totalPassed = reports.stream()
                    .filter(YearlyReportDTO::getPassed)
                    .count();

            Double classYearlyAverage = reports.stream()
                    .filter(r -> r.getYearlyAverage() != null)
                    .mapToDouble(YearlyReportDTO::getYearlyAverage)
                    .average()
                    .orElse(0.0);

            Double averagePassRate = reports.stream()
                    .filter(r -> r.getPassRate() != null)
                    .mapToDouble(YearlyReportDTO::getPassRate)
                    .average()
                    .orElse(0.0);

            long aGradeCount = reports.stream()
                    .filter(r -> "A".equals(r.getOverallGrade()))
                    .count();
            long bGradeCount = reports.stream()
                    .filter(r -> "B".equals(r.getOverallGrade()))
                    .count();
            long cGradeCount = reports.stream()
                    .filter(r -> "C".equals(r.getOverallGrade()))
                    .count();
            long belowAvgCount = reports.stream()
                    .filter(r -> {
                        String grade = r.getOverallGrade();
                        return grade != null && (grade.equals("D") || grade.equals("F") || grade.equals("U"));
                    })
                    .count();

            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalPassed", totalPassed);
            model.addAttribute("totalFailed", totalStudents - totalPassed);
            model.addAttribute("classYearlyAverage", String.format("%.2f", classYearlyAverage));
            model.addAttribute("averagePassRate", String.format("%.1f%%", averagePassRate));
            model.addAttribute("aGradeCount", aGradeCount);
            model.addAttribute("bGradeCount", bGradeCount);
            model.addAttribute("cGradeCount", cGradeCount);
            model.addAttribute("belowAvgCount", belowAvgCount);
        }

        return "class_reports_yearly";
    }
}