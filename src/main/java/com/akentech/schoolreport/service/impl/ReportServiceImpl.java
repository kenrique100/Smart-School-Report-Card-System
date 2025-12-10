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
    private final StudentSubjectRepository studentSubjectRepository; // Keep for future use
    private final GradeService gradeService;
    private final ReportMapper reportMapper;
    private final ApplicationContext applicationContext;

    @Override
    public ReportDTO generateReportForStudent(Long studentId, Integer term) {
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

        return reportMapper.toReportDTO(student, subjectReports, term, statistics);
    }

    @Override
    public YearlyReportDTO generateYearlyReportForStudent(Long studentId) {
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

        // Calculate subject pass rate
        long totalSubjects = yearlySubjectReports.size();
        long subjectsPassed = yearlySubjectReports.stream()
                .filter(YearlySubjectReport::getPassed)
                .count();
        double passRate = totalSubjects > 0 ? (subjectsPassed * 100.0) / totalSubjects : 0.0;

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
                .subjectsPassed((int) subjectsPassed)
                .totalSubjects((int) totalSubjects)
                .subjectReports(yearlySubjectReports)
                .termSummaries(termSummaries)
                .build();
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
                log.error("Error generating report for student {}: {}", student.getId(), e.getMessage());
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
                log.error("Error generating yearly report for student {}: {}", student.getId(), e.getMessage());
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

        // Get class rank
        Integer rankInClass = calculateStudentRank(student.getId(), student.getClassRoom().getId(), term);

        // Get total students in class
        Integer totalStudentsInClass = (int) studentRepository.countByClassRoomId(student.getClassRoom().getId());

        // Generate remarks using GradeService
        String remarks = gradeService.generateRemarks(termAverage);

        statistics.put("termAverage", termAverage);
        statistics.put("formattedAverage", String.format("%.2f/20", termAverage));
        statistics.put("rankInClass", rankInClass);
        statistics.put("totalStudentsInClass", totalStudentsInClass);
        statistics.put("remarks", remarks);

        return statistics;
    }

    private Integer calculateStudentRank(Long studentId, Long classRoomId, Integer term) {
        // Use self-proxy for cache
        ReportServiceImpl self = applicationContext.getBean(ReportServiceImpl.class);
        List<Student> students = self.getCachedClassStudentsInternal(classRoomId);

        // Calculate average for each student
        Map<Long, Double> studentAverages = new HashMap<>();
        for (Student student : students) {
            List<Assessment> studentAssessments = self.getCachedStudentTermAssessmentsInternal(student.getId(), term);
            Double average = calculateStudentTermAverage(studentAssessments);
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

    private Double calculateStudentTermAverage(List<Assessment> assessments) {
        if (assessments.isEmpty()) return 0.0;

        // Calculate simple average of all assessments
        return assessments.stream()
                .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0)
                .average()
                .orElse(0.0);
    }

    private Map<Integer, Double> calculateAllTermAverages(Long studentId) {
        // Single query to get all assessments grouped by term
        List<Assessment> allAssessments = assessmentRepository.findByStudentId(studentId);
        Map<Integer, List<Assessment>> assessmentsByTerm = allAssessments.stream()
                .collect(Collectors.groupingBy(Assessment::getTerm));

        Map<Integer, Double> termAverages = new HashMap<>();

        for (Map.Entry<Integer, List<Assessment>> entry : assessmentsByTerm.entrySet()) {
            double average = calculateStudentTermAverage(entry.getValue());
            termAverages.put(entry.getKey(), average);
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

            // Calculate yearly grade (assuming ordinary level for now)
            String yearlyGrade = gradeService.calculateLetterGrade(yearlyAvg, "");

            yearlySubjectReports.add(YearlySubjectReport.builder()
                    .subjectName(subjectName)
                    .coefficient(coefficient)
                    .term1Average(term1Avg)
                    .term2Average(term2Avg)
                    .term3Average(term3Avg)
                    .yearlyAverage(yearlyAvg)
                    .yearlyGrade(yearlyGrade)
                    .passed(yearlyGrade.matches("[ABC]"))
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
                YearlyReportDTO yearlyReport = generateYearlyReportForStudent(student.getId());
                if (yearlyReport.getPassed()) {
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
                .remarks("No assessment data available")
                .subjectReports(new ArrayList<>())
                .academicYear(getAcademicYear(student))
                .classTeacher(getClassTeacher(student))
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

    private String getClassTeacher(Student student) {
        return student.getClassRoom() != null ?
                student.getClassRoom().getClassTeacher() : "Not Assigned";
    }
}