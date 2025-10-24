package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Teacher;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.repository.TeacherRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Optional<Teacher> getTeacherById(Long id) {
        return teacherRepository.findById(id);
    }

    @Transactional
    public Teacher saveTeacher(Teacher teacher, List<Long> subjectIds, List<Long> classroomIds) {
        // Auto-generate teacher ID if not provided or empty
        if (teacher.getTeacherId() == null || teacher.getTeacherId().trim().isEmpty()) {
            String teacherId = generateTeacherId();
            teacher.setTeacherId(teacherId);
            log.info("Auto-generated teacher ID: {}", teacherId);
        }

        // Handle subjects
        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> selectedSubjects = subjectRepository.findAllById(subjectIds);
            teacher.setSubjects(selectedSubjects);
        } else {
            teacher.setSubjects(new ArrayList<>());
        }

        // Handle classrooms
        if (classroomIds != null && !classroomIds.isEmpty()) {
            List<ClassRoom> selectedClassrooms = classRoomRepository.findAllById(classroomIds);
            teacher.setClassrooms(selectedClassrooms);
        } else {
            teacher.setClassrooms(new ArrayList<>());
        }

        Teacher saved = teacherRepository.save(teacher);
        log.info("Saved teacher: {} {} (ID: {}) with {} subjects and {} classrooms",
                saved.getFirstName(),
                saved.getLastName(),
                saved.getTeacherId(),
                saved.getSubjects().size(),
                saved.getClassrooms().size());
        return saved;
    }

    @Transactional
    public Teacher updateTeacher(Teacher teacher, List<Long> subjectIds, List<Long> classroomIds) {
        // Get existing teacher to preserve IDs and teacherId
        Teacher existingTeacher = teacherRepository.findById(teacher.getId())
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacher.getId()));

        // Update basic fields
        existingTeacher.setFirstName(teacher.getFirstName());
        existingTeacher.setLastName(teacher.getLastName());
        existingTeacher.setDateOfBirth(teacher.getDateOfBirth());
        existingTeacher.setGender(teacher.getGender());
        existingTeacher.setEmail(teacher.getEmail());
        existingTeacher.setAddress(teacher.getAddress());
        existingTeacher.setContact(teacher.getContact());
        existingTeacher.setSkills(teacher.getSkills());

        // Handle subjects
        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> selectedSubjects = subjectRepository.findAllById(subjectIds);
            existingTeacher.setSubjects(selectedSubjects);
        } else {
            existingTeacher.setSubjects(new ArrayList<>());
        }

        // Handle classrooms
        if (classroomIds != null && !classroomIds.isEmpty()) {
            List<ClassRoom> selectedClassrooms = classRoomRepository.findAllById(classroomIds);
            existingTeacher.setClassrooms(selectedClassrooms);
        } else {
            existingTeacher.setClassrooms(new ArrayList<>());
        }

        Teacher updated = teacherRepository.save(existingTeacher);
        log.info("Updated teacher: {} {} (ID: {}) with {} subjects and {} classrooms",
                updated.getFirstName(),
                updated.getLastName(),
                updated.getTeacherId(),
                updated.getSubjects().size(),
                updated.getClassrooms().size());
        return updated;
    }

    @Transactional
    public Teacher saveTeacher(Teacher teacher) {
        // Overloaded method for backward compatibility
        return saveTeacher(teacher, null, null);
    }

    public void deleteTeacher(Long id) {
        teacherRepository.deleteById(id);
        log.info("Deleted teacher id: {}", id);
    }

    public long getTeacherCount() {
        return teacherRepository.count();
    }

    public String generateTeacherId() {
        long count = teacherRepository.count() + 1;
        return String.format("TC%010d", count);
    }

    // Helper method to get full name
    public String getTeacherFullName(Teacher teacher) {
        return teacher.getFirstName() + " " + teacher.getLastName();
    }

    // Helper method to get subjects as comma-separated string
    public String getSubjectsAsString(Teacher teacher) {
        if (teacher.getSubjects() == null || teacher.getSubjects().isEmpty()) {
            return "No subjects assigned";
        }
        return teacher.getSubjects().stream()
                .map(Subject::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No subjects assigned");
    }

    // Helper method to get classrooms as comma-separated string
    public String getClassroomsAsString(Teacher teacher) {
        if (teacher.getClassrooms() == null || teacher.getClassrooms().isEmpty()) {
            return "No classrooms assigned";
        }
        return teacher.getClassrooms().stream()
                .map(ClassRoom::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("No classrooms assigned");
    }
}