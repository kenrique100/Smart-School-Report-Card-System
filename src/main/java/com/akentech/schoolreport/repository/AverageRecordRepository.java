package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.AverageRecord;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AverageRecordRepository extends JpaRepository<AverageRecord, Long> {
    List<AverageRecord> findByStudentInAndTerm(List<Student> students, Integer term);
    Optional<AverageRecord> findByStudentAndTerm(Student student, Integer term);
    List<AverageRecord> findByTerm(Integer term);
}
