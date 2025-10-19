package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ClassRoom;
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
        // Minimal validation: ensure no duplicate roll in same class
        studentRepository.findByRollNumberAndClassRoom(student.getRollNumber(), student.getClassRoom())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(student.getId())) {
                        throw new IllegalArgumentException("Duplicate roll number in same class");
                    }
                });
        Student s = studentRepository.save(student);
        log.info("Saved student: {} (id={})", s.getFullName(), s.getId());
        return s;
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
        log.info("Deleted student id={}", id);
    }
}
