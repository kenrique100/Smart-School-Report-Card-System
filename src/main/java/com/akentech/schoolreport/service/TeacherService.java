package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.Teacher;
import com.akentech.schoolreport.model.enums.ClassLevel;
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
import java.util.stream.Collectors;

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

    // ENHANCED: Teacher creation with subject and classroom validation
    public Teacher createTeacher(Teacher teacher, List<Long> subjectIds, List<Long> classroomIds) {
        log.info("Creating new teacher: {} {}", teacher.getFirstName(), teacher.getLastName());

        // Generate teacher ID if not provided
        if (teacher.getTeacherId() == null || teacher.getTeacherId().trim().isEmpty()) {
            String teacherId = idGenerationService.generateTeacherId();
            teacher.setTeacherId(teacherId);
        }

        // Validate and set subjects and classrooms
        setTeacherSubjectsWithValidation(teacher, subjectIds);
        setTeacherClassroomsWithValidation(teacher, classroomIds);

        Teacher savedTeacher = teacherRepository.save(teacher);

        log.info("Created teacher: {} {} (ID: {}) with {} subjects and {} classrooms",
                savedTeacher.getFirstName(), savedTeacher.getLastName(),
                savedTeacher.getTeacherId(), savedTeacher.getSubjects().size(),
                savedTeacher.getClassrooms().size());

        return savedTeacher;
    }

    // NEW: Validate subjects against teacher's skills and department
    private void setTeacherSubjectsWithValidation(Teacher teacher, List<Long> subjectIds) {
        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> selectedSubjects = subjectRepository.findAllById(subjectIds);

            // Validate that teacher has skills for the subjects
            List<Subject> validSubjects = selectedSubjects.stream()
                    .filter(subject -> hasTeacherSkillForSubject(teacher, subject))
                    .collect(Collectors.toList());

            if (validSubjects.size() < selectedSubjects.size()) {
                log.warn("Teacher {} lacks skills for some selected subjects", teacher.getTeacherId());
            }

            teacher.setSubjects(validSubjects);
        } else {
            teacher.setSubjects(new ArrayList<>());
        }
    }

    // NEW: Check if teacher has skills for subject
    private boolean hasTeacherSkillForSubject(Teacher teacher, Subject subject) {
        if (teacher.getSkills() == null || subject.getName() == null) {
            return false;
        }

        String subjectName = subject.getName();
        String teacherSkills = teacher.getSkills().toLowerCase();

        // Check if subject name or key terms appear in teacher's skills
        return teacherSkills.contains(subjectName.toLowerCase()) ||
                teacherSkills.contains(subjectName.replace("O-", "").replace("A-", "").toLowerCase()) ||
                hasRelatedSkill(teacherSkills, subjectName);
    }

    // NEW: Check for related skills
    private boolean hasRelatedSkill(String teacherSkills, String subjectName) {
        // Map subject names to related skill keywords
        if (subjectName.contains("Mathematics") && teacherSkills.contains("math")) return true;
        if (subjectName.contains("Physics") && teacherSkills.contains("physic")) return true;
        if (subjectName.contains("Chemistry") && teacherSkills.contains("chem")) return true;
        if (subjectName.contains("Biology") && teacherSkills.contains("bio")) return true;
        if (subjectName.contains("History") && teacherSkills.contains("hist")) return true;
        if (subjectName.contains("Geography") && teacherSkills.contains("geo")) return true;
        if (subjectName.contains("Literature") && teacherSkills.contains("liter")) return true;
        if (subjectName.contains("Accounting") && teacherSkills.contains("account")) return true;
        if (subjectName.contains("Commerce") && teacherSkills.contains("commer")) return true;
        if (subjectName.contains("Economics") && teacherSkills.contains("econ")) return true;

        return false;
    }

    // NEW: Validate classrooms against teacher's subject levels
    private void setTeacherClassroomsWithValidation(Teacher teacher, List<Long> classroomIds) {
        if (classroomIds != null && !classroomIds.isEmpty()) {
            List<ClassRoom> selectedClassrooms = classRoomRepository.findAllById(classroomIds);

            // Validate that teacher's subjects are appropriate for classroom levels
            List<ClassRoom> validClassrooms = selectedClassrooms.stream()
                    .filter(classroom -> isTeacherSuitableForClassroom(teacher, classroom))
                    .collect(Collectors.toList());

            if (validClassrooms.size() < selectedClassrooms.size()) {
                log.warn("Teacher {} has subjects unsuitable for some selected classrooms", teacher.getTeacherId());
            }

            teacher.setClassrooms(validClassrooms);
        } else {
            teacher.setClassrooms(new ArrayList<>());
        }
    }

    // NEW: Check if teacher is suitable for classroom level
    private boolean isTeacherSuitableForClassroom(Teacher teacher, ClassRoom classroom) {
        if (teacher.getSubjects() == null || teacher.getSubjects().isEmpty()) {
            return true; // Teacher without subjects can teach any class
        }

        ClassLevel classLevel = classroom.getCode();

        // Check if teacher has subjects appropriate for the class level
        return teacher.getSubjects().stream()
                .anyMatch(subject -> isSubjectForClassLevel(subject, classLevel));
    }

    // NEW: Check if subject is appropriate for class level
    private boolean isSubjectForClassLevel(Subject subject, ClassLevel classLevel) {
        if (subject.getName() == null) {
            return false;
        }

        String subjectName = subject.getName();

        if (classLevel.isSixthForm()) {
            // Sixth Form: A-Level subjects
            return subjectName.startsWith("A-") ||
                    (!subjectName.startsWith("O-") &&
                            subject.getDepartment() != null &&
                            !subject.getDepartment().getCode().name().equals("GEN"));
        } else {
            // Forms 1-5: O-Level subjects
            return subjectName.startsWith("O-") ||
                    (!subjectName.startsWith("A-") &&
                            (subject.getDepartment() == null ||
                                    subject.getDepartment().getCode().name().equals("GEN")));
        }
    }

    // ENHANCED: Get teachers by subject and class level
    @Transactional(readOnly = true)
    public List<Teacher> getTeachersBySubjectAndClassLevel(Long subjectId, String classCode) {
        if (subjectId == null || classCode == null) {
            return new ArrayList<>();
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject", subjectId));

        ClassLevel classLevel = ClassLevel.fromCode(classCode);

        return teacherRepository.findAll().stream()
                .filter(teacher -> teacher.getSubjects().contains(subject) &&
                        isTeacherSuitableForClassLevel(teacher, classLevel))
                .collect(Collectors.toList());
    }

    // NEW: Check if teacher is suitable for class level
    private boolean isTeacherSuitableForClassLevel(Teacher teacher, ClassLevel classLevel) {
        if (teacher.getSubjects() == null || teacher.getSubjects().isEmpty()) {
            return true;
        }

        return teacher.getSubjects().stream()
                .anyMatch(subject -> isSubjectForClassLevel(subject, classLevel));
    }

    // ENHANCED: Update teacher with validation
    public Teacher updateTeacher(Long id, Teacher teacherDetails, List<Long> subjectIds, List<Long> classroomIds) {
        log.info("Updating teacher with id: {}", id);

        Teacher existingTeacher = getTeacherByIdOrThrow(id);

        // Update basic information
        existingTeacher.setFirstName(teacherDetails.getFirstName());
        existingTeacher.setLastName(teacherDetails.getLastName());
        existingTeacher.setGender(teacherDetails.getGender());
        existingTeacher.setContact(teacherDetails.getContact());
        existingTeacher.setSkills(teacherDetails.getSkills());

        // Update relationships with validation
        setTeacherSubjectsWithValidation(existingTeacher, subjectIds);
        setTeacherClassroomsWithValidation(existingTeacher, classroomIds);

        Teacher updatedTeacher = teacherRepository.save(existingTeacher);

        log.info("Updated teacher: {} {} (ID: {}) with {} subjects and {} classrooms",
                updatedTeacher.getFirstName(), updatedTeacher.getLastName(),
                updatedTeacher.getTeacherId(), updatedTeacher.getSubjects().size(),
                updatedTeacher.getClassrooms().size());

        return updatedTeacher;
    }

    // Rest of the existing methods remain the same...
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

    @Transactional(readOnly = true)
    public Teacher getTeacherByIdOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Teacher", id));
    }

    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherByIdOrThrow(id);
        teacherRepository.delete(teacher);
        log.info("Deleted teacher with id: {}", id);
    }

    @Transactional(readOnly = true)
    public long getTeacherCount() {
        return teacherRepository.count();
    }

    @Transactional(readOnly = true)
    public String generateTeacherId() {
        return idGenerationService.generateTeacherId();
    }
}