package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(DepartmentCode code);
    Optional<Department> findByName(String name);
}