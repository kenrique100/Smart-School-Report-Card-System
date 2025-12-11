package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.YearlyReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ReportService {
    ReportDTO generateReportForStudent(Long studentId, Integer term);
    YearlyReportDTO generateYearlyReportForStudent(Long studentId);
    List<ReportDTO> generateReportsForClass(Long classId, Integer term);

    // NEW: Add paginated methods to interface
    Page<ReportDTO> generatePaginatedReportsForClass(Long classId, Integer term, Pageable pageable);
    Page<YearlyReportDTO> generatePaginatedYearlyReportsForClass(Long classId, Pageable pageable);

    CompletableFuture<List<ReportDTO>> generateReportsForClassAsync(Long classId, Integer term);
    List<YearlyReportDTO> generateYearlyReportsForClass(Long classId);
    List<Integer> getAvailableTermsForStudent(Long studentId);
    List<Integer> getAvailableAcademicYearsForStudent(Long studentId);
}