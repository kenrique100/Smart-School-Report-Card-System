package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.YearlyReportDTO;
import com.akentech.schoolreport.dto.TermReportSummary;
import com.akentech.schoolreport.dto.YearlySubjectReport;
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
public class YearlyReportPdfService extends BasePdfService {

    public YearlyReportPdfService(GradeService gradeService) {
        super(gradeService);
    }

    public byte[] generateYearlyReportPdf(YearlyReportDTO report) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        addSchoolHeader(document, report, writer);
        addYearlySummarySection(document, report);
        addTermComparisonSection(document, report);
        addYearlySubjectPerformance(document, report);
        addPromotionSection(document, report);
        addSignatureSection(document, report);

        document.close();
        return outputStream.toByteArray();
    }

    // ====== PRIVATE METHODS ======

    private void addSchoolHeader(Document document, YearlyReportDTO report, PdfWriter writer)
            throws DocumentException, IOException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 6, 2});
        headerTable.setSpacingBefore(5);

        try {
            PdfPCell leftImageCell = createModernImageCell(LEFT_LOGO_PATH, Element.ALIGN_LEFT, 50);
            headerTable.addCell(leftImageCell);

            PdfPCell centerCell = createModernHeaderContentCell(report);
            headerTable.addCell(centerCell);

            PdfPCell rightImageCell = createModernImageCell(RIGHT_LOGO_PATH, Element.ALIGN_RIGHT, 50);
            headerTable.addCell(rightImageCell);

        } catch (IOException e) {
            log.warn("Could not load logo images: {}", e.getMessage());
            createModernFallbackHeader(document, report);
            return;
        }

        document.add(headerTable);
        addModernSeparator(document);
    }

    private PdfPCell createModernHeaderContentCell(YearlyReportDTO report) {
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

        Paragraph arcEffect = new Paragraph();
        for (int i = 0; i < 20; i++) {
            arcEffect.add(new Chunk("• ",
                    FontFactory.getFont(FontFactory.HELVETICA, 6, new Color(200, 200, 200))));
        }
        arcEffect.setAlignment(Element.ALIGN_CENTER);
        arcEffect.setSpacingAfter(5);
        contentCell.addElement(arcEffect);

        Paragraph motto = new Paragraph("Excellence • In • Creativity • And • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA, 9, ACCENT_COLOR));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(5);
        contentCell.addElement(motto);

        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(50);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell badgeCell = new PdfPCell();
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setBackgroundColor(new Color(240, 240, 240));
        badgeCell.setPadding(4);
        badgeCell.setCellEvent(new RoundedBorderCellEvent(8, new Color(200, 200, 200), 0.5f));

        Paragraph academicInfo = new Paragraph(
                String.format("Academic Year: %s | YEARLY REPORT", report.getAcademicYear()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        badgeCell.addElement(academicInfo);

        badgeTable.addCell(badgeCell);
        contentCell.addElement(badgeTable);

        Paragraph address = new Paragraph("Kotto Road Kombe | Phone: 677755377/670252217",
                FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(150, 150, 150)));
        address.setAlignment(Element.ALIGN_CENTER);
        address.setSpacingBefore(5);
        contentCell.addElement(address);

        return contentCell;
    }

    private void createModernFallbackHeader(Document document, YearlyReportDTO report)
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

        Paragraph underline = new Paragraph();
        for (int i = 0; i < 15; i++) {
            underline.add(new Chunk("∼ ",
                    FontFactory.getFont(FontFactory.HELVETICA, 6, ACCENT_COLOR)));
        }
        underline.setAlignment(Element.ALIGN_CENTER);
        underline.setSpacingAfter(5);
        headerCell.addElement(underline);

        Paragraph motto = new Paragraph("Excellence • In • Creativity • And • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(10);
        headerCell.addElement(motto);

        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(60);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setBackgroundColor(new Color(240, 240, 240));
        infoCell.setPadding(6);
        infoCell.setCellEvent(new RoundedBorderCellEvent(8, PRIMARY_COLOR, 1f));

        Paragraph academicInfo = new Paragraph(
                String.format("ACADEMIC YEAR: %s | YEARLY REPORT", report.getAcademicYear()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INFO_COLOR));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        infoCell.addElement(academicInfo);

        infoTable.addCell(infoCell);
        headerCell.addElement(infoTable);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        addModernSeparator(document);
    }

    private void addYearlySummarySection(Document document, YearlyReportDTO report) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        PdfPCell summaryCell = createModernCardCell("YEARLY PERFORMANCE SUMMARY", PRIMARY_COLOR);
        summaryCell.setColspan(2);

        Paragraph averageBadge = new Paragraph(report.getFormattedYearlyAverage(),
                FontFactory.getFont("Helvetica-Bold", 16, SUCCESS_COLOR));
        averageBadge.setAlignment(Element.ALIGN_CENTER);
        averageBadge.setSpacingAfter(5);
        summaryCell.addElement(averageBadge);

        Paragraph overallGrade = new Paragraph("Overall Grade: " + report.getOverallGrade(),
                FontFactory.getFont("Helvetica-Bold", 12, getGradeColor(report.getOverallGrade())));
        overallGrade.setAlignment(Element.ALIGN_CENTER);
        overallGrade.setSpacingAfter(3);
        summaryCell.addElement(overallGrade);

        Paragraph rankParagraph = new Paragraph("Yearly Rank: " + report.getYearlyRank() + " of " + report.getTotalStudentsInClass(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INFO_COLOR));
        rankParagraph.setAlignment(Element.ALIGN_CENTER);
        summaryCell.addElement(rankParagraph);

        Color statusColor = report.getPassed() ? SUCCESS_COLOR : DANGER_COLOR;
        Paragraph statusParagraph = new Paragraph(report.getPassed() ? "PASSED" : "FAILED",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, getContrastColor(statusColor)));
        statusParagraph.setAlignment(Element.ALIGN_CENTER);
        PdfPTable statusTable = new PdfPTable(1);
        statusTable.setWidthPercentage(30);
        statusTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell statusCell = new PdfPCell(statusParagraph);
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setBackgroundColor(statusColor);
        statusCell.setPadding(5);
        statusCell.setCellEvent(new RoundedBorderCellEvent(8, statusColor, 0));
        statusTable.addCell(statusCell);
        summaryCell.addElement(statusTable);

        table.addCell(summaryCell);
        document.add(table);
    }

    private void addTermComparisonSection(Document document, YearlyReportDTO report) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("TERM-BY-TERM COMPARISON", SECONDARY_COLOR);
        titleCell.setColspan(1);

        PdfPTable comparisonTable = new PdfPTable(6);
        comparisonTable.setWidthPercentage(100);
        comparisonTable.setWidths(new float[]{2, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

        String[] headers = {"Term", "Average", "Grade", "Rank", "Pass/Fail", "Remarks"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(new RoundedBorderCellEvent(3, PRIMARY_COLOR, 0));
            comparisonTable.addCell(cell);
        }

        java.util.List<TermReportSummary> termSummaries = report.getTermSummaries();
        boolean alternate = false;

        for (TermReportSummary termSummary : termSummaries) {
            Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;
            alternate = !alternate;

            addTermComparisonCell(comparisonTable, "Term " + termSummary.getTerm(), rowColor, Element.ALIGN_CENTER);
            addTermComparisonCell(comparisonTable,
                    termSummary.getFormattedAverage() != null ? termSummary.getFormattedAverage() : "N/A",
                    rowColor, Element.ALIGN_CENTER);

            String grade = gradeService.calculateLetterGrade(termSummary.getTermAverage(), report.getClassName());
            Color gradeColor = getGradeColor(grade);
            addTermComparisonCell(comparisonTable, grade, gradeColor, Element.ALIGN_CENTER);

            addTermComparisonCell(comparisonTable,
                    termSummary.getRankInClass() != null ? termSummary.getRankInClass().toString() : "N/A",
                    rowColor, Element.ALIGN_CENTER);

            String passFail = termSummary.getPassed() ? "PASS" : "FAIL";
            Color pfColor = termSummary.getPassed() ? SUCCESS_COLOR : DANGER_COLOR;
            addTermComparisonCell(comparisonTable, passFail, pfColor, Element.ALIGN_CENTER);

            addTermComparisonCell(comparisonTable,
                    termSummary.getRemarks() != null ? termSummary.getRemarks() : "No remarks",
                    rowColor, Element.ALIGN_LEFT);
        }

        titleCell.addElement(comparisonTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }

    private void addTermComparisonCell(PdfPTable table, String text, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(bgColor))));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new RoundedBorderCellEvent(3, bgColor, 0));
        table.addCell(cell);
    }

    private void addYearlySubjectPerformance(Document document, YearlyReportDTO report) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell titleCell = createModernCardCell("YEARLY SUBJECT PERFORMANCE", ACCENT_COLOR);
        titleCell.setColspan(1);

        PdfPTable subjectTable = new PdfPTable(8);
        subjectTable.setWidthPercentage(100);
        subjectTable.setWidths(new float[]{2.5f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

        String[] headers = {"Subject", "Coeff", "Term 1", "Term 2", "Term 3", "Yearly Avg", "Grade", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(new RoundedBorderCellEvent(3, PRIMARY_COLOR, 0));
            subjectTable.addCell(cell);
        }

        java.util.List<YearlySubjectReport> yearlySubjectReports = report.getSubjectReports();
        boolean alternate = false;

        for (YearlySubjectReport subjectReport : yearlySubjectReports) {
            Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;
            alternate = !alternate;

            addYearlySubjectCell(subjectTable, subjectReport.getSubjectName(), rowColor, Element.ALIGN_LEFT);
            addYearlySubjectCell(subjectTable,
                    subjectReport.getCoefficient() != null ? subjectReport.getCoefficient().toString() : "1",
                    rowColor, Element.ALIGN_CENTER);
            addYearlySubjectCell(subjectTable,
                    subjectReport.getTerm1Average() != null ? formatDecimal(subjectReport.getTerm1Average()) : "-",
                    rowColor, Element.ALIGN_CENTER);
            addYearlySubjectCell(subjectTable,
                    subjectReport.getTerm2Average() != null ? formatDecimal(subjectReport.getTerm2Average()) : "-",
                    rowColor, Element.ALIGN_CENTER);
            addYearlySubjectCell(subjectTable,
                    subjectReport.getTerm3Average() != null ? formatDecimal(subjectReport.getTerm3Average()) : "-",
                    rowColor, Element.ALIGN_CENTER);

            Color avgColor = getScoreColor(subjectReport.getYearlyAverage());
            addYearlySubjectCell(subjectTable,
                    formatDecimal(subjectReport.getYearlyAverage()),
                    avgColor, Element.ALIGN_CENTER);

            Color gradeColor = getGradeColor(subjectReport.getYearlyGrade());
            addYearlySubjectCell(subjectTable, subjectReport.getYearlyGrade(), gradeColor, Element.ALIGN_CENTER);

            String status = subjectReport.getPassed() ? "PASS" : "FAIL";
            Color statusColor = subjectReport.getPassed() ? SUCCESS_COLOR : DANGER_COLOR;
            addYearlySubjectCell(subjectTable, status, statusColor, Element.ALIGN_CENTER);
        }

        titleCell.addElement(subjectTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }

    private void addYearlySubjectCell(PdfPTable table, String text, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(bgColor))));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new RoundedBorderCellEvent(3, bgColor, 0));
        table.addCell(cell);
    }

    private void addPromotionSection(Document document, YearlyReportDTO report) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(10);

        PdfPCell promotionCell = createModernCardCell("PROMOTION RECOMMENDATION",
                report.getPassed() ? SUCCESS_COLOR : DANGER_COLOR);

        String promotionStatus = report.getPassed() ? "RECOMMENDED FOR PROMOTION" : "NOT RECOMMENDED FOR PROMOTION";
        Paragraph statusParagraph = new Paragraph(promotionStatus,
                FontFactory.getFont("Helvetica-Bold", 14, getContrastColor(report.getPassed() ? SUCCESS_COLOR : DANGER_COLOR)));
        statusParagraph.setAlignment(Element.ALIGN_CENTER);
        statusParagraph.setSpacingAfter(10);
        promotionCell.addElement(statusParagraph);

        PdfPTable criteriaTable = new PdfPTable(2);
        criteriaTable.setWidthPercentage(80);
        criteriaTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        criteriaTable.setWidths(new float[]{1.5f, 1});

        String[] criteriaHeaders = {"Criteria", "Status"};
        for (String header : criteriaHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            cell.setBackgroundColor(INFO_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            criteriaTable.addCell(cell);
        }

        addPromotionCriteria(criteriaTable, "Yearly Average ≥ 10/20",
                report.getYearlyAverage() >= 10.0, report.getFormattedYearlyAverage());
        addPromotionCriteria(criteriaTable, "Pass Rate ≥ 60%",
                report.getPassRate() >= 60.0, report.getFormattedPassRate());
        addPromotionCriteria(criteriaTable, "Overall Passing Status",
                report.getPassed(), report.getPassed() ? "PASSED" : "FAILED");
        addPromotionCriteria(criteriaTable, "Class Rank",
                true, report.getYearlyRank() + "/" + report.getTotalStudentsInClass());

        promotionCell.addElement(criteriaTable);

        Paragraph actionParagraph = new Paragraph("\nRecommendation: " + report.getAction(),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        actionParagraph.setSpacingBefore(10);
        promotionCell.addElement(actionParagraph);

        Paragraph remarksParagraph = new Paragraph("\nYearly Remarks: " + report.getRemarks(),
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 100, 100)));
        remarksParagraph.setSpacingBefore(5);
        promotionCell.addElement(remarksParagraph);

        sectionTable.addCell(promotionCell);
        document.add(sectionTable);
    }

    private void addPromotionCriteria(PdfPTable table, String criterion, boolean met, String value) {
        PdfPCell criterionCell = new PdfPCell(new Phrase(criterion,
                FontFactory.getFont(FontFactory.HELVETICA, 8, INFO_COLOR)));
        criterionCell.setBorder(Rectangle.NO_BORDER);
        criterionCell.setPadding(5);
        table.addCell(criterionCell);

        Color statusColor = met ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell statusCell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, getContrastColor(statusColor))));
        statusCell.setBackgroundColor(statusColor);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setPadding(5);
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setCellEvent(new RoundedBorderCellEvent(5, statusColor, 0));
        table.addCell(statusCell);
    }

    private void addSignatureSection(Document document, YearlyReportDTO report) throws DocumentException {
        document.add(new Paragraph("\n"));

        PdfPTable signatureTable = new PdfPTable(1);
        signatureTable.setWidthPercentage(100);
        signatureTable.setSpacingBefore(15);

        PdfPCell vpCell = new PdfPCell();
        vpCell.setBorder(Rectangle.NO_BORDER);
        vpCell.setPaddingTop(10);
        vpCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(40);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setFixedHeight(1);
        lineCell.setBackgroundColor(PRIMARY_COLOR);
        lineTable.addCell(lineCell);
        vpCell.addElement(lineTable);

        Paragraph vpSignature = new Paragraph("___________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 100, 100)));
        vpSignature.setAlignment(Element.ALIGN_RIGHT);
        vpSignature.setSpacingBefore(3);
        vpCell.addElement(vpSignature);

        Paragraph vpTitle = new Paragraph("VICE PRINCIPAL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR));
        vpTitle.setAlignment(Element.ALIGN_RIGHT);
        vpTitle.setSpacingBefore(3);
        vpCell.addElement(vpTitle);

        Paragraph stamp = new Paragraph("\nOFFICIAL STAMP",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, SECONDARY_COLOR));
        stamp.setAlignment(Element.ALIGN_RIGHT);
        vpCell.addElement(stamp);

        signatureTable.addCell(vpCell);
        document.add(signatureTable);

        Paragraph note = new Paragraph(
                "Note: This is an official document. Any alteration renders it invalid.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, new Color(150, 150, 150)));
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(10);
        document.add(note);
    }
}