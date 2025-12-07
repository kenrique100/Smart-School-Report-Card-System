package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.*;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentSubjectRepository studentSubjectRepository;

    // ========== PUBLIC METHODS CALLED BY CONTROLLER ==========

    /**
     * Generate term report for a specific student
     */
    public ReportDTO generateReportForStudent(Long studentId, Integer term) {
        log.info("Generating term {} report for student ID: {}", term, studentId);

        // First generate the internal DTO
        StudentTermReportDTO internalReport = generateStudentTermReportInternal(studentId, term);

        // Convert to the expected ReportDTO
        return convertToReportDTO(internalReport);
    }

    /**
     * Generate yearly report for a student
     */
    public YearlyReportDTO generateYearlyReportForStudent(Long studentId) {
        log.info("Generating yearly report for student ID: {}", studentId);

        Student student = studentRepository.findByIdWithClassRoomAndDepartment(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Get academic year from student
        Integer academicYear = student.getAcademicYearStart();
        if (academicYear == null) {
            academicYear = Year.now().getValue();
        }

        // Generate term reports
        List<ReportDTO> termReports = new ArrayList<>();
        for (int term = 1; term <= 3; term++) {
            try {
                ReportDTO termReport = generateReportForStudent(studentId, term);
                termReports.add(termReport);
            } catch (Exception e) {
                log.warn("Could not generate term {} report: {}", term, e.getMessage());
            }
        }

        // Calculate yearly statistics
        return calculateYearlyReportDTO(student, termReports, academicYear);
    }

    /**
     * Generate term reports for a class
     */
    public List<ReportDTO> generateReportsForClass(Long classId, Integer term) {
        log.info("Generating term {} reports for class ID: {}", term, classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        List<Student> students = studentRepository.findByClassRoomId(classId);
        List<ReportDTO> reports = new ArrayList<>();

        for (Student student : students) {
            try {
                ReportDTO report = generateReportForStudent(student.getId(), term);
                reports.add(report);
            } catch (Exception e) {
                log.error("Error generating report for student {}: {}", student.getId(), e.getMessage());
            }
        }

        // Sort by rank
        reports.sort(Comparator.comparing(ReportDTO::getRankInClass));

        return reports;
    }

    /**
     * Generate yearly reports for a class
     */
    public List<YearlyReportDTO> generateYearlyReportsForClass(Long classId) {
        log.info("Generating yearly reports for class ID: {}", classId);

        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("ClassRoom", classId));

        List<Student> students = studentRepository.findByClassRoomId(classId);
        List<YearlyReportDTO> yearlyReports = new ArrayList<>();

        for (Student student : students) {
            try {
                YearlyReportDTO yearlyReport = generateYearlyReportForStudent(student.getId());
                yearlyReports.add(yearlyReport);
            } catch (Exception e) {
                log.error("Error generating yearly report for student {}: {}", student.getId(), e.getMessage());
            }
        }

        // Sort by yearly rank
        yearlyReports.sort(Comparator.comparing(YearlyReportDTO::getYearlyRank));

        return yearlyReports;
    }

    /**
     * Get available terms for student
     */
    public List<Integer> getAvailableTermsForStudent(Long studentId) {
        return assessmentRepository.findDistinctTermByStudentId(studentId);
    }

    /**
     * Get available academic years for student
     */
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

    // ========== INTERNAL METHODS ==========

    /**
     * Internal method to generate student term report (for internal use)
     */
    private StudentTermReportDTO generateStudentTermReportInternal(Long studentId, Integer term) {
        Student student = studentRepository.findByIdWithClassRoomAndDepartment(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student", studentId));

        // Get all assessments for the student in the given term
        List<Assessment> assessments = assessmentRepository.findByStudentIdAndTerm(studentId, term);

        // Get student subjects
        List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(studentId);

        // Calculate subject reports
        List<SubjectReport> subjectReports = calculateSubjectReports(student, assessments, studentSubjects, term);

        // Calculate term statistics
        Map<String, Object> statistics = calculateTermStatistics(subjectReports, student, term);

        return buildStudentTermReportDTO(student, subjectReports, statistics, term);
    }

    private List<SubjectReport> calculateSubjectReports(Student student, List<Assessment> assessments,
                                                        List<StudentSubject> studentSubjects, Integer term) {
        List<SubjectReport> subjectReports = new ArrayList<>();

        // Group assessments by subject
        Map<Subject, List<Assessment>> assessmentsBySubject = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getSubject));

        for (Map.Entry<Subject, List<Assessment>> entry : assessmentsBySubject.entrySet()) {
            Subject subject = entry.getKey();
            List<Assessment> subjectAssessments = entry.getValue();

            SubjectReport subjectReport = calculateSubjectReport(subject, subjectAssessments, term);
            subjectReports.add(subjectReport);
        }

        return subjectReports;
    }

    private SubjectReport calculateSubjectReport(Subject subject, List<Assessment> assessments, Integer term) {
        Double assessment1 = null;
        Double assessment2 = null;
        Double exam = null;

        // Separate assessments by type
        for (Assessment assessment : assessments) {
            String type = assessment.getType();
            if (type != null) {
                if (type.equalsIgnoreCase("Assessment1") || type.equalsIgnoreCase("Quiz") || type.equalsIgnoreCase("Test1")) {
                    assessment1 = assessment.getScore();
                } else if (type.equalsIgnoreCase("Assessment2") || type.equalsIgnoreCase("Assignment") || type.equalsIgnoreCase("Test2")) {
                    assessment2 = assessment.getScore();
                } else if (type.equalsIgnoreCase("Exam") || type.equalsIgnoreCase("Final")) {
                    exam = assessment.getScore();
                }
            }
        }

        // Calculate subject average based on term
        Double subjectAverage = calculateSubjectAverage(assessment1, assessment2, exam, term);
        String letterGrade = calculateLetterGrade(subjectAverage);

        return SubjectReport.builder()
                .subjectName(subject.getName())
                .coefficient(subject.getCoefficient())
                .assessment1(assessment1)
                .assessment2(assessment2)
                .exam(exam)
                .subjectAverage(subjectAverage)
                .letterGrade(letterGrade)
                .build();
    }

    private Double calculateSubjectAverage(Double assessment1, Double assessment2, Double exam, Integer term) {
        if (term == 1 || term == 2) {
            // Terms 1 and 2: average of two assessments
            if (assessment1 != null && assessment2 != null) {
                return (assessment1 + assessment2) / 2;
            } else if (assessment1 != null) {
                return assessment1;
            } else if (assessment2 != null) {
                return assessment2;
            }
        } else if (term == 3) {
            // Term 3: exam only
            return exam != null ? exam : 0.0;
        }
        return 0.0;
    }

    private String calculateLetterGrade(Double average) {
        if (average == null || average < 0) return "U";
        if (average >= 18) return "A";
        else if (average >= 15) return "B";
        else if (average >= 10) return "C";
        else if (average >= 5) return "D";
        else return "U";
    }

    private Map<String, Object> calculateTermStatistics(List<SubjectReport> subjectReports,
                                                        Student student, Integer term) {
        Map<String, Object> statistics = new HashMap<>();

        // Calculate term average
        double totalWeightedScore = 0;
        int totalCoefficient = 0;

        for (SubjectReport subject : subjectReports) {
            if (subject.getSubjectAverage() != null) {
                totalWeightedScore += subject.getSubjectAverage() * subject.getCoefficient();
                totalCoefficient += subject.getCoefficient();
            }
        }

        Double termAverage = totalCoefficient > 0 ? totalWeightedScore / totalCoefficient : 0.0;

        // Get class rank
        Integer rankInClass = calculateStudentRank(student.getId(), student.getClassRoom().getId(), term);

        // Get total students in class
        Integer totalStudentsInClass = (int) studentRepository.countByClassRoomId(student.getClassRoom().getId());

        // Generate remarks
        String remarks = generateRemarks(termAverage);

        statistics.put("termAverage", termAverage);
        statistics.put("formattedAverage", String.format("%.2f", termAverage));
        statistics.put("rankInClass", rankInClass);
        statistics.put("totalStudentsInClass", totalStudentsInClass);
        statistics.put("remarks", remarks);

        return statistics;
    }

    private Integer calculateStudentRank(Long studentId, Long classRoomId, Integer term) {
        List<Student> students = studentRepository.findByClassRoomId(classRoomId);

        // Calculate average for each student
        Map<Long, Double> studentAverages = new HashMap<>();
        for (Student student : students) {
            List<Assessment> studentAssessments = assessmentRepository.findByStudentIdAndTerm(student.getId(), term);
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

        return students.size(); // Default to last position
    }

    private Double calculateStudentTermAverage(List<Assessment> assessments) {
        if (assessments.isEmpty()) return 0.0;

        double total = 0;
        for (Assessment assessment : assessments) {
            total += assessment.getScore() != null ? assessment.getScore() : 0;
        }

        return total / assessments.size();
    }

    private String generateRemarks(Double average) {
        if (average == null) return "No assessment data";
        if (average >= 18) return "Excellent performance! Keep up the good work.";
        else if (average >= 15) return "Very good performance. Continue with the good work.";
        else if (average >= 10) return "Good performance. Room for improvement.";
        else if (average >= 5) return "Satisfactory. Needs to work harder.";
        else return "Needs significant improvement. Please seek additional help.";
    }

    private StudentTermReportDTO buildStudentTermReportDTO(Student student, List<SubjectReport> subjectReports,
                                                           Map<String, Object> statistics, Integer term) {
        return StudentTermReportDTO.builder()
                .student(student)
                .studentFullName(student.getFullName())
                .term(term)
                .academicYear(student.getAcademicYearStart() + "-" + student.getAcademicYearEnd())
                .classTeacher(student.getClassRoom() != null ? student.getClassRoom().getClassTeacher() : "Not Assigned")
                .termAverage((Double) statistics.get("termAverage"))
                .formattedAverage((String) statistics.get("formattedAverage"))
                .rankInClass((Integer) statistics.get("rankInClass"))
                .totalStudentsInClass((Integer) statistics.get("totalStudentsInClass"))
                .remarks((String) statistics.get("remarks"))
                .subjectReports(subjectReports)
                .build();
    }

    // ========== DTO CONVERSION METHODS ==========

    private ReportDTO convertToReportDTO(StudentTermReportDTO internalReport) {
        Student student = (Student) internalReport.getStudent();

        return ReportDTO.builder()
                .id(student.getId())
                .student(student)
                .studentFullName(internalReport.getStudentFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom() != null ? student.getClassRoom().getName() : "")
                .department(student.getDepartment() != null ? student.getDepartment().getName() : "")
                .specialty(student.getSpecialty())
                .term(internalReport.getTerm())
                .termAverage(internalReport.getTermAverage())
                .formattedAverage(internalReport.getFormattedAverage())
                .rankInClass(internalReport.getRankInClass())
                .totalStudentsInClass(internalReport.getTotalStudentsInClass())
                .remarks(internalReport.getRemarks())
                .subjectReports(internalReport.getSubjectReports())
                .academicYear(internalReport.getAcademicYear())
                .classTeacher(internalReport.getClassTeacher())
                .build();
    }

    private YearlyReportDTO calculateYearlyReportDTO(Student student, List<ReportDTO> termReports, Integer academicYear) {
        // Calculate yearly average
        Double yearlyAverage = termReports.stream()
                .filter(r -> r.getTermAverage() != null)
                .mapToDouble(ReportDTO::getTermAverage)
                .average()
                .orElse(0.0);

        // Calculate pass/fail status
        boolean passed = termReports.stream()
                .allMatch(r -> r.getTermAverage() != null && r.getTermAverage() >= 10);

        // Calculate overall grade
        String overallGrade = calculateLetterGrade(yearlyAverage);

        // Calculate pass rate (percentage of passed subjects)
        long totalSubjects = termReports.stream()
                .mapToLong(r -> r.getSubjectReports().size())
                .sum();

        long passedSubjects = termReports.stream()
                .flatMap(r -> r.getSubjectReports().stream())
                .filter(sr -> {
                    String grade = sr.getLetterGrade();
                    return grade != null && grade.matches("[ABC]");
                })
                .count();

        double passRate = totalSubjects > 0 ? (passedSubjects * 100.0) / totalSubjects : 0.0;

        // Get class statistics
        Long classId = student.getClassRoom().getId();
        int totalStudentsInClass = (int) studentRepository.countByClassRoomId(classId);

        // Count passed/failed students in class
        List<Student> classStudents = studentRepository.findByClassRoomId(classId);
        long totalPassed = 0;
        long totalFailed = 0;

        for (Student classmate : classStudents) {
            try {
                // Simplified logic - check if any term report exists
                boolean classmatePassed = true;
                for (int term = 1; term <= 3; term++) {
                    List<Assessment> assessments = assessmentRepository.findByStudentIdAndTerm(classmate.getId(), term);
                    if (!assessments.isEmpty()) {
                        Double avg = calculateStudentTermAverage(assessments);
                        if (avg < 10) {
                            classmatePassed = false;
                            break;
                        }
                    }
                }
                if (classmatePassed) {
                    totalPassed++;
                } else {
                    totalFailed++;
                }
            } catch (Exception e) {
                log.warn("Could not calculate pass/fail for student {}: {}", classmate.getId(), e.getMessage());
                totalFailed++;
            }
        }

        // Create term summaries
        List<TermReportSummary> termSummaries = termReports.stream()
                .map(tr -> TermReportSummary.builder()
                        .term(tr.getTerm())
                        .termAverage(tr.getTermAverage())
                        .formattedAverage(tr.getFormattedAverage())
                        .rankInClass(tr.getRankInClass())
                        .remarks(tr.getRemarks())
                        .passed(tr.getTermAverage() != null && tr.getTermAverage() >= 10)
                        .build())
                .collect(Collectors.toList());

        // Create yearly subject reports
        List<YearlySubjectReport> yearlySubjectReports = calculateYearlySubjectReports(termReports);

        return YearlyReportDTO.builder()
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(student.getClassRoom().getName())
                .department(student.getDepartment().getName())
                .specialty(student.getSpecialty())
                .academicYear(academicYear)
                .yearlyAverage(yearlyAverage)
                .formattedYearlyAverage(String.format("%.2f", yearlyAverage))
                .passRate(passRate)
                .formattedPassRate(String.format("%.1f%%", passRate))
                .yearlyRank(calculateYearlyRank(student.getId(), classId))
                .term1Rank(getTermRank(termReports, 1))
                .term2Rank(getTermRank(termReports, 2))
                .term3Rank(getTermRank(termReports, 3))
                .remarks(generateYearlyRemarks(yearlyAverage, passRate))
                .passed(passed)
                .overallGrade(overallGrade)
                .totalStudentsInClass(totalStudentsInClass)
                .totalPassed((int) totalPassed)
                .totalFailed((int) totalFailed)
                .subjectsPassed((int) passedSubjects)
                .totalSubjects((int) totalSubjects)
                .subjectReports(yearlySubjectReports)
                .termSummaries(termSummaries)
                .build();
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

            // Calculate yearly average
            double yearlyAvg = 0.0;
            int count = 0;
            if (term1Avg != null) { yearlyAvg += term1Avg; count++; }
            if (term2Avg != null) { yearlyAvg += term2Avg; count++; }
            if (term3Avg != null) { yearlyAvg += term3Avg; count++; }
            yearlyAvg = count > 0 ? yearlyAvg / count : 0.0;

            // Calculate yearly grade
            String yearlyGrade = calculateLetterGrade(yearlyAvg);

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
                .filter(r -> {
                    // Find which term this report belongs to
                    for (ReportDTO termReport : termReports) {
                        if (termReport.getTerm() == term &&
                                termReport.getSubjectReports().contains(r)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(SubjectReport::getSubjectAverage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Integer getTermRank(List<ReportDTO> termReports, int term) {
        return termReports.stream()
                .filter(r -> r.getTerm() == term)
                .map(ReportDTO::getRankInClass)
                .findFirst()
                .orElse(null);
    }

    private Integer calculateYearlyRank(Long studentId, Long classId) {
        List<Student> students = studentRepository.findByClassRoomId(classId);

        // Calculate yearly average for each student
        Map<Long, Double> studentYearlyAverages = new HashMap<>();
        for (Student student : students) {
            double total = 0.0;
            int count = 0;

            for (int term = 1; term <= 3; term++) {
                List<Assessment> assessments = assessmentRepository.findByStudentIdAndTerm(student.getId(), term);
                if (!assessments.isEmpty()) {
                    Double avg = calculateStudentTermAverage(assessments);
                    total += avg;
                    count++;
                }
            }

            studentYearlyAverages.put(student.getId(), count > 0 ? total / count : 0.0);
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

    private String generateYearlyRemarks(Double yearlyAverage, Double passRate) {
        if (yearlyAverage == null) return "No assessment data available.";

        if (yearlyAverage >= 16 && passRate >= 80) {
            return "Outstanding performance throughout the year! Consistent excellence in all subjects.";
        } else if (yearlyAverage >= 14 && passRate >= 70) {
            return "Very good yearly performance. Shows consistent improvement and dedication.";
        } else if (yearlyAverage >= 10 && passRate >= 60) {
            return "Satisfactory yearly performance. Good effort shown across terms.";
        } else if (yearlyAverage >= 5) {
            return "Yearly performance needs improvement. Some subjects require more attention.";
        } else {
            return "Concern about yearly performance. Significant improvement needed in most subjects.";
        }
    }
}