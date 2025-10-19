package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    public List<Student> getByClass(ClassRoom classRoom) {
        return studentRepository.findByClassRoom(classRoom);
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student save(Student student) {
        // Auto-generate roll number based on class and department
        if (student.getRollNumber() == null || student.getRollNumber().isEmpty()) {
            String classCode = student.getClassRoom().getName().replaceAll("\\s+", "").toUpperCase();
            String deptCode = student.getDepartment() != null ? student.getDepartment().getName().substring(0,3).toUpperCase() : "GEN";

            // Count existing students in the same class and department
            long count = studentRepository.countByClassRoomAndDepartment(student.getClassRoom(), student.getDepartment()) + 1;
            String roll = classCode + "-" + deptCode + "-" + String.format("%03d", count);
            student.setRollNumber(roll);
        }

        Student s = studentRepository.save(student);
        log.info("Saved student: {} (id={}, roll={})", s.getFullName(), s.getId(), s.getRollNumber());
        return s;
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
        log.info("Deleted student id={}", id);
    }
}
