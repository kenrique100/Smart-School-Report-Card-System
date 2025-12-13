package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.enums.ClassLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    Optional<ClassRoom> findByName(String name);

    Optional<ClassRoom> findByCode(ClassLevel code);

    @Query("SELECT c FROM ClassRoom c WHERE c.academicYear = :academicYear")
    List<ClassRoom> findByAcademicYear(@Param("academicYear") String academicYear);

    @Query("SELECT c FROM ClassRoom c WHERE c.department.id = :departmentId")
    List<ClassRoom> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT DISTINCT c.academicYear FROM ClassRoom c ORDER BY c.academicYear DESC")
    List<String> findDistinctAcademicYears();

    @Query("SELECT DISTINCT c FROM ClassRoom c LEFT JOIN FETCH c.students WHERE c.id = :id")
    Optional<ClassRoom> findByIdWithStudents(Long id);

    // Add this method to fetch all classes with students
    @Query("SELECT DISTINCT c FROM ClassRoom c LEFT JOIN FETCH c.students")
    List<ClassRoom> findAllWithStudents();
}