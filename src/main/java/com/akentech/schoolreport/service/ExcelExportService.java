package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.AssessmentType;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentEnrollmentService studentEnrollmentService;

    /**
     * Generate Excel file for a specific class with all students' assessment data
     */
    public byte[] generateClassAssessmentExcel(Long classRoomId, Integer term,
                                                Integer academicYearStart, Integer academicYearEnd) throws IOException {
        log.info("Generating Excel for class {} term {} academic year {}-{}", classRoomId, term, academicYearStart, academicYearEnd);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Class Assessments - Term " + term);

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle studentInfoStyle = createStudentInfoStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle lockedStyle = createLockedStyle(workbook);

            // Get students in the class
            List<Student> students = studentRepository.findAll().stream()
                    .filter(s -> s.getClassRoom() != null && s.getClassRoom().getId().equals(classRoomId))
                    .sorted(Comparator.comparing(Student::getRollNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            if (students.isEmpty()) {
                log.warn("No students found for class {}", classRoomId);
                throw new IllegalArgumentException("No students found in this class");
            }

            // Get all unique subjects for students in this class
            Set<Subject> allSubjects = new LinkedHashSet<>();
            for (Student student : students) {
                List<StudentSubject> enrollments = studentEnrollmentService.getStudentEnrollments(student.getId());
                enrollments.stream()
                        .map(StudentSubject::getSubject)
                        .forEach(allSubjects::add);
            }
            List<Subject> subjects = new ArrayList<>(allSubjects);
            subjects.sort(Comparator.comparing(Subject::getName));

            // Get assessment types for this term
            AssessmentType[] assessmentTypes = AssessmentType.getAssessmentsForTerm(term);

            // Build header row
            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            String academicYearLabel = (academicYearStart != null && academicYearEnd != null)
                    ? " - " + academicYearStart + "-" + academicYearEnd
                    : "";
            titleCell.setCellValue("CLASS ASSESSMENT SHEET - TERM " + term + academicYearLabel);
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6 + (subjects.size() * assessmentTypes.length) - 1));

            rowNum++; // Empty row

            // Column headers
            Row headerRow = sheet.createRow(rowNum++);
            int colNum = 0;

            // Student info columns (locked)
            headerRow.createCell(colNum++).setCellValue("Student ID");
            headerRow.createCell(colNum++).setCellValue("Roll Number");
            headerRow.createCell(colNum++).setCellValue("First Name");
            headerRow.createCell(colNum++).setCellValue("Last Name");
            headerRow.createCell(colNum++).setCellValue("Gender");
            headerRow.createCell(colNum++).setCellValue("Department");
            headerRow.createCell(colNum++).setCellValue("Specialty");

            // Apply header style to info columns
            for (int i = 0; i < colNum; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // Subject assessment columns (editable)
            for (Subject subject : subjects) {
                for (AssessmentType assessmentType : assessmentTypes) {
                    Cell cell = headerRow.createCell(colNum++);
                    cell.setCellValue(subject.getName() + " - " + assessmentType.getDisplayName());
                    cell.setCellStyle(headerStyle);
                }
            }

            // Populate student data
            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                colNum = 0;

                // Student info (locked cells)
                createLockedCell(row, colNum++, student.getStudentId(), lockedStyle);
                createLockedCell(row, colNum++, student.getRollNumber(), lockedStyle);
                createLockedCell(row, colNum++, student.getFirstName(), lockedStyle);
                createLockedCell(row, colNum++, student.getLastName(), lockedStyle);
                createLockedCell(row, colNum++, student.getGender() != null ? student.getGender().toString() : "", lockedStyle);
                createLockedCell(row, colNum++, student.getDepartment() != null ? student.getDepartment().getName() : "", lockedStyle);
                createLockedCell(row, colNum++, student.getSpecialty() != null ? student.getSpecialty() : "", lockedStyle);

                // Get student's enrolled subjects
                List<Subject> studentSubjects = studentEnrollmentService.getStudentEnrollments(student.getId())
                        .stream()
                        .map(StudentSubject::getSubject)
                        .collect(Collectors.toList());

                // Assessment scores (editable cells)
                for (Subject subject : subjects) {
                    // Check if student is enrolled in this subject
                    boolean isEnrolled = studentSubjects.stream()
                            .anyMatch(s -> s.getId().equals(subject.getId()));

                    for (AssessmentType assessmentType : assessmentTypes) {
                        Cell cell = row.createCell(colNum++);

                        if (isEnrolled) {
                            // Get existing assessment, filtered by academic year if provided
                            Optional<Assessment> assessment;
                            if (academicYearStart != null && academicYearEnd != null) {
                                assessment = assessmentRepository
                                        .findByStudentIdAndSubjectIdAndTermAndTypeAndAcademicYear(
                                                student.getId(), subject.getId(), term, assessmentType,
                                                academicYearStart, academicYearEnd);
                            } else {
                                assessment = assessmentRepository
                                        .findByStudentIdAndSubjectIdAndTermAndType(
                                                student.getId(), subject.getId(), term, assessmentType);
                            }

                            if (assessment.isPresent()) {
                                cell.setCellValue(assessment.get().getScore());
                            }
                            cell.setCellStyle(dataStyle);
                        } else {
                            // Not enrolled - mark as N/A and lock
                            cell.setCellValue("N/A");
                            cell.setCellStyle(lockedStyle);
                        }
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < 7 + (subjects.size() * assessmentTypes.length); i++) {
                sheet.autoSizeColumn(i);
            }

            // Add instructions sheet
            createInstructionsSheet(workbook, term, academicYearStart, academicYearEnd);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Generate Excel template for all students in a class (all terms)
     */
    public byte[] generateClassAssessmentExcelAllTerms(Long classRoomId,
                                                        Integer academicYearStart, Integer academicYearEnd) throws IOException {
        log.info("Generating Excel for class {} - all terms academic year {}-{}", classRoomId, academicYearStart, academicYearEnd);

        try (Workbook workbook = new XSSFWorkbook()) {

            // Create a sheet for each term
            for (int term = 1; term <= 3; term++) {
                createTermSheet(workbook, classRoomId, term, academicYearStart, academicYearEnd);
            }

            // Add instructions sheet
            createInstructionsSheet(workbook, null, academicYearStart, academicYearEnd);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createTermSheet(Workbook workbook, Long classRoomId, Integer term,
                                  Integer academicYearStart, Integer academicYearEnd) {
        Sheet sheet = workbook.createSheet("Term " + term);

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle lockedStyle = createLockedStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Get students
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getClassRoom() != null && s.getClassRoom().getId().equals(classRoomId))
                .sorted(Comparator.comparing(Student::getRollNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Get subjects
        Set<Subject> allSubjects = new LinkedHashSet<>();
        for (Student student : students) {
            List<StudentSubject> enrollments = studentEnrollmentService.getStudentEnrollments(student.getId());
            enrollments.stream()
                    .map(StudentSubject::getSubject)
                    .forEach(allSubjects::add);
        }
        List<Subject> subjects = new ArrayList<>(allSubjects);
        subjects.sort(Comparator.comparing(Subject::getName));

        // Get assessment types
        AssessmentType[] assessmentTypes = AssessmentType.getAssessmentsForTerm(term);

        // Build headers
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        String academicYearLabel = (academicYearStart != null && academicYearEnd != null)
                ? " - " + academicYearStart + "-" + academicYearEnd
                : "";
        titleCell.setCellValue("TERM " + term + " ASSESSMENTS" + academicYearLabel);
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6 + (subjects.size() * assessmentTypes.length) - 1));

        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int colNum = 0;

        // Student info headers
        String[] headers = {"Student ID", "Roll Number", "First Name", "Last Name", "Gender", "Department", "Specialty"};
        for (String header : headers) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }

        // Subject headers
        for (Subject subject : subjects) {
            for (AssessmentType assessmentType : assessmentTypes) {
                Cell cell = headerRow.createCell(colNum++);
                cell.setCellValue(subject.getName() + " - " + assessmentType.getDisplayName());
                cell.setCellStyle(headerStyle);
            }
        }

        // Populate data
        for (Student student : students) {
            Row row = sheet.createRow(rowNum++);
            colNum = 0;

            createLockedCell(row, colNum++, student.getStudentId(), lockedStyle);
            createLockedCell(row, colNum++, student.getRollNumber(), lockedStyle);
            createLockedCell(row, colNum++, student.getFirstName(), lockedStyle);
            createLockedCell(row, colNum++, student.getLastName(), lockedStyle);
            createLockedCell(row, colNum++, student.getGender() != null ? student.getGender().toString() : "", lockedStyle);
            createLockedCell(row, colNum++, student.getDepartment() != null ? student.getDepartment().getName() : "", lockedStyle);
            createLockedCell(row, colNum++, student.getSpecialty() != null ? student.getSpecialty() : "", lockedStyle);

            List<Subject> studentSubjects = studentEnrollmentService.getStudentEnrollments(student.getId())
                    .stream()
                    .map(StudentSubject::getSubject)
                    .collect(Collectors.toList());

            for (Subject subject : subjects) {
                boolean isEnrolled = studentSubjects.stream()
                        .anyMatch(s -> s.getId().equals(subject.getId()));

                for (AssessmentType assessmentType : assessmentTypes) {
                    Cell cell = row.createCell(colNum++);

                    if (isEnrolled) {
                        // Get existing assessment, filtered by academic year if provided
                        Optional<Assessment> assessment;
                        if (academicYearStart != null && academicYearEnd != null) {
                            assessment = assessmentRepository
                                    .findByStudentIdAndSubjectIdAndTermAndTypeAndAcademicYear(
                                            student.getId(), subject.getId(), term, assessmentType,
                                            academicYearStart, academicYearEnd);
                        } else {
                            assessment = assessmentRepository
                                    .findByStudentIdAndSubjectIdAndTermAndType(
                                            student.getId(), subject.getId(), term, assessmentType);
                        }

                        if (assessment.isPresent()) {
                            cell.setCellValue(assessment.get().getScore());
                        }
                        cell.setCellStyle(dataStyle);
                    } else {
                        cell.setCellValue("N/A");
                        cell.setCellStyle(lockedStyle);
                    }
                }
            }
        }

        // Auto-size
        for (int i = 0; i < 7 + (subjects.size() * assessmentTypes.length); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createInstructionsSheet(Workbook workbook, Integer term,
                                          Integer academicYearStart, Integer academicYearEnd) {
        Sheet sheet = workbook.createSheet("Instructions");
        CellStyle titleStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createDataStyle(workbook);

        int rowNum = 0;
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue("ASSESSMENT EXCEL SHEET - INSTRUCTIONS");
        cell.setCellStyle(titleStyle);

        rowNum++;

        String academicYearInfo = (academicYearStart != null && academicYearEnd != null)
                ? "ACADEMIC YEAR: " + academicYearStart + "-" + academicYearEnd
                : "ACADEMIC YEAR: Using student's current academic year";

        String[] instructions = {
                "1. DO NOT modify Student ID, Roll Number, Name, Gender, Department, or Specialty columns",
                "2. Only edit the assessment score columns (those with subject names)",
                "3. Scores must be between 0 and 20",
                "4. Leave cells empty if no score is available",
                "5. Cells marked 'N/A' indicate the student is not enrolled in that subject",
                "6. Save the file and upload it back to the system",
                "7. The system will validate all entries before importing",
                "",
                academicYearInfo,
                "",
                "TERM INFORMATION:",
                term != null ? "  - This sheet is for Term " + term + " only" : "  - This workbook contains sheets for all 3 terms",
                term != null && term == 1 ? "  - Term 1 has Assessment 1 and Assessment 2" : "",
                term != null && term == 2 ? "  - Term 2 has Assessment 3 and Assessment 4" : "",
                term != null && term == 3 ? "  - Term 3 has Assessment 5 only" : "",
                "",
                "For support, contact your system administrator."
        };

        for (String instruction : instructions) {
            if (!instruction.isEmpty()) {
                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue(instruction);
                cell.setCellStyle(textStyle);
            } else {
                rowNum++;
            }
        }

        sheet.setColumnWidth(0, 15000);
    }

    private void createLockedCell(Row row, int colNum, String value, CellStyle style) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createStudentInfoStyle(Workbook workbook) {
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

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setLocked(false);
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
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setLocked(true);
        return style;
    }
}
