package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // Core methods used by services
    List<Subject> findByDepartmentId(Long departmentId);
    List<Subject> findByDepartmentIdAndSpecialty(Long departmentId, String specialty);

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.specialty IS NOT NULL")
    List<String> findDistinctSpecialties();

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.department.id = :departmentId AND s.specialty IS NOT NULL")
    List<String> findDistinctSpecialtiesByDepartment(@Param("departmentId") Long departmentId);

    List<Subject> findByNameIn(@Param("names") List<String> names);

    @Query("SELECT s FROM Subject s WHERE " +
            "(:name IS NULL OR s.name LIKE %:name%) AND " +
            "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    Page<Subject> findByFilters(@Param("name") String name,
                                @Param("departmentId") Long departmentId,
                                @Param("specialty") String specialty,
                                Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE " +
            "(:classCode IS NULL OR s.subjectCode LIKE CONCAT(:classCode, '%')) AND " +
            "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    List<Subject> findFilteredSubjects(@Param("classCode") String classCode,
                                       @Param("departmentId") Long departmentId,
                                       @Param("specialty") String specialty);

    @Query("SELECT COUNT(s) FROM Subject s WHERE s.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    boolean existsBySubjectCode(String subjectCode);
    Optional<Subject> findBySubjectCode(String subjectCode);

    // NEW: Method for ordering
    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.department ORDER BY s.name ASC")
    List<Subject> findAllByOrderByNameAsc();
}