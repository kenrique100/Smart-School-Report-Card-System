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

    // NEW: Find by classroom
    List<Subject> findByClassRoomId(Long classroomId);

    // NEW: Find by classroom and department
    List<Subject> findByClassRoomIdAndDepartmentId(Long classroomId, Long departmentId);

    // NEW: Find by classroom, department, and specialty
    List<Subject> findByClassRoomIdAndDepartmentIdAndSpecialty(Long classroomId, Long departmentId, String specialty);

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.specialty IS NOT NULL")
    List<String> findDistinctSpecialties();

    @Query("SELECT DISTINCT s.specialty FROM Subject s WHERE s.department.id = :departmentId AND s.specialty IS NOT NULL")
    List<String> findDistinctSpecialtiesByDepartment(@Param("departmentId") Long departmentId);

    List<Subject> findByNameIn(@Param("names") List<String> names);

    List<Subject> findByClassRoomIdAndDepartmentIdAndSpecialtyIsNull(Long classRoomId, Long departmentId);

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

    @Query("SELECT s FROM Subject s WHERE " +
            "s.classRoom.id = :classroomId AND " +
            "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    List<Subject> findByClassroomAndDepartmentAndSpecialty(@Param("classroomId") Long classroomId,
                                                           @Param("departmentId") Long departmentId,
                                                           @Param("specialty") String specialty);

    @Query("SELECT COUNT(s) FROM Subject s WHERE s.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(s) FROM Subject s WHERE s.classRoom.id = :classroomId")
    long countByClassroomId(@Param("classroomId") Long classroomId);

    boolean existsBySubjectCode(String subjectCode);
    Optional<Subject> findBySubjectCode(String subjectCode);

    // NEW: Method for ordering
    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.department ORDER BY s.name ASC")
    List<Subject> findAllByOrderByNameAsc();

    // NEW: Get subjects by classroom ID with department
    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.department WHERE s.classRoom.id = :classroomId")
    List<Subject> findByClassroomIdWithDepartment(@Param("classroomId") Long classroomId);

    @Query("SELECT s FROM Subject s WHERE s.classRoom.id = :classroomId AND s.department.id = :departmentId AND s.specialty IS NOT NULL")
    List<Subject> findByClassRoomIdAndDepartmentIdAndSpecialtyIsNotNull(
            @Param("classroomId") Long classroomId,
            @Param("departmentId") Long departmentId
    );
}