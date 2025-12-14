package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.YearlySummaryDTO;
import com.akentech.schoolreport.dto.ClassSummaryDTO;
import com.lowagie.text.*;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class YearlySummaryPdfService extends BasePdfService {

    public YearlySummaryPdfService(GradeService gradeService) {
        super(gradeService);
    }

    public byte[] generateYearlySummaryPdf(YearlySummaryDTO summary) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        addYearlySummaryHeader(document, summary, writer);
        addOverallStatisticsSection(document, summary);
        addClassSummariesTable(document, summary);
        addAnalysisAndRecommendations(document, summary);

        document.close();
        return outputStream.toByteArray();
    }

    // ====== PRIVATE METHODS ======

    private void addYearlySummaryHeader(Document document, YearlySummaryDTO summary, PdfWriter writer)
            throws DocumentException, IOException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 6, 2});
        headerTable.setSpacingBefore(5);

        try {
            PdfPCell leftImageCell = createModernImageCell(LEFT_LOGO_PATH, Element.ALIGN_LEFT, 50);
            headerTable.addCell(leftImageCell);

            PdfPCell centerCell = createYearlySummaryHeaderContentCell(summary);
            headerTable.addCell(centerCell);

            PdfPCell rightImageCell = createModernImageCell(RIGHT_LOGO_PATH, Element.ALIGN_RIGHT, 50);
            headerTable.addCell(rightImageCell);

        } catch (IOException e) {
            log.warn("Could not load logo images: {}", e.getMessage());
            createYearlySummaryFallbackHeader(document, summary);
            return;
        }

        document.add(headerTable);
        addModernSeparator(document);
    }

    private PdfPCell createYearlySummaryHeaderContentCell(YearlySummaryDTO summary) {
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

        Paragraph title = new Paragraph("ACADEMIC YEAR SUMMARY REPORT",
                FontFactory.getFont("Helvetica-Bold", 12, SECONDARY_COLOR));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(3);
        contentCell.addElement(title);

        Paragraph yearParagraph = new Paragraph("Academic Year: " + summary.getAcademicYear(),
                FontFactory.getFont(FontFactory.HELVETICA, 10, ACCENT_COLOR));
        yearParagraph.setAlignment(Element.ALIGN_CENTER);
        yearParagraph.setSpacingAfter(5);
        contentCell.addElement(yearParagraph);

        return contentCell;
    }

    private void createYearlySummaryFallbackHeader(Document document, YearlySummaryDTO summary)
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

        Paragraph title = new Paragraph("ACADEMIC YEAR SUMMARY REPORT",
                FontFactory.getFont("Helvetica-Bold", 14, SECONDARY_COLOR));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        headerCell.addElement(title);

        Paragraph yearParagraph = new Paragraph("Academic Year: " + summary.getAcademicYear(),
                FontFactory.getFont(FontFactory.HELVETICA, 11, ACCENT_COLOR));
        yearParagraph.setAlignment(Element.ALIGN_CENTER);
        yearParagraph.setSpacingAfter(5);
        headerCell.addElement(yearParagraph);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        addModernSeparator(document);
    }

    private void addOverallStatisticsSection(Document document, YearlySummaryDTO summary) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("OVERALL ACADEMIC PERFORMANCE", PRIMARY_COLOR);

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setWidths(new float[]{1, 1, 1, 1});

        addStatCell(statsTable, "Total Classes", String.valueOf(summary.getTotalClasses()), PRIMARY_COLOR);
        addStatCell(statsTable, "Total Students", String.valueOf(summary.getTotalStudents()), ACCENT_COLOR);
        addStatCell(statsTable, "Overall Average", String.format("%.2f/20", summary.getOverallAverage()),
                getPerformanceColor(summary.getOverallAverage()));
        addStatCell(statsTable, "Overall Pass Rate", String.format("%.1f%%", summary.getOverallPassRate()),
                getPassRateColor(summary.getOverallPassRate()));

        titleCell.addElement(statsTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }

    private void addClassSummariesTable(Document document, YearlySummaryDTO summary) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("CLASS PERFORMANCE SUMMARY", SECONDARY_COLOR);

        PdfPTable classTable = new PdfPTable(8);
        classTable.setWidthPercentage(100);
        classTable.setWidths(new float[]{2, 2, 1, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

        String[] headers = {"Class", "Teacher", "Size", "Average", "Pass Rate", "Passed", "Failed", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(new RoundedBorderCellEvent(3, PRIMARY_COLOR, 0));
            classTable.addCell(cell);
        }

        boolean alternate = false;
        for (ClassSummaryDTO classSummary : summary.getClassSummaries()) {
            Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;
            alternate = !alternate;

            addClassSummaryCell(classTable, classSummary.getClassName(), rowColor, Element.ALIGN_LEFT);
            addClassSummaryCell(classTable, classSummary.getClassTeacher() != null ? classSummary.getClassTeacher() : "N/A",
                    rowColor, Element.ALIGN_LEFT);
            addClassSummaryCell(classTable, String.valueOf(classSummary.getClassSize()), rowColor, Element.ALIGN_CENTER);

            Color avgColor = getPerformanceColor(classSummary.getClassAverage());
            addClassSummaryCell(classTable, String.format("%.2f", classSummary.getClassAverage()),
                    avgColor, Element.ALIGN_CENTER);

            Color prColor = getPassRateColor(classSummary.getPassRate());
            addClassSummaryCell(classTable, String.format("%.1f%%", classSummary.getPassRate()),
                    prColor, Element.ALIGN_CENTER);

            addClassSummaryCell(classTable, String.valueOf(classSummary.getTotalPassed()),
                    SUCCESS_COLOR, Element.ALIGN_CENTER);
            addClassSummaryCell(classTable, String.valueOf(classSummary.getTotalFailed()),
                    DANGER_COLOR, Element.ALIGN_CENTER);

            String status = classSummary.getPassRate() >= 60 ? "GOOD" :
                    classSummary.getPassRate() >= 40 ? "FAIR" : "POOR";
            Color statusColor = classSummary.getPassRate() >= 60 ? SUCCESS_COLOR :
                    classSummary.getPassRate() >= 40 ? WARNING_COLOR : DANGER_COLOR;
            addClassSummaryCell(classTable, status, statusColor, Element.ALIGN_CENTER);
        }

        titleCell.addElement(classTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }

    private void addClassSummaryCell(PdfPTable table, String text, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(bgColor))));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new RoundedBorderCellEvent(3, bgColor, 0));
        table.addCell(cell);
    }

    private void addAnalysisAndRecommendations(Document document, YearlySummaryDTO summary) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("ANALYSIS AND RECOMMENDATIONS", ACCENT_COLOR);

        StringBuilder analysis = new StringBuilder();
        analysis.append("Overall Academic Year Performance Analysis:\n\n");

        if (summary.getOverallPassRate() >= 70) {
            analysis.append("• EXCELLENT performance across all classes\n");
            analysis.append("• Strong academic standards maintained\n");
            analysis.append("• High promotion rate indicates effective teaching\n");
        } else if (summary.getOverallPassRate() >= 50) {
            analysis.append("• SATISFACTORY performance with room for improvement\n");
            analysis.append("• Some classes require additional support\n");
            analysis.append("• Consider targeted interventions for weaker subjects\n");
        } else {
            analysis.append("• NEEDS IMPROVEMENT across the school\n");
            analysis.append("• Urgent action required for struggling classes\n");
            analysis.append("• Consider curriculum review and teacher training\n");
        }

        analysis.append("\nKey Recommendations:\n");
        analysis.append("1. Review performance data with department heads\n");
        analysis.append("2. Implement targeted support for struggling students\n");
        analysis.append("3. Schedule teacher workshops for effective teaching methods\n");
        analysis.append("4. Monitor progress with regular assessments\n");
        analysis.append("5. Engage parents in academic improvement plans\n");

        Paragraph analysisParagraph = new Paragraph(analysis.toString(),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        analysisParagraph.setSpacingBefore(5);
        titleCell.addElement(analysisParagraph);

        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }
}