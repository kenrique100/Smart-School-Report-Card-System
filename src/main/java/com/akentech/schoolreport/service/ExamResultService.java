package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ExamResult;
import com.akentech.schoolreport.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final GradeService gradeService; // Added to use centralized grading

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
        // Validate marks are on 20 marks scale
        validateMarksOn20Scale(examResult);

        // Calculate grade using centralized GradeService
        if (examResult.getGrade() == null && examResult.getMarks() != null) {
            String className = examResult.getStudent().getClassRoom() != null ?
                    examResult.getStudent().getClassRoom().getName() : "";
            String grade = gradeService.calculateLetterGrade(examResult.getMarks(), className);
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

    private void validateMarksOn20Scale(ExamResult examResult) {
        if (examResult.getMarks() != null &&
                (examResult.getMarks() < 0 || examResult.getMarks() > 20)) {
            throw new IllegalArgumentException("Exam marks must be between 0 and 20 (out of 20 marks)");
        }
    }
}