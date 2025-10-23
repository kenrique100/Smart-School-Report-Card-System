package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Student> getByClass(ClassRoom classRoom) {
        return studentRepository.findByClassRoom(classRoom);
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student saveStudent(Student student) {
        log.info("Saving student: {} {}", student.getFirstName(), student.getLastName());

        // For new students, generate IDs
        if (student.getId() == null) {
            log.info("Creating new student");
            // Auto-generate student ID and roll number for new students
            if (student.getStudentId() == null) {
                String studentId = generateStudentId();
                student.setStudentId(studentId);
                log.info("Generated student ID: {}", studentId);
            }

            if (student.getRollNumber() == null || student.getRollNumber().isEmpty()) {
                String rollNumber = generateRollNumber(student);
                student.setRollNumber(rollNumber);
                log.info("Generated roll number: {}", rollNumber);
            }
        } else {
            // For existing students, preserve the existing studentId and rollNumber
            log.info("Updating existing student ID: {}", student.getId());
            Optional<Student> existingStudent = studentRepository.findById(student.getId());
            if (existingStudent.isPresent()) {
                Student existing = existingStudent.get();
                student.setStudentId(existing.getStudentId());
                student.setRollNumber(existing.getRollNumber());
                log.info("Preserved existing IDs - Student ID: {}, Roll: {}",
                        existing.getStudentId(), existing.getRollNumber());
            }
        }

        Student savedStudent = studentRepository.save(student);
        log.info("Successfully saved student: {} {} (ID: {}, Roll: {})",
                savedStudent.getFirstName(),
                savedStudent.getLastName(),
                savedStudent.getStudentId(),
                savedStudent.getRollNumber());
        return savedStudent;
    }

    private String generateStudentId() {
        long count = studentRepository.count() + 1;
        return String.format("STU%06d", count);
    }

    private String generateRollNumber(Student student) {
        ClassRoom classRoom = student.getClassRoom();
        Department department = student.getDepartment();

        String classCode = "CL";
        if (classRoom != null) {
            classCode = classRoom.getCode() != null ?
                    classRoom.getCode().replaceAll("\\s+", "").toUpperCase() :
                    classRoom.getName().substring(0, 2).toUpperCase();
        }

        String deptCode = "GEN";
        if (department != null) {
            deptCode = department.getCode() != null ?
                    department.getCode().toUpperCase() :
                    department.getName().substring(0, 3).toUpperCase();
        }

        long count = studentRepository.countByClassRoomAndDepartment(classRoom, department);
        String sequence = String.format("%03d", count + 1);

        return classCode + deptCode + sequence;
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
            log.info("Successfully deleted student id: {}", id);
        } else {
            log.warn("Attempted to delete non-existent student id: {}", id);
            throw new IllegalArgumentException("Student not found with id: " + id);
        }
    }

    public long getStudentCount() {
        return studentRepository.count();
    }

    public String getStudentFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }
}