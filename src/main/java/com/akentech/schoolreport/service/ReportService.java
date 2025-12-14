package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.YearlyReportDTO;
import com.akentech.schoolreport.dto.YearlySummaryDTO;
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

    // NEW: Methods with academic year parameter
    ReportDTO getTermReportForStudentAndYear(Long studentId, Integer term, String academicYear);
    YearlyReportDTO getYearlyReportForStudentAndYear(Long studentId, String academicYear);

    List<ReportDTO> getTermReportsForClassAndYear(Long classId, Integer term, String academicYear);
    Page<ReportDTO> getPaginatedTermReportsForClassAndYear(Long classId, Integer term, String academicYear, Pageable pageable);

    List<YearlyReportDTO> getYearlyReportsForClassAndYear(Long classId, String academicYear);
    Page<YearlyReportDTO> getPaginatedYearlyReportsForClassAndYear(Long classId, String academicYear, Pageable pageable);

    YearlySummaryDTO getYearlySummary(String academicYear);

    // Deprecated: Remove or keep for backward compatibility with warning
    @Deprecated
    ReportDTO generateReportForStudent(Long studentId, Integer term);

    @Deprecated
    YearlyReportDTO generateYearlyReportForStudent(Long studentId);

    List<Integer> getAvailableTermsForStudent(Long studentId);
    List<Integer> getAvailableAcademicYearsForStudent(Long studentId);
}