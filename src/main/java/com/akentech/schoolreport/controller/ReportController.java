package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.*;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final TermReportPdfService termReportPdfService;
    private final YearlyReportPdfService yearlyReportPdfService;
    private final ClassTermReportPdfService classTermReportPdfService;
    private final ClassYearlyReportPdfService classYearlyReportPdfService;
    private final YearlySummaryPdfService yearlySummaryPdfService;
    private final ClassRoomRepository classRoomRepository;
    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;

    @GetMapping("/select")
    public String selectView(Model model) {
        List<ClassRoom> classes = classRoomRepository.findAll();
        model.addAttribute("classes", classes);
        model.addAttribute("currentAcademicYear", "2025-2026");
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
            @RequestParam(required = false) String academicYear,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("classId", classId);
        redirectAttributes.addAttribute("term", term);
        if (academicYear != null && !academicYear.isEmpty()) {
            redirectAttributes.addAttribute("academicYear", academicYear);
        } else {
            redirectAttributes.addAttribute("academicYear", "2025-2026");
        }
        return "redirect:/reports/student/list";
    }

    @GetMapping("/student/list")
    public String listStudentsInClass(
            @RequestParam Long classId,
            @RequestParam Integer term,
            @RequestParam(required = false) String academicYear,
            Model model) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        List<Student> students = studentRepository.findByClassRoomId(classId);

        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);
        model.addAttribute("students", students);
        model.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");

        return "select_student";
    }

    // ====== GENERATE REPORTS ======

    @GetMapping("/student/report")
    public String generateStudentReport(
            @RequestParam Long classId,
            @RequestParam Integer term,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false, defaultValue = "view") String action,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            if (studentId == null) {
                redirectAttributes.addAttribute("classId", classId);
                redirectAttributes.addAttribute("term", term);
                redirectAttributes.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");
                return "redirect:/reports/student/list";
            }

            if ("download".equals(action)) {
                redirectAttributes.addAttribute("studentId", studentId);
                redirectAttributes.addAttribute("term", term);
                redirectAttributes.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");
                return "redirect:/reports/pdf/student/term";
            }

            ReportDTO report = reportService.getTermReportForStudentAndYear(
                    studentId, term, academicYear != null ? academicYear : "2025-2026");

            model.addAttribute("report", report);
            return "report_term";

        } catch (Exception e) {
            log.error("Error generating report for student {} in term {}: {}",
                    studentId, term, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Error generating report: " + e.getMessage());
            redirectAttributes.addAttribute("classId", classId);
            redirectAttributes.addAttribute("term", term);
            redirectAttributes.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");
            return "redirect:/reports/student/list";
        }
    }

    // ====== PDF DOWNLOAD ENDPOINTS ======

    @GetMapping("/pdf/student/term")
    public ResponseEntity<InputStreamResource> downloadStudentTermReportPdf(
            @RequestParam Long studentId,
            @RequestParam Integer term,
            @RequestParam(required = false) String academicYear) throws IOException, DocumentException {

        log.info("Generating PDF report for student {} term {} academic year {}",
                studentId, term, academicYear);

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        ReportDTO report = reportService.getTermReportForStudentAndYear(studentId, term, effectiveAcademicYear);
        byte[] pdfBytes = termReportPdfService.generateTermReportPdf(report);

        ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
        InputStreamResource resource = new InputStreamResource(bis);

        String filename = String.format("Term_%d_Report_%s_%s_%s.pdf",
                term,
                report.getRollNumber(),
                report.getStudentFullName().replace(" ", "_"),
                effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @GetMapping("/pdf/student/term/class")
    public ResponseEntity<InputStreamResource> downloadStudentTermReportPdfByClass(
            @RequestParam Long classId,
            @RequestParam Long studentId,
            @RequestParam Integer term,
            @RequestParam(required = false) String academicYear) throws IOException, DocumentException {

        log.info("Generating PDF report for student {} in class {} term {} academic year {}",
                studentId, classId, term, academicYear);

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        ReportDTO report = reportService.getTermReportForStudentAndYear(studentId, term, effectiveAcademicYear);
        byte[] pdfBytes = termReportPdfService.generateTermReportPdf(report);

        ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
        InputStreamResource resource = new InputStreamResource(bis);

        String filename = String.format("Report_%s_Term%d_%s_%s.pdf",
                report.getClassName().replace(" ", "_"),
                term,
                report.getRollNumber(),
                effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @GetMapping("/pdf/student/yearly")
    public ResponseEntity<InputStreamResource> downloadStudentYearlyReportPdf(
            @RequestParam Long studentId,
            @RequestParam(required = false) String academicYear) throws IOException, DocumentException {

        log.info("Generating PDF yearly report for student {} academic year {}", studentId, academicYear);

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        YearlyReportDTO report = reportService.getYearlyReportForStudentAndYear(studentId, effectiveAcademicYear);
        byte[] pdfBytes = yearlyReportPdfService.generateYearlyReportPdf(report);

        ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
        InputStreamResource resource = new InputStreamResource(bis);

        String filename = String.format("Yearly_Report_%s_%s_%s.pdf",
                report.getRollNumber(),
                report.getStudentFullName().replace(" ", "_"),
                effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @GetMapping("/pdf/class")
    public ResponseEntity<byte[]> downloadClassTermPdf(
            @RequestParam Long classId,
            @RequestParam Integer term,
            @RequestParam(required = false) String academicYear) throws IOException, DocumentException {

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        List<ReportDTO> reports = reportService.getTermReportsForClassAndYear(
                classId, term, effectiveAcademicYear);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        byte[] pdfBytes = classTermReportPdfService.generateClassTermReportPdf(
                reports, classRoom, term, effectiveAcademicYear);

        String filename = String.format("Class_Term_%d_Report_%s_%s.pdf",
                term,
                classRoom.getName().replace(" ", "_"),
                effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/pdf/class/yearly")
    public ResponseEntity<byte[]> downloadClassYearlyPdf(
            @RequestParam Long classId,
            @RequestParam(required = false) String academicYear) throws IOException, DocumentException {

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        List<YearlyReportDTO> reports = reportService.getYearlyReportsForClassAndYear(
                classId, effectiveAcademicYear);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        byte[] pdfBytes = classYearlyReportPdfService.generateClassYearlyReportPdf(
                reports, classRoom, effectiveAcademicYear);

        String filename = String.format("Class_Yearly_Report_%s_%s.pdf",
                classRoom.getName().replace(" ", "_"),
                effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/batch/pdf")
    public ResponseEntity<byte[]> downloadBatchPdf(
            @RequestParam Long classId,
            @RequestParam String reportType,
            @RequestParam(required = false) Integer term,
            @RequestParam String academicYear) throws IOException, DocumentException {

        log.info("Generating batch PDF for class {} - Type: {}, Term: {}, Year: {}",
                classId, reportType, term, academicYear);

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        List<Student> students = studentRepository.findByClassRoomId(classId);
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        log.info("Found {} students in class {}", students.size(), classRoom.getName());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            int processedCount = 0;
            int errorCount = 0;

            for (Student student : students) {
                log.debug("Processing student: {} (ID: {})", student.getFullName(), student.getId());
                byte[] pdfBytes;
                String filename;

                try {
                    if ("yearly".equals(reportType)) {
                        YearlyReportDTO report = reportService.getYearlyReportForStudentAndYear(
                                student.getId(), effectiveAcademicYear);

                        pdfBytes = yearlyReportPdfService.generateYearlyReportPdf(report);
                        filename = String.format("%s_%s_Yearly_%s.pdf",
                                student.getRollNumber(),
                                student.getFullName().replace(" ", "_"),
                                effectiveAcademicYear);
                    } else {
                        if (term == null) {
                            throw new IllegalArgumentException("Term is required for term reports");
                        }

                        ReportDTO report = reportService.getTermReportForStudentAndYear(
                                student.getId(), term, effectiveAcademicYear);

                        pdfBytes = termReportPdfService.generateTermReportPdf(report);
                        filename = String.format("%s_%s_Term_%d_%s.pdf",
                                student.getRollNumber(),
                                student.getFullName().replace(" ", "_"),
                                term,
                                effectiveAcademicYear);
                    }

                    ZipEntry zipEntry = new ZipEntry(filename);
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(pdfBytes);
                    zipOut.closeEntry();
                    processedCount++;

                } catch (Exception e) {
                    log.error("Error generating report for student {} (ID: {}): {}",
                            student.getFullName(), student.getId(), e.getMessage(), e);
                    errorCount++;

                    byte[] errorPdf = createErrorPdf(student, reportType, term, effectiveAcademicYear, e);
                    String errorFilename = String.format("ERROR_%s_%s_%s.pdf",
                            student.getRollNumber(),
                            student.getFullName().replace(" ", "_"),
                            effectiveAcademicYear);
                    ZipEntry zipEntry = new ZipEntry(errorFilename);
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(errorPdf);
                    zipOut.closeEntry();
                }
            }

            log.info("Batch PDF generation completed: Processed={}, Errors={}, Total={}",
                    processedCount, errorCount, students.size());

        }

        String zipFilename = String.format("Batch_Reports_%s_%s_%s.zip",
                classRoom.getName().replace(" ", "_"),
                "yearly".equals(reportType) ? "Yearly" : "Term_" + term,
                effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(baos.toByteArray());
    }

    private byte[] createErrorPdf(Student student, String reportType, Integer term,
                                  String academicYear, Exception exception)
            throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.RED);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        document.add(new Paragraph("ERROR GENERATING REPORT", titleFont));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Student Details:", headerFont));
        document.add(new Paragraph("Name: " + student.getFullName(), contentFont));
        document.add(new Paragraph("Roll Number: " + student.getRollNumber(), contentFont));
        document.add(new Paragraph("Student ID: " + student.getStudentId(), contentFont));
        document.add(new Paragraph("Academic Year: " + academicYear, contentFont));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Report Details:", headerFont));
        document.add(new Paragraph("Report Type: " + reportType, contentFont));
        document.add(new Paragraph("Academic Year: " + academicYear, contentFont));
        if (term != null) {
            document.add(new Paragraph("Term: " + term, contentFont));
        }
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Error Information:", headerFont));
        document.add(new Paragraph("Error Message: " + exception.getMessage(), contentFont));

        document.close();

        return outputStream.toByteArray();
    }

    @GetMapping("/class")
    public String classReport(@RequestParam Long classId,
                              @RequestParam(defaultValue = "1") Integer term,
                              @RequestParam(required = false) String academicYear,
                              @RequestParam(required = false, defaultValue = "view") String action,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(defaultValue = "rankInClass") String sortBy,
                              @RequestParam(defaultValue = "asc") String sortDir,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        if ("download".equals(action)) {
            redirectAttributes.addAttribute("classId", classId);
            redirectAttributes.addAttribute("term", term);
            redirectAttributes.addAttribute("academicYear", effectiveAcademicYear);
            return "redirect:/reports/pdf/class";
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReportDTO> reportPage = reportService.getPaginatedTermReportsForClassAndYear(
                classId, term, effectiveAcademicYear, pageable);

        List<ReportDTO> reports = reportPage.getContent();

        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);
        model.addAttribute("academicYear", effectiveAcademicYear);

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
                                    @RequestParam(required = false) String academicYear,
                                    @RequestParam(required = false, defaultValue = "view") String action,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "yearlyRank") String sortBy,
                                    @RequestParam(defaultValue = "asc") String sortDir,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        if ("download".equals(action)) {
            redirectAttributes.addAttribute("classId", classId);
            redirectAttributes.addAttribute("academicYear", effectiveAcademicYear);
            return "redirect:/reports/pdf/class/yearly";
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<YearlyReportDTO> reportPage = reportService.getPaginatedYearlyReportsForClassAndYear(
                classId, effectiveAcademicYear, pageable);

        List<YearlyReportDTO> reports = reportPage.getContent();

        model.addAttribute("reports", reports);
        model.addAttribute("classRoom", classRoom);
        model.addAttribute("academicYear", effectiveAcademicYear);

        model.addAttribute("currentPage", reportPage.getNumber());
        model.addAttribute("totalPages", reportPage.getTotalPages());
        model.addAttribute("totalItems", reportPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "class_reports_yearly";
    }

    @GetMapping("/student/yearly/report")
    public String generateStudentYearlyReport(
            @RequestParam Long studentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false, defaultValue = "view") String action,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            if ("download".equals(action)) {
                redirectAttributes.addAttribute("studentId", studentId);
                redirectAttributes.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");
                return "redirect:/reports/pdf/student/yearly";
            }

            YearlyReportDTO report = reportService.getYearlyReportForStudentAndYear(
                    studentId, academicYear != null ? academicYear : "2025-2026");

            model.addAttribute("report", report);
            return "report_yearly";

        } catch (Exception e) {
            log.error("Error generating yearly report for student {}: {}", studentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Error generating yearly report: " + e.getMessage());
            return "redirect:/reports/select";
        }
    }

    @GetMapping("/student/term/report")
    public String termReportByStudentId(
            @RequestParam Long studentId,
            @RequestParam Integer term,
            @RequestParam(required = false) String academicYear,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            ReportDTO report = reportService.getTermReportForStudentAndYear(
                    studentId, term, academicYear != null ? academicYear : "2025-2026");

            model.addAttribute("report", report);
            return "report_term";

        } catch (Exception e) {
            log.error("Error generating term report for student {}: {}", studentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Error generating report: " + e.getMessage());
            return "redirect:/reports/select";
        }
    }

    // ====== ADDITIONAL UTILITY ENDPOINTS ======

    @GetMapping("/student/yearly/select")
    public String selectStudentYearlyReportForm(Model model) {
        List<ClassRoom> classes = classRoomRepository.findAll();
        model.addAttribute("classes", classes);
        return "select_student_yearly_form";
    }

    @PostMapping("/student/yearly/select")
    public String processStudentYearlySelection(
            @RequestParam Long classId,
            @RequestParam(required = false) String academicYear,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("classId", classId);
        redirectAttributes.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");
        return "redirect:/reports/student/yearly/list";
    }

    @GetMapping("/student/yearly/list")
    public String listStudentsForYearlyReport(
            @RequestParam Long classId,
            @RequestParam(required = false) String academicYear,
            Model model) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        List<Student> students = studentRepository.findByClassRoomId(classId);

        model.addAttribute("classRoom", classRoom);
        model.addAttribute("students", students);
        model.addAttribute("academicYear", academicYear != null ? academicYear : "2025-2026");

        return "select_student_yearly";
    }

    @GetMapping("/class/performance-summary")
    public String getClassPerformanceSummary(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "1") Integer term,
            @RequestParam(required = false) String academicYear,
            Model model) {

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID: " + classId));

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        List<ReportDTO> reports = reportService.getTermReportsForClassAndYear(
                classId, term, effectiveAcademicYear);

        double classAverage = reports.stream()
                .filter(r -> r.getTermAverage() != null)
                .mapToDouble(ReportDTO::getTermAverage)
                .average()
                .orElse(0.0);

        long passedStudents = reports.stream()
                .filter(ReportDTO::getPassed)
                .count();

        double passRate = !reports.isEmpty() ? (passedStudents * 100.0) / reports.size() : 0.0;

        List<ReportDTO> topStudents = reports.stream()
                .sorted((r1, r2) -> {
                    Double avg1 = r1.getTermAverage() != null ? r1.getTermAverage() : 0.0;
                    Double avg2 = r2.getTermAverage() != null ? r2.getTermAverage() : 0.0;
                    return avg2.compareTo(avg1);
                })
                .limit(5)
                .collect(Collectors.toList());

        List<ReportDTO> bottomStudents = reports.stream()
                .sorted((r1, r2) -> {
                    Double avg1 = r1.getTermAverage() != null ? r1.getTermAverage() : 0.0;
                    Double avg2 = r2.getTermAverage() != null ? r2.getTermAverage() : 0.0;
                    return avg1.compareTo(avg2);
                })
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Double> subjectAverages = new HashMap<>();
        if (!reports.isEmpty() && reports.getFirst().getSubjectReports() != null) {
            for (SubjectReport subject : reports.getFirst().getSubjectReports()) {
                String subjectName = subject.getSubjectName();
                double average = reports.stream()
                        .map(r -> {
                            SubjectReport sr = r.getSubjectReports().stream()
                                    .filter(s -> s.getSubjectName().equals(subjectName))
                                    .findFirst()
                                    .orElse(null);
                            return sr != null ? sr.getSubjectAverage() : null;
                        })
                        .filter(Objects::nonNull)
                        .mapToDouble(d -> d)
                        .average()
                        .orElse(0.0);
                subjectAverages.put(subjectName, average);
            }
        }

        model.addAttribute("classRoom", classRoom);
        model.addAttribute("term", term);
        model.addAttribute("academicYear", effectiveAcademicYear);
        model.addAttribute("reports", reports);
        model.addAttribute("classAverage", classAverage);
        model.addAttribute("totalStudents", reports.size());
        model.addAttribute("passedStudents", passedStudents);
        model.addAttribute("passRate", passRate);
        model.addAttribute("topStudents", topStudents);
        model.addAttribute("bottomStudents", bottomStudents);
        model.addAttribute("subjectAverages", subjectAverages);

        return "class_performance_summary";
    }

    @GetMapping("/pdf/summary/yearly")
    public ResponseEntity<byte[]> downloadYearlySummary(@RequestParam String academicYear)
            throws IOException, DocumentException {

        String effectiveAcademicYear = academicYear != null && !academicYear.isEmpty()
                ? academicYear : "2025-2026";

        YearlySummaryDTO summary = reportService.getYearlySummary(effectiveAcademicYear);
        byte[] pdfBytes = yearlySummaryPdfService.generateYearlySummaryPdf(summary);

        String filename = String.format("Yearly_Summary_%s.pdf", effectiveAcademicYear);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/health")
    @ResponseBody
    public String healthCheck() {
        return "Report Controller is running.";
    }

    @GetMapping("/debug/batch")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugBatchProcessing(
            @RequestParam Long classId,
            @RequestParam Integer term,
            @RequestParam String academicYear) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Student> students = studentRepository.findByClassRoomId(classId);
            result.put("totalStudents", students.size());

            List<Map<String, Object>> studentDetails = new ArrayList<>();

            for (Student student : students) {
                Map<String, Object> studentInfo = new HashMap<>();
                studentInfo.put("id", student.getId());
                studentInfo.put("name", student.getFullName());
                studentInfo.put("rollNumber", student.getRollNumber());

                ReportDTO report = reportService.getTermReportForStudentAndYear(
                        student.getId(), term, academicYear);

                List<Map<String, Object>> subjectInfo = new ArrayList<>();
                for (SubjectReport sr : report.getSubjectReports()) {
                    Map<String, Object> subjectDetail = new HashMap<>();
                    subjectDetail.put("name", sr.getSubjectName());
                    subjectDetail.put("average", sr.getSubjectAverage());
                    subjectDetail.put("coefficient", sr.getCoefficient());
                    subjectDetail.put("grade", sr.getLetterGrade());
                    subjectInfo.add(subjectDetail);
                }

                studentInfo.put("termAverage", report.getTermAverage());
                studentInfo.put("subjectCount", report.getSubjectReports().size());
                studentInfo.put("subjectDetails", subjectInfo);

                studentDetails.add(studentInfo);
            }

            result.put("students", studentDetails);
            result.put("classId", classId);
            result.put("term", term);
            result.put("academicYear", academicYear);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/check/student")
    public ResponseEntity<String> checkStudentReports(@RequestParam Long studentId) {
        try {
            List<Integer> terms = reportService.getAvailableTermsForStudent(studentId);
            List<Integer> years = reportService.getAvailableAcademicYearsForStudent(studentId);

            return ResponseEntity.ok(String.format(
                    "Student %s has reports for terms: %s and academic years: %s",
                    studentId, terms, years));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
