package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByClassRoom(ClassRoom classRoom);
    Optional<Student> findByRollNumberAndClassRoom(String rollNumber, ClassRoom classRoom);
    long countByClassRoomAndDepartment(ClassRoom classRoom, Department department);
}
