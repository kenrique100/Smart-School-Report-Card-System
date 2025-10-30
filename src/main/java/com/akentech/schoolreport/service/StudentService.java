package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.util.IdGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final IdGenerationService idGenerationService;
    private final StudentEnrollmentService enrollmentService;
    private final SpecialtyService specialtyService;
    private final SubjectService subjectService;

    public StudentService(StudentRepository studentRepository,
                          IdGenerationService idGenerationService,
                          StudentEnrollmentService enrollmentService,
                          SpecialtyService specialtyService,
                          SubjectService subjectService) {
        this.studentRepository = studentRepository;
        this.idGenerationService = idGenerationService;
        this.enrollmentService = enrollmentService;
        this.specialtyService = specialtyService;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Student> getStudentsByFilters(String firstName, String lastName, Long classRoomId,
                                              Long departmentId, String specialty, Pageable pageable) {
        return studentRepository.findByFilters(firstName, lastName, classRoomId, departmentId, specialty, pageable);
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByClass(ClassRoom classRoom) {
        return studentRepository.findByClassRoom(classRoom);
    }

    @Transactional(readOnly = true)
    public Student getStudentByIdOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student", id));
    }

    public Student createStudent(Student student, List<Long> subjectIds) {
        log.info("Creating new student: {} {}", student.getFirstName(), student.getLastName());

        validateStudent(student);

        // Generate IDs safely
        String studentId = idGenerationService.generateStudentId(student);
        String rollNumber = idGenerationService.generateRollNumber(student);

        // Ensure uniqueness before saving
        studentId = ensureUniqueStudentId(studentId);
        rollNumber = ensureUniqueRollNumber(rollNumber, student.getClassRoom());

        student.setStudentId(studentId);
        student.setRollNumber(rollNumber);

        // Save and enroll
        Student savedStudent = studentRepository.save(student);

        if (subjectIds != null && !subjectIds.isEmpty()) {
            enrollmentService.enrollStudentInSubjects(savedStudent, subjectIds);
        }

        log.info("Created student: {} {} (ID: {}, Roll: {})",
                savedStudent.getFirstName(), savedStudent.getLastName(),
                savedStudent.getStudentId(), savedStudent.getRollNumber());

        return savedStudent;
    }

    /**
     * Ensures studentId is unique. If not, regenerates until unique.
     */
    private String ensureUniqueStudentId(String baseId) {
        String newId = baseId;
        int attempt = 1;
        while (studentRepository.findByStudentId(newId).isPresent()) {
            newId = baseId + "-" + attempt++;
            log.warn("Duplicate studentId found. Regenerated as {}", newId);
        }
        return newId;
    }

    /**
     * Ensures rollNumber is unique within the same class.
     */
    private String ensureUniqueRollNumber(String baseRoll, ClassRoom classRoom) {
        String newRoll = baseRoll;
        int attempt = 1;
        while (studentRepository.findByRollNumberAndClassRoom(newRoll, classRoom).isPresent()) {
            newRoll = baseRoll + "-" + attempt++;
            log.warn("Duplicate rollNumber found for class {}. Regenerated as {}", classRoom.getCode(), newRoll);
        }
        return newRoll;
    }

    public Student updateStudent(Long id, Student studentDetails, List<Long> subjectIds) {
        log.info("Updating student with id: {}", id);

        Student existingStudent = getStudentByIdOrThrow(id);

        // Update basic information
        existingStudent.setFirstName(studentDetails.getFirstName());
        existingStudent.setLastName(studentDetails.getLastName());
        existingStudent.setClassRoom(studentDetails.getClassRoom());
        existingStudent.setDepartment(studentDetails.getDepartment());
        existingStudent.setSpecialty(studentDetails.getSpecialty());
        existingStudent.setGender(studentDetails.getGender());
        existingStudent.setDateOfBirth(studentDetails.getDateOfBirth());
        existingStudent.setEmail(studentDetails.getEmail());
        existingStudent.setAddress(studentDetails.getAddress());
        existingStudent.setAcademicYearStart(studentDetails.getAcademicYearStart());
        existingStudent.setAcademicYearEnd(studentDetails.getAcademicYearEnd());

        validateStudent(existingStudent);

        Student updatedStudent = studentRepository.save(existingStudent);

        // Update enrollments if provided
        if (subjectIds != null) {
            enrollmentService.enrollStudentInSubjects(updatedStudent, subjectIds);
        }

        log.info("Updated student: {} {} (ID: {})",
                updatedStudent.getFirstName(), updatedStudent.getLastName(),
                updatedStudent.getStudentId());

        return updatedStudent;
    }

    public void deleteStudent(Long id) {
        Student student = getStudentByIdOrThrow(id);
        studentRepository.delete(student);
        log.info("Deleted student with id: {}", id);
    }

    private void validateStudent(Student student) {
        validateSpecialtyRequirement(student);
        validateAcademicYears(student);
        validateEmailUniqueness(student);
    }

    private void validateSpecialtyRequirement(Student student) {
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null) {
            ClassLevel classLevel = student.getClassRoom().getCode();
            DepartmentCode departmentCode = student.getDepartment() != null ? student.getDepartment().getCode() : null;

            // For Forms 1-3 with General department, no specialty allowed
            if ((classLevel == ClassLevel.FORM_1 || classLevel == ClassLevel.FORM_2 || classLevel == ClassLevel.FORM_3) &&
                    student.getDepartment() != null && departmentCode == DepartmentCode.GEN) {
                student.setSpecialty(null);
            }

            // For Sixth Form with Science/Arts departments, specialty is required
            if ((classLevel == ClassLevel.LOWER_SIXTH || classLevel == ClassLevel.UPPER_SIXTH) &&
                    student.getDepartment() != null &&
                    (departmentCode == DepartmentCode.SCI || departmentCode == DepartmentCode.ART)) {

                if (student.getSpecialty() == null || student.getSpecialty().trim().isEmpty()) {
                    throw new BusinessRuleException("Specialty is required for Sixth Form Science/Arts students");
                }
            }
        }
    }

    private void validateAcademicYears(Student student) {
        if (student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
            if (student.getAcademicYearStart() >= student.getAcademicYearEnd()) {
                throw new BusinessRuleException("Academic year start must be before academic year end");
            }
        }
    }

    private void validateEmailUniqueness(Student student) {
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            studentRepository.findByStudentId(student.getStudentId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(student.getId())) {
                            throw new DataIntegrityException("Email already exists: " + student.getEmail());
                        }
                    });
        }
    }

    // Delegate methods to specialty service
    @Transactional(readOnly = true)
    public List<String> getAllSpecialties() {
        return specialtyService.getAllSpecialties();
    }

    @Transactional(readOnly = true)
    public SpecialtyService.SpecialtyRequirement checkSpecialtyRequirement(String classCode, String departmentCode) {
        return specialtyService.checkSpecialtyRequirement(classCode, departmentCode);
    }

    // Delegate methods to subject service
    @Transactional(readOnly = true)
    public List<Subject> getAvailableSubjectsForStudent(Student student) {
        Set<Subject> availableSubjects = new HashSet<>();

        // Add compulsory subjects for Forms 1-5
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null) {
            ClassLevel classLevel = student.getClassRoom().getCode();
            if (classLevel.isFormLevel()) {
                availableSubjects.addAll(subjectService.getCompulsorySubjectsForClass(
                        classLevel.getCode()
                ));
            }
        }

        // Add department subjects based on specialty
        if (student.getDepartment() != null) {
            if (student.getSpecialty() != null && !student.getSpecialty().isEmpty()) {
                availableSubjects.addAll(subjectService.getSubjectsByDepartmentAndSpecialty(
                        student.getDepartment().getId(), student.getSpecialty()
                ));
            } else {
                availableSubjects.addAll(subjectService.getSubjectsByDepartment(
                        student.getDepartment().getId()
                ));
            }
        }

        // Add optional Computer Science
        List<Subject> computerScience = subjectService.getAllSubjects().stream()
                .filter(s -> "Computer Science".equals(s.getName()))
                .toList();
        availableSubjects.addAll(computerScience);

        return new ArrayList<>(availableSubjects);
    }

    // Delegate methods to enrollment service
    @Transactional(readOnly = true)
    public List<StudentSubject> getStudentSubjects(Long studentId) {
        return enrollmentService.getStudentEnrollments(studentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentSubjectsSummary(Long studentId) {
        List<StudentSubject> enrollments = getStudentSubjects(studentId);
        Map<String, Object> summary = new HashMap<>();

        int totalSubjects = enrollments.size();
        int subjectsWithScores = (int) enrollments.stream()
                .filter(ss -> ss.getScore() != null)
                .count();
        int compulsorySubjects = (int) enrollments.stream()
                .filter(StudentSubject::getIsCompulsory)
                .count();

        summary.put("totalSubjects", totalSubjects);
        summary.put("subjectsWithScores", subjectsWithScores);
        summary.put("compulsorySubjects", compulsorySubjects);
        summary.put("studentSubjects", enrollments); // CRITICAL: Add the actual list

        return summary;
    }

    // New methods for grouped subjects
    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedAvailableSubjects(Student student) {
        List<Subject> availableSubjects = getAvailableSubjectsForStudent(student);
        Map<String, List<Subject>> groupedSubjects = new LinkedHashMap<>();

        // Group by category
        List<Subject> compulsorySubjects = new ArrayList<>();
        List<Subject> departmentSubjects = new ArrayList<>();
        List<Subject> optionalSubjects = new ArrayList<>();

        for (Subject subject : availableSubjects) {
            if (isCompulsorySubject(subject, student.getClassRoom())) {
                compulsorySubjects.add(subject);
            } else if ("Computer Science".equals(subject.getName())) {
                optionalSubjects.add(subject);
            } else {
                departmentSubjects.add(subject);
            }
        }

        groupedSubjects.put("compulsory", compulsorySubjects);
        groupedSubjects.put("department", departmentSubjects);
        groupedSubjects.put("optional", optionalSubjects);

        return groupedSubjects;
    }

    private boolean isCompulsorySubject(Subject subject, ClassRoom classRoom) {
        if (classRoom != null && classRoom.getCode() != null) {
            ClassLevel classLevel = classRoom.getCode();
            if (classLevel.isFormLevel()) {
                List<String> compulsoryNames = Arrays.asList("Mathematics", "English Language", "French Language");
                return compulsoryNames.contains(subject.getName());
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByStudentSelection(Long studentId) {
        return getStudentSubjects(studentId).stream()
                .map(StudentSubject::getSubject)
                .collect(Collectors.toList());
    }

    // Additional utility methods
    @Transactional(readOnly = true)
    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return studentRepository.findByNameContaining(query.trim());
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByDepartment(Department department) {
        return studentRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(department.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsBySpecialty(String specialty) {
        return studentRepository.findBySpecialty(specialty);
    }

    // Statistics methods - FIXED: Use existing repository method
    @Transactional(readOnly = true)
    public long countByClassRoom(ClassRoom classRoom) {
        if (classRoom == null || classRoom.getId() == null) {
            return 0L;
        }
        return studentRepository.countByClassRoomId(classRoom.getId());
    }

    @Transactional(readOnly = true)
    public long countByDepartment(Department department) {
        return studentRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(department.getId()))
                .count();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getGenderStatistics() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getGender() != null)
                .collect(Collectors.groupingBy(
                        student -> student.getGender().name(),
                        Collectors.counting()
                ));
    }
    @Transactional(readOnly = true)
    public Map<String, Long> getGenderStatisticsByClassroom(ClassRoom classRoom) {
        List<Student> students = getStudentsByClass(classRoom);
        return students.stream()
                .filter(student -> student.getGender() != null)
                .collect(Collectors.groupingBy(
                        student -> student.getGender().name(),
                        Collectors.counting()
                ));
    }
    @Transactional(readOnly = true)
    public long getStudentCount() {
        return studentRepository.count();
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedSubjectsForStudent(Long studentId) {
        Student student = getStudentByIdOrThrow(studentId);
        return getGroupedAvailableSubjects(student);
    }

    @Transactional(readOnly = true)
    public List<Subject> getAvailableSubjectsForStudentView(Long studentId) {
        Student student = getStudentByIdOrThrow(studentId);
        return getAvailableSubjectsForStudent(student);
    }

    // Enhanced method to ensure proper subject filtering
    @Transactional(readOnly = true)
    public List<Subject> getAvailableSubjects(String classCode, Long departmentId, String specialty) {
        if (classCode == null || departmentId == null) {
            return new ArrayList<>();
        }

        return subjectService.getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
    }
}