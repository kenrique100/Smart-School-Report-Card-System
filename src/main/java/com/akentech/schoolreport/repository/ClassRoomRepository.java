package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    Optional<ClassRoom> findByName(String name);

    Optional<ClassRoom> findByCode(String code);

    @Query("SELECT c FROM ClassRoom c WHERE c.academicYear = :academicYear")
    List<ClassRoom> findByAcademicYear(@Param("academicYear") String academicYear);

    @Query("SELECT c FROM ClassRoom c WHERE c.department.id = :departmentId")
    List<ClassRoom> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT DISTINCT c.academicYear FROM ClassRoom c ORDER BY c.academicYear DESC")
    List<String> findDistinctAcademicYears();
}