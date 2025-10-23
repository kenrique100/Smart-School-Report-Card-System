package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Exam;
import com.akentech.schoolreport.model.ExamResult;
import com.akentech.schoolreport.repository.ExamRepository;
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
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository; // Add this

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    @Transactional
    public Exam saveExam(Exam exam) {
        Exam saved = examRepository.save(exam);
        log.info("Saved exam: {} (Date: {})", saved.getName(), saved.getExamDate());
        return saved;
    }

    public void deleteExam(Long id) {
        examRepository.deleteById(id);
        log.info("Deleted exam id: {}", id);
    }

    // Remove these methods from ExamService and keep them only in ExamResultService
    @Deprecated
    public ExamResult saveExamResult(ExamResult examResult) {
        log.warn("saveExamResult should be called from ExamResultService");
        return examResultRepository.save(examResult);
    }

    @Deprecated
    public List<ExamResult> getAllExamResults() {
        log.warn("getAllExamResults should be called from ExamResultService");
        return examResultRepository.findAll();
    }
}