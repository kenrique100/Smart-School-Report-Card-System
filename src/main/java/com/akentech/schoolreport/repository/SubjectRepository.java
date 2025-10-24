package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDepartmentId(Long departmentId);

    @Query("SELECT s FROM Subject s WHERE s.department.id = :departmentId AND s.specialty = :specialty")
    List<Subject> findByDepartmentIdAndSpecialty(@Param("departmentId") Long departmentId,
                                                 @Param("specialty") String specialty);

    List<Subject> findBySpecialty(String specialty);

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.specialty IS NOT NULL")
    List<String> findDistinctSpecialties();

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.department.id = :departmentId AND s.specialty IS NOT NULL")
    List<String> findDistinctSpecialtiesByDepartment(@Param("departmentId") Long departmentId);
}