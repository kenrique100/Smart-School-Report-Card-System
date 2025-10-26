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

    // ADD THIS MISSING METHOD
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

    @Query("SELECT s FROM Student s WHERE " +
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

    // Add method to get all students with proper joins for sorting
    @Query("SELECT s FROM Student s LEFT JOIN s.classRoom c LEFT JOIN s.department d " +
            "ORDER BY s.firstName, s.lastName")
    List<Student> findAllWithJoins();

    // Add method for sorting by class
    @Query("SELECT s FROM Student s LEFT JOIN s.classRoom c ORDER BY c.name")
    List<Student> findAllOrderByClassName();

    // Add method for sorting by department
    @Query("SELECT s FROM Student s LEFT JOIN s.department d ORDER BY d.name")
    List<Student> findAllOrderByDepartmentName();

    // Add method for sorting by specialty
    @Query("SELECT s FROM Student s ORDER BY s.specialty")
    List<Student> findAllOrderBySpecialty();
}