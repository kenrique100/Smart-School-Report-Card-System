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

    // Modern color palette
    protected static final Color PRIMARY_COLOR = new Color(0, 102, 204);
    protected static final Color SECONDARY_COLOR = new Color(102, 0, 204);
    protected static final Color ACCENT_COLOR = new Color(0, 153, 153);
    protected static final Color SUCCESS_COLOR = new Color(0, 153, 76);
    protected static final Color WARNING_COLOR = new Color(255, 153, 0);
    protected static final Color DANGER_COLOR = new Color(204, 0, 0);
    protected static final Color INFO_COLOR = new Color(64, 64, 64);
    protected static final Color A_GRADE_COLOR = new Color(0, 102, 204);
    protected static final Color B_GRADE_COLOR = new Color(0, 153, 76);
    protected static final Color C_GRADE_COLOR = new Color(255, 204, 0);
    protected static final Color D_GRADE_COLOR = new Color(255, 102, 0);
    protected static final Color E_GRADE_COLOR = new Color(149, 165, 166);
    protected static final Color O_GRADE_COLOR = new Color(155, 89, 182);
    protected static final Color U_GRADE_COLOR = new Color(204, 0, 0);
    protected static final Color F_GRADE_COLOR = new Color(128, 0, 0);
    protected static final Color ROW_COLOR1 = new Color(255, 255, 255);
    protected static final Color ROW_COLOR2 = new Color(248, 249, 250);
    protected static final Color HEADER_COLOR = new Color(106, 13, 173);

    // Image paths
    protected static final String LEFT_LOGO_PATH = "static/images/cambridge-badge.png";
    protected static final String RIGHT_LOGO_PATH = "static/images/cameroon-flag.png";

    protected final GradeService gradeService;

    // Constructor with GradeService dependency
    protected BasePdfService(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    // Custom cell event for rounded borders
    protected static class RoundedBorderCellEvent implements PdfPCellEvent {
        private final float radius;
        private final Color borderColor;
        private final float borderWidth;

        public RoundedBorderCellEvent(float radius, Color borderColor, float borderWidth) {
            this.radius = radius;
            this.borderColor = borderColor;
            this.borderWidth = borderWidth;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            canvas.saveState();
            canvas.setLineWidth(borderWidth);
            canvas.setColorStroke(borderColor);

            float x = position.getLeft() + borderWidth/2;
            float y = position.getBottom() + borderWidth/2;
            float width = position.getWidth() - borderWidth;
            float height = position.getHeight() - borderWidth;

            canvas.moveTo(x + radius, y);
            canvas.lineTo(x + width - radius, y);
            canvas.curveTo(x + width, y, x + width, y, x + width, y + radius);
            canvas.lineTo(x + width, y + height - radius);
            canvas.curveTo(x + width, y + height, x + width, y + height, x + width - radius, y + height);
            canvas.lineTo(x + radius, y + height);
            canvas.curveTo(x, y + height, x, y + height, x, y + height - radius);
            canvas.lineTo(x, y + radius);
            canvas.curveTo(x, y, x, y, x + radius, y);
            canvas.stroke();
            canvas.restoreState();
        }
    }

    // Common Helper Methods
    protected PdfPCell createModernImageCell(String imagePath, int alignment, float height) throws IOException {
        PdfPCell imageCell = new PdfPCell();
        imageCell.setBorder(Rectangle.NO_BORDER);
        imageCell.setBackgroundColor(Color.WHITE);
        imageCell.setPadding(3);
        imageCell.setHorizontalAlignment(alignment);
        imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            ClassPathResource resource = new ClassPathResource(imagePath);
            if (resource.exists()) {
                Image image = Image.getInstance(resource.getURL());
                float scale = height / image.getHeight();
                image.scaleAbsolute(image.getWidth() * scale, height);
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

    protected void addImagePlaceholder(PdfPCell cell, int alignment) {
        Phrase placeholder = new Phrase("[LOGO]",
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(200, 200, 200)));
        cell.addElement(placeholder);
    }

    protected void addModernSeparator(Document document) throws DocumentException {
        PdfPTable separatorTable = new PdfPTable(1);
        separatorTable.setWidthPercentage(80);
        separatorTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        separatorTable.setSpacingBefore(3);
        separatorTable.setSpacingAfter(3);

        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(Rectangle.NO_BORDER);
        separatorCell.setFixedHeight(1);
        separatorCell.setBackgroundColor(new Color(230, 230, 230));
        separatorTable.addCell(separatorCell);
        document.add(separatorTable);
    }

    protected PdfPCell createModernCardCell(String title, Color titleColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(10);
        cell.setCellEvent(new RoundedBorderCellEvent(8, new Color(200, 200, 200), 0.5f));

        Paragraph titleParagraph = new Paragraph(title,
                FontFactory.getFont("Helvetica-Bold", 10, titleColor));
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10);
        cell.addElement(titleParagraph);
        return cell;
    }

    protected void addStatCell(PdfPTable table, String label, String value, Color color) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        labelCell.setBackgroundColor(color);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        labelCell.setPadding(6);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setCellEvent(new RoundedBorderCellEvent(5, color, 0));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, getContrastColor(color))));
        valueCell.setBackgroundColor(color);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setPadding(6);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setCellEvent(new RoundedBorderCellEvent(5, color, 0));
        table.addCell(valueCell);
    }

    protected void addDistributionRow(PdfPTable table, String label, long count, int total, Color color) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, getContrastColor(color))));
        labelCell.setBackgroundColor(color);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        labelCell.setPadding(4);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell countCell = new PdfPCell(new Phrase(String.valueOf(count),
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(color))));
        countCell.setBackgroundColor(color);
        countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        countCell.setPadding(4);
        countCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(countCell);

        double percentage = total > 0 ? (count * 100.0) / total : 0;
        PdfPCell percentCell = new PdfPCell(new Phrase(String.format("%.1f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA, 8, getContrastColor(color))));
        percentCell.setBackgroundColor(color);
        percentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        percentCell.setPadding(4);
        percentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(percentCell);
    }

    protected void styleModernCell(PdfPCell cell, Color backgroundColor, int alignment) {
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new RoundedBorderCellEvent(3, new Color(230, 230, 230), 0.3f));
    }

    protected Color getContrastColor(Color backgroundColor) {
        double luminance = (0.299 * backgroundColor.getRed() +
                0.587 * backgroundColor.getGreen() +
                0.114 * backgroundColor.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    protected Color getPerformanceColor(Double average) {
        if (average == null) return new Color(150, 150, 150);
        if (average >= 18) return SUCCESS_COLOR;
        if (average >= 15) return PRIMARY_COLOR;
        if (average >= 10) return WARNING_COLOR;
        return DANGER_COLOR;
    }

    protected Color getScoreColor(Double score) {
        if (score == null) return new Color(240, 240, 240);
        if (score >= 18) return new Color(220, 255, 220);
        if (score >= 15) return new Color(220, 240, 255);
        if (score >= 10) return new Color(255, 255, 220);
        if (score >= 5) return new Color(255, 240, 220);
        return new Color(255, 220, 220);
    }

    protected Color getGradeColor(String grade) {
        if (grade == null) return INFO_COLOR;
        return switch (grade.toUpperCase()) {
            case "A" -> A_GRADE_COLOR;
            case "B" -> B_GRADE_COLOR;
            case "C" -> C_GRADE_COLOR;
            case "D" -> D_GRADE_COLOR;
            case "E" -> E_GRADE_COLOR;
            case "O" -> O_GRADE_COLOR;
            case "U" -> U_GRADE_COLOR;
            case "F" -> F_GRADE_COLOR;
            default -> INFO_COLOR;
        };
    }

    protected Color getPassRateColor(Double passRate) {
        if (passRate == null) return INFO_COLOR;
        if (passRate >= 70) return SUCCESS_COLOR;
        if (passRate >= 50) return WARNING_COLOR;
        return DANGER_COLOR;
    }

    protected String formatDecimal(Double value) {
        if (value == null) return "-";
        return String.format("%.2f", value);
    }

    protected boolean isAdvancedLevelClass(String className) {
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
}