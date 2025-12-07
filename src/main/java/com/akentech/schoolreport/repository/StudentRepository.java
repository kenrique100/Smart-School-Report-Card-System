package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByEmail(String email);

    List<Student> findByClassRoom(ClassRoom classRoom);

    List<Student> findByDepartment(Department department);

    List<Student> findBySpecialty(String specialty);

    List<Student> findByClassRoomId(Long classRoomId);

    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Student> findByNameContaining(@Param("query") String query);

    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Student> findByNameContaining(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE " +
            "(:firstName IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
            "(:lastName IS NULL OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
            "(:classRoomId IS NULL OR s.classRoom.id = :classRoomId) AND " +
            "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    Page<Student> findByFilters(@Param("firstName") String firstName,
                                @Param("lastName") String lastName,
                                @Param("classRoomId") Long classRoomId,
                                @Param("departmentId") Long departmentId,
                                @Param("specialty") String specialty,
                                Pageable pageable);

    Page<Student> findBySpecialty(String specialty, Pageable pageable);

    long countByClassRoom(ClassRoom classRoom);

    long countByClassRoomId(Long classRoomId);

    long countByDepartmentId(Long departmentId);

    @Query("SELECT COUNT(DISTINCT s.specialty) FROM Student s WHERE s.specialty IS NOT NULL AND s.specialty <> ''")
    Long countDistinctSpecialties();

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.classRoom LEFT JOIN FETCH s.department WHERE s.id = :id")
    Optional<Student> findByIdWithClassRoomAndDepartment(@Param("id") Long id);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.studentSubjects WHERE s.id = :id")
    Optional<Student> findByIdWithSubjects(@Param("id") Long id);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId AND s.department.id = :departmentId")
    List<Student> findByClassRoomIdAndDepartmentId(@Param("classRoomId") Long classRoomId,
                                                   @Param("departmentId") Long departmentId);

    @Query("SELECT s FROM Student s WHERE s.academicYearStart = :year OR s.academicYearEnd = :year")
    List<Student> findByAcademicYear(@Param("year") Integer year);

    Optional<Student> findByRollNumberAndClassRoom(String rollNumber, ClassRoom classRoom);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId AND s.department.id = :departmentId " +
            "AND (:specialty IS NULL OR s.specialty = :specialty)")
    List<Student> findByClassDepartmentAndSpecialty(@Param("classRoomId") Long classRoomId,
                                                    @Param("departmentId") Long departmentId,
                                                    @Param("specialty") String specialty);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.gender = 'MALE'")
    long countMaleStudents();

    @Query("SELECT COUNT(s) FROM Student s WHERE s.gender = 'FEMALE'")
    long countFemaleStudents();

    @Query("SELECT s FROM Student s WHERE s.academicYearStart >= :startYear AND s.academicYearEnd <= :endYear")
    List<Student> findByAcademicYearRange(@Param("startYear") Integer startYear,
                                          @Param("endYear") Integer endYear);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByStudentId(String studentId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.classRoom = :classRoom AND s.department = :department AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    long countByClassRoomAndDepartmentAndSpecialty(@Param("classRoom") ClassRoom classRoom,
                                                   @Param("department") Department department,
                                                   @Param("specialty") String specialty);

    // OR use this alternative which might work better with null specialties
    @Query("SELECT COUNT(s) FROM Student s WHERE s.classRoom.id = :classRoomId AND s.department.id = :departmentId AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    long countByClassRoomIdAndDepartmentIdAndSpecialty(@Param("classRoomId") Long classRoomId,
                                                       @Param("departmentId") Long departmentId,
                                                       @Param("specialty") String specialty);
}