package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.dto.ImportResult;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.service.AssessmentService;
import com.akentech.schoolreport.service.ExcelExportService;
import com.akentech.schoolreport.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assessments")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    @GetMapping("/entry")
    public String entryForm(Model model) {
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("classrooms", classRoomRepository.findAll());
        model.addAttribute("assessment", new Assessment());
        return "assessments";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Assessment assessment) {
        assessmentService.save(assessment);
        return "redirect:/assessments/entry?success";
    }

    /**
     * Download Excel template for a single term
     */
    @GetMapping("/download-excel")
    public ResponseEntity<byte[]> downloadExcelTemplate(
            @RequestParam Long classRoomId,
            @RequestParam Integer term) {
        try {
            ClassRoom classRoom = classRoomRepository.findById(classRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found"));

            byte[] excelData = excelExportService.exportAssessmentTemplate(classRoomId, term);
            String fileName = excelExportService.generateFileName(classRoom, term);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            log.error("Error generating Excel template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download Excel template for all terms
     */
    @GetMapping("/download-excel-all-terms")
    public ResponseEntity<byte[]> downloadExcelTemplateAllTerms(@RequestParam Long classRoomId) {
        try {
            ClassRoom classRoom = classRoomRepository.findById(classRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found"));

            byte[] excelData = excelExportService.exportAssessmentTemplateAllTerms(classRoomId);
            String fileName = excelExportService.generateFileName(classRoom, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            log.error("Error generating Excel template for all terms", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Upload and import Excel file with assessments
     */
    @PostMapping("/upload-excel")
    public String uploadExcel(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            ImportResult result = excelImportService.importAssessments(file);

            if (result.hasErrors()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Import completed with errors. Success: " + result.getSuccessCount() +
                        ", Errors: " + result.getErrorCount());
                redirectAttributes.addFlashAttribute("importErrors", result.getErrors());
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Import successful! " + result.getSuccessCount() + " assessments imported.");
            }

            if (result.hasWarnings()) {
                redirectAttributes.addFlashAttribute("importWarnings", result.getWarnings());
            }

        } catch (Exception e) {
            log.error("Error uploading Excel file", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error uploading file: " + e.getMessage());
        }

        return "redirect:/assessments/entry";
    }
}