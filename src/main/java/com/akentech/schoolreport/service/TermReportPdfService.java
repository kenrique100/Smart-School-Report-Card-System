package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.SubjectReport;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Generates a clean, print-ready A4 Term Report Card PDF.
 *
 * Watermark: school-logo.png is stamped onto the PDF FOREGROUND layer
 * (getDirectContent) at 18 % opacity using PdfGState — visible over all
 * cell backgrounds, centred in the subject-table area at ≈ 200 pt wide.
 *
 * Layout (top → bottom):
 *   gold rule → HEADER (emblem | school text | flag) → gold rule
 *   PROFILE BLOCK | STUDENT INFORMATION BLOCK
 *   SUBJECT PERFORMANCE ANALYSIS table
 *   PERFORMANCE SUMMARY | SUBJECT STATISTICS
 *   SIGNATURE ROW
 *   gold rule + footer note
 */
@Service
@Slf4j
public class TermReportPdfService extends BasePdfService {

    public TermReportPdfService(GradeService gradeService) {
        super(gradeService);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  WATERMARK PAGE EVENT
    //  Renders school-logo.png centred on every page, below all content.
    // ═══════════════════════════════════════════════════════════════════════

    private static class WatermarkEvent extends PdfPageEventHelper {

        private Image watermarkImage;

        // 200 pt ≈ 70 mm — big enough to read clearly, small enough not to dominate
        private static final float WM_SIZE = 240f;

        WatermarkEvent() {
            try {
                ClassPathResource res = new ClassPathResource(SCHOOL_LOGO_PATH);
                if (res.exists()) {
                    watermarkImage = Image.getInstance(res.getURL());
                    watermarkImage.scaleToFit(WM_SIZE, WM_SIZE);
                } else {
                    log.warn("Watermark image not found: {}", SCHOOL_LOGO_PATH);
                }
            } catch (Exception e) {
                log.warn("Could not load watermark image: {}", e.getMessage());
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (watermarkImage == null) return;

            try {
                // FOREGROUND layer — drawn OVER all content so it shows through
                // cell backgrounds.  PdfGState keeps it translucent.
                PdfContentByte canvas = writer.getDirectContent();

                canvas.saveState();

                // 18 % opacity: clearly visible as an official-mark without
                // obscuring the data underneath.
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.21f);
                gs.setStrokeOpacity(0.21f);
                canvas.setGState(gs);

                float pageW = document.getPageSize().getWidth();
                float pageH = document.getPageSize().getHeight();
                float imgW  = watermarkImage.getScaledWidth();
                float imgH  = watermarkImage.getScaledHeight();

                // Horizontally centred; vertically placed at ~45% from the bottom
                // — lands in the middle of the subject table area.
                float x = (pageW - imgW) / 2f;
                float y = (pageH * 0.55f) - (imgH / 2f);

                watermarkImage.setAbsolutePosition(x, y);
                canvas.addImage(watermarkImage);

                canvas.restoreState();
            } catch (Exception e) {
                log.warn("Could not draw watermark: {}", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    public byte[] generateTermReportPdf(ReportDTO report)
            throws IOException, DocumentException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // A4 margins: left/right ≈ 11 mm (31 pt), top/bottom ≈ 9 mm (25 pt)
        Document doc = new Document(PageSize.A4, 31, 31, 25, 25);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // Attach watermark page event BEFORE doc.open()
        writer.setPageEvent(new WatermarkEvent());

        doc.open();

        buildHeader(doc, report);
        buildProfileAndInfo(doc, report);
        buildSubjectTable(doc, report);
        buildSummaryRow(doc, report);
        buildSignatureRow(doc);
        buildFooter(doc);

        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1.  HEADER
    // ═══════════════════════════════════════════════════════════════════════

    private void buildHeader(Document doc, ReportDTO report)
            throws DocumentException, IOException {

        doc.add(goldRule(3.5f));

        PdfPTable hdr = new PdfPTable(3);
        hdr.setWidthPercentage(100);
        hdr.setWidths(new float[]{ 1.8f, 6.4f, 1.8f });
        hdr.setSpacingBefore(0);
        hdr.setSpacingAfter(0);

        hdr.addCell(logoCell(LEFT_LOGO_PATH,  Element.ALIGN_RIGHT));
        hdr.addCell(centreHeaderCell(report));
        hdr.addCell(logoCell(RIGHT_LOGO_PATH, Element.ALIGN_LEFT));

        doc.add(hdr);

        PdfPTable bottomRule = goldRule(3f);
        bottomRule.setSpacingBefore(0);
        bottomRule.setSpacingAfter(5);
        doc.add(bottomRule);
    }

    private PdfPCell logoCell(String path, int align) {
        PdfPCell c = blankCell();
        c.setBackgroundColor(Color.WHITE);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            ClassPathResource res = new ClassPathResource(path);
            if (res.exists()) {
                Image img = Image.getInstance(res.getURL());
                img.scaleToFit(56f, 56f);
                c.addElement(img);
                return c;
            }
        } catch (Exception e) {
            log.warn("Logo '{}' not found: {}", path, e.getMessage());
        }
        // Fallback: CSS-drawn Cameroon flag for the right side
        if (align == Element.ALIGN_LEFT) return inlineFlagCell();
        return c;
    }

    /** Inline CSS-drawn Cameroon flag — right-side fallback. */
    private PdfPCell inlineFlagCell() {
        PdfPTable flag = new PdfPTable(3);
        flag.setWidthPercentage(100);
        addStripe(flag, new Color(0, 122, 94));
        addStripe(flag, new Color(206, 17, 38));
        PdfPCell yc = new PdfPCell(new Phrase("\u2605",
                FontFactory.getFont(FontFactory.HELVETICA, 9f, new Color(0, 122, 94))));
        yc.setBackgroundColor(new Color(252, 209, 22));
        yc.setBorder(Rectangle.NO_BORDER);
        yc.setHorizontalAlignment(Element.ALIGN_CENTER);
        yc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        yc.setFixedHeight(34f);
        flag.addCell(yc);

        PdfPCell wrap = blankCell();
        wrap.setBackgroundColor(Color.WHITE);
        wrap.setPadding(4);
        wrap.addElement(flag);
        Paragraph lbl = new Paragraph("CAMEROON",
                FontFactory.getFont(FontFactory.HELVETICA, 5.5f, MUTED_CLR));
        lbl.setAlignment(Element.ALIGN_LEFT);
        lbl.setSpacingBefore(2);
        wrap.addElement(lbl);
        return wrap;
    }

    private void addStripe(PdfPTable t, Color color) {
        PdfPCell s = blankCell();
        s.setBackgroundColor(color);
        s.setFixedHeight(34f);
        t.addCell(s);
    }

    private PdfPCell centreHeaderCell(ReportDTO report) {
        PdfPCell c = blankCell();
        c.setBackgroundColor(Color.WHITE);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);

        c.addElement(centred(
                "DEBOS Bilingual Secondary And High School Kombe",
                FontFactory.HELVETICA_BOLD, 12.5f, NAVY, 2f));
        c.addElement(centred(
                ". . . . . . . . . . . . . . .",
                FontFactory.HELVETICA, 5f, MUTED_CLR, 2f));
        c.addElement(centred(
                "Excellence  \u2022  In  \u2022  Creativity  \u2022  And  \u2022  Innovation",
                FontFactory.HELVETICA_BOLD, 7f, GOLD, 5f));

        // Term badge
        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(66);
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell bc = new PdfPCell(new Phrase(
                "Academic Year: " + report.getAcademicYear()
                        + "  |  TERM " + report.getTerm() + " REPORT",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
        bc.setBackgroundColor(NAVY);
        bc.setBorder(Rectangle.NO_BORDER);
        bc.setPadding(4);
        bc.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.addCell(bc);
        c.addElement(badge);

        c.addElement(centred(
                "Kotto Road Kombe  |  Phone: 677755377 / 670252217",
                FontFactory.HELVETICA_OBLIQUE, 6f, MUTED_CLR, 0f));
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2.  PROFILE CIRCLE  +  STUDENT INFO BLOCK
    // ═══════════════════════════════════════════════════════════════════════

    private void buildProfileAndInfo(Document doc, ReportDTO report)
            throws DocumentException, IOException {

        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{ 1.15f, 5.85f });
        row.setSpacingBefore(2);
        row.setSpacingAfter(5);

        // ── BLOCK 1: Profile circle ───────────────────────────────────────
        PdfPCell profileBlock = new PdfPCell();
        profileBlock.setBorder(Rectangle.BOX);
        profileBlock.setBorderColor(BORDER_CLR);
        profileBlock.setBorderWidth(0.6f);
        profileBlock.setBackgroundColor(new Color(238, 242, 248));
        profileBlock.setPadding(8);
        profileBlock.setHorizontalAlignment(Element.ALIGN_CENTER);
        profileBlock.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable circleWrap = new PdfPTable(1);
        circleWrap.setWidthPercentage(88);
        circleWrap.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell avatarCell = new PdfPCell();
        avatarCell.setBorder(Rectangle.NO_BORDER);
        avatarCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        avatarCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        avatarCell.setFixedHeight(68f);

        try {
            ClassPathResource res = new ClassPathResource(AVATAR_PATH);
            if (!res.exists()) throw new IOException("avatar not found");
            Image avatar = Image.getInstance(res.getURL());
            avatar.scaleToFit(60f, 60f);
            avatarCell.setBackgroundColor(new Color(238, 242, 248));
            avatarCell.addElement(avatar);
        } catch (Exception e) {
            // Coloured-initial fallback
            boolean male = "MALE".equalsIgnoreCase(report.getStudentGender());
            Color bg = male ? new Color(37, 99, 160) : new Color(190, 80, 130);
            avatarCell.setBackgroundColor(bg);
            avatarCell.setCellEvent(new RoundedBorderCellEvent(34f, bg, 0f));
            Paragraph init = new Paragraph(male ? "M" : "F",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28f, Color.WHITE));
            init.setAlignment(Element.ALIGN_CENTER);
            avatarCell.addElement(init);
        }

        circleWrap.addCell(avatarCell);
        profileBlock.addElement(circleWrap);
        row.addCell(profileBlock);

        // ── BLOCK 2: Student info ─────────────────────────────────────────
        PdfPCell infoBlock = blankCell();
        infoBlock.setPadding(0);

        PdfPTable titleBar = new PdfPTable(1);
        titleBar.setWidthPercentage(100);
        PdfPCell tc = new PdfPCell(new Phrase("  STUDENT INFORMATION",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)));
        tc.setBackgroundColor(NAVY);
        tc.setBorder(Rectangle.NO_BORDER);
        tc.setPadding(5);
        titleBar.addCell(tc);
        infoBlock.addElement(titleBar);

        PdfPTable grid = new PdfPTable(4);
        grid.setWidthPercentage(100);
        grid.setWidths(new float[]{ 1.7f, 2.8f, 1.7f, 2.8f });

        addInfoRow(grid, "Full Name:",     report.getStudentFullName(),
                "Roll No:",       report.getRollNumber(),          0);
        addInfoRow(grid, "Student ID:",    report.getStudentIdString(),
                "Class:",         report.getClassName(),            1);
        addInfoRow(grid, "Department:",    report.getDepartment(),
                "Specialty:",     report.getSpecialty(),            0);
        addInfoRow(grid, "Date of Birth:", report.getFormattedDateOfBirth(),
                "Gender:",        report.getStudentGender(),        1);

        infoBlock.addElement(grid);
        row.addCell(infoBlock);
        doc.add(row);
    }

    private void addInfoRow(PdfPTable t,
                            String l1, String v1,
                            String l2, String v2,
                            int idx) {
        Color bg = idx % 2 == 0 ? ROW_EVEN : ROW_ODD;
        t.addCell(iLbl(l1, bg)); t.addCell(iVal(v1, bg));
        t.addCell(iLbl(l2, bg)); t.addCell(iVal(v2, bg));
    }

    private PdfPCell iLbl(String text, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL_CLR)));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_CLR);
        c.setBorderWidth(0.4f);
        c.setPadding(4.5f);
        c.setPaddingLeft(8f);
        return c;
    }

    private PdfPCell iVal(String text, Color bg) {
        String s = (text != null && !text.isBlank()) ? text : "N/A";
        PdfPCell c = new PdfPCell(new Phrase(s,
                FontFactory.getFont(FontFactory.HELVETICA, 7.5f, VALUE_CLR)));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_CLR);
        c.setBorderWidth(0.4f);
        c.setPadding(4.5f);
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3.  SUBJECT PERFORMANCE TABLE
    // ═══════════════════════════════════════════════════════════════════════

    private void buildSubjectTable(Document doc, ReportDTO report)
            throws DocumentException {

        doc.add(navyBar("  SUBJECT PERFORMANCE ANALYSIS"));

        int    term   = report.getTerm();
        boolean t3    = (term == 3);
        String[] hdrs = t3
                ? new String[]{ "SUBJECT","COEFF","EXAM","TOTAL","AVERAGE","GRADE","STATUS" }
                : (term == 1)
                ? new String[]{ "SUBJECT","COEFF","A1","A2","TOTAL","AVERAGE","GRADE","STATUS" }
                : new String[]{ "SUBJECT","COEFF","A3","A4","TOTAL","AVERAGE","GRADE","STATUS" };
        float[] widths = t3
                ? new float[]{ 3.4f, 0.6f, 1f, 1f, 1f, 0.9f, 1.1f }
                : new float[]{ 3.2f, 0.6f, 0.85f, 0.85f, 1f, 1f, 0.9f, 1.1f };

        PdfPTable tbl = new PdfPTable(hdrs.length);
        tbl.setWidthPercentage(100);
        tbl.setWidths(widths);
        tbl.setSpacingBefore(1);
        tbl.setSpacingAfter(5);

        // Header row
        for (int i = 0; i < hdrs.length; i++) {
            PdfPCell h = new PdfPCell(new Phrase(hdrs[i],
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
            h.setBackgroundColor(NAVY);
            h.setBorder(Rectangle.NO_BORDER);
            h.setPadding(4.5f);
            h.setPaddingLeft(i == 0 ? 8f : 4f);
            h.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            tbl.addCell(h);
        }

        List<SubjectReport> subjects = report.getSubjectReports();
        String cn = report.getClassName();

        for (int i = 0; i < subjects.size(); i++) {
            SubjectReport s  = subjects.get(i);
            Color rowBg      = (i % 2 == 0) ? ROW_EVEN : ROW_ODD;

            // Subject name
            PdfPCell nc = new PdfPCell(new Phrase(s.getSubjectName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, VALUE_CLR)));
            nc.setBackgroundColor(rowBg);
            bottomBorder(nc);
            nc.setPadding(4.5f);
            nc.setPaddingLeft(8f);
            tbl.addCell(nc);

            // Coefficient
            tbl.addCell(dCell(
                    s.getCoefficient() != null ? String.valueOf(s.getCoefficient()) : "1",
                    rowBg, VALUE_CLR));

            // Assessments
            if (t3) {
                tbl.addCell(sCell(s.getAssessment1(), rowBg));
            } else {
                tbl.addCell(sCell(s.getAssessment1(), rowBg));
                tbl.addCell(sCell(s.getAssessment2(), rowBg));
            }

            // Total
            Double tot = s.getTotalScore(term);
            tbl.addCell(dCell(formatDecimal(tot), rowBg,
                    tot != null && tot >= 10 ? VALUE_CLR : FAIL_RED));

            // Average — tinted background
            Double  avg   = s.getSubjectAverage();
            boolean aPass = avg != null && avg >= 10;
            PdfPCell ac   = new PdfPCell(new Phrase(formatDecimal(avg),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f,
                            aPass ? PASS_GREEN : FAIL_RED)));
            ac.setBackgroundColor(aPass ? PASS_BG : FAIL_BG);
            bottomBorder(ac);
            ac.setPadding(4.5f);
            ac.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(ac);

            // Grade pill
            String grade = s.getLetterGrade() != null ? s.getLetterGrade() : "U";
            PdfPCell gc  = new PdfPCell(new Phrase(grade,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, getGradeTextColor(grade))));
            gc.setBackgroundColor(getGradeColor(grade));
            gc.setBorder(Rectangle.NO_BORDER);
            gc.setPadding(4.5f);
            gc.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(gc);

            // Status
            boolean pass = isSubjectPassing(grade, cn);
            PdfPCell sc2 = new PdfPCell(new Phrase(pass ? "PASS" : "FAIL",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f,
                            pass ? PASS_GREEN : FAIL_RED)));
            sc2.setBackgroundColor(pass ? PASS_BG : FAIL_BG);
            sc2.setBorder(Rectangle.NO_BORDER);
            sc2.setPadding(4.5f);
            sc2.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(sc2);
        }

        doc.add(tbl);
    }

    private void bottomBorder(PdfPCell c) {
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_CLR);
        c.setBorderWidth(0.4f);
    }

    private PdfPCell dCell(String text, Color bg, Color tc) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—",
                FontFactory.getFont(FontFactory.HELVETICA, 7.5f, tc)));
        c.setBackgroundColor(bg);
        bottomBorder(c);
        c.setPadding(4.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private PdfPCell sCell(Double score, Color bg) {
        String txt = score != null ? formatDecimal(score) : "—";
        Color  tc  = score == null ? MUTED_CLR : (score < 10 ? FAIL_RED : PASS_GREEN);
        PdfPCell c = new PdfPCell(new Phrase(txt,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, tc)));
        c.setBackgroundColor(bg);
        bottomBorder(c);
        c.setPadding(4.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private boolean isSubjectPassing(String grade, String cn) {
        if (grade == null || cn == null) return false;
        return isAdvancedLevelClass(cn)
                ? grade.matches("[ABCDE]")
                : grade.matches("[ABC]");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4.  SUMMARY ROW
    // ═══════════════════════════════════════════════════════════════════════

    private void buildSummaryRow(Document doc, ReportDTO report)
            throws DocumentException {

        String  cn      = report.getClassName();
        Double  avg     = report.getTermAverage();
        double  rate    = report.getPassRate()       != null ? report.getPassRate()       : 0.0;
        int     passed  = report.getSubjectsPassed() != null ? report.getSubjectsPassed() : 0;
        int     total   = report.getTotalSubjects()  != null ? report.getTotalSubjects()  : 0;
        boolean overall = Boolean.TRUE.equals(report.getPassed());

        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{ 1f, 1f });
        row.setSpacingBefore(2);
        row.setSpacingAfter(5);

        // ── LEFT: Performance Summary ──────────────────────────────────────
        PdfPCell left = cardCell("PERFORMANCE SUMMARY", NAVY);

        PdfPTable pb = new PdfPTable(1);
        pb.setWidthPercentage(56);
        pb.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell pbc = new PdfPCell(new Phrase(gradeService.getPerformanceStatus(avg),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)));
        pbc.setBackgroundColor(getPerformanceColor(avg));
        pbc.setBorder(Rectangle.NO_BORDER);
        pbc.setPadding(5);
        pbc.setHorizontalAlignment(Element.ALIGN_CENTER);
        pb.addCell(pbc);
        left.addElement(pb);
        left.addElement(thinRule());

        sumLine(left, "Term Average:", report.getFormattedAverage(),
                avg != null && avg >= 10);
        sumNeutral(left, "Overall Grade:",
                gradeService.calculateLetterGrade(avg, cn));
        sumNeutral(left, "Class Rank:",
                report.getRankInClass() + " / " + report.getTotalStudentsInClass());

        if (report.getRankInDepartment() != null
                && !"N/A".equalsIgnoreCase(report.getDepartment())) {
            sumNeutral(left,
                    "Department Rank (" + report.getDepartment() + "):",
                    report.getRankInDepartment() + " / " + report.getTotalStudentsInDepartment());
        }

        Paragraph stLine = new Paragraph();
        stLine.add(new Chunk("Overall Status:  ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL_CLR)));
        stLine.add(new Chunk(overall ? "PASSED" : "FAILED",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f,
                        overall ? PASS_GREEN : FAIL_RED)));
        stLine.setSpacingBefore(4);
        left.addElement(stLine);
        row.addCell(left);

        // ── RIGHT: Subject Statistics ──────────────────────────────────────
        PdfPCell right = cardCell("SUBJECT STATISTICS", TEAL);

        Paragraph rl = new Paragraph();
        rl.add(new Chunk("Pass Rate:  ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL_CLR)));
        rl.add(new Chunk(String.format("%.1f%%", rate),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, PASS_GREEN)));
        rl.setSpacingBefore(4);
        right.addElement(rl);

        int filled = Math.min((int)(rate / 10), 10);
        Paragraph bar = new Paragraph(
                "\u2588".repeat(filled) + "\u2591".repeat(10 - filled),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, PASS_GREEN));
        bar.setSpacingBefore(1);
        right.addElement(bar);
        right.addElement(thinRule());

        String crit = isAdvancedLevelClass(cn) ? " (A–E are passing)" : " (A–C are passing)";
        sumNeutral(right, "Subjects Passed:", passed + " of " + total + " subjects" + crit);
        right.addElement(thinRule());

        Paragraph rlbl = new Paragraph("TEACHER'S REMARKS:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL_CLR));
        rlbl.setSpacingBefore(4);
        rlbl.setSpacingAfter(3);
        right.addElement(rlbl);

        PdfPTable rmk = new PdfPTable(1);
        rmk.setWidthPercentage(100);
        PdfPCell rc = new PdfPCell(new Phrase(
                report.getRemarks() != null ? report.getRemarks() : "—",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, VALUE_CLR)));
        rc.setBackgroundColor(new Color(250, 251, 253));
        rc.setBorder(Rectangle.BOX);
        rc.setBorderColor(BORDER_CLR);
        rc.setBorderWidth(0.5f);
        rc.setPadding(5);
        rmk.addCell(rc);
        right.addElement(rmk);

        row.addCell(right);
        doc.add(row);
    }

    private PdfPCell cardCell(String title, Color hdrColor) {
        PdfPCell card = new PdfPCell();
        card.setBorder(Rectangle.BOX);
        card.setBorderColor(BORDER_CLR);
        card.setBorderWidth(0.6f);
        card.setPaddingLeft(9);
        card.setPaddingRight(9);
        card.setPaddingBottom(9);
        card.setPaddingTop(0);

        PdfPTable tb = new PdfPTable(1);
        tb.setWidthPercentage(100);
        PdfPCell tc = new PdfPCell(new Phrase("  " + title,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE)));
        tc.setBackgroundColor(hdrColor);
        tc.setBorder(Rectangle.NO_BORDER);
        tc.setPadding(5);
        tb.addCell(tc);
        card.addElement(tb);
        return card;
    }

    private void sumLine(PdfPCell cell, String lbl, String val, boolean positive) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(lbl + "  ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL_CLR)));
        p.add(new Chunk(val, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f,
                positive ? PASS_GREEN : FAIL_RED)));
        p.setSpacingBefore(4);
        cell.addElement(p);
    }

    private void sumNeutral(PdfPCell cell, String lbl, String val) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(lbl + "  ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL_CLR)));
        p.add(new Chunk(val, FontFactory.getFont(FontFactory.HELVETICA, 7.5f, VALUE_CLR)));
        p.setSpacingBefore(4);
        cell.addElement(p);
    }

    private PdfPTable thinRule() {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(5);
        t.setSpacingAfter(0);
        PdfPCell c = blankCell();
        c.setFixedHeight(0.6f);
        c.setBackgroundColor(BORDER_CLR);
        t.addCell(c);
        return t;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  5.  SIGNATURE ROW
    // ═══════════════════════════════════════════════════════════════════════

    private void buildSignatureRow(Document doc) throws DocumentException {
        PdfPTable row = new PdfPTable(3);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{ 1f, 1f, 1f });
        row.setSpacingBefore(6);
        row.setSpacingAfter(3);

        row.addCell(sigCell("CLASS TEACHER", null));

        // Official Stamp placeholder
        PdfPCell stampWrap = blankCell();
        stampWrap.setHorizontalAlignment(Element.ALIGN_CENTER);
        stampWrap.setVerticalAlignment(Element.ALIGN_MIDDLE);
        PdfPTable sb = new PdfPTable(1);
        sb.setWidthPercentage(65);
        sb.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell sc = new PdfPCell(new Phrase("OFFICIAL STAMP",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, BORDER_CLR)));
        sc.setBorder(Rectangle.BOX);
        sc.setBorderColor(BORDER_CLR);
        sc.setBorderWidth(0.8f);
        sc.setFixedHeight(38f);
        sc.setPadding(5);
        sc.setHorizontalAlignment(Element.ALIGN_CENTER);
        sc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sb.addCell(sc);
        stampWrap.addElement(sb);
        row.addCell(stampWrap);

        row.addCell(sigCell("VICE PRINCIPAL", "Kohsu Rodolphe Rinwi"));
        doc.add(row);
    }

    private PdfPCell sigCell(String role, String name) {
        PdfPCell c = blankCell();
        c.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPTable space = new PdfPTable(1);
        space.setWidthPercentage(72);
        space.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell sp = blankCell();
        sp.setFixedHeight(22f);
        sp.setBackgroundColor(Color.WHITE);
        space.addCell(sp);
        c.addElement(space);

        PdfPTable ul = new PdfPTable(1);
        ul.setWidthPercentage(72);
        ul.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell uc = blankCell();
        uc.setFixedHeight(0.8f);
        uc.setBackgroundColor(new Color(170, 170, 170));
        ul.addCell(uc);
        c.addElement(ul);

        if (name != null) {
            Paragraph nm = new Paragraph(name,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, NAVY));
            nm.setAlignment(Element.ALIGN_CENTER);
            nm.setSpacingBefore(2);
            c.addElement(nm);
        }

        Paragraph rp = new Paragraph(role,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, NAVY_LIGHT));
        rp.setAlignment(Element.ALIGN_CENTER);
        rp.setSpacingBefore(1);
        c.addElement(rp);
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  6.  FOOTER
    // ═══════════════════════════════════════════════════════════════════════

    private void buildFooter(Document doc) throws DocumentException {
        PdfPTable rule = goldRule(2f);
        rule.setSpacingBefore(6);
        doc.add(rule);

        Paragraph note = new Paragraph(
                "Note: This is an official document. Any alteration renders it invalid.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6.5f, MUTED_CLR));
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(4);
        doc.add(note);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TINY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private PdfPTable navyBar(String text) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        t.setSpacingAfter(1);
        PdfPCell c = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)));
        c.setBackgroundColor(NAVY);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(5);
        t.addCell(c);
        return t;
    }

    private Paragraph centred(String text, String font, float size, Color color, float spacingAfter) {
        Paragraph p = new Paragraph(text, FontFactory.getFont(font, size, color));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(spacingAfter);
        return p;
    }
}