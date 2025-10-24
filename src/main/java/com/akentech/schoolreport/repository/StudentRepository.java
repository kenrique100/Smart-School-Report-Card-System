package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByClassRoom(ClassRoom classRoom);
    Optional<Student> findByRollNumberAndClassRoom(String rollNumber, ClassRoom classRoom);
    long countByClassRoomAndDepartment(ClassRoom classRoom, Department department);
    long countByClassRoomAndDepartmentAndSpecialty(ClassRoom classRoom, Department department, String specialty);
    Optional<Student> findByStudentId(String studentId);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId")
    List<Student> findByClassRoomId(@Param("classRoomId") Long classRoomId);

    @Query("SELECT s FROM Student s WHERE s.firstName LIKE %:name% OR s.lastName LIKE %:name%")
    List<Student> findByNameContaining(@Param("name") String name);

    @Query("SELECT s FROM Student s WHERE s.classRoom.id = :classRoomId AND s.gender = :gender")
    List<Student> findByClassRoomIdAndGender(@Param("classRoomId") Long classRoomId, @Param("gender") String gender);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.classRoom.id = :classRoomId")
    long countByClassRoomId(@Param("classRoomId") Long classRoomId);

    List<Student> findBySpecialty(String specialty);
}