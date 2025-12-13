package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.SubjectReport;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPdfService {

    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font GRADE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    private static final Color HEADER_BG_COLOR = new Color(41, 128, 185); // Blue
    private static final Color ROW_COLOR1 = new Color(255, 255, 255); // White
    private static final Color ROW_COLOR2 = new Color(245, 245, 245); // Light Gray
    private static final Color PASS_COLOR = new Color(39, 174, 96); // Green
    private static final Color FAIL_COLOR = new Color(231, 76, 60); // Red
    private static final Color EXCELLENT_COLOR = new Color(46, 204, 113); // Bright Green
    private static final Color GOOD_COLOR = new Color(52, 152, 219); // Blue
    private static final Color AVERAGE_COLOR = new Color(241, 196, 15); // Yellow
    private static final Color BELOW_AVG_COLOR = new Color(230, 126, 34); // Orange
    private static final Color POOR_COLOR = new Color(192, 57, 43); // Dark Red

    private final GradeService gradeService; // Add this dependency

    public byte[] generateTermReportPdf(ReportDTO report) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // Landscape orientation for Excel-like view
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Add school header
        addSchoolHeader(document, report);

        // Add report title
        addReportTitle(document, report);

        // Add student information section
        addStudentInfoSection(document, report);

        // Add assessment details
        addAssessmentDetails(document, report);

        // Add subject performance table (Excel-like)
        addSubjectPerformanceTable(document, report);

        // Add summary section
        addSummarySection(document, report);

        // Add footer with signatures
        addFooter(document, report);

        document.close();
        return outputStream.toByteArray();
    }

    private void addSchoolHeader(Document document, ReportDTO report) throws DocumentException, IOException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 3, 2});

        // Left column: School logo
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            Image logo = Image.getInstance(new ClassPathResource("static/images/school-logo.png").getURL());
            logo.scaleToFit(80, 80);
            leftCell.addElement(logo);
        } catch (Exception e) {
            // If logo not found, add text
            Paragraph schoolLogoText = new Paragraph("SCHOOL LOGO", TITLE_FONT);
            schoolLogoText.setAlignment(Element.ALIGN_LEFT);
            leftCell.addElement(schoolLogoText);
        }

        // Center column: School info with flag
        PdfPCell centerCell = new PdfPCell();
        centerCell.setBorder(Rectangle.NO_BORDER);
        centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        centerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Add Cameroon flag (simulated)
        Paragraph flag = new Paragraph("🇨🇲", new Font(FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24)));
        flag.setAlignment(Element.ALIGN_CENTER);
        centerCell.addElement(flag);

        Paragraph schoolName = new Paragraph("EXCELLENCE ACADEMIC COMPLEX", HEADER_FONT);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(5);
        centerCell.addElement(schoolName);

        Paragraph schoolMotto = new Paragraph("Knowledge, Excellence, Integrity", SUBTITLE_FONT);
        schoolMotto.setAlignment(Element.ALIGN_CENTER);
        schoolMotto.setSpacingAfter(5);
        centerCell.addElement(schoolMotto);

        Paragraph address = new Paragraph("P.O. Box 1234, Yaoundé, Cameroon | Tel: (+237) 6XX XXX XXX", NORMAL_FONT);
        address.setAlignment(Element.ALIGN_CENTER);
        address.setSpacingAfter(5);
        centerCell.addElement(address);

        Paragraph email = new Paragraph("Email: info@excellence-academic.cm | Website: www.excellence-academic.cm", SMALL_FONT);
        email.setAlignment(Element.ALIGN_CENTER);
        centerCell.addElement(email);

        // Right column: Academic year and term
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph academicYear = new Paragraph("ACADEMIC YEAR\n" + report.getAcademicYear(), BOLD_FONT);
        academicYear.setAlignment(Element.ALIGN_RIGHT);
        academicYear.setSpacingAfter(10);
        rightCell.addElement(academicYear);

        Paragraph term = new Paragraph("TERM " + report.getTerm() + " REPORT", BOLD_FONT);
        term.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(term);

        Paragraph date = new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), SMALL_FONT);
        date.setAlignment(Element.ALIGN_RIGHT);
        date.setSpacingBefore(10);
        rightCell.addElement(date);

        headerTable.addCell(leftCell);
        headerTable.addCell(centerCell);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Add separator line
        addSeparatorLine(document);
    }

    private void addReportTitle(Document document, ReportDTO report) throws DocumentException {
        Paragraph title = new Paragraph("OFFICIAL ACADEMIC REPORT CARD", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(10);
        title.setSpacingAfter(15);
        document.add(title);
    }

    private void addStudentInfoSection(Document document, ReportDTO report) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{2, 3, 2, 3});
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(15);

        // Header row
        PdfPCell headerCell = new PdfPCell(new Phrase("STUDENT INFORMATION", BOLD_FONT));
        headerCell.setColspan(4);
        headerCell.setBackgroundColor(HEADER_BG_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(8);
        headerCell.setBorderColor(Color.WHITE);
        infoTable.addCell(headerCell);

        // Student details
        addInfoRow(infoTable, "Student ID:", report.getStudentIdString());
        addInfoRow(infoTable, "Full Name:", report.getStudentFullName());
        addInfoRow(infoTable, "Date of Birth:", report.getFormattedDateOfBirth());
        addInfoRow(infoTable, "Gender:", report.getStudentGender());
        addInfoRow(infoTable, "Roll Number:", report.getRollNumber());
        addInfoRow(infoTable, "Class:", report.getClassName());
        addInfoRow(infoTable, "Department:", report.getDepartment());
        addInfoRow(infoTable, "Specialty:", report.getSpecialty());

        document.add(infoTable);
    }

    private void addAssessmentDetails(Document document, ReportDTO report) throws DocumentException {
        PdfPTable assessmentTable = new PdfPTable(2);
        assessmentTable.setWidthPercentage(100);
        assessmentTable.setWidths(new float[]{1, 3});
        assessmentTable.setSpacingBefore(10);
        assessmentTable.setSpacingAfter(15);

        // Header
        PdfPCell headerCell = new PdfPCell(new Phrase("ASSESSMENT DETAILS", BOLD_FONT));
        headerCell.setColspan(2);
        headerCell.setBackgroundColor(HEADER_BG_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(8);
        headerCell.setBorderColor(Color.WHITE);
        assessmentTable.addCell(headerCell);

        // Assessment info based on term
        String assessmentInfo = switch (report.getTerm()) {
            case 1 -> """
                    Term 1 includes two assessments:
                    • Assessment 1: 40% of term mark
                    • Assessment 2: 60% of term mark
                    Total: 100% of term mark""";
            case 2 -> """
                    Term 2 includes two assessments:
                    • Assessment 3: 40% of term mark
                    • Assessment 4: 60% of term mark
                    Total: 100% of term mark""";
            case 3 -> "Term 3 includes final examination only:\n" +
                    "• Final Exam: 100% of term mark";
            default -> "";
        };

        addInfoRow(assessmentTable, "Term:", "Term " + report.getTerm());
        addInfoRow(assessmentTable, "Assessments:", assessmentInfo);
        addInfoRow(assessmentTable, "Grading Scale:", "A: 18-20 | B: 15-17.9 | C: 10-14.9 | D: 5-9.9 | U: Below 5");
        addInfoRow(assessmentTable, "Passing Grade:", "C and above (≥10/20)");

        document.add(assessmentTable);
    }

    private void addSubjectPerformanceTable(Document document, ReportDTO report) throws DocumentException {
        Paragraph tableTitle = new Paragraph("SUBJECT PERFORMANCE ANALYSIS", SUBTITLE_FONT);
        tableTitle.setAlignment(Element.ALIGN_CENTER);
        tableTitle.setSpacingBefore(15);
        tableTitle.setSpacingAfter(10);
        document.add(tableTitle);

        List<SubjectReport> subjectReports = report.getSubjectReports();

        PdfPTable subjectTable = new PdfPTable(getColumnCount(report.getTerm()));
        subjectTable.setWidthPercentage(100);
        subjectTable.setWidths(getColumnWidths(report.getTerm()));

        // Table header
        addTableHeader(subjectTable, report.getTerm());

        // Table rows
        boolean alternate = false;

        for (SubjectReport subject : subjectReports) {
            addSubjectRow(subjectTable, subject, report.getTerm(), alternate);
            alternate = !alternate;
        }

        document.add(subjectTable);
    }

    private void addSummarySection(Document document, ReportDTO report) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{1, 1});
        summaryTable.setSpacingBefore(20);

        List<SubjectReport> subjectReports = report.getSubjectReports();

        // Left column: Performance summary
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(10);

        Paragraph summaryTitle = new Paragraph("PERFORMANCE SUMMARY", SUBTITLE_FONT);
        summaryTitle.setAlignment(Element.ALIGN_CENTER);
        summaryTitle.setSpacingAfter(10);
        leftCell.addElement(summaryTitle);

        addSummaryItem(leftCell, "Term Average:", report.getFormattedAverage());
        addSummaryItem(leftCell, "Overall Grade:", getLetterGrade(report.getTermAverage(), report.getClassName()));
        addSummaryItem(leftCell, "Class Rank:", report.getRankInClass() + " out of " + report.getTotalStudentsInClass());
        addSummaryItem(leftCell, "Performance Level:", getPerformanceLevel(report.getTermAverage()));
        addSummaryItem(leftCell, "Subjects Taken:", subjectReports.size() + " subjects");
        addSummaryItem(leftCell, "Subjects Passed:",
                subjectReports.stream().filter(s -> {
                    String grade = s.getLetterGrade();
                    return grade != null && grade.matches("[ABC]");
                }).count() + " subjects");

        // Right column: Statistics and remarks
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(10);

        Paragraph statsTitle = new Paragraph("STATISTICS & REMARKS", SUBTITLE_FONT);
        statsTitle.setAlignment(Element.ALIGN_CENTER);
        statsTitle.setSpacingAfter(10);
        rightCell.addElement(statsTitle);

        // Performance distribution
        long excellentCount = subjectReports.stream()
                .filter(s -> s.getSubjectAverage() != null && s.getSubjectAverage() >= 18)
                .count();
        long goodCount = subjectReports.stream()
                .filter(s -> s.getSubjectAverage() != null && s.getSubjectAverage() >= 15 && s.getSubjectAverage() < 18)
                .count();
        long averageCount = subjectReports.stream()
                .filter(s -> s.getSubjectAverage() != null && s.getSubjectAverage() >= 10 && s.getSubjectAverage() < 15)
                .count();
        long belowAvgCount = subjectReports.stream()
                .filter(s -> s.getSubjectAverage() != null && s.getSubjectAverage() >= 5 && s.getSubjectAverage() < 10)
                .count();
        long poorCount = subjectReports.stream()
                .filter(s -> s.getSubjectAverage() != null && s.getSubjectAverage() < 5)
                .count();

        addSummaryItem(rightCell, "Excellent (≥18):", excellentCount + " subjects");
        addSummaryItem(rightCell, "Good (15-17.9):", goodCount + " subjects");
        addSummaryItem(rightCell, "Average (10-14.9):", averageCount + " subjects");
        addSummaryItem(rightCell, "Below Avg (5-9.9):", belowAvgCount + " subjects");
        addSummaryItem(rightCell, "Poor (<5):", poorCount + " subjects");

        // Remarks
        Paragraph remarksTitle = new Paragraph("\nTEACHER'S REMARKS:", BOLD_FONT);
        remarksTitle.setSpacingBefore(10);
        rightCell.addElement(remarksTitle);

        Paragraph remarks = new Paragraph(report.getRemarks(), NORMAL_FONT);
        remarks.setSpacingBefore(5);
        rightCell.addElement(remarks);

        summaryTable.addCell(leftCell);
        summaryTable.addCell(rightCell);

        document.add(summaryTable);

        // Add performance chart
        addPerformanceChart(document, excellentCount, goodCount, averageCount, belowAvgCount, poorCount);
    }

    private void addFooter(Document document, ReportDTO report) throws DocumentException {
        PdfPTable footerTable = new PdfPTable(3);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{1, 1, 1});
        footerTable.setSpacingBefore(30);

        // Class Teacher
        PdfPCell teacherCell = new PdfPCell();
        teacherCell.setBorder(Rectangle.TOP);
        teacherCell.setPaddingTop(20);
        teacherCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        String classTeacher = report.getClassTeacher();
        Paragraph teacherName = new Paragraph(classTeacher != null ? classTeacher : "___________________________", NORMAL_FONT);
        teacherName.setAlignment(Element.ALIGN_CENTER);
        teacherCell.addElement(teacherName);

        Paragraph teacherTitle = new Paragraph("Class Teacher", BOLD_FONT);
        teacherTitle.setAlignment(Element.ALIGN_CENTER);
        teacherTitle.setSpacingBefore(5);
        teacherCell.addElement(teacherTitle);

        // Head of Department
        PdfPCell hodCell = new PdfPCell();
        hodCell.setBorder(Rectangle.TOP);
        hodCell.setPaddingTop(20);
        hodCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph hodName = new Paragraph("___________________________", NORMAL_FONT);
        hodName.setAlignment(Element.ALIGN_CENTER);
        hodCell.addElement(hodName);

        Paragraph hodTitle = new Paragraph("Head of Department", BOLD_FONT);
        hodTitle.setAlignment(Element.ALIGN_CENTER);
        hodTitle.setSpacingBefore(5);
        hodCell.addElement(hodTitle);

        // School Director
        PdfPCell directorCell = new PdfPCell();
        directorCell.setBorder(Rectangle.TOP);
        directorCell.setPaddingTop(20);
        directorCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph directorName = new Paragraph("___________________________", NORMAL_FONT);
        directorName.setAlignment(Element.ALIGN_CENTER);
        directorCell.addElement(directorName);

        Paragraph directorTitle = new Paragraph("School Director", BOLD_FONT);
        directorTitle.setAlignment(Element.ALIGN_CENTER);
        directorTitle.setSpacingBefore(5);
        directorCell.addElement(directorTitle);

        // Stamp section
        Paragraph stamp = new Paragraph("\n\nOFFICIAL STAMP", SMALL_FONT);
        stamp.setAlignment(Element.ALIGN_CENTER);
        directorCell.addElement(stamp);

        footerTable.addCell(teacherCell);
        footerTable.addCell(hodCell);
        footerTable.addCell(directorCell);

        document.add(footerTable);

        // Final notes
        Paragraph notes = new Paragraph("\n\nNote: This is an official document. Any alteration renders it invalid.", SMALL_FONT);
        notes.setAlignment(Element.ALIGN_CENTER);
        notes.setSpacingBefore(10);
        document.add(notes);
    }

    // Helper methods
    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(ROW_COLOR2);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderWidth(0.5f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", NORMAL_FONT));
        valueCell.setPadding(5);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderWidth(0.5f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private int getColumnCount(int term) {
        return term == 3 ? 8 : 9; // Less columns for term 3 (no separate assessments)
    }

    private float[] getColumnWidths(int term) {
        if (term == 3) {
            return new float[]{3, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f};
        } else {
            return new float[]{3, 1.5f, 1.2f, 1.2f, 1.2f, 1.2f, 1.5f, 1.5f, 1.5f};
        }
    }

    private void addTableHeader(PdfPTable table, int term) {
        String[] headers;
        if (term == 3) {
            headers = new String[]{"SUBJECT", "COEFF", "EXAM", "TOTAL", "AVERAGE", "GRADE", "REMARKS", "STATUS"};
        } else if (term == 1) {
            headers = new String[]{"SUBJECT", "COEFF", "ASSESS 1", "ASSESS 2", "TOTAL", "AVERAGE", "GRADE", "REMARKS", "STATUS"};
        } else {
            headers = new String[]{"SUBJECT", "COEFF", "ASSESS 3", "ASSESS 4", "TOTAL", "AVERAGE", "GRADE", "REMARKS", "STATUS"};
        }

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
            cell.setBackgroundColor(HEADER_BG_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorderColor(Color.WHITE);
            table.addCell(cell);
        }
    }

    private void addSubjectRow(PdfPTable table, SubjectReport subject, int term, boolean alternate) {
        Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;

        // Subject Name
        PdfPCell subjectCell = new PdfPCell(new Phrase(subject.getSubjectName(), BOLD_FONT));
        styleCell(subjectCell, rowColor, Element.ALIGN_LEFT);
        table.addCell(subjectCell);

        // Coefficient
        PdfPCell coeffCell = new PdfPCell(new Phrase(
                subject.getCoefficient() != null ? String.valueOf(subject.getCoefficient()) : "1",
                NORMAL_FONT));
        styleCell(coeffCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(coeffCell);

        // Assessment marks based on term
        if (term == 3) {
            // Term 3: Only exam (use assessment1 as exam score)
            addAssessmentCell(table, subject.getAssessment1(), rowColor);
        } else {
            // Term 1 or 2: Two assessments
            if (term == 1) {
                addAssessmentCell(table, subject.getAssessment1(), rowColor);
                addAssessmentCell(table, subject.getAssessment2(), rowColor);
            } else {
                // For term 2, use assessment1 and assessment2 (or create appropriate getters)
                addAssessmentCell(table, subject.getAssessment1(), rowColor);
                addAssessmentCell(table, subject.getAssessment2(), rowColor);
            }
        }

        // Total - calculate based on term
        Double total = calculateTotal(subject, term);
        PdfPCell totalCell = new PdfPCell(new Phrase(formatDecimal(total), NORMAL_FONT));
        styleCell(totalCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(totalCell);

        // Average
        PdfPCell avgCell = new PdfPCell(new Phrase(formatDecimal(subject.getSubjectAverage()), NORMAL_FONT));
        styleCell(avgCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(avgCell);

        // Grade with colored background
        String grade = subject.getLetterGrade();
        PdfPCell gradeCell = new PdfPCell(new Phrase(grade != null ? grade : "U", GRADE_FONT));
        gradeCell.setBackgroundColor(getGradeColor(grade));
        gradeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        gradeCell.setPadding(3);
        gradeCell.setBorder(Rectangle.BOX);
        gradeCell.setBorderWidth(0.5f);
        table.addCell(gradeCell);

        // Remarks
        PdfPCell remarksCell = new PdfPCell(new Phrase(getGradeRemarks(grade), NORMAL_FONT));
        styleCell(remarksCell, rowColor, Element.ALIGN_CENTER);
        table.addCell(remarksCell);

        // Status
        boolean passed = grade != null && grade.matches("[ABC]");
        String status = passed ? "PASS" : "FAIL";
        Color statusColor = passed ? PASS_COLOR : FAIL_COLOR;
        PdfPCell statusCell = new PdfPCell(new Phrase(status, BOLD_FONT));
        statusCell.setBackgroundColor(statusColor);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setPadding(3);
        statusCell.setBorderColor(Color.WHITE);
        table.addCell(statusCell);
    }

    private void addAssessmentCell(PdfPTable table, Double score, Color rowColor) {
        PdfPCell cell = new PdfPCell(new Phrase(score != null ? formatDecimal(score) : "-", NORMAL_FONT));
        styleCell(cell, rowColor, Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void styleCell(PdfPCell cell, Color backgroundColor, int alignment) {
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
    }

    private void addSeparatorLine(Document document) throws DocumentException {
        Paragraph line = new Paragraph();
        line.add(new Chunk("\n"));
        line.add(new Chunk(new LineSeparator(1, 100, Color.BLACK, Element.ALIGN_CENTER, -2)));
        line.add(new Chunk("\n"));
        document.add(line);
    }

    private void addSummaryItem(PdfPCell cell, String label, String value) {
        Paragraph item = new Paragraph();
        item.add(new Chunk(label, BOLD_FONT));
        item.add(new Chunk(" " + value, NORMAL_FONT));
        item.setSpacingBefore(3);
        cell.addElement(item);
    }

    private void addPerformanceChart(Document document, long excellent, long good, long average, long belowAvg, long poor)
            throws DocumentException {
        Paragraph chartTitle = new Paragraph("PERFORMANCE DISTRIBUTION", BOLD_FONT);
        chartTitle.setAlignment(Element.ALIGN_CENTER);
        chartTitle.setSpacingBefore(15);
        chartTitle.setSpacingAfter(10);
        document.add(chartTitle);

        PdfPTable chartTable = new PdfPTable(5);
        chartTable.setWidthPercentage(100);
        chartTable.setWidths(new float[]{1, 1, 1, 1, 1});

        addChartBar(chartTable, "EXCELLENT", excellent, EXCELLENT_COLOR);
        addChartBar(chartTable, "GOOD", good, GOOD_COLOR);
        addChartBar(chartTable, "AVERAGE", average, AVERAGE_COLOR);
        addChartBar(chartTable, "BELOW AVG", belowAvg, BELOW_AVG_COLOR);
        addChartBar(chartTable, "POOR", poor, POOR_COLOR);

        document.add(chartTable);
    }

    private void addChartBar(PdfPTable table, String label, long count, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Bar visualization
        Paragraph barLabel = new Paragraph(label, new Font(FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        barLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(barLabel);

        // Simple bar representation
        String bar = "█".repeat((int) Math.min(count, 10));
        Paragraph barGraph = new Paragraph(bar, new Font(FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color)));
        barGraph.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(barGraph);

        Paragraph countText = new Paragraph(count + " subjects", SMALL_FONT);
        countText.setAlignment(Element.ALIGN_CENTER);
        countText.setSpacingBefore(2);
        cell.addElement(countText);

        table.addCell(cell);
    }

    // Utility methods
    private String formatDecimal(Double value) {
        if (value == null) return "-";
        return String.format("%.2f", value);
    }

    private Double calculateTotal(SubjectReport subject, int term) {
        if (term == 3) {
            return subject.getAssessment1(); // Exam score for term 3
        } else if (term == 1) {
            // Term 1: weighted average of assessment1 and assessment2
            Double a1 = subject.getAssessment1();
            Double a2 = subject.getAssessment2();
            if (a1 == null && a2 == null) return null;
            double total = (a1 != null ? a1 : 0) * 0.4 + (a2 != null ? a2 : 0) * 0.6;
            return total;
        } else {
            // Term 2: weighted average of assessment1 and assessment2
            Double a1 = subject.getAssessment1();
            Double a2 = subject.getAssessment2();
            if (a1 == null && a2 == null) return null;
            double total = (a1 != null ? a1 : 0) * 0.4 + (a2 != null ? a2 : 0) * 0.6;
            return total;
        }
    }

    private Color getGradeColor(String grade) {
        if (grade == null) return Color.GRAY;

        return switch (grade.toUpperCase()) {
            case "A" -> EXCELLENT_COLOR;
            case "B" -> GOOD_COLOR;
            case "C" -> AVERAGE_COLOR;
            case "D" -> BELOW_AVG_COLOR;
            case "U", "F" -> POOR_COLOR;
            case "O" -> new Color(155, 89, 182); // Purple for Complimentary
            default -> Color.GRAY;
        };
    }

    private String getGradeRemarks(String grade) {
        if (grade == null) return "Not Graded";

        return switch (grade.toUpperCase()) {
            case "A" -> "Outstanding";
            case "B" -> "Very Good";
            case "C" -> "Good";
            case "D" -> "Satisfactory";
            case "U", "F" -> "Needs Improvement";
            case "O" -> "Complimentary";
            default -> "Not Graded";
        };
    }

    private String getLetterGrade(Double average, String className) {
        if (average == null) return "N/A";

        // Use the GradeService to calculate letter grade
        if (gradeService != null) {
            return gradeService.calculateLetterGrade(average, className != null ? className : "");
        }

        // Fallback calculation if GradeService is not available
        if (average >= 18) return "A";
        if (average >= 15) return "B";
        if (average >= 10) return "C";
        if (average >= 5) return "D";
        return "U";
    }

    private String getPerformanceLevel(Double average) {
        if (average == null) return "N/A";
        if (average >= 18) return "EXCELLENT";
        if (average >= 15) return "VERY GOOD";
        if (average >= 10) return "GOOD";
        if (average >= 5) return "SATISFACTORY";
        return "NEEDS IMPROVEMENT";
    }
}