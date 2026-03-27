package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.AssessmentType;
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
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AssessmentRepository assessmentRepository;
    private final StudentEnrollmentService studentEnrollmentService;

    public static class ImportResult {
        private int successCount;
        private int errorCount;
        private List<String> errors;
        private List<String> warnings;

        public ImportResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }

        public void addSuccess() {
            successCount++;
        }

        public void addError(String error) {
            errorCount++;
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Import completed: ")
                    .append(successCount).append(" assessments saved");
            if (errorCount > 0) {
                sb.append(", ").append(errorCount).append(" errors");
            }
            if (!warnings.isEmpty()) {
                sb.append(", ").append(warnings.size()).append(" warnings");
            }
            return sb.toString();
        }
    }

    @Transactional
    public ImportResult importAssessmentsFromExcel(MultipartFile file, Long classRoomId, Integer term,
                                                    Integer academicYearStart, Integer academicYearEnd) throws IOException {
        ImportResult result = new ImportResult();

        if (file.isEmpty()) {
            result.addError("File is empty");
            return result;
        }

        if (!isValidExcelFile(file)) {
            result.addError("Invalid file format. Please upload an Excel file (.xlsx)");
            return result;
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            // Determine which sheet to read
            Sheet sheet;
            if (term != null) {
                // Single term import
                sheet = workbook.getSheetAt(0);
                result = processSheet(sheet, term, academicYearStart, academicYearEnd, result);
            } else {
                // Multi-term import
                for (int t = 1; t <= 3; t++) {
                    sheet = workbook.getSheet("Term " + t);
                    if (sheet != null) {
                        result = processSheet(sheet, t, academicYearStart, academicYearEnd, result);
                    } else {
                        result.addWarning("Sheet 'Term " + t + "' not found, skipping");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error importing Excel file", e);
            result.addError("Error processing file: " + e.getMessage());
        }

        return result;
    }

    private ImportResult processSheet(Sheet sheet, Integer term,
                                       Integer academicYearStart, Integer academicYearEnd,
                                       ImportResult result) {
        log.info("Processing sheet: {} for term {} academic year {}-{}", sheet.getSheetName(), term, academicYearStart, academicYearEnd);

        // Find header row (should be row 2, index 2)
        Row headerRow = sheet.getRow(2);
        if (headerRow == null) {
            result.addError("Header row not found in sheet " + sheet.getSheetName());
            return result;
        }

        // Parse headers to determine column structure
        Map<Integer, String> subjectAssessmentMap = parseHeaders(headerRow);

        // Get assessment types for this term
        AssessmentType[] assessmentTypes = AssessmentType.getAssessmentsForTerm(term);

        // Process each data row (starting from row 3, index 3)
        int lastRowNum = sheet.getLastRowNum();
        for (int rowIndex = 3; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            try {
                processDataRow(row, subjectAssessmentMap, term, assessmentTypes, academicYearStart, academicYearEnd, result);
            } catch (Exception e) {
                result.addError("Row " + (rowIndex + 1) + ": " + e.getMessage());
            }
        }

        return result;
    }

    private Map<Integer, String> parseHeaders(Row headerRow) {
        Map<Integer, String> subjectAssessmentMap = new HashMap<>();

        for (int colIndex = 7; colIndex < headerRow.getLastCellNum(); colIndex++) {
            Cell cell = headerRow.getCell(colIndex);
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String header = cell.getStringCellValue().trim();
                if (!header.isEmpty() && !header.equals("N/A")) {
                    subjectAssessmentMap.put(colIndex, header);
                }
            }
        }

        return subjectAssessmentMap;
    }

    private void processDataRow(Row row, Map<Integer, String> subjectAssessmentMap,
                                 Integer term, AssessmentType[] assessmentTypes,
                                 Integer academicYearStart, Integer academicYearEnd,
                                 ImportResult result) {
        // Get student ID from column 0
        Cell studentIdCell = row.getCell(0);
        if (studentIdCell == null) {
            return; // Skip empty rows
        }

        String studentId = getCellValueAsString(studentIdCell);
        if (studentId == null || studentId.trim().isEmpty()) {
            return;
        }

        // Find student
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            result.addError("Student not found: " + studentId);
            return;
        }

        Student student = studentOpt.get();

        // Use provided academic year if available, otherwise use student's academic year
        Integer effectiveAcademicYearStart;
        Integer effectiveAcademicYearEnd;

        if (academicYearStart != null && academicYearEnd != null) {
            effectiveAcademicYearStart = academicYearStart;
            effectiveAcademicYearEnd = academicYearEnd;
        } else {
            // Validate academic year from student
            if (student.getAcademicYearStart() == null || student.getAcademicYearEnd() == null) {
                result.addError("Student " + studentId + " has no academic year set and no academic year provided");
                return;
            }
            effectiveAcademicYearStart = student.getAcademicYearStart();
            effectiveAcademicYearEnd = student.getAcademicYearEnd();
        }

        // Get student's enrolled subjects
        List<Subject> enrolledSubjects = studentEnrollmentService.getStudentEnrollments(student.getId())
                .stream()
                .map(StudentSubject::getSubject)
                .collect(Collectors.toList());

        // Process each assessment column
        for (Map.Entry<Integer, String> entry : subjectAssessmentMap.entrySet()) {
            int colIndex = entry.getKey();
            String header = entry.getValue();

            Cell cell = row.getCell(colIndex);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue; // Skip empty cells
            }

            // Check if cell contains "N/A"
            if (cell.getCellType() == CellType.STRING && "N/A".equals(cell.getStringCellValue())) {
                continue;
            }

            try {
                // Parse header: "SubjectName - Assessment X"
                String[] parts = header.split(" - ");
                if (parts.length != 2) {
                    result.addWarning("Invalid header format: " + header);
                    continue;
                }

                String subjectName = parts[0].trim();
                String assessmentName = parts[1].trim();

                // Find subject from student's enrolled subjects by NAME (not ID)
                // This matches the export logic which deduplicates by name
                Optional<Subject> subjectOpt = enrolledSubjects.stream()
                        .filter(s -> s.getName().equals(subjectName))
                        .findFirst();

                if (subjectOpt.isEmpty()) {
                    result.addWarning("Student " + studentId + " is not enrolled in " + subjectName);
                    continue;
                }

                Subject subject = subjectOpt.get();

                // Determine assessment type
                AssessmentType assessmentType = findAssessmentType(assessmentName, assessmentTypes);
                if (assessmentType == null) {
                    result.addWarning("Invalid assessment type: " + assessmentName);
                    continue;
                }

                // Validate that the assessment type matches the term
                if (!assessmentType.getTerm().equals(term)) {
                    result.addError("Assessment type mismatch for " + studentId + " - " + subjectName + ": " +
                            assessmentName + " belongs to Term " + assessmentType.getTerm() +
                            " but is in Term " + term + " sheet");
                    continue;
                }

                // Get score
                Double score = getCellValueAsDouble(cell);
                if (score == null) {
                    result.addWarning("Invalid score format for " + studentId + " - " + subjectName);
                    continue;
                }

                // Validate score range
                if (score < 0 || score > 20) {
                    result.addError("Invalid score " + score + " for " + studentId + " - " + subjectName +
                                    " (must be between 0 and 20)");
                    continue;
                }

                // Save or update assessment with the effective academic year
                saveAssessment(student, subject, term, assessmentType, score,
                              effectiveAcademicYearStart, effectiveAcademicYearEnd, result);

            } catch (Exception e) {
                result.addError("Error processing " + studentId + " - " + header + ": " + e.getMessage());
            }
        }
    }

    private AssessmentType findAssessmentType(String assessmentName, AssessmentType[] validTypes) {
        for (AssessmentType type : validTypes) {
            if (type.getDisplayName().equals(assessmentName)) {
                return type;
            }
        }
        return null;
    }

    private void saveAssessment(Student student, Subject subject, Integer term,
                                 AssessmentType assessmentType, Double score,
                                 Integer academicYearStart, Integer academicYearEnd,
                                 ImportResult result) {
        try {
            // Check if assessment already exists for this academic year
            Optional<Assessment> existingOpt = assessmentRepository
                    .findByStudentIdAndSubjectIdAndTermAndTypeAndAcademicYear(
                            student.getId(), subject.getId(), term, assessmentType,
                            academicYearStart, academicYearEnd);

            Assessment assessment;
            if (existingOpt.isPresent()) {
                // Update existing
                assessment = existingOpt.get();
                assessment.setScore(score);
                // Ensure academic year fields are up to date
                assessment.setAcademicYearStart(academicYearStart);
                assessment.setAcademicYearEnd(academicYearEnd);
                assessment.setAcademicYear(academicYearStart + "-" + academicYearEnd);
                log.debug("Updating assessment: {} {} {} - {} (Academic Year: {}-{})",
                        student.getStudentId(), subject.getName(), term, assessmentType.getDisplayName(),
                        academicYearStart, academicYearEnd);
            } else {
                // Create new
                assessment = Assessment.builder()
                        .student(student)
                        .subject(subject)
                        .term(term)
                        .type(assessmentType)
                        .score(score)
                        .academicYearStart(academicYearStart)
                        .academicYearEnd(academicYearEnd)
                        .academicYear(academicYearStart + "-" + academicYearEnd)
                        .build();
                log.debug("Creating new assessment: {} {} {} - {} (Academic Year: {}-{})",
                        student.getStudentId(), subject.getName(), term, assessmentType.getDisplayName(),
                        academicYearStart, academicYearEnd);
            }

            assessmentRepository.save(assessment);
            result.addSuccess();

        } catch (Exception e) {
            log.error("Error saving assessment", e);
            result.addError("Failed to save assessment for " + student.getStudentId() +
                    " - " + subject.getName() + ": " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> {
                    String value = cell.getStringCellValue().trim();
                    yield value.isEmpty() ? null : Double.parseDouble(value);
                }
                case FORMULA -> cell.getNumericCellValue();
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isValidExcelFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        return (contentType != null && contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                || (filename != null && filename.endsWith(".xlsx"));
    }
}
