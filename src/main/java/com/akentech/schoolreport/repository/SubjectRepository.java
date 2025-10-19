package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
