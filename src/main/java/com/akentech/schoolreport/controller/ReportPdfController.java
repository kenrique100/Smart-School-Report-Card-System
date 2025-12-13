package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.service.ReportPdfService;
import com.akentech.schoolreport.service.ReportService;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Controller
@RequestMapping("/reports/pdf")
@RequiredArgsConstructor
@Slf4j
public class ReportPdfController {

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;

    @GetMapping("/student/term")
    public ResponseEntity<InputStreamResource> downloadStudentTermReportPdf(
            @RequestParam Long studentId,
            @RequestParam Integer term) throws IOException, DocumentException {

        log.info("Generating PDF report for student {} term {}", studentId, term);

        // Generate report
        ReportDTO report = reportService.generateReportForStudent(studentId, term);

        // Generate PDF
        byte[] pdfBytes = reportPdfService.generateTermReportPdf(report);

        // Create response
        ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
        InputStreamResource resource = new InputStreamResource(bis);

        String filename = String.format("Report_Term%d_%s_%s.pdf",
                term,
                report.getStudent().getRollNumber(),
                report.getStudentFullName().replace(" ", "_"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @GetMapping("/student/term/class")
    public ResponseEntity<InputStreamResource> downloadStudentTermReportPdfByClass(
            @RequestParam Long classId,
            @RequestParam Long studentId,
            @RequestParam Integer term) throws IOException, DocumentException {

        log.info("Generating PDF report for student {} in class {} term {}", studentId, classId, term);

        // Generate report
        ReportDTO report = reportService.generateReportForStudent(studentId, term);

        // Generate PDF
        byte[] pdfBytes = reportPdfService.generateTermReportPdf(report);

        // Create response
        ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
        InputStreamResource resource = new InputStreamResource(bis);

        String filename = String.format("Report_%s_Term%d_%s.pdf",
                report.getStudent().getClassRoom().getName().replace(" ", "_"),
                term,
                report.getStudent().getRollNumber());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }
}