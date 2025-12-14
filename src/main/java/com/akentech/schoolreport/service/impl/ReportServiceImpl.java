package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.dto.*;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.service.GradeService;
import com.akentech.schoolreport.service.ReportMapper;
import com.akentech.schoolreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = {"reports"})
public class ReportServiceImpl implements ReportService {

    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final GradeService gradeService;
    private final ReportMapper reportMapper;
    private final ApplicationContext applicationContext;

    // ========== TERM REPORTS ==========

    @Override
    @Deprecated
    public ReportDTO generateReportForStudent(Long studentId, Integer term) {
        log.warn("DEPRECATED: Using deprecated method generateReportForStudent with ID. Use class-based method instead.");
        log.info("Generating term {} report for student ID: {}", term, studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Load relationships if needed
        loadStudentRelationships(student);

        // Get all assessments for the student in the given term
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Assessment> assessments = self.getCachedStudentTermAssessmentsInternal(studentId, term);

        // Calculate subject reports
        List<SubjectReport> subjectReports = calculateSubjectReports(student, assessments, term);

        // Calculate term statistics
        Map<String, Object> statistics = calculateTermStatistics(subjectReports, student, term);

        // Generate ReportDTO with all required fields
        ReportDTO reportDTO = reportMapper.toReportDTO(student, subjectReports, term, statistics);

        // Add action recommendation
        reportDTO.setAction(generateStudentAction(reportDTO));

        return reportDTO;
    }

    @Override
    public ReportDTO generateReportForStudentByClassAndRollNumber(Long classId, String rollNumber, Integer term) {
        log.info("Generating term {} report for student with roll {} in class ID: {}", term, rollNumber, classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        Student student = studentRepository.findByRollNumberAndClassRoom(rollNumber, classRoom)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Student with roll number " + rollNumber + " in class " + classRoom.getName()));

        return generateReportForStudent(student.getId(), term);
    }

    @Override
    public ReportDTO getTermReportForStudentAndYear(Long studentId, Integer term, String academicYear) {
        log.info("Generating term {} report for student ID: {} for academic year: {}", term, studentId, academicYear);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Load relationships if needed
        loadStudentRelationships(student);

        // FIXED: Use the REQUESTED academic year parameter, not student's academic year
        // Parse the REQUESTED academic year
        int[] years = parseAcademicYear(academicYear);
        int requestedYearStart = years[0];
        int requestedYearEnd = years[1];

        String effectiveAcademicYear = academicYear; // Use the requested year

        log.info("Using REQUESTED academic year: {}", effectiveAcademicYear);

        // Get assessments for the student in the term and REQUESTED academic year
        List<Assessment> assessments = assessmentRepository.findByStudentIdAndTermAndAcademicYear(
                studentId, term, requestedYearStart, requestedYearEnd);

        log.debug("Found {} assessments for student {} term {} REQUESTED academic year {}-{}",
                assessments.size(), studentId, term, requestedYearStart, requestedYearEnd);

        if (assessments.isEmpty()) {
            log.warn("No assessments found for student {} term {} REQUESTED academic year {}",
                    studentId, term, effectiveAcademicYear);
            // Try without academic year filter as fallback
            assessments = assessmentRepository.findByStudentIdAndTerm(studentId, term);
            log.debug("Found {} assessments without academic year filter", assessments.size());
        }

        // Calculate subject reports
        List<SubjectReport> subjectReports = calculateSubjectReports(student, assessments, term);

        // Calculate term statistics
        Map<String, Object> statistics = calculateTermStatistics(subjectReports, student, term);

        // Generate ReportDTO
        ReportDTO reportDTO = reportMapper.toReportDTO(student, subjectReports, term, statistics);
        reportDTO.setAcademicYear(effectiveAcademicYear); // Set to REQUESTED year

        // Ensure term average is set correctly
        if (reportDTO.getTermAverage() == null || reportDTO.getTermAverage() == 0.0) {
            log.debug("Term average is null or 0.0, recalculating from subject reports...");
            Double recalculatedAverage = gradeService.calculateWeightedTermAverage(subjectReports);
            reportDTO.setTermAverage(recalculatedAverage);
            reportDTO.setFormattedAverage(String.format("%.2f/20", recalculatedAverage));
        }

        reportDTO.setAction(generateStudentAction(reportDTO));

        log.info("Successfully generated term report for student {} term {}: Average={}, Subjects={}, AcademicYear={}",
                studentId, term, reportDTO.getTermAverage(), subjectReports.size(), reportDTO.getAcademicYear());

        return reportDTO;
    }

    @Override
    public List<ReportDTO> generateReportsForClass(Long classId, Integer term) {
        log.info("Generating term {} reports for class ID: {}", term, classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get all students for the class
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);
        List<ReportDTO> reports = new ArrayList<>();

        for (Student student : students) {
            try {
                ReportDTO report = generateReportForStudent(student.getId(), term);
                reports.add(report);
            } catch (Exception e) {
                log.error("Error generating report for student {}: {}", student.getId(), e.getMessage());
                reports.add(createEmptyReport(student, term, getCurrentAcademicYear()));
            }
        }

        // Sort by rank
        reports.sort(Comparator.comparing(ReportDTO::getRankInClass));

        return reports;
    }

    @Override
    public List<ReportDTO> getTermReportsForClassAndYear(Long classId, Integer term, String academicYear) {
        log.info("Generating term {} reports for class ID: {} for academic year: {}",
                term, classId, academicYear);

        // Parse the REQUESTED academic year
        int[] years = parseAcademicYear(academicYear);
        int requestedYearStart = years[0];
        int requestedYearEnd = years[1];

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get students for the REQUESTED academic year
        List<Student> students = studentRepository.findByClassRoomIdAndAcademicYear(
                classId, requestedYearStart, requestedYearEnd);

        // If no students found for that year, try without year filter
        if (students.isEmpty()) {
            log.warn("No students found for class {} in academic year {}, fetching all students",
                    classId, academicYear);
            students = studentRepository.findByClassRoomId(classId);
        }

        log.info("Found {} students in class {} for academic year {}",
                students.size(), classRoom.getName(), academicYear);

        // Batch fetch all assessments for these students in one query WITH academic year filter
        Map<Long, List<Assessment>> assessmentsByStudent = new HashMap<>();
        if (!students.isEmpty()) {
            List<Long> studentIds = students.stream()
                    .map(Student::getId)
                    .collect(Collectors.toList());

            // FIXED: Fetch assessments with academic year filter
            List<Assessment> allAssessments = assessmentRepository.findByStudentIdInAndTermAndAcademicYear(
                    studentIds, term, requestedYearStart, requestedYearEnd);

            assessmentsByStudent = allAssessments.stream()
                    .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

            log.debug("Batch fetched {} assessments for {} students for academic year {}-{}",
                    allAssessments.size(), studentIds.size(), requestedYearStart, requestedYearEnd);
        }

        List<ReportDTO> reports = new ArrayList<>();

        for (Student student : students) {
            try {
                // Get assessments for this student
                List<Assessment> studentAssessments = assessmentsByStudent.getOrDefault(
                        student.getId(), new ArrayList<>());

                log.debug("Student {}: {} assessments for REQUESTED academic year {}",
                        student.getFullName(), studentAssessments.size(), academicYear);

                // Load student relationships
                loadStudentRelationships(student);

                // Calculate subject reports
                List<SubjectReport> subjectReports = calculateSubjectReports(student, studentAssessments, term);

                // Calculate term statistics
                Map<String, Object> statistics = calculateTermStatistics(subjectReports, student, term);

                // Generate ReportDTO
                ReportDTO report = reportMapper.toReportDTO(student, subjectReports, term, statistics);
                report.setAcademicYear(academicYear); // Set to REQUESTED year

                // Ensure term average is set
                if (report.getTermAverage() == null || report.getTermAverage() == 0.0) {
                    Double recalculatedAverage = gradeService.calculateWeightedTermAverage(subjectReports);
                    report.setTermAverage(recalculatedAverage);
                    report.setFormattedAverage(String.format("%.2f/20", recalculatedAverage));
                }

                report.setAction(generateStudentAction(report));
                reports.add(report);

            } catch (Exception e) {
                log.error("Error generating report for student {}: {}", student.getId(), e.getMessage());
                reports.add(createEmptyReport(student, term, academicYear)); // Use REQUESTED year
            }
        }

        // Calculate ranks after all reports are generated
        calculateRanksForReports(reports);

        // Sort by rank
        reports.sort(Comparator.comparing(ReportDTO::getRankInClass));

        return reports;
    }

    @Override
    public Page<ReportDTO> generatePaginatedReportsForClass(Long classId, Integer term, Pageable pageable) {
        log.info("Generating paginated term {} reports for class ID: {}", term, classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get all students for the class
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);

        // Create page of students
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), students.size());

        if (start > students.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, students.size());
        }

        List<Student> pageStudents = students.subList(start, end);

        // Generate reports for this page
        List<ReportDTO> reports = new ArrayList<>();
        for (Student student : pageStudents) {
            try {
                ReportDTO report = generateReportForStudent(student.getId(), term);
                reports.add(report);
            } catch (Exception e) {
                log.error("Error generating report for student {}: {}", student.getId(), e.getMessage());
                reports.add(createEmptyReport(student, term, getCurrentAcademicYear()));
            }
        }

        // Sort by the requested sort field
        sortReports(reports, pageable.getSort());

        return new PageImpl<>(reports, pageable, students.size());
    }

    @Override
    public Page<ReportDTO> getPaginatedTermReportsForClassAndYear(Long classId, Integer term,
                                                                  String academicYear, Pageable pageable) {
        log.info("Generating paginated term {} reports for class ID: {} for academic year: {}",
                term, classId, academicYear);

        // Get all reports
        List<ReportDTO> allReports = getTermReportsForClassAndYear(classId, term, academicYear);

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allReports.size());

        if (start > allReports.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allReports.size());
        }

        List<ReportDTO> pageReports = allReports.subList(start, end);

        // Sort by the requested sort field
        sortReports(pageReports, pageable.getSort());

        return new PageImpl<>(pageReports, pageable, allReports.size());
    }

    @Override
    @Async
    public CompletableFuture<List<ReportDTO>> generateReportsForClassAsync(Long classId, Integer term) {
        log.info("Starting async generation of term {} reports for class ID: {}", term, classId);
        return CompletableFuture.supplyAsync(() -> generateReportsForClass(classId, term));
    }

    // ========== YEARLY REPORTS ==========

    @Override
    @Deprecated
    public YearlyReportDTO generateYearlyReportForStudent(Long studentId) {
        log.warn("DEPRECATED: Using deprecated method generateYearlyReportForStudent with ID.");
        log.info("Generating yearly report for student ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Calculate averages for all terms
        Map<Integer, Double> termAverages = calculateAllTermAverages(studentId);

        // Calculate overall yearly average
        Double yearlyAverage = termAverages.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        boolean passed = gradeService.isPassing(yearlyAverage);

        // Get academic year from student
        Integer academicYear = student.getAcademicYearStart();
        if (academicYear == null) {
            academicYear = Year.now().getValue();
        }

        // Generate term reports for detailed view
        List<ReportDTO> termReports = new ArrayList<>();
        List<TermReportSummary> termSummaries = new ArrayList<>();

        for (int term = 1; term <= 3; term++) {
            try {
                ReportDTO termReport = generateReportForStudent(studentId, term);
                termReports.add(termReport);

                termSummaries.add(TermReportSummary.builder()
                        .term(term)
                        .termAverage(termReport.getTermAverage())
                        .formattedAverage(termReport.getFormattedAverage())
                        .rankInClass(termReport.getRankInClass())
                        .remarks(termReport.getRemarks())
                        .passed(gradeService.isPassing(termReport.getTermAverage()))
                        .build());
            } catch (Exception e) {
                log.warn("Could not generate term {} report: {}", term, e.getMessage());
                termSummaries.add(createEmptyTermSummary(term));
            }
        }

        // Calculate subject-wise yearly performance
        List<YearlySubjectReport> yearlySubjectReports = calculateYearlySubjectReports(termReports);

        // Calculate subject pass rate
        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";
        double passRate = gradeService.calculatePassRate(
                yearlySubjectReports.stream()
                        .map(ysr -> SubjectReport.builder()
                                .subjectName(ysr.getSubjectName())
                                .letterGrade(ysr.getYearlyGrade())
                                .className(className)
                                .build())
                        .collect(Collectors.toList()),
                className
        );

        // Calculate yearly rank
        Integer yearlyRank = calculateYearlyRank(studentId, student.getClassRoom().getId());

        // Get class statistics
        int totalStudentsInClass = (int) studentRepository.countByClassRoomId(student.getClassRoom().getId());
        int totalPassed = calculateTotalPassedInClass(student.getClassRoom().getId());
        int totalFailed = totalStudentsInClass - totalPassed;

        // Generate yearly remarks
        String remarks = generateYearlyRemarks(yearlyAverage, passRate);

        // Calculate overall grade
        String overallGrade = gradeService.calculateLetterGrade(yearlyAverage,
                student.getClassRoom() != null ? student.getClassRoom().getName() : "");

        // Generate action recommendation
        String action = generateYearlyStudentAction(yearlyAverage, passRate, passed);

        return YearlyReportDTO.builder()
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom().getName())
                .department(student.getDepartment() != null ? student.getDepartment().getName() : "")
                .specialty(student.getSpecialty())
                .academicYear(academicYear)
                .yearlyAverage(yearlyAverage)
                .formattedYearlyAverage(String.format("%.2f/20", yearlyAverage))
                .passRate(passRate)
                .formattedPassRate(String.format("%.1f%%", passRate))
                .yearlyRank(yearlyRank)
                .term1Rank(getTermRank(termReports, 1))
                .term2Rank(getTermRank(termReports, 2))
                .term3Rank(getTermRank(termReports, 3))
                .remarks(remarks)
                .passed(passed)
                .overallGrade(overallGrade)
                .totalStudentsInClass(totalStudentsInClass)
                .totalPassed(totalPassed)
                .totalFailed(totalFailed)
                .subjectsPassed((int) passRate)
                .totalSubjects(yearlySubjectReports.size())
                .subjectReports(yearlySubjectReports)
                .termSummaries(termSummaries)
                .action(action)
                .build();
    }

    @Override
    public YearlyReportDTO generateYearlyReportForStudentByClassAndRollNumber(Long classId, String rollNumber) {
        log.info("Generating yearly report for student with roll {} in class ID: {}", rollNumber, classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        Student student = studentRepository.findByRollNumberAndClassRoom(rollNumber, classRoom)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Student with roll number " + rollNumber + " in class " + classRoom.getName()));

        return generateYearlyReportForStudent(student.getId());
    }

    @Override
    public YearlyReportDTO getYearlyReportForStudentAndYear(Long studentId, String academicYear) {
        log.info("Generating yearly report for student ID: {} for academic year: {}",
                studentId, academicYear);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Parse REQUESTED academic year
        int[] years = parseAcademicYear(academicYear);
        int academicYearStart = years[0];
        int academicYearEnd = years[1];

        // Calculate averages for all terms for the REQUESTED year
        Map<Integer, Double> termAverages = calculateAllTermAveragesForYear(studentId, academicYearStart, academicYearEnd);

        // Calculate overall yearly average
        Double yearlyAverage = termAverages.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        boolean passed = gradeService.isPassing(yearlyAverage);

        // Generate term reports for detailed view - USING REQUESTED YEAR
        List<ReportDTO> termReports = new ArrayList<>();
        List<TermReportSummary> termSummaries = new ArrayList<>();

        for (int term = 1; term <= 3; term++) {
            try {
                ReportDTO termReport = getTermReportForStudentAndYear(studentId, term, academicYear);
                termReports.add(termReport);

                termSummaries.add(TermReportSummary.builder()
                        .term(term)
                        .termAverage(termReport.getTermAverage())
                        .formattedAverage(termReport.getFormattedAverage())
                        .rankInClass(termReport.getRankInClass())
                        .remarks(termReport.getRemarks())
                        .passed(gradeService.isPassing(termReport.getTermAverage()))
                        .build());
            } catch (Exception e) {
                log.warn("Could not generate term {} report for REQUESTED year {}: {}", term, academicYear, e.getMessage());
                termSummaries.add(createEmptyTermSummary(term));
            }
        }

        // Calculate subject-wise yearly performance
        List<YearlySubjectReport> yearlySubjectReports = calculateYearlySubjectReports(termReports);

        // Calculate subject pass rate
        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";
        double passRate = gradeService.calculatePassRate(
                yearlySubjectReports.stream()
                        .map(ysr -> SubjectReport.builder()
                                .subjectName(ysr.getSubjectName())
                                .letterGrade(ysr.getYearlyGrade())
                                .className(className)
                                .build())
                        .collect(Collectors.toList()),
                className
        );

        // Calculate yearly rank for the specific year
        Integer yearlyRank = calculateYearlyRankForYear(studentId, student.getClassRoom().getId(),
                academicYearStart, academicYearEnd);

        // Get class statistics for the specific year
        int totalStudentsInClass = (int) studentRepository.countByClassRoomIdAndAcademicYear(
                student.getClassRoom().getId(), academicYearStart, academicYearEnd);
        int totalPassed = calculateTotalPassedInClassForYear(student.getClassRoom().getId(),
                academicYearStart, academicYearEnd);
        int totalFailed = totalStudentsInClass - totalPassed;

        // Generate yearly remarks
        String remarks = generateYearlyRemarks(yearlyAverage, passRate);

        // Calculate overall grade
        String overallGrade = gradeService.calculateLetterGrade(yearlyAverage,
                student.getClassRoom() != null ? student.getClassRoom().getName() : "");

        // Generate action recommendation
        String action = generateYearlyStudentAction(yearlyAverage, passRate, passed);

        return YearlyReportDTO.builder()
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom().getName())
                .department(student.getDepartment() != null ? student.getDepartment().getName() : "")
                .specialty(student.getSpecialty())
                .academicYear(Integer.parseInt(academicYear.split("-")[0]))
                .yearlyAverage(yearlyAverage)
                .formattedYearlyAverage(String.format("%.2f/20", yearlyAverage))
                .passRate(passRate)
                .formattedPassRate(String.format("%.1f%%", passRate))
                .yearlyRank(yearlyRank)
                .term1Rank(getTermRank(termReports, 1))
                .term2Rank(getTermRank(termReports, 2))
                .term3Rank(getTermRank(termReports, 3))
                .remarks(remarks)
                .passed(passed)
                .overallGrade(overallGrade)
                .totalStudentsInClass(totalStudentsInClass)
                .totalPassed(totalPassed)
                .totalFailed(totalFailed)
                .subjectsPassed((int) passRate)
                .totalSubjects(yearlySubjectReports.size())
                .subjectReports(yearlySubjectReports)
                .termSummaries(termSummaries)
                .action(action)
                .build();
    }

    @Override
    public List<YearlyReportDTO> generateYearlyReportsForClass(Long classId) {
        log.info("Generating yearly reports for class ID: {}", classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get all students for the class
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);
        List<YearlyReportDTO> yearlyReports = new ArrayList<>();

        for (Student student : students) {
            try {
                YearlyReportDTO yearlyReport = generateYearlyReportForStudent(student.getId());
                yearlyReports.add(yearlyReport);
            } catch (Exception e) {
                log.error("Error generating yearly report for student {}: {}", student.getId(), e.getMessage());
                yearlyReports.add(createEmptyYearlyReport(student));
            }
        }

        // Sort by yearly rank
        yearlyReports.sort(Comparator.comparing(YearlyReportDTO::getYearlyRank));

        return yearlyReports;
    }

    @Override
    public List<YearlyReportDTO> getYearlyReportsForClassAndYear(Long classId, String academicYear) {
        log.info("Generating yearly reports for class ID: {} for academic year: {}",
                classId, academicYear);

        // Parse academic year
        int[] years = parseAcademicYear(academicYear);
        int academicYearStart = years[0];
        int academicYearEnd = years[1];

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get students filtered by academic year
        List<Student> students = studentRepository.findByClassRoomIdAndAcademicYear(
                classId, academicYearStart, academicYearEnd);

        List<YearlyReportDTO> yearlyReports = new ArrayList<>();

        for (Student student : students) {
            try {
                YearlyReportDTO yearlyReport = getYearlyReportForStudentAndYear(student.getId(), academicYear);
                yearlyReports.add(yearlyReport);
            } catch (Exception e) {
                log.error("Error generating yearly report for student {}: {}", student.getId(), e.getMessage());
                yearlyReports.add(createEmptyYearlyReport(student));
            }
        }

        // Sort by yearly rank
        yearlyReports.sort(Comparator.comparing(YearlyReportDTO::getYearlyRank));

        return yearlyReports;
    }

    @Override
    public Page<YearlyReportDTO> generatePaginatedYearlyReportsForClass(Long classId, Pageable pageable) {
        log.info("Generating paginated yearly reports for class ID: {}", classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get all students for the class
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);

        // Create page of students
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), students.size());

        if (start > students.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, students.size());
        }

        List<Student> pageStudents = students.subList(start, end);

        // Generate yearly reports for this page
        List<YearlyReportDTO> yearlyReports = new ArrayList<>();
        for (Student student : pageStudents) {
            try {
                YearlyReportDTO yearlyReport = generateYearlyReportForStudent(student.getId());
                yearlyReports.add(yearlyReport);
            } catch (Exception e) {
                log.error("Error generating yearly report for student {}: {}", student.getId(), e.getMessage());
                yearlyReports.add(createEmptyYearlyReport(student));
            }
        }

        // Sort by the requested sort field
        sortYearlyReports(yearlyReports, pageable.getSort());

        return new PageImpl<>(yearlyReports, pageable, students.size());
    }

    @Override
    public Page<YearlyReportDTO> getPaginatedYearlyReportsForClassAndYear(Long classId,
                                                                          String academicYear, Pageable pageable) {
        log.info("Generating paginated yearly reports for class ID: {} for academic year: {}",
                classId, academicYear);

        // Parse academic year
        int[] years = parseAcademicYear(academicYear);
        int academicYearStart = years[0];
        int academicYearEnd = years[1];

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        // Get all students for the class and year
        List<Student> students = studentRepository.findByClassRoomIdAndAcademicYear(
                classId, academicYearStart, academicYearEnd);

        // Create page of students
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), students.size());

        if (start > students.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, students.size());
        }

        List<Student> pageStudents = students.subList(start, end);

        // Generate yearly reports for this page
        List<YearlyReportDTO> yearlyReports = new ArrayList<>();
        for (Student student : pageStudents) {
            try {
                YearlyReportDTO yearlyReport = getYearlyReportForStudentAndYear(student.getId(), academicYear);
                yearlyReports.add(yearlyReport);
            } catch (Exception e) {
                log.error("Error generating yearly report for student {}: {}", student.getId(), e.getMessage());
                yearlyReports.add(createEmptyYearlyReport(student));
            }
        }

        // Sort by the requested sort field
        sortYearlyReports(yearlyReports, pageable.getSort());

        return new PageImpl<>(yearlyReports, pageable, students.size());
    }

    // ========== UTILITY METHODS ==========

    @Override
    public List<Integer> getAvailableTermsForStudent(Long studentId) {
        return assessmentRepository.findDistinctTermByStudentId(studentId);
    }

    @Override
    public List<Integer> getAvailableAcademicYearsForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        List<Integer> academicYears = new ArrayList<>();
        if (student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
            for (int year = student.getAcademicYearStart(); year <= student.getAcademicYearEnd(); year++) {
                academicYears.add(year);
            }
        }
        return academicYears;
    }

    @Override
    public YearlySummaryDTO getYearlySummary(String academicYear) {
        log.info("Generating yearly summary for academic year: {}", academicYear);

        // Parse academic year
        int[] years = parseAcademicYear(academicYear);
        int academicYearStart = years[0];
        int academicYearEnd = years[1];

        // Get all classes
        List<ClassRoom> classes = classRoomRepository.findAll();

        // Calculate statistics
        int totalStudents = 0;
        int totalClasses = classes.size();
        int totalPassed = 0;
        double overallAverage = 0.0;

        List<ClassSummaryDTO> classSummaries = new ArrayList<>();

        for (ClassRoom classRoom : classes) {
            List<Student> students = studentRepository.findByClassRoomId(classRoom.getId());

            int classSize = students.size();
            totalStudents += classSize;

            // Calculate class average
            double classTotal = 0.0;
            int classPassed = 0;

            for (Student student : students) {
                try {
                    YearlyReportDTO report = getYearlyReportForStudentAndYear(student.getId(), academicYear);
                    if (report.getYearlyAverage() != null) {
                        classTotal += report.getYearlyAverage();
                        overallAverage += report.getYearlyAverage();
                    }
                    if (report.getPassed()) {
                        classPassed++;
                        totalPassed++;
                    }
                } catch (Exception e) {
                    log.warn("Could not generate report for student {}: {}", student.getId(), e.getMessage());
                }
            }

            double classAverage = classSize > 0 ? classTotal / classSize : 0.0;
            double passRate = classSize > 0 ? (classPassed * 100.0) / classSize : 0.0;

            classSummaries.add(ClassSummaryDTO.builder()
                    .className(classRoom.getName())
                    .classTeacher(classRoom.getClassTeacher())
                    .classSize(classSize)
                    .classAverage(classAverage)
                    .passRate(passRate)
                    .totalPassed(classPassed)
                    .totalFailed(classSize - classPassed)
                    .build());
        }

        overallAverage = totalStudents > 0 ? overallAverage / totalStudents : 0.0;
        double overallPassRate = totalStudents > 0 ? (totalPassed * 100.0) / totalStudents : 0.0;

        return YearlySummaryDTO.builder()
                .academicYear(academicYear)
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .totalPassed(totalPassed)
                .totalFailed(totalStudents - totalPassed)
                .overallAverage(overallAverage)
                .overallPassRate(overallPassRate)
                .classSummaries(classSummaries)
                .build();
    }

    // ========== CACHEABLE METHODS ==========

    @Cacheable(value = "studentTermAssessments", key = "#studentId + '-' + #term")
    public List<Assessment> getCachedStudentTermAssessmentsInternal(Long studentId, Integer term) {
        log.debug("Cache miss for student {} term {} assessments", studentId, term);
        return assessmentRepository.findByStudentIdAndTerm(studentId, term);
    }

    @Cacheable(value = "classStudents", key = "#classId")
    public List<Student> getCachedClassStudentsInternal(Long classId) {
        log.debug("Cache miss for class {} students", classId);
        return studentRepository.findByClassRoomId(classId);
    }

    @Cacheable(value = "classStudentsForYear", key = "#classId + '-' + #academicYearStart + '-' + #academicYearEnd")
    public List<Student> getCachedClassStudentsForYearInternal(Long classId, int academicYearStart, int academicYearEnd) {
        log.debug("Cache miss for class {} students for academic year {}-{}",
                classId, academicYearStart, academicYearEnd);
        return studentRepository.findByClassRoomIdAndAcademicYear(classId, academicYearStart, academicYearEnd);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void loadStudentRelationships(Student student) {
        if (student.getStudentSubjects() == null || student.getStudentSubjects().isEmpty()) {
            List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(student.getId());
            student.setStudentSubjects(studentSubjects);
        }
    }

    private List<SubjectReport> calculateSubjectReports(Student student, List<Assessment> assessments, Integer term) {
        List<SubjectReport> subjectReports = new ArrayList<>();

        // Get all subjects for the student
        List<StudentSubject> studentSubjects = student.getStudentSubjects();
        if (studentSubjects == null || studentSubjects.isEmpty()) {
            studentSubjects = studentSubjectRepository.findByStudentId(student.getId());
        }

        if (studentSubjects.isEmpty()) {
            log.warn("Student {} has no subjects assigned!", student.getFullName());
            return subjectReports;
        }

        // Group assessments by subject ID
        Map<Long, List<Assessment>> assessmentsBySubjectId = assessments.stream()
                .collect(Collectors.groupingBy(a -> a.getSubject().getId()));

        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";

        for (StudentSubject studentSubject : studentSubjects) {
            Subject subject = studentSubject.getSubject();
            List<Assessment> subjectAssessments = assessmentsBySubjectId.getOrDefault(subject.getId(), new ArrayList<>());

            // Filter assessments for current term
            List<Assessment> termAssessments = subjectAssessments.stream()
                    .filter(a -> a.getTerm().equals(term))
                    .collect(Collectors.toList());

            SubjectReport subjectReport = reportMapper.toSubjectReport(subject, termAssessments, term, className);
            subjectReports.add(subjectReport);
        }

        return subjectReports;
    }

    private Map<String, Object> calculateTermStatistics(List<SubjectReport> subjectReports,
                                                        Student student, Integer term) {
        Map<String, Object> statistics = new HashMap<>();

        Double termAverage = gradeService.calculateWeightedTermAverage(subjectReports);
        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";
        Double passRate = gradeService.calculatePassRate(subjectReports, className);
        Long subjectsPassed = gradeService.countPassedSubjects(subjectReports, className);
        int totalSubjects = subjectReports.size();

        statistics.put("termAverage", termAverage);
        statistics.put("formattedAverage", String.format("%.2f/20", termAverage));
        statistics.put("passRate", passRate);
        statistics.put("subjectsPassed", subjectsPassed.intValue());
        statistics.put("totalSubjects", totalSubjects);
        statistics.put("remarks", gradeService.generateRemarks(termAverage));

        return statistics;
    }

    private void calculateRanksForReports(List<ReportDTO> reports) {
        if (reports == null || reports.isEmpty()) {
            return;
        }

        // Sort by average descending
        List<ReportDTO> sorted = reports.stream()
                .sorted((r1, r2) -> {
                    Double avg1 = r1.getTermAverage() != null ? r1.getTermAverage() : 0.0;
                    Double avg2 = r2.getTermAverage() != null ? r2.getTermAverage() : 0.0;
                    return Double.compare(avg2, avg1);
                })
                .collect(Collectors.toList());

        // Assign ranks (handling ties)
        int currentRank = 0;
        Double lastAvg = null;
        for (int i = 0; i < sorted.size(); i++) {
            ReportDTO report = sorted.get(i);
            Double avg = report.getTermAverage();

            if (lastAvg == null || Double.compare(avg, lastAvg) != 0) {
                currentRank = i + 1;
                lastAvg = avg;
            }

            report.setRankInClass(currentRank);
            report.setTotalStudentsInClass(sorted.size());
        }
    }

    private void sortReports(List<ReportDTO> reports, Sort sort) {
        if (sort.isUnsorted()) {
            // Default sort by rank
            reports.sort(Comparator.comparing(ReportDTO::getRankInClass));
            return;
        }

        Comparator<ReportDTO> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<ReportDTO> currentComparator = getReportComparator(order.getProperty());
            if (currentComparator != null) {
                if (order.isDescending()) {
                    currentComparator = currentComparator.reversed();
                }
                comparator = comparator == null ? currentComparator : comparator.thenComparing(currentComparator);
            }
        }

        if (comparator != null) {
            reports.sort(comparator);
        }
    }

    private Comparator<ReportDTO> getReportComparator(String property) {
        return switch (property) {
            case "rankInClass" -> Comparator.comparing(ReportDTO::getRankInClass);
            case "termAverage" -> Comparator.comparing(ReportDTO::getTermAverage,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            case "studentFullName" -> Comparator.comparing(ReportDTO::getStudentFullName,
                    Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
            case "rollNumber" -> Comparator.comparing(ReportDTO::getRollNumber,
                    Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
            case "passRate" -> Comparator.comparing(ReportDTO::getPassRate,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private void sortYearlyReports(List<YearlyReportDTO> reports, Sort sort) {
        if (sort.isUnsorted()) {
            // Default sort by yearly rank
            reports.sort(Comparator.comparing(YearlyReportDTO::getYearlyRank));
            return;
        }

        Comparator<YearlyReportDTO> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<YearlyReportDTO> currentComparator = getYearlyReportComparator(order.getProperty());
            if (currentComparator != null) {
                if (order.isDescending()) {
                    currentComparator = currentComparator.reversed();
                }
                comparator = comparator == null ? currentComparator : comparator.thenComparing(currentComparator);
            }
        }

        if (comparator != null) {
            reports.sort(comparator);
        }
    }

    private Comparator<YearlyReportDTO> getYearlyReportComparator(String property) {
        return switch (property) {
            case "yearlyRank" -> Comparator.comparing(YearlyReportDTO::getYearlyRank);
            case "yearlyAverage" -> Comparator.comparing(YearlyReportDTO::getYearlyAverage,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            case "studentFullName" -> Comparator.comparing(YearlyReportDTO::getStudentFullName,
                    Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
            case "rollNumber" -> Comparator.comparing(YearlyReportDTO::getRollNumber,
                    Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
            case "overallGrade" -> Comparator.comparing(YearlyReportDTO::getOverallGrade,
                    Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
            case "passRate" -> Comparator.comparing(YearlyReportDTO::getPassRate,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Map<Integer, Double> calculateAllTermAverages(Long studentId) {
        // Single query to get all assessments grouped by term
        List<Assessment> allAssessments = assessmentRepository.findByStudentId(studentId);
        Map<Integer, List<Assessment>> assessmentsByTerm = allAssessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        Map<Integer, Double> termAverages = new HashMap<>();

        for (Map.Entry<Integer, List<Assessment>> entry : assessmentsByTerm.entrySet()) {
            List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(studentId);

            if (studentSubjects.isEmpty()) {
                termAverages.put(entry.getKey(), 0.0);
                continue;
            }

            double totalWeightedScore = 0.0;
            int totalCoefficient = 0;

            for (StudentSubject studentSubject : studentSubjects) {
                Subject subject = studentSubject.getSubject();
                List<Assessment> subjectAssessments = entry.getValue().stream()
                        .filter(a -> a.getSubject().getId().equals(subject.getId()))
                        .collect(Collectors.toList());

                if (!subjectAssessments.isEmpty()) {
                    double subjectAverage = gradeService.calculateSubjectAverageForTerm(subjectAssessments, entry.getKey());
                    totalWeightedScore += subjectAverage * subject.getCoefficient();
                    totalCoefficient += subject.getCoefficient();
                }
            }

            double termAverage = totalCoefficient > 0 ? totalWeightedScore / totalCoefficient : 0.0;
            termAverages.put(entry.getKey(), termAverage);
        }

        // Ensure we have entries for all terms (1-3)
        for (int term = 1; term <= 3; term++) {
            termAverages.putIfAbsent(term, 0.0);
        }

        return termAverages;
    }

    private Map<Integer, Double> calculateAllTermAveragesForYear(Long studentId, int academicYearStart, int academicYearEnd) {
        // Get all assessments for the student
        List<Assessment> allAssessments = assessmentRepository.findByStudentId(studentId);

        // FIXED: Filter by REQUESTED academic year
        List<Assessment> filteredAssessments = allAssessments.stream()
                .filter(a -> a.getAcademicYearStart() != null && a.getAcademicYearEnd() != null &&
                        a.getAcademicYearStart() == academicYearStart &&
                        a.getAcademicYearEnd() == academicYearEnd)
                .collect(Collectors.toList());

        Map<Integer, List<Assessment>> assessmentsByTerm = filteredAssessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        Map<Integer, Double> termAverages = new HashMap<>();

        for (Map.Entry<Integer, List<Assessment>> entry : assessmentsByTerm.entrySet()) {
            List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(studentId);

            if (studentSubjects.isEmpty()) {
                termAverages.put(entry.getKey(), 0.0);
                continue;
            }

            double totalWeightedScore = 0.0;
            int totalCoefficient = 0;

            for (StudentSubject studentSubject : studentSubjects) {
                Subject subject = studentSubject.getSubject();
                List<Assessment> subjectAssessments = entry.getValue().stream()
                        .filter(a -> a.getSubject().getId().equals(subject.getId()))
                        .collect(Collectors.toList());

                if (!subjectAssessments.isEmpty()) {
                    double subjectAverage = gradeService.calculateSubjectAverageForTerm(subjectAssessments, entry.getKey());
                    totalWeightedScore += subjectAverage * subject.getCoefficient();
                    totalCoefficient += subject.getCoefficient();
                }
            }

            double termAverage = totalCoefficient > 0 ? totalWeightedScore / totalCoefficient : 0.0;
            termAverages.put(entry.getKey(), termAverage);
        }

        // Ensure we have entries for all terms (1-3)
        for (int term = 1; term <= 3; term++) {
            termAverages.putIfAbsent(term, 0.0);
        }

        return termAverages;
    }

    private List<YearlySubjectReport> calculateYearlySubjectReports(List<ReportDTO> termReports) {
        // Group subject reports by subject name
        Map<String, List<SubjectReport>> subjectReportsByTerm = new HashMap<>();

        for (ReportDTO termReport : termReports) {
            for (SubjectReport subjectReport : termReport.getSubjectReports()) {
                String subjectName = subjectReport.getSubjectName();
                subjectReportsByTerm.computeIfAbsent(subjectName, k -> new ArrayList<>()).add(subjectReport);
            }
        }

        // Create yearly subject reports
        List<YearlySubjectReport> yearlySubjectReports = new ArrayList<>();

        for (Map.Entry<String, List<SubjectReport>> entry : subjectReportsByTerm.entrySet()) {
            String subjectName = entry.getKey();
            List<SubjectReport> reports = entry.getValue();

            // Find coefficients
            Integer coefficient = reports.stream()
                    .map(SubjectReport::getCoefficient)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(1);

            // Calculate averages for each term
            Double term1Avg = getTermAverage(reports, termReports, 1);
            Double term2Avg = getTermAverage(reports, termReports, 2);
            Double term3Avg = getTermAverage(reports, termReports, 3);

            // Calculate yearly average
            double yearlyAvg = calculateYearlySubjectAverage(term1Avg, term2Avg, term3Avg);

            // Get class name for grade calculation
            String className = !termReports.isEmpty() ? termReports.getFirst().getClassName() : "";

            // Calculate yearly grade
            String yearlyGrade = gradeService.calculateLetterGrade(yearlyAvg, className);

            // Determine if passed
            boolean passed = gradeService.isSubjectPassing(yearlyGrade, className);

            yearlySubjectReports.add(YearlySubjectReport.builder()
                    .subjectName(subjectName)
                    .coefficient(coefficient)
                    .term1Average(term1Avg)
                    .term2Average(term2Avg)
                    .term3Average(term3Avg)
                    .yearlyAverage(yearlyAvg)
                    .yearlyGrade(yearlyGrade)
                    .passed(passed)
                    .build());
        }

        return yearlySubjectReports;
    }

    private Double getTermAverage(List<SubjectReport> reports, List<ReportDTO> termReports, int term) {
        return reports.stream()
                .filter(r -> isReportInTerm(r, termReports, term))
                .map(SubjectReport::getSubjectAverage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean isReportInTerm(SubjectReport subjectReport, List<ReportDTO> termReports, int term) {
        for (ReportDTO termReport : termReports) {
            if (termReport.getTerm() == term && termReport.getSubjectReports().contains(subjectReport)) {
                return true;
            }
        }
        return false;
    }

    private Integer getTermRank(List<ReportDTO> termReports, int term) {
        return termReports.stream()
                .filter(r -> r.getTerm() == term)
                .map(ReportDTO::getRankInClass)
                .findFirst()
                .orElse(null);
    }

    private Integer calculateYearlyRank(Long studentId, Long classId) {
        // Get all students in the class
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);

        // Calculate yearly average for each student
        Map<Long, Double> studentYearlyAverages = new HashMap<>();
        for (Student student : students) {
            Map<Integer, Double> termAverages = calculateAllTermAverages(student.getId());
            double yearlyAvg = termAverages.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            studentYearlyAverages.put(student.getId(), yearlyAvg);
        }

        // Sort by average descending
        List<Map.Entry<Long, Double>> sorted = studentYearlyAverages.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        // Find rank
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(studentId)) {
                return i + 1;
            }
        }

        return students.size();
    }

    private Integer calculateYearlyRankForYear(Long studentId, Long classId, int academicYearStart, int academicYearEnd) {
        // Get students for the specific academic year
        List<Student> students = studentRepository.findByClassRoomIdAndAcademicYear(classId, academicYearStart, academicYearEnd);

        // Calculate yearly average for each student
        Map<Long, Double> studentYearlyAverages = new HashMap<>();
        for (Student student : students) {
            Map<Integer, Double> termAverages = calculateAllTermAveragesForYear(student.getId(), academicYearStart, academicYearEnd);
            double yearlyAvg = termAverages.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            studentYearlyAverages.put(student.getId(), yearlyAvg);
        }

        // Sort by average descending
        List<Map.Entry<Long, Double>> sorted = studentYearlyAverages.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        // Find rank
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(studentId)) {
                return i + 1;
            }
        }

        return students.size();
    }

    private int calculateTotalPassedInClass(Long classId) {
        // Get all students in the class
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);
        int passedCount = 0;

        for (Student student : students) {
            try {
                Map<Integer, Double> termAverages = calculateAllTermAverages(student.getId());
                double yearlyAverage = termAverages.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                if (gradeService.isPassing(yearlyAverage)) {
                    passedCount++;
                }
            } catch (Exception e) {
                log.warn("Couldn't calculate pass/fail for student {}: {}", student.getId(), e.getMessage());
            }
        }

        return passedCount;
    }

    private int calculateTotalPassedInClassForYear(Long classId, int academicYearStart, int academicYearEnd) {
        List<Student> students = studentRepository.findByClassRoomIdAndAcademicYear(classId, academicYearStart, academicYearEnd);
        int passedCount = 0;

        for (Student student : students) {
            try {
                Map<Integer, Double> termAverages = calculateAllTermAveragesForYear(student.getId(), academicYearStart, academicYearEnd);
                double yearlyAverage = termAverages.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                if (gradeService.isPassing(yearlyAverage)) {
                    passedCount++;
                }
            } catch (Exception e) {
                log.warn("Could not calculate pass/fail for student {}: {}", student.getId(), e.getMessage());
            }
        }

        return passedCount;
    }

    private String generateYearlyRemarks(Double yearlyAverage, Double passRate) {
        if (yearlyAverage == null) return "No assessment data available.";

        if (yearlyAverage >= 16.0 && passRate >= 80) {
            return "Outstanding performance throughout the year! Consistent excellence in all subjects.";
        } else if (yearlyAverage >= 14.0 && passRate >= 70) {
            return "Very good yearly performance. Shows consistent improvement and dedication.";
        } else if (yearlyAverage >= 10.0 && passRate >= 60) {
            return "Satisfactory yearly performance. Good effort shown across terms.";
        } else if (yearlyAverage >= 5.0) {
            return "Yearly performance needs improvement. Some subjects require more attention.";
        } else {
            return "Concern about yearly performance. Significant improvement needed in most subjects.";
        }
    }

    private String generateStudentAction(ReportDTO report) {
        if (report.getTermAverage() == null) {
            return "No assessment data available. Check if all assessments are entered.";
        }

        if (report.getTermAverage() >= 18) {
            return "Continue excellent performance. Consider advanced coursework or leadership roles.";
        } else if (report.getTermAverage() >= 15) {
            return "Very good performance. Focus on maintaining consistency and addressing minor weaknesses.";
        } else if (report.getTermAverage() >= 10) {
            return "Good performance. Identify weak subjects for targeted improvement.";
        } else if (report.getTermAverage() >= 5) {
            return "Needs significant improvement. Schedule parent-teacher meeting and remedial classes.";
        } else {
            return "Critical concern. Immediate intervention required. Consider repeating the term.";
        }
    }

    private String generateYearlyStudentAction(Double yearlyAverage, Double passRate, Boolean passed) {
        if (yearlyAverage == null) {
            return "Incomplete yearly data. Review all term assessments.";
        }

        if (yearlyAverage >= 16 && passRate >= 80) {
            return "Outstanding yearly performance. Eligible for academic awards and accelerated program.";
        } else if (yearlyAverage >= 14 && passRate >= 70) {
            return "Good yearly performance. Continue current study habits. Consider subject specialization.";
        } else if (yearlyAverage >= 10 && passRate >= 60) {
            return "Satisfactory performance. Monitor weak subjects. Consider additional tutoring.";
        } else if (yearlyAverage >= 5) {
            return "Borderline performance. Requires remedial classes and regular progress monitoring.";
        } else {
            return "Failed to meet promotion criteria. Requires repeating the academic year.";
        }
    }

    private ReportDTO createEmptyReport(Student student, Integer term, String academicYear) {
        return ReportDTO.builder()
                .id(student.getId())
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom() != null ? student.getClassRoom().getName() : "")
                .department(student.getDepartment() != null ? student.getDepartment().getName() : "")
                .specialty(student.getSpecialty())
                .term(term)
                .termAverage(0.0)
                .formattedAverage("0.00/20")
                .rankInClass(0)
                .totalStudentsInClass(0)
                .passRate(0.0)
                .subjectsPassed(0)
                .totalSubjects(0)
                .remarks("No assessment data available")
                .subjectReports(new ArrayList<>())
                .academicYear(academicYear)
                .action("No action recommendation available due to missing data")
                .build();
    }

    private YearlyReportDTO createEmptyYearlyReport(Student student) {
        return YearlyReportDTO.builder()
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom() != null ? student.getClassRoom().getName() : "")
                .department(student.getDepartment() != null ? student.getDepartment().getName() : "")
                .specialty(student.getSpecialty())
                .academicYear(Year.now().getValue())
                .yearlyAverage(0.0)
                .formattedYearlyAverage("0.00/20")
                .passRate(0.0)
                .formattedPassRate("0.0%")
                .yearlyRank(0)
                .remarks("No assessment data available")
                .passed(false)
                .overallGrade("U")
                .totalStudentsInClass(0)
                .totalPassed(0)
                .totalFailed(0)
                .subjectsPassed(0)
                .totalSubjects(0)
                .subjectReports(new ArrayList<>())
                .termSummaries(new ArrayList<>())
                .action("No action recommendation available due to missing data")
                .build();
    }

    private TermReportSummary createEmptyTermSummary(Integer term) {
        return TermReportSummary.builder()
                .term(term)
                .termAverage(0.0)
                .formattedAverage("0.00/20")
                .rankInClass(0)
                .remarks("No data")
                .passed(false)
                .build();
    }

    private double calculateYearlySubjectAverage(Double term1Avg, Double term2Avg, Double term3Avg) {
        double total = 0.0;
        int count = 0;

        if (term1Avg != null) { total += term1Avg; count++; }
        if (term2Avg != null) { total += term2Avg; count++; }
        if (term3Avg != null) { total += term3Avg; count++; }

        return count > 0 ? total / count : 0.0;
    }

    private String getCurrentAcademicYear() {
        int currentYear = Year.now().getValue();
        return currentYear + "-" + (currentYear + 1);
    }

    private int[] parseAcademicYear(String academicYear) {
        try {
            String[] parts = academicYear.split("-");
            int startYear = Integer.parseInt(parts[0]);
            int endYear = Integer.parseInt(parts[1]);
            return new int[]{startYear, endYear};
        } catch (Exception e) {
            log.warn("Invalid academic year format: {}, using current year", academicYear);
            int currentYear = Year.now().getValue();
            return new int[]{currentYear, currentYear + 1};
        }
    }

    private Integer calculateStudentTermRank(Long studentId, Integer term, String academicYear,
                                             List<SubjectReport> studentSubjectReports) {

        // Parse academic year
        int[] years = parseAcademicYear(academicYear);
        int yearStart = years[0];
        int yearEnd = years[1];

        // Get all students in the same class for this academic year
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        Long classId = student.getClassRoom().getId();
        List<Student> classStudents = studentRepository.findByClassRoomIdAndAcademicYear(
                classId, yearStart, yearEnd);

        // Calculate average for each student
        Map<Long, Double> studentAverages = new HashMap<>();

        for (Student classStudent : classStudents) {
            // Get assessments for this student
            List<Assessment> assessments = assessmentRepository.findByStudentIdAndTermAndAcademicYear(
                    classStudent.getId(), term, yearStart, yearEnd);

            // Calculate subject reports for this student
            List<SubjectReport> reports = calculateSubjectReports(classStudent, assessments, term);

            // Calculate term average
            Double average = gradeService.calculateWeightedTermAverage(reports);
            studentAverages.put(classStudent.getId(), average);
        }

        // Sort students by average descending
        List<Map.Entry<Long, Double>> sortedEntries = studentAverages.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .toList();

        // Find the rank
        for (int i = 0; i < sortedEntries.size(); i++) {
            if (sortedEntries.get(i).getKey().equals(studentId)) {
                return i + 1; // Rank is 1-based
            }
        }

        return sortedEntries.size(); // Last rank if not found
    }
}