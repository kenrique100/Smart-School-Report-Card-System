package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.YearlyReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ReportService {

    // Class reports (unchanged)
    List<ReportDTO> generateReportsForClass(Long classId, Integer term);
    Page<ReportDTO> generatePaginatedReportsForClass(Long classId, Integer term, Pageable pageable);
    CompletableFuture<List<ReportDTO>> generateReportsForClassAsync(Long classId, Integer term);

    List<YearlyReportDTO> generateYearlyReportsForClass(Long classId);
    Page<YearlyReportDTO> generatePaginatedYearlyReportsForClass(Long classId, Pageable pageable);

    // NEW: Student reports by class and roll number (instead of ID)
    ReportDTO generateReportForStudentByClassAndRollNumber(Long classId, String rollNumber, Integer term);
    YearlyReportDTO generateYearlyReportForStudentByClassAndRollNumber(Long classId, String rollNumber);

    // Deprecated: Remove or keep for backward compatibility with warning
    @Deprecated
    ReportDTO generateReportForStudent(Long studentId, Integer term);

    @Deprecated
    YearlyReportDTO generateYearlyReportForStudent(Long studentId);

    List<Integer> getAvailableTermsForStudent(Long studentId);
    List<Integer> getAvailableAcademicYearsForStudent(Long studentId);
}