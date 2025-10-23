package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ExamResult;
import com.akentech.schoolreport.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamResultService {

    private final ExamResultRepository examResultRepository;

    public List<ExamResult> getAllExamResults() {
        return examResultRepository.findAll();
    }

    public Optional<ExamResult> getExamResultById(Long id) {
        return examResultRepository.findById(id);
    }

    public List<ExamResult> getExamResultsByExam(Long examId) {
        return examResultRepository.findByExamId(examId);
    }

    public List<ExamResult> getExamResultsByStudent(Long studentId) {
        return examResultRepository.findByStudentId(studentId);
    }

    @Transactional
    public ExamResult saveExamResult(ExamResult examResult) {
        // Calculate grade if not provided
        if (examResult.getGrade() == null && examResult.getMarks() != null && examResult.getExam() != null) {
            String grade = calculateGrade(examResult.getMarks(), examResult.getExam().getTotalMarks());
            examResult.setGrade(grade);
        }

        ExamResult saved = examResultRepository.save(examResult);
        log.info("Saved exam result for student {} in exam {}: {} marks, {} grade",
                saved.getStudent().getFirstName(),
                saved.getExam().getName(),
                saved.getMarks(),
                saved.getGrade());
        return saved;
    }

    public void deleteExamResult(Long id) {
        examResultRepository.deleteById(id);
        log.info("Deleted exam result id: {}", id);
    }

    private String calculateGrade(Double marksObtained, Double totalMarks) {
        if (marksObtained == null || totalMarks == null || totalMarks == 0) {
            return "N/A";
        }

        double percentage = (marksObtained / totalMarks) * 100;

        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C+";
        if (percentage >= 40) return "C";
        return "F";
    }
}