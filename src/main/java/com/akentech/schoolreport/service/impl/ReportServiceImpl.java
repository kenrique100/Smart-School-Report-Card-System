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
    private final GradeService gradeService;
    private final ReportMapper reportMapper;
    private final ApplicationContext applicationContext;

    @Override
    @Deprecated
    public ReportDTO generateReportForStudent(Long studentId, Integer term) {
        log.warn("DEPRECATED: Using deprecated method generateReportForStudent with ID. Use class-based method instead.");
        log.info("Generating term {} report for student ID: {}", term, studentId);

        Student student = studentRepository.findByIdWithClassRoomAndDepartment(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Get all assessments for the student in the given term using self-proxy to avoid cache bypass
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

        // Find student by class and roll number
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        Student student = studentRepository.findByRollNumberAndClassRoom(rollNumber, classRoom)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Student with roll number " + rollNumber + " in class " + classRoom.getName()));

        // Use the deprecated method (which still works internally)
        return generateReportForStudent(student.getId(), term);
    }

    @Override
    @Deprecated
    public YearlyReportDTO generateYearlyReportForStudent(Long studentId) {
        log.warn("DEPRECATED: Using deprecated method generateYearlyReportForStudent with ID. Use class-based method instead.");
        log.info("Generating yearly report for student ID: {}", studentId);

        Student student = studentRepository.findByIdWithClassRoomAndDepartment(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Calculate averages for all terms
        Map<Integer, Double> termAverages = calculateAllTermAverages(studentId);

        // Calculate overall yearly average (simple average of all terms)
        Double yearlyAverage = termAverages.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // Yearly pass criteria - overall average ≥ 10/20
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

                // Create term summary
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

        // Calculate subject pass rate using the class name
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

        // Generate action recommendation - use the passed parameter
        String action = generateYearlyStudentAction(yearlyAverage, passRate, passed);

        return YearlyReportDTO.builder()
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom().getName())
                .department(student.getDepartment().getName())
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
                .subjectsPassed((int) passRate) // Use the pass rate percentage
                .totalSubjects(yearlySubjectReports.size())
                .subjectReports(yearlySubjectReports)
                .termSummaries(termSummaries)
                .action(action)
                .build();
    }

    @Override
    public YearlyReportDTO generateYearlyReportForStudentByClassAndRollNumber(Long classId, String rollNumber) {
        log.info("Generating yearly report for student with roll {} in class ID: {}", rollNumber, classId);

        // Find student by class and roll number
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        Student student = studentRepository.findByRollNumberAndClassRoom(rollNumber, classRoom)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Student with roll number " + rollNumber + " in class " + classRoom.getName()));

        // Use the deprecated method (which still works internally)
        return generateYearlyReportForStudent(student.getId());
    }

    @Override
    public List<ReportDTO> generateReportsForClass(Long classId, Integer term) {
        log.info("Generating term {} reports for class ID: {}", term, classId);

        // Get classroom and log it
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));
        log.debug("Generating reports for class: {}", classRoom.getName());

        // Use self-proxy for cache
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);
        List<ReportDTO> reports = new ArrayList<>();

        for (Student student : students) {
            try {
                ReportDTO report = generateReportForStudent(student.getId(), term);
                reports.add(report);
            } catch (Exception e) {
                log.error("Error generating report for student {}: {}", student.getId(), e.getMessage());
                // Create empty report for student
                reports.add(createEmptyReport(student, term));
            }
        }

        // Sort by rank
        reports.sort(Comparator.comparing(ReportDTO::getRankInClass));

        return reports;
    }

    @Override
    public Page<ReportDTO> generatePaginatedReportsForClass(Long classId, Integer term, Pageable pageable) {
        log.info("Generating paginated term {} reports for class ID: {}", term, classId);

        // Get classroom
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));
        log.debug("Generating paginated reports for class: {}", classRoom.getName());

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
                log.error("Error generating report for students {}: {}", student.getId(), e.getMessage());
                reports.add(createEmptyReport(student, term));
            }
        }

        // Sort by the requested sort field
        sortReports(reports, pageable.getSort());

        return new PageImpl<>(reports, pageable, students.size());
    }

    @Override
    public Page<YearlyReportDTO> generatePaginatedYearlyReportsForClass(Long classId, Pageable pageable) {
        log.info("Generating paginated yearly reports for class ID: {}", classId);

        // Get classroom
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));
        log.debug("Generating paginated yearly reports for class: {}", classRoom.getName());

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
    @Async
    public CompletableFuture<List<ReportDTO>> generateReportsForClassAsync(Long classId, Integer term) {
        log.info("Starting async generation of term {} reports for class ID: {}", term, classId);
        return CompletableFuture.supplyAsync(() -> generateReportsForClass(classId, term));
    }

    @Override
    public List<YearlyReportDTO> generateYearlyReportsForClass(Long classId) {
        log.info("Generating yearly reports for class ID: {}", classId);

        // Get classroom
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));
        log.debug("Generating yearly reports for class: {}", classRoom.getName());

        // Use self-proxy for cache
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classId);
        List<YearlyReportDTO> yearlyReports = new ArrayList<>();

        for (Student student : students) {
            try {
                YearlyReportDTO yearlyReport = generateYearlyReportForStudent(student.getId());
                yearlyReports.add(yearlyReport);
            } catch (Exception e) {
                log.error("Errors generating yearly report for student {}: {}", student.getId(), e.getMessage());
                // Create empty yearly report for student
                yearlyReports.add(createEmptyYearlyReport(student));
            }
        }

        // Sort by yearly rank
        yearlyReports.sort(Comparator.comparing(YearlyReportDTO::getYearlyRank));

        return yearlyReports;
    }

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

    // ========== CACHEABLE METHODS (Public for self-invocation) ==========

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

    // ========== PRIVATE HELPER METHODS ==========

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

    private List<SubjectReport> calculateSubjectReports(Student student, List<Assessment> assessments, Integer term) {
        List<SubjectReport> subjectReports = new ArrayList<>();

        // Group assessments by subject
        Map<Subject, List<Assessment>> assessmentsBySubject = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getSubject));

        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";

        for (Map.Entry<Subject, List<Assessment>> entry : assessmentsBySubject.entrySet()) {
            Subject subject = entry.getKey();
            List<Assessment> subjectAssessments = entry.getValue();

            SubjectReport subjectReport = reportMapper.toSubjectReport(subject, subjectAssessments, term, className);
            subjectReports.add(subjectReport);
        }

        return subjectReports;
    }

    private Map<String, Object> calculateTermStatistics(List<SubjectReport> subjectReports,
                                                        Student student, Integer term) {
        Map<String, Object> statistics = new HashMap<>();

        // Calculate term average using GradeService
        Double termAverage = gradeService.calculateWeightedTermAverage(subjectReports);

        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";

        // Calculate pass rate and subjects passed using GradeService with className
        Double passRate = gradeService.calculatePassRate(subjectReports, className);
        Long subjectsPassed = gradeService.countPassedSubjects(subjectReports, className);
        int totalSubjects = subjectReports.size();

        // Get class rank
        Integer rankInClass = calculateStudentRank(student.getId(), student.getClassRoom().getId(), term);

        // Get total students in class
        Integer totalStudentsInClass = (int) studentRepository.countByClassRoomId(student.getClassRoom().getId());

        // Generate remarks using GradeService
        String remarks = gradeService.generateRemarks(termAverage);

        statistics.put("termAverage", termAverage);
        statistics.put("formattedAverage", String.format("%.2f/20", termAverage));
        statistics.put("passRate", passRate);
        statistics.put("subjectsPassed", subjectsPassed.intValue());
        statistics.put("totalSubjects", totalSubjects);
        statistics.put("rankInClass", rankInClass);
        statistics.put("totalStudentsInClass", totalStudentsInClass);
        statistics.put("remarks", remarks);

        return statistics;
    }

    // REMOVED: Unused private methods calculatePassRate, countPassedSubjects, and isSubjectPassing
    // These are now handled by GradeService

    private boolean hasSubjectData(SubjectReport subjectReport) {
        return subjectReport.getAssessment1() != null ||
                subjectReport.getAssessment2() != null ||
                subjectReport.getSubjectAverage() != null;
    }

    private Integer calculateStudentRank(Long studentId, Long classRoomId, Integer term) {
        // Use self-proxy for cache
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classRoomId);

        // Calculate average for each student
        Map<Long, Double> studentAverages = new HashMap<>();
        for (Student student : students) {
            List<Assessment> studentAssessments = self.getCachedStudentTermAssessmentsInternal(student.getId(), term);
            List<SubjectReport> studentSubjectReports = calculateSubjectReports(student, studentAssessments, term);
            Double average = gradeService.calculateWeightedTermAverage(studentSubjectReports);
            studentAverages.put(student.getId(), average);
        }

        // Sort by average descending
        List<Map.Entry<Long, Double>> sorted = studentAverages.entrySet().stream()
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

    private Map<Integer, Double> calculateAllTermAverages(Long studentId) {
        // Single query to get all assessments grouped by term
        List<Assessment> allAssessments = assessmentRepository.findByStudentId(studentId);
        Map<Integer, List<Assessment>> assessmentsByTerm = allAssessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        Map<Integer, Double> termAverages = new HashMap<>();

        for (Map.Entry<Integer, List<Assessment>> entry : assessmentsByTerm.entrySet()) {
            List<Subject> studentSubjects = studentRepository.findById(studentId)
                    .map(s -> s.getStudentSubjects().stream()
                            .map(StudentSubject::getSubject)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            if (studentSubjects.isEmpty()) {
                termAverages.put(entry.getKey(), 0.0);
                continue;
            }

            double totalWeightedScore = 0.0;
            int totalCoefficient = 0;

            for (Subject subject : studentSubjects) {
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

            // Find coefficients (should be the same for all terms)
            Integer coefficient = reports.stream()
                    .map(SubjectReport::getCoefficient)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(1);

            // Calculate averages for each term
            Double term1Avg = getTermAverage(reports, termReports, 1);
            Double term2Avg = getTermAverage(reports, termReports, 2);
            Double term3Avg = getTermAverage(reports, termReports, 3);

            // Calculate yearly average (simple average of all terms)
            double yearlyAvg = calculateYearlySubjectAverage(term1Avg, term2Avg, term3Avg);

            // Get class name from the first term report for grade calculation
            String className = termReports.get(0).getClassName();

            // Calculate yearly grade using the correct class name
            String yearlyGrade = gradeService.calculateLetterGrade(yearlyAvg, className);

            // Determine if passed based on class level
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
        // Use self-proxy for cache
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

    private int calculateTotalPassedInClass(Long classId) {
        // Use self-proxy for cache
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

    // ========== ACTION RECOMMENDATION METHODS ==========

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

    // ========== HELPER METHODS FOR CREATING EMPTY REPORTS ==========

    private ReportDTO createEmptyReport(Student student, Integer term) {
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
                .academicYear(getAcademicYear(student))
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

    private String getAcademicYear(Student student) {
        if (student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
            return student.getAcademicYearStart() + "-" + student.getAcademicYearEnd();
        }
        return Year.now().getValue() + "-" + (Year.now().getValue() + 1);
    }
}