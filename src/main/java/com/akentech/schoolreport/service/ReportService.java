package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.StudentRank;
import com.akentech.schoolreport.dto.SubjectReport;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AssessmentRepository assessmentRepository;
    private final AverageRecordRepository averageRecordRepository;
    private final ExamResultRepository examResultRepository;
    private final ClassRoomRepository classRoomRepository;
    private final RankingService rankingService;

    /**
     * Builds a ReportDTO for the student and term.
     */
    @Transactional
    public ReportDTO generateReportForStudent(Long studentId, Integer term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));

        List<Subject> subjects = subjectRepository.findAll();
        List<SubjectReport> subjectReports = new ArrayList<>();
        double totalWeighted = 0.0;
        int totalCoeff = 0;

        for (Subject subj : subjects) {
            SubjectReport subjectReport = calculateSubjectReport(student, subj, term);
            subjectReports.add(subjectReport);

            if (subjectReport.getSubjectAverage() > 0 || subjectReport.getExam() != null) {
                totalWeighted += subjectReport.getSubjectAverage() * subj.getCoefficient();
                totalCoeff += subj.getCoefficient();
            }
        }

        double termAverage = (totalCoeff == 0) ? 0.0 : totalWeighted / totalCoeff;
        termAverage = round(termAverage, 2);

        saveAverageRecord(student, term, termAverage);
        Integer rank = computeStudentRank(student, term);

        ReportDTO dto = ReportDTO.builder()
                .student(student)
                .term(term)
                .termAverage(termAverage)
                .rankInClass(rank)
                .remarks(GradeCalculator.remarkForAverage(termAverage))
                .subjectReports(subjectReports)
                .build();

        log.info("Generated report for student {} {} term {} - Average: {}, Rank: {}",
                student.getFirstName(),
                student.getLastName(),
                term,
                termAverage,
                rank);
        return dto;
    }

    private SubjectReport calculateSubjectReport(Student student, Subject subject, Integer term) {
        List<Assessment> assessments = assessmentRepository.findByStudentAndSubjectAndTerm(student, subject, term);
        List<ExamResult> examResults = examResultRepository.findByStudentAndTerm(student, term);

        Optional<ExamResult> subjectExamResult = examResults.stream()
                .filter(er -> er.getExam() != null && er.getExam().getSubject() != null)
                .filter(er -> er.getExam().getSubject().getId().equals(subject.getId()))
                .findFirst();

        Double a1 = null, a2 = null, exam = null;

        for (Assessment a : assessments) {
            switch (a.getType()) {
                case "Assessment1" -> a1 = a.getScore();
                case "Assessment2" -> a2 = a.getScore();
                case "Exam" -> exam = a.getScore();
            }
        }

        if (term == 3 && subjectExamResult.isPresent()) {
            exam = subjectExamResult.get().getMarks();
            if (subjectExamResult.get().getExam().getTotalMarks() != null &&
                    subjectExamResult.get().getExam().getTotalMarks() != 20) {
                exam = (exam / subjectExamResult.get().getExam().getTotalMarks()) * 20;
            }
        }

        double subjectAverage = calculateSubjectAverage(term, a1, a2, exam);
        String letterGrade = GradeCalculator.toLetterGrade(subjectAverage);

        return SubjectReport.builder()
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .coefficient(subject.getCoefficient())
                .assessment1(a1)
                .assessment2(a2)
                .exam(exam)
                .subjectAverage(round(subjectAverage, 2))
                .letterGrade(letterGrade)
                .build();
    }

    private double calculateSubjectAverage(Integer term, Double a1, Double a2, Double exam) {
        if (term == 3) {
            return exam != null ? exam : 0.0;
        } else {
            double v1 = (a1 != null) ? a1 : 0.0;
            double v2 = (a2 != null) ? a2 : 0.0;

            if (a1 != null || a2 != null) {
                int count = (a1 != null ? 1 : 0) + (a2 != null ? 1 : 0);
                return (v1 + v2) / count;
            } else {
                return 0.0;
            }
        }
    }

    private void saveAverageRecord(Student student, Integer term, double termAverage) {
        Optional<AverageRecord> opt = averageRecordRepository.findByStudentAndTerm(student, term);
        AverageRecord record;

        if (opt.isPresent()) {
            record = opt.get();
            record.setAverage(termAverage);
            record.setRemarks(GradeCalculator.remarkForAverage(termAverage));
        } else {
            record = AverageRecord.builder()
                    .student(student)
                    .term(term)
                    .average(termAverage)
                    .remarks(GradeCalculator.remarkForAverage(termAverage))
                    .build();
        }
        averageRecordRepository.save(record);
    }

    private Integer computeStudentRank(Student student, Integer term) {
        // Use the corrected repository method
        List<Student> classStudents = studentRepository.findByClassRoomId(student.getClassRoom().getId());

        if (classStudents.isEmpty()) {
            return null;
        }

        List<StudentRank> ranks = rankingService.computeRanking(classStudents, term);

        return ranks.stream()
                .filter(r -> r.getStudent().getId().equals(student.getId()))
                .findFirst()
                .map(StudentRank::getRank)
                .orElse(null);
    }

    /**
     * Generate reports for all students in a class for a specific term
     */
    @Transactional
    public List<ReportDTO> generateReportsForClass(Long classId, Integer term) {
        // Use the corrected repository method
        List<Student> students = studentRepository.findByClassRoomId(classId);

        return students.stream()
                .map(student -> generateReportForStudent(student.getId(), term))
                .collect(Collectors.toList());
    }

    /**
     * Get student's performance summary for dashboard
     */
    public Map<String, Object> getStudentPerformanceSummary(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));

        Map<String, Object> summary = new HashMap<>();
        summary.put("studentName", student.getFirstName() + " " + student.getLastName());
        summary.put("studentId", student.getStudentId());
        summary.put("class", student.getClassRoom().getName());

        // Use the corrected repository method
        List<AverageRecord> records = averageRecordRepository.findByStudentId(studentId);
        Map<Integer, Double> termAverages = new HashMap<>();

        for (AverageRecord record : records) {
            termAverages.put(record.getTerm(), record.getAverage());
        }

        summary.put("termAverages", termAverages);

        // Use the corrected repository method
        long recentAssessments = assessmentRepository.findByStudentId(studentId).stream()
                .filter(a -> a.getTerm() == 3)
                .count();

        summary.put("recentAssessments", recentAssessments);
        summary.put("attendanceRate", 95.0); // Placeholder

        return summary;
    }

    /**
     * Get class performance summary for teacher dashboard
     */
    public Map<String, Object> getClassPerformanceSummary(Long classId, Integer term) {
        // Use the corrected repository method
        List<Student> students = studentRepository.findByClassRoomId(classId);
        Map<String, Object> summary = new HashMap<>();

        if (students.isEmpty()) {
            return summary;
        }

        double classAverage = students.stream()
                .mapToDouble(student -> {
                    Optional<AverageRecord> record = averageRecordRepository.findByStudentAndTerm(student, term);
                    return record.map(AverageRecord::getAverage).orElse(0.0);
                })
                .average()
                .orElse(0.0);

        summary.put("classAverage", round(classAverage, 2));
        summary.put("totalStudents", students.size());

        long excellent = students.stream()
                .filter(student -> {
                    Optional<AverageRecord> record = averageRecordRepository.findByStudentAndTerm(student, term);
                    return record.map(r -> r.getAverage() >= 18.0).orElse(false);
                })
                .count();

        long good = students.stream()
                .filter(student -> {
                    Optional<AverageRecord> record = averageRecordRepository.findByStudentAndTerm(student, term);
                    double avg = record.map(AverageRecord::getAverage).orElse(0.0);
                    return avg >= 15.0 && avg < 18.0;
                })
                .count();

        long needsImprovement = students.stream()
                .filter(student -> {
                    Optional<AverageRecord> record = averageRecordRepository.findByStudentAndTerm(student, term);
                    double avg = record.map(AverageRecord::getAverage).orElse(0.0);
                    return avg < 15.0 && avg >= 10.0;
                })
                .count();

        long failing = students.stream()
                .filter(student -> {
                    Optional<AverageRecord> record = averageRecordRepository.findByStudentAndTerm(student, term);
                    double avg = record.map(AverageRecord::getAverage).orElse(0.0);
                    return avg < 10.0;
                })
                .count();

        summary.put("excellentCount", excellent);
        summary.put("goodCount", good);
        summary.put("needsImprovementCount", needsImprovement);
        summary.put("failingCount", failing);

        return summary;
    }

    private static double round(double v, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        return Math.round(v * factor) / (double) factor;
    }
}