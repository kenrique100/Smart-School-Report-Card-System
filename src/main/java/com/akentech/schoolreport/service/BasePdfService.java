package com.akentech.schoolreport.service;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;

@Component
@Slf4j
public abstract class BasePdfService {

    // ── Colour palette ────────────────────────────────────────────────────────
    protected static final Color NAVY        = new Color(17,  50,  92);
    protected static final Color NAVY_LIGHT  = new Color(30,  75, 130);
    protected static final Color GOLD        = new Color(180, 140,  40);
    protected static final Color TEAL        = new Color(15,  100,  90);
    protected static final Color PASS_GREEN  = new Color(20,  100,  50);
    protected static final Color FAIL_RED    = new Color(160,  30,  30);
    protected static final Color PASS_BG     = new Color(220, 242, 228);
    protected static final Color FAIL_BG     = new Color(253, 224, 224);
    protected static final Color ROW_ODD     = new Color(248, 250, 253);
    protected static final Color ROW_EVEN    = Color.WHITE;
    protected static final Color BORDER_CLR  = new Color(200, 210, 225);
    protected static final Color LABEL_CLR   = new Color(70,  90, 120);
    protected static final Color VALUE_CLR   = new Color(10,  20,  45);
    protected static final Color MUTED_CLR   = new Color(130, 145, 165);

    // Legacy aliases kept so existing subclasses compile without changes
    protected static final Color PRIMARY_COLOR   = NAVY;
    protected static final Color SECONDARY_COLOR = new Color(220, 53, 69);
    protected static final Color ACCENT_COLOR    = new Color(0, 128, 96);
    protected static final Color SUCCESS_COLOR   = PASS_GREEN;
    protected static final Color DANGER_COLOR    = FAIL_RED;
    protected static final Color WARNING_COLOR   = new Color(255, 152, 0);
    protected static final Color INFO_COLOR      = new Color(52,  58, 64);
    protected static final Color LIGHT_GRAY      = new Color(248, 249, 250);
    protected static final Color MEDIUM_GRAY     = new Color(222, 226, 230);
    protected static final Color TEXT_COLOR      = new Color(33,  37, 41);
    protected static final Color ROW_COLOR1      = Color.WHITE;
    protected static final Color ROW_COLOR2      = new Color(248, 249, 250);
    protected static final Color HEADER_BG       = NAVY;
    protected static final Color HEADER_TEXT     = Color.WHITE;

    // Grade colours
    protected static final Color A_GRADE_COLOR = new Color(40, 167, 69);
    protected static final Color B_GRADE_COLOR = new Color(209, 250, 229);
    protected static final Color C_GRADE_COLOR = new Color(207, 226, 255);
    protected static final Color D_GRADE_COLOR = new Color(255, 152,   0);
    protected static final Color E_GRADE_COLOR = new Color(255, 237, 213);
    protected static final Color O_GRADE_COLOR = new Color(111,  66, 193);
    protected static final Color U_GRADE_COLOR = new Color(220,  53, 69);
    protected static final Color F_GRADE_COLOR = new Color(254, 226, 226);

    // ── Image paths (served from src/main/resources/static/images/) ──────────
    protected static final String LEFT_LOGO_PATH    = "static/images/emblem.png";   // Cameroon coat-of-arms
    protected static final String RIGHT_LOGO_PATH   = "static/images/cameroon-flag.png";
    protected static final String SCHOOL_LOGO_PATH  = "static/images/school-logo.png";
    protected static final String AVATAR_PATH       = "static/images/avatar.png";
    protected static final String CAMEROON_FLAG_PATH = "static/images/cameroon-flag.png";
    protected static final String CAMBRIDGE_BADGE_PATH = "static/images/cambridge-badge.png";
    protected static final String DEFAULT_AVATAR_PATH  = "static/images/avatar.png";

    protected final GradeService gradeService;

    protected BasePdfService(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    // ── Rounded-border cell event ─────────────────────────────────────────────
    protected static class RoundedBorderCellEvent implements PdfPCellEvent {
        private final float   radius;
        private final Color   borderColor;
        private final float   borderWidth;

        public RoundedBorderCellEvent(float radius, Color borderColor, float borderWidth) {
            this.radius      = radius;
            this.borderColor = borderColor;
            this.borderWidth = borderWidth;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
            PdfContentByte cv = canvases[PdfPTable.LINECANVAS];
            cv.saveState();
            cv.setLineWidth(borderWidth);
            cv.setColorStroke(borderColor);
            float x = pos.getLeft()   + borderWidth / 2f;
            float y = pos.getBottom() + borderWidth / 2f;
            float w = pos.getWidth()  - borderWidth;
            float h = pos.getHeight() - borderWidth;
            cv.moveTo(x + radius, y);
            cv.lineTo(x + w - radius, y);
            cv.curveTo(x + w, y, x + w, y, x + w, y + radius);
            cv.lineTo(x + w, y + h - radius);
            cv.curveTo(x + w, y + h, x + w, y + h, x + w - radius, y + h);
            cv.lineTo(x + radius, y + h);
            cv.curveTo(x, y + h, x, y + h, x, y + h - radius);
            cv.lineTo(x, y + radius);
            cv.curveTo(x, y, x, y, x + radius, y);
            cv.stroke();
            cv.restoreState();
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /** Load an image from classpath and return a borderless cell containing it. */
    protected PdfPCell createModernImageCell(String imagePath, int alignment, float height)
            throws IOException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(3);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            ClassPathResource res = new ClassPathResource(imagePath);
            if (res.exists()) {
                Image img = Image.getInstance(res.getURL());
                float scale = height / img.getHeight();
                img.scaleAbsolute(img.getWidth() * scale, height);
                cell.addElement(img);
            } else {
                addImagePlaceholder(cell, alignment);
            }
        } catch (Exception e) {
            log.warn("Could not load image '{}': {}", imagePath, e.getMessage());
            addImagePlaceholder(cell, alignment);
        }
        return cell;
    }

    protected void addImagePlaceholder(PdfPCell cell, int alignment) {
        cell.addElement(new Phrase("[IMG]",
                FontFactory.getFont(FontFactory.HELVETICA, 7, MUTED_CLR)));
    }

    protected void addModernSeparator(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(80);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(3);
        t.setSpacingAfter(3);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(1f);
        c.setBackgroundColor(BORDER_CLR);
        t.addCell(c);
        doc.add(t);
    }

    protected PdfPCell createModernCardCell(String title, Color titleColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(10);
        Paragraph p = new Paragraph(title,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, titleColor));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(8);
        cell.addElement(p);
        return cell;
    }

    protected void addStatCell(PdfPTable table, String label, String value, Color color) {
        PdfPCell lc = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        lc.setBackgroundColor(color); lc.setHorizontalAlignment(Element.ALIGN_CENTER);
        lc.setPadding(6); lc.setBorder(Rectangle.NO_BORDER);
        table.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, getContrastColor(color))));
        vc.setBackgroundColor(color); vc.setHorizontalAlignment(Element.ALIGN_CENTER);
        vc.setPadding(6); vc.setBorder(Rectangle.NO_BORDER);
        table.addCell(vc);
    }

    protected void addDistributionRow(PdfPTable t, String label, long count, int total, Color color) {
        PdfPCell lc = labelCell(label, color); t.addCell(lc);
        PdfPCell cc = labelCell(String.valueOf(count), color); t.addCell(cc);
        double pct = total > 0 ? (count * 100.0) / total : 0;
        PdfPCell pc = labelCell(String.format("%.1f%%", pct), color); t.addCell(pc);
    }

    private PdfPCell labelCell(String text, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(bg))));
        c.setBackgroundColor(bg); c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(4); c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    protected void styleModernCell(PdfPCell cell, Color bg, int alignment) {
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
    }

    // ── Colour utilities ──────────────────────────────────────────────────────

    protected Color getContrastColor(Color bg) {
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return lum > 0.5 ? Color.BLACK : Color.WHITE;
    }

    protected Color getPerformanceColor(Double avg) {
        if (avg == null) return MUTED_CLR;
        if (avg >= 18) return PASS_GREEN;
        if (avg >= 15) return NAVY;
        if (avg >= 10) return WARNING_COLOR;
        return FAIL_RED;
    }

    protected Color getScoreColor(Double score) {
        if (score == null) return new Color(240, 240, 240);
        if (score >= 18) return new Color(220, 255, 220);
        if (score >= 15) return new Color(220, 240, 255);
        if (score >= 10) return new Color(255, 255, 220);
        if (score >= 5)  return new Color(255, 240, 220);
        return new Color(255, 220, 220);
    }

    protected Color getGradeColor(String grade) {
        if (grade == null) return MUTED_CLR;
        return switch (grade.toUpperCase()) {
            case "A"  -> new Color(187, 247, 208);
            case "B"  -> new Color(209, 250, 229);
            case "C"  -> new Color(207, 226, 255);
            case "D"  -> new Color(254, 243, 199);
            case "E"  -> new Color(255, 237, 213);
            case "F","U" -> new Color(254, 226, 226);
            default   -> new Color(230, 230, 230);
        };
    }

    protected Color getGradeTextColor(String grade) {
        if (grade == null) return MUTED_CLR;
        return switch (grade.toUpperCase()) {
            case "A"  -> new Color(5,  90,  40);
            case "B"  -> new Color(10, 100, 55);
            case "C"  -> new Color(20,  60, 140);
            case "D"  -> new Color(120,  80,  0);
            case "E"  -> new Color(150,  70, 10);
            case "F","U" -> FAIL_RED;
            default   -> VALUE_CLR;
        };
    }

    protected Color getPassRateColor(Double rate) {
        if (rate == null) return MUTED_CLR;
        if (rate >= 70) return PASS_GREEN;
        if (rate >= 50) return WARNING_COLOR;
        return FAIL_RED;
    }

    protected String formatDecimal(Double v) {
        return v == null ? "—" : String.format("%.2f", v);
    }

    protected boolean isAdvancedLevelClass(String className) {
        if (className == null) return false;
        String lc = className.toLowerCase();
        return lc.contains("sixth") || lc.contains("upper") || lc.contains("lower")
                || lc.contains("advanced") || lc.contains("a level") || lc.contains("higher");
    }

    // ── Thin full-width rule ──────────────────────────────────────────────────
    protected PdfPTable goldRule(float heightPx) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(0); t.setSpacingAfter(0);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(heightPx);
        c.setBackgroundColor(GOLD);
        t.addCell(c);
        return t;
    }

    /** Borderless white cell — convenience. */
    protected PdfPCell blankCell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }
}