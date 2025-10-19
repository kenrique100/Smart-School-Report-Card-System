package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;

    public List<Assessment> findByStudentAndTerm(Student student, Integer term) {
        return assessmentRepository.findByStudentAndTerm(student, term);
    }

    public Optional<Assessment> findByStudentSubjectTermType(Student student, Subject subject, Integer term, String type) {
        return assessmentRepository.findByStudentAndSubjectAndTermAndType(student, subject, term, type);
    }

    @Transactional
    public Assessment save(Assessment assessment) {
        Assessment a = assessmentRepository.save(assessment);
        log.info("Saved assessment: student={} subject={} term={} type={} score={}",
                a.getStudent().getFullName(), a.getSubject().getName(), a.getTerm(), a.getType(), a.getScore());
        return a;
    }

    public void delete(Long id) {
        assessmentRepository.deleteById(id);
        log.info("Deleted assessment id={}", id);
    }
}
