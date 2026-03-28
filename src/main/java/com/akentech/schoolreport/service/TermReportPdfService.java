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
        // Reduced margins
        Document document = new Document(PageSize.A4, 15, 15, 15, 15);
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
        headerTable.setWidths(new float[]{2f, 6f, 2f});
        headerTable.setSpacingBefore(5);
        headerTable.setSpacingAfter(3);

        try {
            // Left: School Logo
            PdfPCell leftImageCell = createModernImageCell(SCHOOL_LOGO_PATH, Element.ALIGN_CENTER, 50);
            leftImageCell.setPadding(5);
            leftImageCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(leftImageCell);

            // Center: School Information
            PdfPCell centerCell = createModernHeaderContentCell(report);
            headerTable.addCell(centerCell);

            // Right: Cameroon Flag
            PdfPCell rightImageCell = createModernImageCell(CAMEROON_FLAG_PATH, Element.ALIGN_CENTER, 50);
            rightImageCell.setPadding(5);
            rightImageCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(rightImageCell);

        } catch (IOException e) {
            log.warn("Could not load logo images: {}", e.getMessage());
            createModernFallbackHeader(document, report);
            return;
        }

        document.add(headerTable);

        // Add Cambridge badge below header
        addCambridgeBadge(document);

        // Add decorative separator
        addModernSeparator(document);
    }

    private PdfPCell createModernHeaderContentCell(ReportDTO report) {
        PdfPCell contentCell = new PdfPCell();
        contentCell.setBorder(Rectangle.NO_BORDER);
        contentCell.setBackgroundColor(Color.WHITE);
        contentCell.setPadding(5);
        contentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        contentCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // School name - larger and more prominent
        Paragraph schoolName = new Paragraph("DEBOS BILINGUAL SECONDARY AND HIGH SCHOOL KOMBE",
                FontFactory.getFont("Helvetica-Bold", 14, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(3);
        contentCell.addElement(schoolName);

        // Motto - smaller and elegant
        Paragraph motto = new Paragraph("Excellence • Creativity • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, ACCENT_COLOR));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(5);
        contentCell.addElement(motto);

        // Academic year badge - modern card style
        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(70);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell badgeCell = new PdfPCell();
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setBackgroundColor(PRIMARY_COLOR);
        badgeCell.setPadding(6);
        badgeCell.setCellEvent(new RoundedBorderCellEvent(5, PRIMARY_COLOR, 0));

        Paragraph academicInfo = new Paragraph(
                String.format("ACADEMIC YEAR %s - TERM %d REPORT CARD", report.getAcademicYear(), report.getTerm()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        badgeCell.addElement(academicInfo);

        badgeTable.addCell(badgeCell);
        contentCell.addElement(badgeTable);

        // Contact information - subtle
        Paragraph contact = new Paragraph("P.O. Box 123 Kombe | Tel: 677755377 / 670252217",
                FontFactory.getFont(FontFactory.HELVETICA, 7, INFO_COLOR));
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingBefore(4);
        contentCell.addElement(contact);

        return contentCell;
    }

    private void addCambridgeBadge(Document document) throws DocumentException {
        try {
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setWidthPercentage(100);
            badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeTable.setSpacingBefore(2);
            badgeTable.setSpacingAfter(2);

            PdfPCell badgeCell = createModernImageCell(CAMBRIDGE_BADGE_PATH, Element.ALIGN_CENTER, 35);
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setPadding(0);
            badgeTable.addCell(badgeCell);

            document.add(badgeTable);
        } catch (IOException e) {
            log.debug("Cambridge badge not available: {}", e.getMessage());
        }
    }

    private void createModernFallbackHeader(Document document, ReportDTO report)
            throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(2);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setBackgroundColor(Color.WHITE);
        headerCell.setPadding(4);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School",
                FontFactory.getFont("Helvetica-Bold", 12, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(1);
        headerCell.addElement(schoolName);

        Paragraph underline = new Paragraph();
        for (int i = 0; i < 12; i++) {
            underline.add(new Chunk("∼ ",
                    FontFactory.getFont(FontFactory.HELVETICA, 5, ACCENT_COLOR)));
        }
        underline.setAlignment(Element.ALIGN_CENTER);
        underline.setSpacingAfter(2);
        headerCell.addElement(underline);

        Paragraph motto = new Paragraph("Excellence • In • Creativity • And • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA, 8, SECONDARY_COLOR));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(4);
        headerCell.addElement(motto);

        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(60);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setBackgroundColor(new Color(240, 240, 240));
        infoCell.setPadding(3);
        infoCell.setCellEvent(new RoundedBorderCellEvent(4, PRIMARY_COLOR, 0.8f));

        Paragraph academicInfo = new Paragraph(
                String.format("ACADEMIC YEAR: %s | TERM %d REPORT", report.getAcademicYear(), report.getTerm()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        infoCell.addElement(academicInfo);

        infoTable.addCell(infoCell);
        headerCell.addElement(infoTable);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        addModernSeparator(document);
    }

    private void addStudentInfoSection(Document document, ReportDTO report) throws DocumentException {
        // Modern card-based student information section
        PdfPTable mainContainer = new PdfPTable(1);
        mainContainer.setWidthPercentage(100);
        mainContainer.setSpacingBefore(5);
        mainContainer.setSpacingAfter(5);

        PdfPCell containerCell = new PdfPCell();
        containerCell.setBorder(Rectangle.NO_BORDER);
        containerCell.setBackgroundColor(LIGHT_GRAY);
        containerCell.setPadding(10);
        containerCell.setCellEvent(new RoundedBorderCellEvent(8, MEDIUM_GRAY, 1f));

        // Section title
        Paragraph sectionTitle = new Paragraph("STUDENT INFORMATION",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR));
        sectionTitle.setAlignment(Element.ALIGN_CENTER);
        sectionTitle.setSpacingAfter(8);
        containerCell.addElement(sectionTitle);

        // Student details grid - 3 columns for better space utilization
        PdfPTable detailsGrid = new PdfPTable(6);
        detailsGrid.setWidthPercentage(100);
        detailsGrid.setWidths(new float[]{2f, 3f, 2f, 3f, 2f, 3f});
        detailsGrid.setSpacingBefore(0);

        // Add student details in a clean grid format
        addCleanDetailRow(detailsGrid, "Student Name:", report.getStudentFullName());
        addCleanDetailRow(detailsGrid, "Roll Number:", report.getRollNumber());
        addCleanDetailRow(detailsGrid, "Student ID:", report.getStudentIdString());

        addCleanDetailRow(detailsGrid, "Class:", report.getClassName());
        addCleanDetailRow(detailsGrid, "Department:", report.getDepartment());
        addCleanDetailRow(detailsGrid, "Specialty:", report.getSpecialty());

        addCleanDetailRow(detailsGrid, "Date of Birth:", report.getFormattedDateOfBirth());
        addCleanDetailRow(detailsGrid, "Gender:", report.getStudentGender());
        addCleanDetailRow(detailsGrid, "Academic Year:", report.getAcademicYear());

        containerCell.addElement(detailsGrid);
        mainContainer.addCell(containerCell);

        document.add(mainContainer);
    }

    private void addCleanDetailRow(PdfPTable table, String label, String value) {
        // Label cell - bold and distinct
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(Color.WHITE);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setCellEvent(new RoundedBorderCellEvent(3, MEDIUM_GRAY, 0.5f));

        // Value cell - regular weight
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_COLOR)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(Color.WHITE);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setCellEvent(new RoundedBorderCellEvent(3, MEDIUM_GRAY, 0.5f));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }


    private void addSubjectPerformanceTable(Document document, ReportDTO report) throws DocumentException {
        // Section title with modern styling
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(5);
        titleTable.setSpacingAfter(3);

        PdfPCell titleCell = new PdfPCell(new Phrase("ACADEMIC PERFORMANCE",
                FontFactory.getFont("Helvetica-Bold", 11, PRIMARY_COLOR)));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(3);
        titleTable.addCell(titleCell);

        document.add(titleTable);

        List<SubjectReport> subjectReports = report.getSubjectReports();
        int term = report.getTerm();
        String className = report.getClassName();

        // Create compact subject table
        PdfPTable subjectTable = new PdfPTable(getColumnCount(term));
        subjectTable.setWidthPercentage(100);
        subjectTable.setWidths(getColumnWidths(term));
        subjectTable.setSpacingBefore(0);
        subjectTable.setSpacingAfter(3);

        addCompactTableHeader(subjectTable, term);

        boolean alternate = false;
        for (SubjectReport subject : subjectReports) {
            addCompactSubjectRow(subjectTable, subject, term, alternate, className);
            alternate = !alternate;
        }

        document.add(subjectTable);
    }

    private int getColumnCount(int term) {
        return term == 3 ? 7 : 8;
    }

    private float[] getColumnWidths(int term) {
        if (term == 3) {
            return new float[]{2f, 0.6f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f};
        } else {
            return new float[]{2f, 0.6f, 0.8f, 0.8f, 0.9f, 0.9f, 0.9f, 0.9f};
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

    private void addCompactTableHeader(PdfPTable table, int term) {
        String[] headers = getHeadersForTerm(term);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, HEADER_TEXT)));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private void addCompactSubjectRow(PdfPTable table, SubjectReport subject, int term, boolean alternate, String className) {
        Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;

        // Subject name cell
        PdfPCell subjectCell = new PdfPCell(new Phrase(subject.getSubjectName(),
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_COLOR)));
        subjectCell.setBackgroundColor(rowColor);
        subjectCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        subjectCell.setPadding(5);
        subjectCell.setBorder(Rectangle.BOTTOM);
        subjectCell.setBorderColor(MEDIUM_GRAY);
        subjectCell.setBorderWidth(0.5f);
        table.addCell(subjectCell);

        // Coefficient cell
        PdfPCell coeffCell = new PdfPCell(new Phrase(
                subject.getCoefficient() != null ? String.valueOf(subject.getCoefficient()) : "1",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_COLOR)));
        coeffCell.setBackgroundColor(rowColor);
        coeffCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        coeffCell.setPadding(5);
        coeffCell.setBorder(Rectangle.BOTTOM);
        coeffCell.setBorderColor(MEDIUM_GRAY);
        coeffCell.setBorderWidth(0.5f);
        table.addCell(coeffCell);

        // Assessment cells
        if (term == 3) {
            addCompactAssessmentCell(table, subject.getAssessment1(), rowColor);
        } else {
            addCompactAssessmentCell(table, subject.getAssessment1(), rowColor);
            addCompactAssessmentCell(table, subject.getAssessment2(), rowColor);
        }

        // Total score cell
        Double total = subject.getTotalScore(term);
        Color totalColor = (total != null && total >= 10) ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell totalCell = new PdfPCell(new Phrase(formatDecimal(total),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, totalColor)));
        totalCell.setBackgroundColor(rowColor);
        totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalCell.setPadding(5);
        totalCell.setBorder(Rectangle.BOTTOM);
        totalCell.setBorderColor(MEDIUM_GRAY);
        totalCell.setBorderWidth(0.5f);
        table.addCell(totalCell);

        // Average cell with colored background
        Double average = subject.getSubjectAverage();
        Color avgBgColor = (average != null && average >= 10) ? new Color(220, 255, 220) : new Color(255, 220, 220);
        PdfPCell avgCell = new PdfPCell(new Phrase(formatDecimal(average),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_COLOR)));
        avgCell.setBackgroundColor(avgBgColor);
        avgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        avgCell.setPadding(5);
        avgCell.setBorder(Rectangle.BOTTOM);
        avgCell.setBorderColor(MEDIUM_GRAY);
        avgCell.setBorderWidth(0.5f);
        table.addCell(avgCell);

        // Grade cell with colored background
        String grade = subject.getLetterGrade() != null ? subject.getLetterGrade() : "U";
        Color gradeColor = getGradeColor(grade);
        PdfPCell gradeCell = new PdfPCell(new Phrase(grade,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, getContrastColor(gradeColor))));
        gradeCell.setBackgroundColor(gradeColor);
        gradeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        gradeCell.setPadding(5);
        gradeCell.setBorder(Rectangle.BOTTOM);
        gradeCell.setBorderColor(MEDIUM_GRAY);
        gradeCell.setBorderWidth(0.5f);
        table.addCell(gradeCell);

        // Pass/Fail status cell
        boolean passed = isSubjectPassing(grade, className);
        String status = passed ? "PASS" : "FAIL";
        Color statusColor = passed ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell statusCell = new PdfPCell(new Phrase(status,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        statusCell.setBackgroundColor(statusColor);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setPadding(5);
        statusCell.setBorder(Rectangle.BOTTOM);
        statusCell.setBorderColor(MEDIUM_GRAY);
        statusCell.setBorderWidth(0.5f);
        table.addCell(statusCell);
    }

    private void addCompactAssessmentCell(PdfPTable table, Double score, Color rowColor) {
        Font scoreFont;
        Color textColor;

        if (score != null) {
            if (score < 10) {
                textColor = DANGER_COLOR;
                scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textColor);
            } else {
                textColor = SUCCESS_COLOR;
                scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textColor);
            }
        } else {
            textColor = INFO_COLOR;
            scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textColor);
        }

        PdfPCell cell = new PdfPCell(new Phrase(
                score != null ? formatDecimal(score) : "-",
                scoreFont));
        cell.setBackgroundColor(rowColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(MEDIUM_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
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

    private void addSummarySection(Document document, ReportDTO report) throws DocumentException {
        // Modern summary section with two-column layout
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{1, 1});
        summaryTable.setSpacingBefore(5);
        summaryTable.setSpacingAfter(5);

        Double termAverage = report.getTermAverage();
        double passRate = report.getPassRate() != null ? report.getPassRate() : 0.0;
        Integer subjectsPassed = report.getSubjectsPassed() != null ? report.getSubjectsPassed() : 0;
        int totalSubjects = report.getTotalSubjects() != null ? report.getTotalSubjects() : 0;
        boolean overallPassed = report.getPassed() != null ? report.getPassed() : false;
        String className = report.getClassName();

        // Left card: Performance Summary
        PdfPCell leftCell = createSummaryCard("PERFORMANCE SUMMARY", PRIMARY_COLOR);

        // Performance status badge
        Color performanceColor = getPerformanceColor(termAverage);
        String performanceStatus = gradeService.getPerformanceStatus(termAverage);

        PdfPTable statusBadge = new PdfPTable(1);
        statusBadge.setWidthPercentage(80);
        statusBadge.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell statusCell = new PdfPCell(new Phrase(performanceStatus,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setBackgroundColor(performanceColor);
        statusCell.setPadding(8);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setCellEvent(new RoundedBorderCellEvent(5, performanceColor, 0));
        statusBadge.addCell(statusCell);
        leftCell.addElement(statusBadge);

        // Performance metrics
        addSummaryMetric(leftCell, "Term Average:", report.getFormattedAverage(), termAverage);
        addSummaryMetric(leftCell, "Overall Grade:",
                gradeService.calculateLetterGrade(termAverage, className), null);
        addSummaryMetric(leftCell, "Class Rank:",
                report.getRankInClass() + " of " + report.getTotalStudentsInClass(), null);

        if (report.getRankInDepartment() != null && !report.getDepartment().equals("N/A")) {
            addSummaryMetric(leftCell, "Department Rank:",
                    String.valueOf(report.getRankInDepartment()), null);
        }

        Color statusColor = overallPassed ? SUCCESS_COLOR : DANGER_COLOR;
        addSummaryMetric(leftCell, "Term Status:", overallPassed ? "PASSED ✓" : "FAILED ✗", null);

        // Right card: Subject Statistics & Remarks
        PdfPCell rightCell = createSummaryCard("SUBJECT STATISTICS", SECONDARY_COLOR);

        // Pass rate bar
        addProgressMetric(rightCell, "Pass Rate:", passRate,
                passRate >= 60 ? SUCCESS_COLOR : (passRate >= 40 ? WARNING_COLOR : DANGER_COLOR));

        String passingCriteria = isAdvancedLevelClass(className) ?
                "(Grades A-E passing)" : "(Grades A-C passing)";
        addSummaryMetric(rightCell, "Subjects Passed:",
                subjectsPassed + " / " + totalSubjects + " " + passingCriteria, null);

        // Teacher's remarks section
        Paragraph remarksTitle = new Paragraph("Teacher's Remarks:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ACCENT_COLOR));
        remarksTitle.setSpacingBefore(6);
        remarksTitle.setSpacingAfter(3);
        rightCell.addElement(remarksTitle);

        PdfPTable remarksBox = new PdfPTable(1);
        remarksBox.setWidthPercentage(100);

        PdfPCell remarksCell = new PdfPCell(new Phrase(report.getRemarks(),
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_COLOR)));
        remarksCell.setBorder(Rectangle.NO_BORDER);
        remarksCell.setBackgroundColor(Color.WHITE);
        remarksCell.setPadding(8);
        remarksCell.setMinimumHeight(40);
        remarksCell.setCellEvent(new RoundedBorderCellEvent(4, MEDIUM_GRAY, 0.5f));
        remarksBox.addCell(remarksCell);
        rightCell.addElement(remarksBox);

        summaryTable.addCell(leftCell);
        summaryTable.addCell(rightCell);
        document.add(summaryTable);
    }

    private PdfPCell createSummaryCard(String title, Color titleColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(10);
        cell.setCellEvent(new RoundedBorderCellEvent(8, MEDIUM_GRAY, 1f));

        Paragraph titleParagraph = new Paragraph(title,
                FontFactory.getFont("Helvetica-Bold", 10, titleColor));
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(8);
        cell.addElement(titleParagraph);
        return cell;
    }

    private void addSummaryMetric(PdfPCell cell, String label, String value, Double numericValue) {
        Paragraph metric = new Paragraph();
        metric.add(new Chunk(label + " ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));

        Color valueColor = TEXT_COLOR;
        if (numericValue != null) {
            valueColor = (numericValue >= 10) ? SUCCESS_COLOR : DANGER_COLOR;
        }

        metric.add(new Chunk(value,
                FontFactory.getFont(FontFactory.HELVETICA, 8, valueColor)));
        metric.setSpacingBefore(4);
        cell.addElement(metric);
    }

    private void addProgressMetric(PdfPCell cell, String label, double percentage, Color color) {
        Paragraph progress = new Paragraph();
        progress.add(new Chunk(label + " ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INFO_COLOR)));
        progress.add(new Chunk(String.format("%.1f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, color)));
        progress.setSpacingBefore(4);

        // Progress bar visualization
        int filledBars = (int) (percentage / 10);
        String progressBar = "█".repeat(Math.min(filledBars, 10)) + "░".repeat(10 - Math.min(filledBars, 10));
        Paragraph bar = new Paragraph(progressBar,
                FontFactory.getFont(FontFactory.HELVETICA, 10, color));
        bar.setSpacingBefore(2);

        cell.addElement(progress);
        cell.addElement(bar);
    }

    private void addSignatureSection(Document document, ReportDTO report) throws DocumentException {
        // Modern signature section
        PdfPTable signatureTable = new PdfPTable(1);
        signatureTable.setWidthPercentage(100);
        signatureTable.setSpacingBefore(10);

        PdfPCell signatureCell = new PdfPCell();
        signatureCell.setBorder(Rectangle.NO_BORDER);
        signatureCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signatureCell.setPadding(0);

        // Signature line
        PdfPTable signatureLineTable = new PdfPTable(1);
        signatureLineTable.setWidthPercentage(40);
        signatureLineTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderColor(PRIMARY_COLOR);
        lineCell.setBorderWidth(1.5f);
        lineCell.setFixedHeight(1);
        signatureLineTable.addCell(lineCell);

        signatureCell.addElement(signatureLineTable);

        // Vice Principal title
        Paragraph vpTitle = new Paragraph("VICE PRINCIPAL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR));
        vpTitle.setAlignment(Element.ALIGN_RIGHT);
        vpTitle.setSpacingBefore(3);
        signatureCell.addElement(vpTitle);

        // Name
        Paragraph vpName = new Paragraph("Kohsu Rodolphe Rinwi",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_COLOR));
        vpName.setAlignment(Element.ALIGN_RIGHT);
        vpName.setSpacingBefore(1);
        signatureCell.addElement(vpName);

        // Official stamp note
        Paragraph stamp = new Paragraph("OFFICIAL STAMP",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, SECONDARY_COLOR));
        stamp.setAlignment(Element.ALIGN_RIGHT);
        stamp.setSpacingBefore(4);
        signatureCell.addElement(stamp);

        signatureTable.addCell(signatureCell);
        document.add(signatureTable);

        // Disclaimer note at bottom
        Paragraph disclaimer = new Paragraph(
                "This is an official document. Any unauthorized alteration renders it invalid.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6, INFO_COLOR));
        disclaimer.setAlignment(Element.ALIGN_CENTER);
        disclaimer.setSpacingBefore(8);
        document.add(disclaimer);
    }
}