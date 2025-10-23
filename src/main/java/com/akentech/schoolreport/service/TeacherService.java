package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Teacher;
import com.akentech.schoolreport.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Optional<Teacher> getTeacherById(Long id) {
        return teacherRepository.findById(id);
    }

    @Transactional
    public Teacher saveTeacher(Teacher teacher) {
        if (teacher.getTeacherId() == null) {
            String teacherId = generateTeacherId();
            teacher.setTeacherId(teacherId);
        }

        Teacher saved = teacherRepository.save(teacher);
        log.info("Saved teacher: {} {} (ID: {})",
                saved.getFirstName(),
                saved.getLastName(),
                saved.getTeacherId());
        return saved;
    }

    public void deleteTeacher(Long id) {
        teacherRepository.deleteById(id);
        log.info("Deleted teacher id: {}", id);
    }

    public long getTeacherCount() {
        return teacherRepository.count();
    }

    private String generateTeacherId() {
        long count = teacherRepository.count() + 1;
        return String.format("TC%010d", count);
    }

    // Helper method to get full name
    public String getTeacherFullName(Teacher teacher) {
        return teacher.getFirstName() + " " + teacher.getLastName();
    }
}