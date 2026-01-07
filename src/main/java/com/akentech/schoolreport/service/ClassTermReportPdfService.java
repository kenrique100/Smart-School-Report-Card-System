package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
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
public class ClassTermReportPdfService extends BasePdfService {

    public ClassTermReportPdfService(GradeService gradeService) {
        super(gradeService);
    }

    public byte[] generateClassTermReportPdf(List<ReportDTO> reports, ClassRoom classRoom,
                                             Integer term, String academicYear)
            throws IOException, DocumentException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        // Always use hardcoded academic year
        addClassHeader(document, classRoom, term, academicYear, writer);
        addClassSummarySection(document, reports, classRoom, term);
        addClassStudentPerformanceTable(document, reports, term);
        addClassStatisticsSection(document, reports, classRoom, term);

        document.close();
        return outputStream.toByteArray();
    }

    // ====== PRIVATE METHODS ======

    private void addClassHeader(Document document, ClassRoom classRoom, Integer term,
                                String academicYear, PdfWriter writer)
            throws DocumentException, IOException {

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1.5f, 7, 1.5f});
        headerTable.setSpacingBefore(2);

        try {
            PdfPCell leftImageCell = createModernImageCell(LEFT_LOGO_PATH, Element.ALIGN_LEFT, 40);
            leftImageCell.setPadding(0);
            headerTable.addCell(leftImageCell);

            PdfPCell centerCell = createClassHeaderContentCell(classRoom, term, academicYear);
            headerTable.addCell(centerCell);

            PdfPCell rightImageCell = createModernImageCell(RIGHT_LOGO_PATH, Element.ALIGN_RIGHT, 40);
            rightImageCell.setPadding(0);
            headerTable.addCell(rightImageCell);

        } catch (IOException e) {
            log.warn("Could not load logo images: {}", e.getMessage());
            createClassFallbackHeader(document, classRoom, term, academicYear);
            return;
        }

        document.add(headerTable);
        addModernSeparator(document);
    }

    private PdfPCell createClassHeaderContentCell(ClassRoom classRoom, Integer term, String academicYear) {
        PdfPCell contentCell = new PdfPCell();
        contentCell.setBorder(Rectangle.NO_BORDER);
        contentCell.setBackgroundColor(Color.WHITE);
        contentCell.setPadding(2);
        contentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        contentCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School",
                FontFactory.getFont("Helvetica-Bold", 12, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(1);
        contentCell.addElement(schoolName);

        Paragraph classInfo = new Paragraph("CLASS TERM " + term + " REPORT - " + classRoom.getName(),
                FontFactory.getFont("Helvetica-Bold", 10, SECONDARY_COLOR));
        classInfo.setAlignment(Element.ALIGN_CENTER);
        classInfo.setSpacingAfter(1);
        contentCell.addElement(classInfo);

        // Hardcoded academic year
        Paragraph yearParagraph = new Paragraph("Academic Year: " + academicYear,
                FontFactory.getFont(FontFactory.HELVETICA, 8, ACCENT_COLOR));
        yearParagraph.setAlignment(Element.ALIGN_CENTER);
        yearParagraph.setSpacingAfter(2);
        contentCell.addElement(yearParagraph);

        Paragraph motto = new Paragraph("Excellence • In • Creativity • And • Innovation",
                FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(150, 150, 150)));
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(2);
        contentCell.addElement(motto);

        return contentCell;
    }

    private void createClassFallbackHeader(Document document, ClassRoom classRoom,
                                           Integer term, String academicYear)
            throws DocumentException {

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(2);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setBackgroundColor(Color.WHITE);
        headerCell.setPadding(3);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph schoolName = new Paragraph("DEBOS Bilingual Secondary And High School Kombe",
                FontFactory.getFont("Helvetica-Bold", 12, PRIMARY_COLOR));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        schoolName.setSpacingAfter(1);
        headerCell.addElement(schoolName);

        Paragraph classInfo = new Paragraph("CLASS TERM " + term + " REPORT - " + classRoom.getName(),
                FontFactory.getFont("Helvetica-Bold", 10, SECONDARY_COLOR));
        classInfo.setAlignment(Element.ALIGN_CENTER);
        classInfo.setSpacingAfter(2);
        headerCell.addElement(classInfo);

        // Hardcoded academic year
        Paragraph yearParagraph = new Paragraph("Academic Year: " + academicYear,
                FontFactory.getFont(FontFactory.HELVETICA, 8, ACCENT_COLOR));
        yearParagraph.setAlignment(Element.ALIGN_CENTER);
        yearParagraph.setSpacingAfter(2);
        headerCell.addElement(yearParagraph);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        addModernSeparator(document);
    }

    private void addClassSummarySection(Document document, List<ReportDTO> reports,
                                        ClassRoom classRoom, Integer term)
            throws DocumentException {

        PdfPTable summaryTable = new PdfPTable(1);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(5);

        PdfPCell titleCell = createModernCardCell("CLASS SUMMARY - TERM " + term, PRIMARY_COLOR);
        titleCell.setPadding(2);

        int totalStudents = reports.size();
        long passedStudents = reports.stream().filter(ReportDTO::getPassed).count();
        double classAverage = reports.stream()
                .filter(r -> r.getTermAverage() != null)
                .mapToDouble(ReportDTO::getTermAverage)
                .average()
                .orElse(0.0);
        double passRate = totalStudents > 0 ? (passedStudents * 100.0) / totalStudents : 0.0;

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setWidths(new float[]{1, 1, 1, 1});

        addStatCell(statsTable, "Total Students", String.valueOf(totalStudents), PRIMARY_COLOR);
        addStatCell(statsTable, "Class Average", String.format("%.2f/20", classAverage), ACCENT_COLOR);
        addStatCell(statsTable, "Passed Students", String.valueOf(passedStudents), SUCCESS_COLOR);
        addStatCell(statsTable, "Pass Rate", String.format("%.1f%%", passRate),
                passRate >= 60 ? SUCCESS_COLOR : (passRate >= 40 ? WARNING_COLOR : DANGER_COLOR));

        titleCell.addElement(statsTable);
        summaryTable.addCell(titleCell);
        document.add(summaryTable);
    }

    private void addClassStudentPerformanceTable(Document document, List<ReportDTO> reports, Integer term)
            throws DocumentException {

        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(5);

        PdfPCell titleCell = createModernCardCell("STUDENT PERFORMANCE RANKING", SECONDARY_COLOR);
        titleCell.setPadding(2);

        PdfPTable performanceTable = new PdfPTable(7);
        performanceTable.setWidthPercentage(100);
        performanceTable.setWidths(new float[]{0.7f, 2f, 0.9f, 0.9f, 0.7f, 0.9f, 1.1f});

        String[] headers = {"Rank", "Student Name", "Roll No", "Average", "Grade", "Pass/Fail", "Remarks"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE)));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(2);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setCellEvent(new RoundedBorderCellEvent(2, PRIMARY_COLOR, 0));
            performanceTable.addCell(cell);
        }

        reports.sort((r1, r2) -> {
            Integer rank1 = r1.getRankInClass() != null ? r1.getRankInClass() : Integer.MAX_VALUE;
            Integer rank2 = r2.getRankInClass() != null ? r2.getRankInClass() : Integer.MAX_VALUE;
            return rank1.compareTo(rank2);
        });

        boolean alternate = false;
        for (ReportDTO report : reports) {
            Color rowColor = alternate ? ROW_COLOR2 : ROW_COLOR1;
            alternate = !alternate;

            addClassStudentCell(performanceTable,
                    report.getRankInClass() != null ? report.getRankInClass().toString() : "-",
                    rowColor, Element.ALIGN_CENTER);
            addClassStudentCell(performanceTable, report.getStudentFullName(), rowColor, Element.ALIGN_LEFT);
            addClassStudentCell(performanceTable, report.getRollNumber(), rowColor, Element.ALIGN_CENTER);

            Color avgColor = getScoreColor(report.getTermAverage());
            addClassStudentCell(performanceTable,
                    report.getFormattedAverage() != null ? report.getFormattedAverage() : "N/A",
                    avgColor, Element.ALIGN_CENTER);

            String grade = gradeService.calculateLetterGrade(report.getTermAverage(), report.getClassName());
            Color gradeColor = getGradeColor(grade);
            addClassStudentCell(performanceTable, grade, gradeColor, Element.ALIGN_CENTER);

            String passFail = report.getPassed() ? "PASS" : "FAIL";
            Color pfColor = report.getPassed() ? SUCCESS_COLOR : DANGER_COLOR;
            addClassStudentCell(performanceTable, passFail, pfColor, Element.ALIGN_CENTER);

            addClassStudentCell(performanceTable,
                    report.getRemarks() != null ? report.getRemarks() : "No remarks",
                    rowColor, Element.ALIGN_LEFT);
        }

        titleCell.addElement(performanceTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }

    private void addClassStudentCell(PdfPTable table, String text, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 6, getContrastColor(bgColor))));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(1);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new RoundedBorderCellEvent(1, bgColor, 0));
        table.addCell(cell);
    }

    private void addClassStatisticsSection(Document document, List<ReportDTO> reports,
                                           ClassRoom classRoom, Integer term)
            throws DocumentException {

        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(5);

        PdfPCell titleCell = createModernCardCell("PERFORMANCE DISTRIBUTION", ACCENT_COLOR);
        titleCell.setPadding(2);

        long aCount = reports.stream()
                .filter(r -> "A".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();
        long bCount = reports.stream()
                .filter(r -> "B".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();
        long cCount = reports.stream()
                .filter(r -> "C".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();
        long dCount = reports.stream()
                .filter(r -> "D".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();
        long eCount = reports.stream()
                .filter(r -> "E".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();
        long uCount = reports.stream()
                .filter(r -> "U".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();
        long fCount = reports.stream()
                .filter(r -> "F".equals(gradeService.calculateLetterGrade(r.getTermAverage(), classRoom.getName())))
                .count();

        PdfPTable distributionTable = new PdfPTable(3);
        distributionTable.setWidthPercentage(70);
        distributionTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        distributionTable.setWidths(new float[]{1, 2, 1});

        addDistributionRow(distributionTable, "A Grade", aCount, reports.size(), A_GRADE_COLOR);
        addDistributionRow(distributionTable, "B Grade", bCount, reports.size(), B_GRADE_COLOR);
        addDistributionRow(distributionTable, "C Grade", cCount, reports.size(), C_GRADE_COLOR);
        addDistributionRow(distributionTable, "D Grade", dCount, reports.size(), D_GRADE_COLOR);
        addDistributionRow(distributionTable, "E Grade", eCount, reports.size(), E_GRADE_COLOR);
        addDistributionRow(distributionTable, "U Grade", uCount, reports.size(), U_GRADE_COLOR);
        addDistributionRow(distributionTable, "F Grade", fCount, reports.size(), F_GRADE_COLOR);

        titleCell.addElement(distributionTable);
        sectionTable.addCell(titleCell);
        document.add(sectionTable);
    }
}