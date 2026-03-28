package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ImportResult;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional
    public ImportResult importAssessments(MultipartFile file) {
        ImportResult result = ImportResult.builder().build();

        try {
            // Validate file
            if (file.isEmpty()) {
                result.addError("File is empty");
                return result;
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                result.addError("Invalid file format. Only .xlsx files are supported");
                return result;
            }

            Workbook workbook = new XSSFWorkbook(file.getInputStream());

            // Process each sheet (each sheet represents a term)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();

                // Skip instructions sheet
                if (sheetName.equalsIgnoreCase("Instructions")) {
                    continue;
                }

                // Extract term number from sheet name (e.g., "Term 1" -> 1)
                Integer term = extractTermFromSheetName(sheetName);
                if (term == null) {
                    result.addWarning("Skipping sheet '" + sheetName + "': Unable to determine term number");
                    continue;
                }

                // Process the sheet
                processSheet(sheet, term, result);
            }

            workbook.close();

        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            result.addError("Error reading Excel file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during import", e);
            result.addError("Unexpected error: " + e.getMessage());
        }

        return result;
    }

    private void processSheet(Sheet sheet, Integer term, ImportResult result) {
        // Read header row to get subject-assessment mapping
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            result.addError("Sheet '" + sheet.getSheetName() + "': Header row is missing");
            return;
        }

        List<SubjectAssessmentHeader> headers = parseHeaders(headerRow);

        // Process each data row
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            try {
                processRow(row, rowIndex, headers, term, result);
            } catch (Exception e) {
                result.addError("Row " + (rowIndex + 1) + ": " + e.getMessage());
            }
        }
    }

    private void processRow(Row row, int rowIndex, List<SubjectAssessmentHeader> headers,
                           Integer term, ImportResult result) {
        // Read student info from first 3 columns
        String studentId = getCellValueAsString(row.getCell(0));
        String firstName = getCellValueAsString(row.getCell(1));
        String lastName = getCellValueAsString(row.getCell(2));

        if (studentId == null || studentId.trim().isEmpty()) {
            result.addWarning("Row " + (rowIndex + 1) + ": Skipping row with empty Student ID");
            return;
        }

        // Find or validate student
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            result.addError("Row " + (rowIndex + 1) + ": Student with ID '" + studentId + "' not found in system");
            return;
        }

        Student student = studentOpt.get();

        // Validate student name matches
        if (!student.getFirstName().equalsIgnoreCase(firstName) ||
            !student.getLastName().equalsIgnoreCase(lastName)) {
            result.addWarning("Row " + (rowIndex + 1) + ": Student name mismatch. " +
                            "Expected: " + student.getFullName() + ", Found: " + firstName + " " + lastName);
        }

        // Get student's enrolled subjects
        List<Long> enrolledSubjectIds = student.getSelectedSubjectIds();

        // Process each assessment column
        for (int colIndex = 3; colIndex < headers.size() + 3; colIndex++) {
            SubjectAssessmentHeader header = headers.get(colIndex - 3);
            Cell cell = row.getCell(colIndex);
            String cellValue = getCellValueAsString(cell);

            // Skip empty or N/A cells
            if (cellValue == null || cellValue.trim().isEmpty() || cellValue.equalsIgnoreCase("N/A")) {
                continue;
            }

            // Find subject by name
            List<Subject> subjects = subjectRepository.findByName(header.subjectName);
            if (subjects.isEmpty()) {
                result.addError("Row " + (rowIndex + 1) + ", Column " + getColumnLetter(colIndex) +
                              ": Subject '" + header.subjectName + "' not found in system");
                continue;
            }

            Subject subject = subjects.get(0);

            // Check if student is enrolled in the subject
            if (!enrolledSubjectIds.contains(subject.getId())) {
                result.addError("Row " + (rowIndex + 1) + ", Column " + getColumnLetter(colIndex) +
                              ": Student '" + studentId + "' is not enrolled in subject '" + header.subjectName + "'");
                continue;
            }

            // Parse and validate score
            Double score;
            try {
                score = Double.parseDouble(cellValue);
            } catch (NumberFormatException e) {
                result.addError("Row " + (rowIndex + 1) + ", Column " + getColumnLetter(colIndex) +
                              ": Invalid score '" + cellValue + "'. Must be a number");
                continue;
            }

            if (score < 0 || score > 20) {
                result.addError("Row " + (rowIndex + 1) + ", Column " + getColumnLetter(colIndex) +
                              ": Score " + score + " is out of valid range (0-20)");
                continue;
            }

            // Save or update assessment
            try {
                saveOrUpdateAssessment(student, subject, term, header.assessmentType, score, result);
            } catch (Exception e) {
                result.addError("Row " + (rowIndex + 1) + ", Column " + getColumnLetter(colIndex) +
                              ": Failed to save assessment - " + e.getMessage());
            }
        }
    }

    private void saveOrUpdateAssessment(Student student, Subject subject, Integer term,
                                       String assessmentType, Double score, ImportResult result) {
        // Check if assessment already exists
        Optional<Assessment> existingAssessment = assessmentRepository
                .findByStudentAndSubjectAndTermAndType(student, subject, term, assessmentType);

        if (existingAssessment.isPresent()) {
            // Update existing assessment
            Assessment assessment = existingAssessment.get();
            Double oldScore = assessment.getScore();
            assessment.setScore(score);
            assessmentRepository.save(assessment);

            result.addSuccess("Updated: " + student.getStudentId() + " - " + subject.getName() +
                            " - " + assessmentType + " (Term " + term + "): " + oldScore + " → " + score);
        } else {
            // Create new assessment
            Assessment assessment = Assessment.builder()
                    .student(student)
                    .subject(subject)
                    .term(term)
                    .type(assessmentType)
                    .score(score)
                    .build();
            assessmentRepository.save(assessment);

            result.addSuccess("Created: " + student.getStudentId() + " - " + subject.getName() +
                            " - " + assessmentType + " (Term " + term + "): " + score);
        }
    }

    private List<SubjectAssessmentHeader> parseHeaders(Row headerRow) {
        List<SubjectAssessmentHeader> headers = new ArrayList<>();

        // Skip first 3 columns (Student ID, First Name, Last Name)
        for (int colIndex = 3; colIndex <= headerRow.getLastCellNum(); colIndex++) {
            Cell cell = headerRow.getCell(colIndex);
            if (cell == null) {
                break;
            }

            String headerText = getCellValueAsString(cell);
            if (headerText == null || headerText.trim().isEmpty()) {
                break;
            }

            // Parse header format: "SubjectName-A1", "SubjectName-A2", "SubjectName-Exam"
            String[] parts = headerText.split("-");
            if (parts.length == 2) {
                String subjectName = parts[0].trim();
                String assessmentCode = parts[1].trim();

                String assessmentType = switch (assessmentCode) {
                    case "A1" -> "Assessment1";
                    case "A2" -> "Assessment2";
                    case "Exam" -> "Exam";
                    default -> null;
                };

                if (assessmentType != null) {
                    headers.add(new SubjectAssessmentHeader(subjectName, assessmentType));
                }
            }
        }

        return headers;
    }

    private Integer extractTermFromSheetName(String sheetName) {
        // Extract term number from sheet name like "Term 1", "Term 2", "Term 3"
        String[] parts = sheetName.split("\\s+");
        for (String part : parts) {
            try {
                int term = Integer.parseInt(part);
                if (term >= 1 && term <= 3) {
                    return term;
                }
            } catch (NumberFormatException e) {
                // Continue searching
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Check if it's a whole number
                    if (numericValue == (long) numericValue) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> null;
            default -> null;
        };
    }

    private String getColumnLetter(int columnIndex) {
        StringBuilder columnLetter = new StringBuilder();
        while (columnIndex >= 0) {
            columnLetter.insert(0, (char) ('A' + (columnIndex % 26)));
            columnIndex = (columnIndex / 26) - 1;
        }
        return columnLetter.toString();
    }

    private static class SubjectAssessmentHeader {
        String subjectName;
        String assessmentType;

        SubjectAssessmentHeader(String subjectName, String assessmentType) {
            this.subjectName = subjectName;
            this.assessmentType = assessmentType;
        }
    }
}
