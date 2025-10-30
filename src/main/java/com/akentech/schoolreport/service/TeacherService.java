package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.Teacher;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.repository.TeacherRepository;
import com.akentech.schoolreport.util.IdGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;
    private final IdGenerationService idGenerationService;

    public TeacherService(TeacherRepository teacherRepository,
                          SubjectRepository subjectRepository,
                          ClassRoomRepository classRoomRepository,
                          IdGenerationService idGenerationService) {
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.classRoomRepository = classRoomRepository;
        this.idGenerationService = idGenerationService;
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Teacher> getAllTeachers(Pageable pageable) {
        return teacherRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Teacher> getTeachersByFilters(String firstName, String lastName, Long subjectId, Pageable pageable) {
        return teacherRepository.findByFilters(firstName, lastName, subjectId, pageable);
    }

    // REMOVED: getTeacherById - using getTeacherByIdOrThrow instead
    @Transactional(readOnly = true)
    public Teacher getTeacherByIdOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Teacher", id));
    }

    public Teacher createTeacher(Teacher teacher, List<Long> subjectIds, List<Long> classroomIds) {
        log.info("Creating new teacher: {} {}", teacher.getFirstName(), teacher.getLastName());

        // Generate teacher ID if not provided
        if (teacher.getTeacherId() == null || teacher.getTeacherId().trim().isEmpty()) {
            String teacherId = idGenerationService.generateTeacherId();
            teacher.setTeacherId(teacherId);
        }

        // Set subjects and classrooms
        setTeacherSubjects(teacher, subjectIds);
        setTeacherClassrooms(teacher, classroomIds);

        Teacher savedTeacher = teacherRepository.save(teacher);

        log.info("Created teacher: {} {} (ID: {}) with {} subjects and {} classrooms",
                savedTeacher.getFirstName(), savedTeacher.getLastName(),
                savedTeacher.getTeacherId(), savedTeacher.getSubjects().size(),
                savedTeacher.getClassrooms().size());

        return savedTeacher;
    }

    public Teacher updateTeacher(Long id, Teacher teacherDetails, List<Long> subjectIds, List<Long> classroomIds) {
        log.info("Updating teacher with id: {}", id);

        Teacher existingTeacher = getTeacherByIdOrThrow(id);

        // Update basic information
        existingTeacher.setFirstName(teacherDetails.getFirstName());
        existingTeacher.setLastName(teacherDetails.getLastName());
        existingTeacher.setGender(teacherDetails.getGender());
        existingTeacher.setContact(teacherDetails.getContact());
        existingTeacher.setSkills(teacherDetails.getSkills());

        // Update relationships
        setTeacherSubjects(existingTeacher, subjectIds);
        setTeacherClassrooms(existingTeacher, classroomIds);

        Teacher updatedTeacher = teacherRepository.save(existingTeacher);

        log.info("Updated teacher: {} {} (ID: {}) with {} subjects and {} classrooms",
                updatedTeacher.getFirstName(), updatedTeacher.getLastName(),
                updatedTeacher.getTeacherId(), updatedTeacher.getSubjects().size(),
                updatedTeacher.getClassrooms().size());

        return updatedTeacher;
    }

    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherByIdOrThrow(id);
        teacherRepository.delete(teacher);
        log.info("Deleted teacher with id: {}", id);
    }

    private void setTeacherSubjects(Teacher teacher, List<Long> subjectIds) {
        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> selectedSubjects = subjectRepository.findAllById(subjectIds);
            teacher.setSubjects(selectedSubjects);
        } else {
            teacher.setSubjects(new ArrayList<>());
        }
    }

    private void setTeacherClassrooms(Teacher teacher, List<Long> classroomIds) {
        if (classroomIds != null && !classroomIds.isEmpty()) {
            List<ClassRoom> selectedClassrooms = classRoomRepository.findAllById(classroomIds);
            teacher.setClassrooms(selectedClassrooms);
        } else {
            teacher.setClassrooms(new ArrayList<>());
        }
    }

    // FIXED: This method is used in Dashboard, so we keep it
    @Transactional(readOnly = true)
    public long getTeacherCount() {
        return teacherRepository.count();
    }

    @Transactional(readOnly = true)
    public String generateTeacherId() {
        return idGenerationService.generateTeacherId();
    }
}