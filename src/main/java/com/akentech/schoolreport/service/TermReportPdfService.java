package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.SubjectReport;
import com.lowagie.text.*;
import com.lowagie.text.Font;
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
public class TermReportPdfService extends BasePdfService {

    public TermReportPdfService(GradeService gradeService) {
        super(gradeService);
    }

    public byte[] generateTermReportPdf(ReportDTO report) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        // Add modern header
        addSchoolHeader(document, report, writer);

        // Add student information
        addStudentInfoSection(document, report);

        // Add subject performance table
        addSubjectPerformanceTable(document, report);

        // Add summary section
        addSummarySection(document, report);

        // Add signature section
        addSignatureSection(document, report);

        document.close();
        return outputStream.toByteArray();
    }

    // ====== PRIVATE METHODS ======

    private void addSchoolHeader(Document document, ReportDTO report, PdfWriter writer)
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

    private PdfPCell createModernHeaderContentCell(ReportDTO report) {
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
                String.format("Academic Year: %s | TERM %d REPORT", report.getAcademicYear(), report.getTerm()),
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

    private void createModernFallbackHeader(Document document, ReportDTO report)
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
                String.format("ACADEMIC YEAR: %s | TERM %d REPORT", report.getAcademicYear(), report.getTerm()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INFO_COLOR));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        infoCell.addElement(academicInfo);

        infoTable.addCell(infoCell);
        headerCell.addElement(infoTable);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        addModernSeparator(document);
    }

    private void addStudentInfoSection(Document document, ReportDTO report) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{2, 3, 2, 3});
        infoTable.setSpacingBefore(5);
        infoTable.setSpacingAfter(10);

        PdfPCell headerCell = new PdfPCell(new Phrase("STUDENT INFORMATION",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        headerCell.setColspan(4);
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(6);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setCellEvent(new RoundedBorderCellEvent(5, PRIMARY_COLOR, 0));
        infoTable.addCell(headerCell);

        addModernInfoRow(infoTable, "Full Name:", report.getStudentFullName(), 0);
        addModernInfoRow(infoTable, "Roll Number:", report.getRollNumber(), 1);
        addModernInfoRow(infoTable, "Student ID:", report.getStudentIdString(), 0);
        addModernInfoRow(infoTable, "Class:", report.getClassName(), 1);
        addModernInfoRow(infoTable, "Department:", report.getDepartment(), 0);
        addModernInfoRow(infoTable, "Specialty:", report.getSpecialty(), 1);
        addModernInfoRow(infoTable, "Date of Birth:", report.getFormattedDateOfBirth(), 0);
        addModernInfoRow(infoTable, "Gender:", report.getStudentGender(), 1);

        document.add(infoTable);
    }

    private void addModernInfoRow(PdfPTable table, String label, String value, int rowType) {
        Color bgColor = rowType % 2 == 0 ? ROW_COLOR1 : ROW_COLOR2;

        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));
        styleModernCell(labelCell, bgColor, Element.ALIGN_LEFT);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
        styleModernCell(valueCell, bgColor, Element.ALIGN_LEFT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSubjectPerformanceTable(Document document, ReportDTO report) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(5);
        titleTable.setSpacingAfter(5);

        PdfPCell titleCell = new PdfPCell(new Phrase("SUBJECT PERFORMANCE ANALYSIS",
                FontFactory.getFont("Helvetica-Bold", 11, SECONDARY_COLOR)));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(5);
        titleTable.addCell(titleCell);

        document.add(titleTable);

        List<SubjectReport> subjectReports = report.getSubjectReports();
        int term = report.getTerm();
        String className = report.getClassName();

        PdfPTable subjectTable = new PdfPTable(getColumnCount(term));
        subjectTable.setWidthPercentage(100);
        subjectTable.setWidths(getColumnWidths(term));
        subjectTable.setSpacingBefore(3);

        addModernTableHeader(subjectTable, term);

        boolean alternate = false;
        for (SubjectReport subject : subjectReports) {
            addModernSubjectRow(subjectTable, subject, term, alternate, className);
            alternate = !alternate;
        }

        document.add(subjectTable);
    }

    private int getColumnCount(int term) {
        return term == 3 ? 7 : 8;
    }

    private float[] getColumnWidths(int term) {
        if (term == 3) {
            return new float[]{2.5f, 0.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f};
        } else {
            return new float[]{2.5f, 0.8f, 1f, 1f, 1.2f, 1.2f, 1.2f, 1.2f};
        }
    }

    private String[] getHeadersForTerm(int term) {
        if (term == 3) {
            return new String[]{"SUBJECT", "COEFF", "EXAM", "TOTAL", "AVERAGE", "GRADE", "STATUS"};
        } else if (term == 1) {
            return new String[]{"SUBJECT", "COEFF", "A1", "A2", "TOTAL", "AVERAGE", "GRADE", "STATUS"};
        } else {
            return new String[]{"SUBJECT", "COEFF", "A3", "A4", "TOTAL", "AVERAGE", "GRADE", "STATUS"};
        }
    }

    private void addModernTableHeader(PdfPTable table, int term) {
        String[] headers = getHeadersForTerm(term);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(new RoundedBorderCellEvent(3, PRIMARY_COLOR, 0));
            table.addCell(cell);
        }
    }

    private void addModernSubjectRow(PdfPTable table, SubjectReport subject, int term, boolean alternate, String className) {
        Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;

        PdfPCell subjectCell = new PdfPCell(new Phrase(subject.getSubjectName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));
        styleModernCell(subjectCell, rowColor, Element.ALIGN_LEFT);
        table.addCell(subjectCell);

        PdfPCell coeffCell = new PdfPCell(new Phrase(
                subject.getCoefficient() != null ? String.valueOf(subject.getCoefficient()) : "1",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
        styleModernCell(coeffCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(coeffCell);

        if (term == 3) {
            addModernAssessmentCell(table, subject.getAssessment1(), rowColor);
        } else {
            addModernAssessmentCell(table, subject.getAssessment1(), rowColor);
            addModernAssessmentCell(table, subject.getAssessment2(), rowColor);
        }

        Double total = subject.getTotalScore(term);
        Color totalColor = (total != null && total >= 10) ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell totalCell = new PdfPCell(new Phrase(formatDecimal(total),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, totalColor)));
        styleModernCell(totalCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(totalCell);

        Double average = subject.getSubjectAverage();
        Color avgColor = (average != null && average >= 10) ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell avgCell = new PdfPCell(new Phrase(formatDecimal(average),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, getContrastColor(avgColor))));
        avgCell.setBackgroundColor(avgColor);
        styleModernCell(avgCell, avgColor, Element.ALIGN_CENTER);
        table.addCell(avgCell);

        String grade = subject.getLetterGrade() != null ? subject.getLetterGrade() : "U";
        Color gradeColor = getGradeColor(grade);
        PdfPCell gradeCell = new PdfPCell(new Phrase(grade,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, getContrastColor(gradeColor))));
        gradeCell.setBackgroundColor(gradeColor);
        styleModernCell(gradeCell, gradeColor, Element.ALIGN_CENTER);
        table.addCell(gradeCell);

        boolean passed = isSubjectPassing(grade, className);
        String status = passed ? "PASS" : "FAIL";
        Color statusColor = passed ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell statusCell = new PdfPCell(new Phrase(status,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, getContrastColor(statusColor))));
        statusCell.setBackgroundColor(statusColor);
        styleModernCell(statusCell, statusColor, Element.ALIGN_CENTER);
        table.addCell(statusCell);
    }

    private boolean isSubjectPassing(String grade, String className) {
        if (grade == null || className == null) return false;
        boolean isAdvancedLevel = isAdvancedLevelClass(className);
        if (isAdvancedLevel) {
            return grade.matches("[ABCDE]");
        } else {
            return grade.matches("[ABC]");
        }
    }

    private void addModernAssessmentCell(PdfPTable table, Double score, Color rowColor) {
        Font scoreFont;
        Color textColor = Color.BLACK;

        if (score != null) {
            if (score < 10) {
                textColor = DANGER_COLOR;
                scoreFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, textColor);
            } else {
                textColor = SUCCESS_COLOR;
                scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textColor);
            }
        } else {
            textColor = new Color(150, 150, 150);
            scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textColor);
        }

        PdfPCell cell = new PdfPCell(new Phrase(
                score != null ? formatDecimal(score) : "-",
                scoreFont));
        styleModernCell(cell, rowColor, Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addSummarySection(Document document, ReportDTO report) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{1, 1});
        summaryTable.setSpacingBefore(10);

        Double termAverage = report.getTermAverage();
        double passRate = report.getPassRate() != null ? report.getPassRate() : 0.0;
        Integer subjectsPassed = report.getSubjectsPassed() != null ? report.getSubjectsPassed() : 0;
        int totalSubjects = report.getTotalSubjects() != null ? report.getTotalSubjects() : 0;
        boolean overallPassed = report.getPassed() != null ? report.getPassed() : false;
        String className = report.getClassName();

        PdfPCell leftCell = createModernCardCell("PERFORMANCE SUMMARY", PRIMARY_COLOR);
        leftCell.setBackgroundColor(new Color(240, 248, 255));

        Color performanceColor = getPerformanceColor(termAverage);
        Paragraph performanceBadge = new Paragraph(
                gradeService.getPerformanceStatus(termAverage),
                FontFactory.getFont("Helvetica-Bold", 10, getContrastColor(performanceColor)));
        performanceBadge.setAlignment(Element.ALIGN_CENTER);

        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(70);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell badgeCell = new PdfPCell(performanceBadge);
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setBackgroundColor(performanceColor);
        badgeCell.setPadding(8);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeCell.setCellEvent(new RoundedBorderCellEvent(10, performanceColor, 0));
        badgeTable.addCell(badgeCell);
        leftCell.addElement(badgeTable);

        addModernSummaryItem(leftCell, "Term Average:", report.getFormattedAverage(), termAverage);
        addModernSummaryItem(leftCell, "Overall Grade:",
                gradeService.calculateLetterGrade(termAverage, className));
        addModernSummaryItem(leftCell, "Class Rank:",
                report.getRankInClass() + " / " + report.getTotalStudentsInClass());
        addModernSummaryItem(leftCell, "Overall Status:",
                overallPassed ? "PASSED" : "FAILED");

        PdfPCell rightCell = createModernCardCell("SUBJECT STATISTICS", SECONDARY_COLOR);
        rightCell.setBackgroundColor(new Color(255, 250, 245));

        addModernProgressBar(rightCell, "Pass Rate:", passRate,
                passRate >= 60 ? SUCCESS_COLOR : (passRate >= 40 ? WARNING_COLOR : DANGER_COLOR));

        String passingCriteria = isAdvancedLevelClass(className) ?
                " (A-E are passing)" : " (A-C are passing)";
        addModernSummaryItem(rightCell, "Subjects Passed:",
                subjectsPassed + " of " + totalSubjects + " subjects" + passingCriteria);

        Paragraph remarksTitle = new Paragraph("\nTEACHER'S REMARKS:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ACCENT_COLOR));
        remarksTitle.setSpacingBefore(10);
        rightCell.addElement(remarksTitle);

        PdfPTable remarksTable = new PdfPTable(1);
        remarksTable.setWidthPercentage(100);
        remarksTable.setSpacingBefore(3);

        PdfPCell remarksCell = new PdfPCell();
        remarksCell.setBorder(Rectangle.NO_BORDER);
        remarksCell.setBackgroundColor(new Color(250, 250, 250));
        remarksCell.setPadding(6);
        remarksCell.setCellEvent(new RoundedBorderCellEvent(5, new Color(220, 220, 220), 0.5f));

        Paragraph remarks = new Paragraph(report.getRemarks(),
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
        remarksCell.addElement(remarks);

        remarksTable.addCell(remarksCell);
        rightCell.addElement(remarksTable);

        summaryTable.addCell(leftCell);
        summaryTable.addCell(rightCell);
        document.add(summaryTable);
    }

    private void addModernSummaryItem(PdfPCell cell, String label, String value) {
        Paragraph item = new Paragraph();
        item.add(new Chunk(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));
        item.add(new Chunk(" " + value,
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
        item.setSpacingBefore(5);
        cell.addElement(item);
    }

    private void addModernSummaryItem(PdfPCell cell, String label, String value, Double numericValue) {
        Paragraph item = new Paragraph();
        item.add(new Chunk(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));

        Color valueColor = (numericValue != null && numericValue >= 10) ? SUCCESS_COLOR : DANGER_COLOR;
        item.add(new Chunk(" " + value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, valueColor)));
        item.setSpacingBefore(5);
        cell.addElement(item);
    }

    private void addModernProgressBar(PdfPCell cell, String label, double percentage, Color color) {
        Paragraph progress = new Paragraph();
        progress.add(new Chunk(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));
        progress.add(new Chunk(" " + String.format("%.1f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, color)));
        progress.setSpacingBefore(6);

        int bars = (int) (percentage / 10);
        String progressBar = "█".repeat(Math.min(bars, 10)) + "░".repeat(10 - Math.min(bars, 10));
        Paragraph bar = new Paragraph(progressBar,
                FontFactory.getFont(FontFactory.HELVETICA, 10, color));
        bar.setSpacingBefore(1);
        cell.addElement(progress);
        cell.addElement(bar);
    }

    private void addSignatureSection(Document document, ReportDTO report) throws DocumentException {
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

        // Title
        Paragraph vpTitle = new Paragraph("VICE PRINCIPAL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR));
        vpTitle.setAlignment(Element.ALIGN_RIGHT);
        vpTitle.setSpacingBefore(3);
        vpCell.addElement(vpTitle);

// Name
        Paragraph vpName = new Paragraph("Kohsu Rodolphe Rinwi",
                FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_COLOR));
        vpName.setAlignment(Element.ALIGN_RIGHT);
        vpName.setSpacingBefore(2);
        vpCell.addElement(vpName);


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