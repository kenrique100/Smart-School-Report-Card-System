package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
