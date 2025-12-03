package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.enums.Gender;
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

    List<Student> findByClassRoom(ClassRoom classRoom);
    Page<Student> findByClassRoom(ClassRoom classRoom, Pageable pageable);

    Optional<Student> findByRollNumberAndClassRoom(String rollNumber, ClassRoom classRoom);
    Optional<Student> findByRollNumber(String rollNumber);
    Optional<Student> findByStudentId(String studentId);

    long countByClassRoomAndDepartment(ClassRoom classRoom, Department department);
    long countByClassRoomAndDepartmentAndSpecialty(ClassRoom classRoom, Department department, String specialty);
    long countByClassRoom(ClassRoom classRoom);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId")
    List<Student> findByClassRoomId(@Param("classRoomId") Long classRoomId);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId")
    Page<Student> findByClassRoomId(@Param("classRoomId") Long classRoomId, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.firstName LIKE %:name% OR s.lastName LIKE %:name%")
    List<Student> findByNameContaining(@Param("name") String name);

    @Query("SELECT s FROM Student s WHERE s.firstName LIKE %:name% OR s.lastName LIKE %:name%")
    Page<Student> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId AND s.gender = :gender")
    List<Student> findByClassRoomIdAndGender(@Param("classRoomId") Long classRoomId, @Param("gender") Gender gender);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.classRoom.id = :classRoomId")
    long countByClassRoomId(@Param("classRoomId") Long classRoomId);

    List<Student> findBySpecialty(String specialty);
    Page<Student> findBySpecialty(String specialty, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT s.specialty) FROM Student s WHERE s.specialty IS NOT NULL")
    Long countDistinctSpecialties();

    Optional<Student> findByEmail(String email);

    @Query("SELECT DISTINCT s FROM Student s " +
            "LEFT JOIN FETCH s.classRoom " +
            "LEFT JOIN FETCH s.department " +
            "LEFT JOIN FETCH s.studentSubjects ss " +
            "LEFT JOIN FETCH ss.subject " +
            "WHERE " +
            "(:firstName IS NULL OR s.firstName LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR s.lastName LIKE %:lastName%) AND " +
            "(:classRoomId IS NULL OR s.classRoom.id = :classRoomId) AND " +
            "(:departmentId IS NULL OR s.department.id = :departmentId) AND " +
            "(:specialty IS NULL OR s.specialty = :specialty)")
    Page<Student> findByFilters(@Param("firstName") String firstName,
                                @Param("lastName") String lastName,
                                @Param("classRoomId") Long classRoomId,
                                @Param("departmentId") Long departmentId,
                                @Param("specialty") String specialty,
                                Pageable pageable);

    // NEW: Methods for eager loading
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.classRoom LEFT JOIN FETCH s.department WHERE s.id = :id")
    Optional<Student> findByIdWithClassRoomAndDepartment(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.classRoom LEFT JOIN FETCH s.department")
    List<Student> findAllWithClassRoomAndDepartment();

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.classRoom LEFT JOIN FETCH s.department")
    Page<Student> findAllWithClassRoomAndDepartment(Pageable pageable);
    @Query("SELECT DISTINCT s FROM Student s " +
            "LEFT JOIN FETCH s.classRoom " +
            "LEFT JOIN FETCH s.department " +
            "LEFT JOIN FETCH s.studentSubjects ss " +
            "LEFT JOIN FETCH ss.subject " +
            "ORDER BY s.firstName, s.lastName")
    List<Student> findAllWithAssociations();

    // Also add a paged version
    @Query(value = "SELECT DISTINCT s FROM Student s " +
            "LEFT JOIN FETCH s.classRoom " +
            "LEFT JOIN FETCH s.department " +
            "LEFT JOIN FETCH s.studentSubjects ss " +
            "LEFT JOIN FETCH ss.subject",
            countQuery = "SELECT COUNT(DISTINCT s) FROM Student s")
    Page<Student> findAllWithAssociations(Pageable pageable);


}