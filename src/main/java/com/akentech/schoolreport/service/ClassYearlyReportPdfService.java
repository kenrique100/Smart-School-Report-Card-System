package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.YearlyReportDTO;
import com.akentech.schoolreport.model.ClassRoom;
import com.lowagie.text.*;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ClassYearlyReportPdfService extends BasePdfService {

    // Explicit constructor
    public ClassYearlyReportPdfService(GradeService gradeService) {
        super(gradeService);
    }

    public byte[] generateClassYearlyReportPdf(List<YearlyReportDTO> reports, ClassRoom classRoom, String academicYear)
            throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        addClassYearlyHeader(document, classRoom, academicYear, writer);
        addClassYearlySummarySection(document, reports, classRoom);
        addClassYearlyPerformanceTable(document, reports);
        addClassPromotionStatistics(document, reports, classRoom);

        document.close();
        return outputStream.toByteArray();
    }

    // ====== PRIVATE METHODS ======

    private void addClassYearlyHeader(Document document, ClassRoom classRoom, String academicYear, PdfWriter writer)
            throws DocumentException, IOException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 6, 2});
        headerTable.setSpacingBefore(5);

        try {
            PdfPCell leftImageCell = createModernImageCell(LEFT_LOGO_PATH, Element.ALIGN_LEFT, 50);
            headerTable.addCell(leftImageCell);

            PdfPCell centerCell = createClassYearlyHeaderContentCell(classRoom, academicYear);
            headerTable.addCell(centerCell);

            PdfPCell rightImageCell = createModernImageCell(RIGHT_LOGO_PATH, Element.ALIGN_RIGHT, 50);
            headerTable.addCell(rightImageCell);

        } catch (IOException e) {
            log.warn("Could not load logo images: {}", e.getMessage());
            createClassYearlyFallbackHeader(document, classRoom, academicYear);
            return;
        }

        document.add(headerTable);
        addModernSeparator(document);
    }

    private PdfPCell createClassYearlyHeaderContentCell(ClassRoom classRoom, String academicYear) {
        PdfPCell contentCell = new PdfPCell();
        contentCell.setBorder(Rectangle.NO_BORDER);
        contentCell.setBackgroundColor(Color.WHITE);
        contentCell.setPadding(5);
        contentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        contentCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School",
                FontFactory.getFont("Helvetica-Bold", 16, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(3);
        contentCell.addElement(schoolName);

        Paragraph classInfo = new Paragraph("CLASS YEARLY REPORT - " + classRoom.getName(),
                FontFactory.getFont("Helvetica-Bold", 12, SECONDARY_COLOR));
        classInfo.setAlignment(Element.ALIGN_CENTER);
        classInfo.setSpacingAfter(3);
        contentCell.addElement(classInfo);

        Paragraph yearParagraph = new Paragraph("Academic Year: " + academicYear,
                FontFactory.getFont(FontFactory.HELVETICA, 10, ACCENT_COLOR));
        yearParagraph.setAlignment(Element.ALIGN_CENTER);
        yearParagraph.setSpacingAfter(5);
        contentCell.addElement(yearParagraph);

        return contentCell;
    }

    private void createClassYearlyFallbackHeader(Document document, ClassRoom classRoom, String academicYear)
            throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(5);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setBackgroundColor(Color.WHITE);
        headerCell.setPadding(10);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School",
                FontFactory.getFont("Helvetica-Bold", 18, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(3);
        headerCell.addElement(schoolName);

        Paragraph classInfo = new Paragraph("CLASS YEARLY REPORT - " + classRoom.getName(),
                FontFactory.getFont("Helvetica-Bold", 14, SECONDARY_COLOR));
        classInfo.setAlignment(Element.ALIGN_CENTER);
        classInfo.setSpacingAfter(5);
        headerCell.addElement(classInfo);

        Paragraph yearParagraph = new Paragraph("Academic Year: " + academicYear,
                FontFactory.getFont(FontFactory.HELVETICA, 11, ACCENT_COLOR));
        yearParagraph.setAlignment(Element.ALIGN_CENTER);
        yearParagraph.setSpacingAfter(5);
        headerCell.addElement(yearParagraph);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        addModernSeparator(document);
    }

    private void addClassYearlySummarySection(Document document, List<YearlyReportDTO> reports, ClassRoom classRoom)
            throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(1);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("YEARLY CLASS SUMMARY", PRIMARY_COLOR);

        int totalStudents = reports.size();
        long passedStudents = reports.stream().filter(YearlyReportDTO::getPassed).count();
        double classAverage = reports.stream()
                .filter(r -> r.getYearlyAverage() != null)
                .mapToDouble(YearlyReportDTO::getYearlyAverage)
                .average()
                .orElse(0.0);
        double avgPassRate = reports.stream()
                .filter(r -> r.getPassRate() != null)
                .mapToDouble(YearlyReportDTO::getPassRate)
                .average()
                .orElse(0.0);

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setWidths(new float[]{1, 1, 1, 1});

        addStatCell(statsTable, "Total Students", String.valueOf(totalStudents), PRIMARY_COLOR);
        addStatCell(statsTable, "Class Average", String.format("%.2f/20", classAverage), ACCENT_COLOR);
        addStatCell(statsTable, "Promoted", String.valueOf(passedStudents), SUCCESS_COLOR);
        addStatCell(statsTable, "Avg Pass Rate", String.format("%.1f%%", avgPassRate),
                avgPassRate >= 60 ? SUCCESS_COLOR : WARNING_COLOR);

        titleCell.addElement(statsTable);
        summaryTable.addCell(titleCell);
        document.add(summaryTable);
    }

    private void addClassYearlyPerformanceTable(Document document, List<YearlyReportDTO> reports)
            throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("YEARLY PERFORMANCE RANKING", SECONDARY_COLOR);

        PdfPTable performanceTable = new PdfPTable(8);
        performanceTable.setWidthPercentage(100);
        performanceTable.setWidths(new float[]{1, 2, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

        String[] headers = {"Rank", "Student", "Roll No", "Yearly Avg", "Grade", "Pass Rate", "Status", "Remarks"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(new RoundedBorderCellEvent(3, PRIMARY_COLOR, 0));
            performanceTable.addCell(cell);
        }

        reports.sort((r1, r2) -> {
            Integer rank1 = r1.getYearlyRank() != null ? r1.getYearlyRank() : Integer.MAX_VALUE;
            Integer rank2 = r2.getYearlyRank() != null ? r2.getYearlyRank() : Integer.MAX_VALUE;
            return rank1.compareTo(rank2);
        });

        boolean alternate = false;
        for (YearlyReportDTO report : reports) {
            Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;
            alternate = !alternate;

            addClassYearlyCell(performanceTable,
                    report.getYearlyRank() != null ? report.getYearlyRank().toString() : "-",
                    rowColor, Element.ALIGN_CENTER);
            addClassYearlyCell(performanceTable, report.getStudentFullName(), rowColor, Element.ALIGN_LEFT);
            addClassYearlyCell(performanceTable, report.getRollNumber(), rowColor, Element.ALIGN_CENTER);

            Color avgColor = getScoreColor(report.getYearlyAverage());
            addClassYearlyCell(performanceTable,
                    report.getFormattedYearlyAverage() != null ? report.getFormattedYearlyAverage() : "N/A",
                    avgColor, Element.ALIGN_CENTER);

            Color gradeColor = getGradeColor(report.getOverallGrade());
            addClassYearlyCell(performanceTable, report.getOverallGrade(), gradeColor, Element.ALIGN_CENTER);

            Color prColor = getPassRateColor(report.getPassRate());
            addClassYearlyCell(performanceTable,
                    report.getFormattedPassRate() != null ? report.getFormattedPassRate() : "N/A",
                    prColor, Element.ALIGN_CENTER);

            String status = report.getPassed() ? "PROMOTED" : "RETAINED";
            Color statusColor = report.getPassed() ? SUCCESS_COLOR : DANGER_COLOR;
            addClassYearlyCell(performanceTable, status, statusColor, Element.ALIGN_CENTER);

            addClassYearlyCell(performanceTable,
                    report.getRemarks() != null ? report.getRemarks() : "No remarks",
                    rowColor, Element.ALIGN_LEFT);
        }

        titleCell.addElement(performanceTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }

    private void addClassYearlyCell(PdfPTable table, String text, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(bgColor))));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new RoundedBorderCellEvent(3, bgColor, 0));
        table.addCell(cell);
    }

    private void addClassPromotionStatistics(Document document, List<YearlyReportDTO> reports, ClassRoom classRoom)
            throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("PROMOTION STATISTICS", ACCENT_COLOR);

        int totalStudents = reports.size();
        long promoted = reports.stream().filter(YearlyReportDTO::getPassed).count();
        long retained = totalStudents - promoted;
        double promotionRate = totalStudents > 0 ? (promoted * 100.0) / totalStudents : 0.0;

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(80);
        statsTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        statsTable.setWidths(new float[]{1, 1, 1, 1});

        addStatCell(statsTable, "Total", String.valueOf(totalStudents), PRIMARY_COLOR);
        addStatCell(statsTable, "Promoted", String.valueOf(promoted), SUCCESS_COLOR);
        addStatCell(statsTable, "Retained", String.valueOf(retained), DANGER_COLOR);
        addStatCell(statsTable, "Promotion Rate", String.format("%.1f%%", promotionRate),
                promotionRate >= 70 ? SUCCESS_COLOR : (promotionRate >= 50 ? WARNING_COLOR : DANGER_COLOR));

        titleCell.addElement(statsTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }
}