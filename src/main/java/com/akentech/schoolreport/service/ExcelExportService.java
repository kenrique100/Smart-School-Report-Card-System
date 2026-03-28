package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.AssessmentType;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final AssessmentRepository assessmentRepository;

    /**
     * Export assessments for a single term
     */
    public byte[] exportAssessmentTemplate(Long classRoomId, Integer term) throws IOException {
        ClassRoom classRoom = classRoomRepository.findById(classRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found with id: " + classRoomId));

        Workbook workbook = new XSSFWorkbook();

        // Create the term sheet
        createTermSheet(workbook, classRoom, term);

        // Create instructions sheet
        createInstructionsSheet(workbook);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Export assessments for all terms
     */
    public byte[] exportAssessmentTemplateAllTerms(Long classRoomId) throws IOException {
        ClassRoom classRoom = classRoomRepository.findById(classRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found with id: " + classRoomId));

        Workbook workbook = new XSSFWorkbook();

        // Create sheets for all 3 terms
        for (int term = 1; term <= 3; term++) {
            createTermSheet(workbook, classRoom, term);
        }

        // Create instructions sheet
        createInstructionsSheet(workbook);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private void createTermSheet(Workbook workbook, ClassRoom classRoom, Integer term) {
        Sheet sheet = workbook.createSheet("Term " + term);

        // Fetch all students in the classroom
        List<Student> students = classRoom.getStudents();
        if (students.isEmpty()) {
            log.warn("No students found in classroom: {}", classRoom.getName());
        }

        // Fetch all subjects
        List<Subject> allSubjects = subjectRepository.findAll();

        // Fetch existing assessments for this class and term
        List<Assessment> existingAssessments = assessmentRepository.findByClassIdAndTerm(classRoom.getId(), term);
        Map<String, Map<String, Map<String, Double>>> assessmentMap = buildAssessmentMap(existingAssessments);

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle lockedStyle = createLockedStyle(workbook);
        CellStyle unlockedStyle = createUnlockedStyle(workbook);

        // Create header row
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;

        // Student info columns (locked)
        createHeaderCell(headerRow, colIndex++, "Student ID", headerStyle);
        createHeaderCell(headerRow, colIndex++, "First Name", headerStyle);
        createHeaderCell(headerRow, colIndex++, "Last Name", headerStyle);

        // Subject columns (for each assessment type in this term)
        AssessmentType[] termAssessmentTypes = AssessmentType.getAssessmentsForTerm(term);
        List<String> subjectHeaders = new ArrayList<>();
        for (Subject subject : allSubjects) {
            for (int i = 0; i < termAssessmentTypes.length; i++) {
                subjectHeaders.add(subject.getName() + "-A" + (i + 1));
            }
        }

        for (String header : subjectHeaders) {
            createHeaderCell(headerRow, colIndex++, header, headerStyle);
        }

        // Populate student rows
        int rowIndex = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowIndex++);
            colIndex = 0;

            // Student info (locked)
            createLockedCell(row, colIndex++, student.getStudentId(), lockedStyle);
            createLockedCell(row, colIndex++, student.getFirstName(), lockedStyle);
            createLockedCell(row, colIndex++, student.getLastName(), lockedStyle);

            // Get student's enrolled subjects
            List<Long> enrolledSubjectIds = student.getSelectedSubjectIds();

            // Assessment scores
            for (Subject subject : allSubjects) {
                boolean isEnrolled = enrolledSubjectIds.contains(subject.getId());

                for (int i = 0; i < termAssessmentTypes.length; i++) {
                    String assessmentKey = termAssessmentTypes[i].name();
                    if (!isEnrolled) {
                        // Not enrolled - mark as N/A and lock
                        createLockedCell(row, colIndex++, "N/A", lockedStyle);
                    } else {
                        // Enrolled - get existing score or leave blank for entry
                        Double existingScore = getExistingScore(assessmentMap, student.getStudentId(),
                                                               subject.getName(), assessmentKey);
                        if (existingScore != null) {
                            createUnlockedCell(row, colIndex++, existingScore.toString(), unlockedStyle);
                        } else {
                            createUnlockedCell(row, colIndex++, "", unlockedStyle);
                        }
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < colIndex; i++) {
            sheet.autoSizeColumn(i);
        }

        // Protect the sheet but allow unlocked cells to be edited
        sheet.protectSheet("reportcard");
    }

    private void createInstructionsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Instructions");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(true);

        int rowIndex = 0;
        Row row;

        // Title
        row = sheet.createRow(rowIndex++);
        Cell titleCell = row.createCell(0);
        titleCell.setCellValue("Assessment Excel Import - Instructions");
        titleCell.setCellStyle(titleStyle);

        rowIndex++; // Empty row

        // Instructions
        String[] instructions = {
            "1. STUDENT INFORMATION",
            "   - Student ID, First Name, and Last Name columns are LOCKED and cannot be edited.",
            "   - These columns identify each student uniquely.",
            "",
            "2. ASSESSMENT COLUMNS",
            "   - Each subject has three assessment columns: Subject-A1, Subject-A2, Subject-Exam",
            "   - A1 = Assessment 1, A2 = Assessment 2, Exam = Final Exam",
            "",
            "3. SCORE ENTRY RULES",
            "   - All scores must be between 0 and 20 (inclusive)",
            "   - Use decimal values if needed (e.g., 15.5, 18.75)",
            "   - Leave cells blank if no score is available yet",
            "   - Cells marked 'N/A' indicate the student is not enrolled in that subject",
            "",
            "4. VALIDATION",
            "   - Student ID must exist in the system",
            "   - Student must be enrolled in the subject to receive a score",
            "   - Scores outside 0-20 range will be rejected",
            "   - Assessment type must be valid for the term",
            "",
            "5. UPLOADING",
            "   - Save this file after entering scores",
            "   - Upload through the 'Upload Excel' button on the assessments page",
            "   - Review the import results for any errors or warnings",
            "",
            "6. TIPS",
            "   - Do NOT modify the structure of this file (add/remove columns or rows)",
            "   - Do NOT edit locked cells",
            "   - Check for error messages after upload and correct any issues",
            "   - You can re-upload the file after making corrections"
        };

        for (String instruction : instructions) {
            row = sheet.createRow(rowIndex++);
            Cell cell = row.createCell(0);
            cell.setCellValue(instruction);
            cell.setCellStyle(normalStyle);
        }

        // Auto-size the column
        sheet.setColumnWidth(0, 20000);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setLocked(true);
        return style;
    }

    private CellStyle createLockedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setLocked(true);
        return style;
    }

    private CellStyle createUnlockedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setLocked(false);
        return style;
    }

    private void createHeaderCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createLockedCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createUnlockedCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private Map<String, Map<String, Map<String, Double>>> buildAssessmentMap(List<Assessment> assessments) {
        Map<String, Map<String, Map<String, Double>>> map = new HashMap<>();

        for (Assessment assessment : assessments) {
            String studentId = assessment.getStudent().getStudentId();
            String subjectName = assessment.getSubject().getName();
            String assessmentType = assessment.getType().name();
            Double score = assessment.getScore();

            map.putIfAbsent(studentId, new HashMap<>());
            map.get(studentId).putIfAbsent(subjectName, new HashMap<>());
            map.get(studentId).get(subjectName).put(assessmentType, score);
        }

        return map;
    }

    private Double getExistingScore(Map<String, Map<String, Map<String, Double>>> assessmentMap,
                                   String studentId, String subjectName, String assessmentType) {
        if (assessmentMap.containsKey(studentId)) {
            Map<String, Map<String, Double>> studentMap = assessmentMap.get(studentId);
            if (studentMap.containsKey(subjectName)) {
                return studentMap.get(subjectName).get(assessmentType);
            }
        }
        return null;
    }

    public String generateFileName(ClassRoom classRoom, Integer term) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (term != null) {
            return String.format("Assessments_%s_Term%d_%s.xlsx",
                               classRoom.getName().replaceAll("\\s+", "_"), term, timestamp);
        } else {
            return String.format("Assessments_%s_AllTerms_%s.xlsx",
                               classRoom.getName().replaceAll("\\s+", "_"), timestamp);
        }
    }
}
