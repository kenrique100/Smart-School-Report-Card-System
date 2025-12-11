package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.model.Exam;
import com.akentech.schoolreport.repository.ExamRepository;
import com.akentech.schoolreport.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    @Override
    @Transactional
    public Exam saveExam(Exam exam) {
        validateExam(exam);
        Exam saved = examRepository.save(exam);
        log.info("Saved exam: {} (Date: {})", saved.getName(), saved.getExamDate());
        return saved;
    }

    @Override
    @Transactional
    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new IllegalArgumentException("Exam not found with id: " + id);
        }
        examRepository.deleteById(id);
        log.info("Deleted exam id: {}", id);
    }

    private void validateExam(Exam exam) {
        if (exam == null) {
            throw new BusinessRuleException("Exam cannot be null");
        }

        if (exam.getName() == null || exam.getName().trim().isEmpty()) {
            throw new BusinessRuleException("Exam name is required");
        }

        if (exam.getExamDate() == null) {
            throw new BusinessRuleException("Exam date is required");
        }

        if (exam.getTotalMarks() != null) {
            if (exam.getTotalMarks() <= 0) {
                throw new BusinessRuleException("Total marks must be positive");
            }
            if (exam.getTotalMarks() > 20) {
                throw new BusinessRuleException("Exam total marks cannot exceed 20");
            }
        }

        if (exam.getTerm() == null || exam.getTerm() < 1 || exam.getTerm() > 3) {
            throw new BusinessRuleException("Term must be between 1 and 3");
        }
    }
}