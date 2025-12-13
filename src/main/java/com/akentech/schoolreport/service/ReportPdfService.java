package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.SubjectReport;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPdfService {

    // Modern color palette
    private static final Color PRIMARY_COLOR = new Color(0, 102, 204); // Modern Blue
    private static final Color SECONDARY_COLOR = new Color(102, 0, 204); // Modern Purple
    private static final Color ACCENT_COLOR = new Color(0, 153, 153); // Teal
    private static final Color SUCCESS_COLOR = new Color(0, 153, 76); // Green
    private static final Color WARNING_COLOR = new Color(255, 153, 0); // Orange
    private static final Color DANGER_COLOR = new Color(204, 0, 0); // Red
    private static final Color INFO_COLOR = new Color(64, 64, 64); // Dark Gray
    private static final Color A_GRADE_COLOR = new Color(0, 102, 204); // Blue
    private static final Color B_GRADE_COLOR = new Color(0, 153, 76); // Green
    private static final Color C_GRADE_COLOR = new Color(255, 204, 0); // Gold
    private static final Color D_GRADE_COLOR = new Color(255, 102, 0); // Orange
    private static final Color U_GRADE_COLOR = new Color(204, 0, 0); // Red
    private static final Color ROW_COLOR1 = new Color(255, 255, 255); // White
    private static final Color ROW_COLOR2 = new Color(248, 249, 250); // Light Gray

    // Image paths
    private static final String LEFT_LOGO_PATH = "static/images/school-logo.png";
    private static final String RIGHT_LOGO_PATH = "static/images/cameroon-flag.png";

    private final GradeService gradeService;

    public byte[] generateTermReportPdf(ReportDTO report) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
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

    private void addSchoolHeader(Document document, ReportDTO report, PdfWriter writer)
            throws DocumentException, IOException {

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 6, 2});
        headerTable.setSpacingBefore(10);

        try {
            // Left Image Cell
            PdfPCell leftImageCell = createModernImageCell(LEFT_LOGO_PATH, Element.ALIGN_LEFT);
            headerTable.addCell(leftImageCell);

            // Center Content Cell
            PdfPCell centerCell = createModernHeaderContentCell(report);
            headerTable.addCell(centerCell);

            // Right Image Cell
            PdfPCell rightImageCell = createModernImageCell(RIGHT_LOGO_PATH, Element.ALIGN_RIGHT);
            headerTable.addCell(rightImageCell);

        } catch (IOException e) {
            log.warn("Could not load logo images: {}", e.getMessage());
            createModernFallbackHeader(document, report);
            return;
        }

        document.add(headerTable);

        // Modern decorative line
        addModernSeparator(document);
    }

    private PdfPCell createModernImageCell(String imagePath, int alignment) throws IOException {
        PdfPCell imageCell = new PdfPCell();
        imageCell.setBorder(Rectangle.NO_BORDER);
        imageCell.setBackgroundColor(Color.WHITE);
        imageCell.setPadding(5);
        imageCell.setHorizontalAlignment(alignment);
        imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            ClassPathResource resource = new ClassPathResource(imagePath);
            if (resource.exists()) {
                Image image = Image.getInstance(resource.getURL());

                // Scale image
                float scale = 80f / image.getHeight();
                image.scaleAbsolute(image.getWidth() * scale, 80);

                imageCell.addElement(image);
            } else {
                addImagePlaceholder(imageCell, alignment);
            }
        } catch (Exception e) {
            log.warn("Could not load image: {}", imagePath);
            addImagePlaceholder(imageCell, alignment);
        }

        return imageCell;
    }

    private void addImagePlaceholder(PdfPCell cell, int alignment) {
        Phrase placeholder = new Phrase("[LOGO]",
                FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(200, 200, 200)));
        cell.addElement(placeholder);
    }

    private PdfPCell createModernHeaderContentCell(ReportDTO report) {
        PdfPCell contentCell = new PdfPCell();
        contentCell.setBorder(Rectangle.NO_BORDER);
        contentCell.setBackgroundColor(Color.WHITE);
        contentCell.setPadding(10);
        contentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        contentCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // School name with modern font
        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School",
                FontFactory.getFont("Helvetica-Bold", 22, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(5);
        contentCell.addElement(schoolName);

        // Decorative arc effect using dots
        Paragraph arcEffect = new Paragraph();
        for (int i = 0; i < 25; i++) {
            arcEffect.add(new Chunk("• ",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(200, 200, 200))));
        }
        arcEffect.setAlignment(Element.ALIGN_CENTER);
        arcEffect.setSpacingAfter(8);
        contentCell.addElement(arcEffect);

        // School motto
        Paragraph motto = new Paragraph("Excellence • In • Creativity • And • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA, 12, ACCENT_COLOR));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(8);
        contentCell.addElement(motto);

        // Academic year and term in a bordered box
        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(60);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell badgeCell = new PdfPCell();
        badgeCell.setBorder(Rectangle.BOX);
        badgeCell.setBorderWidth(1);
        badgeCell.setBorderColor(new Color(200, 200, 200));
        badgeCell.setBackgroundColor(new Color(240, 240, 240));
        badgeCell.setPadding(6);

        Paragraph academicInfo = new Paragraph(
                String.format("Academic Year: %s | Term %d Report",
                        report.getAcademicYear(), report.getTerm()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INFO_COLOR));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        badgeCell.addElement(academicInfo);

        badgeTable.addCell(badgeCell);
        contentCell.addElement(badgeTable);

        // School address
        Paragraph address = new Paragraph("Kotto Road Kombe | Phone: 677755377/670252217",
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(150, 150, 150)));
        address.setAlignment(Element.ALIGN_CENTER);
        address.setSpacingBefore(8);
        contentCell.addElement(address);

        return contentCell;
    }

    private void createModernFallbackHeader(Document document, ReportDTO report)
            throws DocumentException {

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(10);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setBackgroundColor(Color.WHITE);
        headerCell.setPadding(20);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // School name
        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School",
                FontFactory.getFont("Helvetica-Bold", 24, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(5);
        headerCell.addElement(schoolName);

        // Decorative line
        Paragraph underline = new Paragraph();
        for (int i = 0; i < 20; i++) {
            underline.add(new Chunk("∼ ",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, ACCENT_COLOR)));
        }
        underline.setAlignment(Element.ALIGN_CENTER);
        underline.setSpacingAfter(10);
        headerCell.addElement(underline);

        // School motto
        Paragraph motto = new Paragraph("Excellence • In • Creativity • And • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA, 14, SECONDARY_COLOR));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(15);
        headerCell.addElement(motto);

        // Academic info in bordered box
        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(70);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.BOX);
        infoCell.setBorderWidth(2);
        infoCell.setBorderColor(PRIMARY_COLOR);
        infoCell.setPadding(10);

        Paragraph academicInfo = new Paragraph(
                String.format("ACADEMIC YEAR: %s | TERM %d REPORT",
                        report.getAcademicYear(), report.getTerm()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INFO_COLOR));
        academicInfo.setAlignment(Element.ALIGN_CENTER);
        infoCell.addElement(academicInfo);

        infoTable.addCell(infoCell);
        headerCell.addElement(infoTable);

        headerTable.addCell(headerCell);
        document.add(headerTable);

        addModernSeparator(document);
    }

    private void addModernSeparator(Document document) throws DocumentException {
        PdfPTable separatorTable = new PdfPTable(1);
        separatorTable.setWidthPercentage(80);
        separatorTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        separatorTable.setSpacingBefore(5);
        separatorTable.setSpacingAfter(5);

        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(Rectangle.NO_BORDER);
        separatorCell.setFixedHeight(2);
        separatorCell.setBackgroundColor(new Color(230, 230, 230));
        separatorTable.addCell(separatorCell);

        document.add(separatorTable);
    }

    private void addStudentInfoSection(Document document, ReportDTO report) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{2, 3, 2, 3});
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(15);

        // Header
        PdfPCell headerCell = new PdfPCell(new Phrase("STUDENT INFORMATION",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
        headerCell.setColspan(4);
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(10);
        headerCell.setBorder(Rectangle.NO_BORDER);
        infoTable.addCell(headerCell);

        // Student details
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
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INFO_COLOR)));
        styleModernCell(labelCell, bgColor, Element.ALIGN_LEFT);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A",
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
        styleModernCell(valueCell, bgColor, Element.ALIGN_LEFT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void styleModernCell(PdfPCell cell, Color backgroundColor, int alignment) {
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(8);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(230, 230, 230));
    }

    private void addSubjectPerformanceTable(Document document, ReportDTO report) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(10);
        titleTable.setSpacingAfter(10);

        PdfPCell titleCell = new PdfPCell(new Phrase("SUBJECT PERFORMANCE ANALYSIS",
                FontFactory.getFont("Helvetica-Bold", 13, SECONDARY_COLOR)));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(10);
        titleTable.addCell(titleCell);

        document.add(titleTable);

        List<SubjectReport> subjectReports = report.getSubjectReports();
        int term = report.getTerm();
        String className = report.getClassName(); // Get class name from report

        PdfPTable subjectTable = new PdfPTable(getColumnCount(term));
        subjectTable.setWidthPercentage(100);
        subjectTable.setWidths(getColumnWidths(term));
        subjectTable.setSpacingBefore(5);

        // Table header
        addModernTableHeader(subjectTable, term);

        // Table rows - pass className to each row
        boolean alternate = false;
        for (SubjectReport subject : subjectReports) {
            addModernSubjectRow(subjectTable, subject, term, alternate, className);
            alternate = !alternate;
        }

        document.add(subjectTable);
    }

    private void addModernTableHeader(PdfPTable table, int term) {
        String[] headers = getHeadersForTerm(term);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setBorderWidthRight(1);
            cell.setBorderColorRight(Color.WHITE);
            table.addCell(cell);
        }
    }

    private void addModernSubjectRow(PdfPTable table, SubjectReport subject, int term, boolean alternate, String className) {
        Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;

        // Subject Name
        PdfPCell subjectCell = new PdfPCell(new Phrase(subject.getSubjectName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INFO_COLOR)));
        styleModernCell(subjectCell, rowColor, Element.ALIGN_LEFT);
        table.addCell(subjectCell);

        // Coefficient
        PdfPCell coeffCell = new PdfPCell(new Phrase(
                subject.getCoefficient() != null ? String.valueOf(subject.getCoefficient()) : "1",
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
        styleModernCell(coeffCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(coeffCell);

        // Assessment marks
        if (term == 3) {
            addModernAssessmentCell(table, subject.getAssessment1(), rowColor);
        } else {
            addModernAssessmentCell(table, subject.getAssessment1(), rowColor);
            addModernAssessmentCell(table, subject.getAssessment2(), rowColor);
        }

        // Total
        Double total = subject.getTotalScore(term);
        PdfPCell totalCell = new PdfPCell(new Phrase(formatDecimal(total),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK)));
        styleModernCell(totalCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(totalCell);

        // Average
        Double average = subject.getSubjectAverage();
        Color avgColor = getScoreColor(average);
        PdfPCell avgCell = new PdfPCell(new Phrase(formatDecimal(average),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, getContrastColor(avgColor))));
        avgCell.setBackgroundColor(avgColor);
        styleModernCell(avgCell, avgColor, Element.ALIGN_CENTER);
        table.addCell(avgCell);

        // Grade
        String grade = subject.getLetterGrade() != null ? subject.getLetterGrade() : "U";
        Color gradeColor = getGradeColor(grade);
        PdfPCell gradeCell = new PdfPCell(new Phrase(grade,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, getContrastColor(gradeColor))));
        gradeCell.setBackgroundColor(gradeColor);
        styleModernCell(gradeCell, gradeColor, Element.ALIGN_CENTER);
        table.addCell(gradeCell);

        // Status - Use correct passing logic based on class level
        boolean passed = isSubjectPassing(grade, className);
        String status = passed ? "PASS" : "FAIL";
        Color statusColor = passed ? SUCCESS_COLOR : DANGER_COLOR;
        PdfPCell statusCell = new PdfPCell(new Phrase(status,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, getContrastColor(statusColor))));
        statusCell.setBackgroundColor(statusColor);
        styleModernCell(statusCell, statusColor, Element.ALIGN_CENTER);
        table.addCell(statusCell);
    }

    private boolean isSubjectPassing(String grade, String className) {
        if (grade == null || className == null) return false;

        // Determine if this is an advanced level class
        boolean isAdvancedLevel = isAdvancedLevelClass(className);

        if (isAdvancedLevel) {
            // Advanced level: A, B, C, D, E are passing
            return grade.matches("[ABCDE]");
        } else {
            // Ordinary level: A, B, C are passing
            return grade.matches("[ABC]");
        }
    }

    private boolean isAdvancedLevelClass(String className) {
        if (className == null) return false;
        String lowerClassName = className.toLowerCase();
        return lowerClassName.contains("sixth") ||
                lowerClassName.contains("upper") ||
                lowerClassName.contains("lower") ||
                lowerClassName.contains("advanced") ||
                lowerClassName.contains("a level") ||
                lowerClassName.contains("as level") ||
                lowerClassName.contains("higher");
    }

    private void addModernAssessmentCell(PdfPTable table, Double score, Color rowColor) {
        Font scoreFont;
        Color textColor = Color.BLACK;

        if (score != null) {
            if (score < 10) {
                textColor = DANGER_COLOR;
                scoreFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor);
            } else {
                textColor = SUCCESS_COLOR;
                scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
            }
        } else {
            textColor = new Color(150, 150, 150);
            scoreFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
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
        summaryTable.setSpacingBefore(20);

        Double termAverage = report.getTermAverage();
        double passRate = report.getPassRate() != null ? report.getPassRate() : 0.0;
        Integer subjectsPassed = report.getSubjectsPassed() != null ? report.getSubjectsPassed() : 0;
        int totalSubjects = report.getTotalSubjects() != null ? report.getTotalSubjects() : 0;
        boolean overallPassed = report.getPassed() != null ? report.getPassed() : false;
        String className = report.getClassName();

        // Left column
        PdfPCell leftCell = createModernCardCell("PERFORMANCE SUMMARY", PRIMARY_COLOR);
        leftCell.setBackgroundColor(new Color(240, 248, 255));

        // Performance badge
        Color performanceColor = getPerformanceColor(termAverage);
        Paragraph performanceBadge = new Paragraph(
                gradeService.getPerformanceStatus(termAverage),
                FontFactory.getFont("Helvetica-Bold", 14, getContrastColor(performanceColor)));
        performanceBadge.setAlignment(Element.ALIGN_CENTER);

        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(80);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell badgeCell = new PdfPCell(performanceBadge);
        badgeCell.setBackgroundColor(performanceColor);
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setPadding(12);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeTable.addCell(badgeCell);
        leftCell.addElement(badgeTable);

        // Summary items
        addModernSummaryItem(leftCell, "Term Average:", report.getFormattedAverage());
        addModernSummaryItem(leftCell, "Overall Grade:",
                gradeService.calculateLetterGrade(termAverage, className));
        addModernSummaryItem(leftCell, "Class Rank:",
                report.getRankInClass() + " / " + report.getTotalStudentsInClass());
        addModernSummaryItem(leftCell, "Overall Status:",
                overallPassed ? "PASSED" : "FAILED");

        // Right column
        PdfPCell rightCell = createModernCardCell("SUBJECT STATISTICS", SECONDARY_COLOR);
        rightCell.setBackgroundColor(new Color(255, 250, 245));

        // Progress bar simulation
        addModernProgressBar(rightCell, "Pass Rate:", passRate,
                passRate >= 60 ? SUCCESS_COLOR : (passRate >= 40 ? WARNING_COLOR : DANGER_COLOR));

        // Subjects passed with correct criteria
        String passingCriteria = isAdvancedLevelClass(className) ?
                " (A-E & O are passing)" : " (A-C are passing)";
        addModernSummaryItem(rightCell, "Subjects Passed:",
                subjectsPassed + " of " + totalSubjects + " subjects" + passingCriteria);

        // Remarks - Using a cell with background color instead of paragraph styling
        Paragraph remarksTitle = new Paragraph("\nTEACHER'S REMARKS:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACCENT_COLOR));
        remarksTitle.setSpacingBefore(15);
        rightCell.addElement(remarksTitle);

        // Create a table cell for remarks with background color
        PdfPTable remarksTable = new PdfPTable(1);
        remarksTable.setWidthPercentage(100);
        remarksTable.setSpacingBefore(5);

        PdfPCell remarksCell = new PdfPCell();
        remarksCell.setBorder(Rectangle.BOX);
        remarksCell.setBorderWidth(1);
        remarksCell.setBorderColor(new Color(220, 220, 220));
        remarksCell.setBackgroundColor(new Color(250, 250, 250));
        remarksCell.setPadding(10);

        Paragraph remarks = new Paragraph(report.getRemarks(),
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY));
        remarksCell.addElement(remarks);

        remarksTable.addCell(remarksCell);
        rightCell.addElement(remarksTable);

        summaryTable.addCell(leftCell);
        summaryTable.addCell(rightCell);
        document.add(summaryTable);
    }

    private PdfPCell createModernCardCell(String title, Color titleColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1);
        cell.setBorderColor(new Color(200, 200, 200));
        cell.setPadding(15);

        Paragraph titleParagraph = new Paragraph(title,
                FontFactory.getFont("Helvetica-Bold", 12, titleColor));
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(15);
        cell.addElement(titleParagraph);

        return cell;
    }

    private void addModernSummaryItem(PdfPCell cell, String label, String value) {
        Paragraph item = new Paragraph();
        item.add(new Chunk(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INFO_COLOR)));
        item.add(new Chunk(" " + value,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
        item.setSpacingBefore(8);
        cell.addElement(item);
    }

    private void addModernProgressBar(PdfPCell cell, String label, double percentage, Color color) {
        Paragraph progress = new Paragraph();
        progress.add(new Chunk(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INFO_COLOR)));
        progress.add(new Chunk(" " + String.format("%.1f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color)));
        progress.setSpacingBefore(10);

        // Progress bar simulation using text
        int bars = (int) (percentage / 10);
        String progressBar = "█".repeat(Math.min(bars, 10)) + "░".repeat(10 - Math.min(bars, 10));
        Paragraph bar = new Paragraph(progressBar,
                FontFactory.getFont(FontFactory.HELVETICA, 12, color));
        bar.setSpacingBefore(2);

        cell.addElement(progress);
        cell.addElement(bar);
    }

    private void addSignatureSection(Document document, ReportDTO report) throws DocumentException {
        document.add(new Paragraph("\n"));

        PdfPTable signatureTable = new PdfPTable(1);
        signatureTable.setWidthPercentage(100);
        signatureTable.setSpacingBefore(30);

        PdfPCell vpCell = new PdfPCell();
        vpCell.setBorder(Rectangle.NO_BORDER);
        vpCell.setPaddingTop(20);
        vpCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        // Decorative line
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(50);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setFixedHeight(2);
        lineCell.setBackgroundColor(PRIMARY_COLOR);
        lineTable.addCell(lineCell);

        vpCell.addElement(lineTable);

        // Signature text
        Paragraph vpSignature = new Paragraph("___________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(100, 100, 100)));
        vpSignature.setAlignment(Element.ALIGN_RIGHT);
        vpSignature.setSpacingBefore(5);
        vpCell.addElement(vpSignature);

        Paragraph vpTitle = new Paragraph(
                "VICE PRINCIPAL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR)
        );
        vpTitle.setAlignment(Element.ALIGN_RIGHT);
        vpTitle.setSpacingBefore(5);
        vpCell.addElement(vpTitle);

        // School stamp - FIXED: Don't create a nested cell, just add elements
        Paragraph stamp = new Paragraph(
                "\nOFFICIAL STAMP",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, SECONDARY_COLOR)
        );
        stamp.setAlignment(Element.ALIGN_RIGHT);

        // Add stamp paragraph directly
        vpCell.addElement(stamp);

        signatureTable.addCell(vpCell);
        document.add(signatureTable);

        // Footer note
        Paragraph note = new Paragraph(
                "Note: This is an official document. Any alteration renders it invalid.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(150, 150, 150))
        );
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(20);
        document.add(note);
    }

    // Helper methods
    private int getColumnCount(int term) {
        return term == 3 ? 7 : 8;
    }

    private float[] getColumnWidths(int term) {
        if (term == 3) {
            return new float[]{3, 1, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f};
        } else {
            return new float[]{3, 1, 1.2f, 1.2f, 1.5f, 1.5f, 1.5f, 1.5f};
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

    private Color getContrastColor(Color backgroundColor) {
        double luminance = (0.299 * backgroundColor.getRed() +
                0.587 * backgroundColor.getGreen() +
                0.114 * backgroundColor.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private Color getPerformanceColor(Double average) {
        if (average == null) return new Color(150, 150, 150);
        if (average >= 18) return SUCCESS_COLOR;
        if (average >= 15) return PRIMARY_COLOR;
        if (average >= 10) return WARNING_COLOR;
        return DANGER_COLOR;
    }

    private Color getScoreColor(Double score) {
        if (score == null) return new Color(240, 240, 240);
        if (score >= 18) return new Color(220, 255, 220);
        if (score >= 15) return new Color(220, 240, 255);
        if (score >= 10) return new Color(255, 255, 220);
        if (score >= 5) return new Color(255, 240, 220);
        return new Color(255, 220, 220);
    }

    private Color getGradeColor(String grade) {
        if (grade == null) return INFO_COLOR;

        return switch (grade.toUpperCase()) {
            case "A" -> A_GRADE_COLOR;
            case "B" -> B_GRADE_COLOR;
            case "C" -> C_GRADE_COLOR;
            case "D" -> D_GRADE_COLOR;
            case "E" -> new Color(149, 165, 166); // Gray for E grade
            case "O", "U", "F" -> U_GRADE_COLOR; // Red for U/F grades
            default -> INFO_COLOR;
        };
    }

    private String formatDecimal(Double value) {
        if (value == null) return "-";
        return String.format("%.2f", value);
    }
}