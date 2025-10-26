package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDepartmentId(Long departmentId);
    Page<Subject> findByDepartmentId(Long departmentId, Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE s.department.id = :departmentId AND s.specialty = :specialty")
    List<Subject> findByDepartmentIdAndSpecialty(@Param("departmentId") Long departmentId,
                                                 @Param("specialty") String specialty);

    @Query("SELECT s FROM Subject s WHERE s.department.id = :departmentId AND s.specialty = :specialty")
    Page<Subject> findByDepartmentIdAndSpecialty(@Param("departmentId") Long departmentId,
                                                 @Param("specialty") String specialty,
                                                 Pageable pageable);

    List<Subject> findBySpecialty(String specialty);
    Page<Subject> findBySpecialty(String specialty, Pageable pageable);

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.specialty IS NOT NULL")
    List<String> findDistinctSpecialties();

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.department.id = :departmentId AND s.specialty IS NOT NULL")
    List<String> findDistinctSpecialtiesByDepartment(@Param("departmentId") Long departmentId);

    @Query("SELECT s FROM Subject s WHERE s.name IN :names AND s.department.code = :departmentCode")
    List<Subject> findByNameInAndDepartmentCode(@Param("names") List<String> names,
                                                @Param("departmentCode") String departmentCode);

    List<Subject> findByName(String name);
    Page<Subject> findByNameContaining(String name, Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE s.department = :department AND s.specialty = :specialty")
    List<Subject> findByDepartmentAndSpecialty(@Param("department") Department department,
                                               @Param("specialty") String specialty);

    @Query("SELECT s FROM Subject s WHERE s.name IN :names")
    List<Subject> findByNameIn(@Param("names") List<String> names);

    // New methods for filtering and pagination
    @Query("SELECT s FROM Subject s WHERE " +
            "(:name IS NULL OR s.name LIKE %:name%) AND " +
            "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    Page<Subject> findByFilters(@Param("name") String name,
                                @Param("departmentId") Long departmentId,
                                @Param("specialty") String specialty,
                                Pageable pageable);
}