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

/**
 * ReportService computes per-term subject averages, term average, and rank for a student.
 * It also persists AverageRecord.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AssessmentRepository assessmentRepository;
    private final AverageRecordRepository averageRecordRepository;
    private final RankingService rankingService;

    /**
     * Builds a ReportDTO for the student and term.
     */
    @Transactional
    public ReportDTO generateReportForStudent(Long studentId, Integer term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));

        List<Subject> subjects = subjectRepository.findAll(); // in a more advanced system, filter by class/department

        List<SubjectReport> subjectReports = new ArrayList<>();
        double totalWeighted = 0.0;
        int totalCoeff = 0;

        for (Subject subj : subjects) {
            List<Assessment> assessments = assessmentRepository.findByStudentAndSubjectAndTerm(student, subj, term);
            // find assessments by type
            Double a1 = null, a2 = null, exam = null;
            for (Assessment a : assessments) {
                switch (a.getType()) {
                    case "Assessment1" -> a1 = a.getScore();
                    case "Assessment2" -> a2 = a.getScore();
                    case "Exam" -> exam = a.getScore();
                }
            }
            double subjectAverage;
            if (term == 3) {
                // Term 3 uses Exam only
                subjectAverage = (exam == null) ? 0.0 : exam;
            } else {
                // Term 1 & 2 average of A1 and A2
                double v1 = (a1 == null) ? 0.0 : a1;
                double v2 = (a2 == null) ? 0.0 : a2;
                // If both missing treat as 0
                subjectAverage = (v1 + v2) / 2.0;
            }
            double weighted = subjectAverage * subj.getCoefficient();
            totalWeighted += weighted;
            totalCoeff += subj.getCoefficient();

            SubjectReport sreport = SubjectReport.builder()
                    .subjectId(subj.getId())
                    .subjectName(subj.getName())
                    .coefficient(subj.getCoefficient())
                    .assessment1(a1)
                    .assessment2(a2)
                    .exam(exam)
                    .subjectAverage(round(subjectAverage, 2))
                    .letterGrade(GradeCalculator.toLetterGrade(subjectAverage))
                    .build();

            subjectReports.add(sreport);
        }

        double termAverage = (totalCoeff == 0) ? 0.0 : totalWeighted / totalCoeff;
        termAverage = round(termAverage, 2);

        // Save AverageRecord (or update if exists)
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

        // Compute ranking for the student's class (we need all students in class)
        List<Student> classStudents = studentRepository.findByClassRoom(student.getClassRoom());
        List<StudentRank> ranks = rankingService.computeRanking(classStudents, term);
        Optional<StudentRank> sr = ranks.stream()
                .filter(r -> r.getStudent().getId().equals(student.getId()))
                .findFirst();

        Integer rank = sr.map(StudentRank::getRank).orElse(null);

        ReportDTO dto = ReportDTO.builder()
                .student(student)
                .term(term)
                .termAverage(termAverage)
                .rankInClass(rank)
                .remarks(record.getRemarks())
                .subjectReports(subjectReports)
                .build();

        log.info("Generated report for student {} term {} avg {}", student.getFullName(), term, termAverage);
        return dto;
    }

    private static double round(double v, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        return Math.round(v * factor) / (double) factor;
    }
}
