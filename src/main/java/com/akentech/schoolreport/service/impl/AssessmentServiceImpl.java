package com.akentech.schoolreport.service.impl;

import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.AssessmentRepository;
import com.akentech.schoolreport.service.AssessmentService;
import com.akentech.schoolreport.service.StudentPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final StudentPerformanceService studentPerformanceService;

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> findByStudentAndTermOrderBySubjectNameAsc(Student student, Integer term) {
        return assessmentRepository.findByStudentIdAndTermOrderBySubjectNameAsc(student.getId(), term);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsByStudentSubjectAndTerm(Long studentId, Long subjectId, Integer term) {
        return assessmentRepository.findByStudentIdAndSubjectIdAndTerm(studentId, subjectId, term);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject, Integer term, String type) {
        return assessmentRepository.findByStudentAndSubjectAndTermAndType(student, subject, term, type);
    }

    @Override
    @Transactional
    public Assessment save(Assessment assessment) {
        validateAssessment(assessment);

        // Check for duplicate assessment
        Optional<Assessment> existing = assessmentRepository.findByStudentAndSubjectAndTermAndType(
                assessment.getStudent(), assessment.getSubject(), assessment.getTerm(), assessment.getType());

        if (existing.isPresent() && !existing.get().getId().equals(assessment.getId())) {
            throw new BusinessRuleException("Assessment of type '" + assessment.getType() +
                    "' already exists for this student, subject, and term");
        }

        Assessment saved = assessmentRepository.save(assessment);
        log.info("Saved assessment: student={} {} subject={} term={} type={} score={}/20",
                saved.getStudent().getFirstName(),
                saved.getStudent().getLastName(),
                saved.getSubject().getName(),
                saved.getTerm(),
                saved.getType(),
                saved.getScore());

        // NEW: Update student subject scores after saving assessment
        try {
            studentPerformanceService.updateStudentSubjectScores(
                    saved.getStudent().getId(), saved.getTerm());
        } catch (Exception e) {
            log.error("Failed to update student scores after assessment save", e);
        }

        return saved;
    }

    @Override
    @Transactional
    public List<Assessment> saveAll(List<Assessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return List.of();
        }

        for (Assessment assessment : assessments) {
            validateAssessment(assessment);
        }

        List<Assessment> saved = assessmentRepository.saveAll(assessments);
        log.info("Saved {} assessments", saved.size());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!assessmentRepository.existsById(id)) {
            throw new IllegalArgumentException("Assessment not found with id: " + id);
        }
        assessmentRepository.deleteById(id);
        log.info("Deleted assessment id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsByStudent(Long studentId) {
        return assessmentRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAssessmentsBySubjectAndTerm(Long subjectId, Integer term) {
        return assessmentRepository.findBySubjectIdAndTermOrderByScoreDesc(subjectId, term);
    }

    private void validateAssessment(Assessment assessment) {
        if (assessment == null) {
            throw new BusinessRuleException("Assessment cannot be null");
        }

        if (assessment.getStudent() == null || assessment.getStudent().getId() == null) {
            throw new BusinessRuleException("Student is required");
        }

        if (assessment.getSubject() == null || assessment.getSubject().getId() == null) {
            throw new BusinessRuleException("Subject is required");
        }

        if (assessment.getTerm() == null || assessment.getTerm() < 1 || assessment.getTerm() > 3) {
            throw new BusinessRuleException("Term must be between 1 and 3");
        }

        if (assessment.getType() == null || assessment.getType().trim().isEmpty()) {
            throw new BusinessRuleException("Assessment type is required");
        }

        if (assessment.getScore() == null) {
            throw new BusinessRuleException("Score is required");
        }

        // FIXED: Ensure all scores are on 20 marks scale
        if (assessment.getScore() < 0 || assessment.getScore() > 20) {
            throw new BusinessRuleException("Score must be between 0 and 20 (out of 20 marks)");
        }
    }
}