package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByStudentAndTerm(Student student, Integer term);

    List<Assessment> findByStudentAndSubjectAndTerm(Student student, Subject subject, Integer term);

    /**
     * All assessments for a given class's students (we will query by students list).
     */
    List<Assessment> findBySubjectAndTerm(Subject subject, Integer term);

    Optional<Assessment> findByStudentAndSubjectAndTermAndType(Student student, Subject subject, Integer term, String type);
}
