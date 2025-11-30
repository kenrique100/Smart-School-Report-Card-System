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

    // ENHANCED: Student creation with subject selection
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

        // Save student first
        Student savedStudent = studentRepository.save(student);

        // Handle subject enrollment
        if (subjectIds != null && !subjectIds.isEmpty()) {
            try {
                enrollmentService.enrollStudentInSubjects(savedStudent, subjectIds);
                log.info("Enrolled student {} in {} subjects", savedStudent.getStudentId(), subjectIds.size());
            } catch (Exception e) {
                log.error("Failed to enroll student in subjects: {}", e.getMessage());
                throw new BusinessRuleException("Failed to enroll student in subjects: " + e.getMessage());
            }
        } else {
            // Auto-assign appropriate subjects if none provided
            List<Long> autoSubjectIds = getAutoAssignedSubjectIds(savedStudent);
            if (!autoSubjectIds.isEmpty()) {
                enrollmentService.enrollStudentInSubjects(savedStudent, autoSubjectIds);
                log.info("Auto-assigned {} subjects for student {}", autoSubjectIds.size(), savedStudent.getStudentId());
            }
        }

        log.info("Created student: {} {} (ID: {}, Roll: {})",
                savedStudent.getFirstName(), savedStudent.getLastName(),
                savedStudent.getStudentId(), savedStudent.getRollNumber());

        return savedStudent;
    }

    // NEW: Get filtered subjects for student selection
    @Transactional(readOnly = true)
    public List<Subject> getFilteredSubjectsForStudent(String classCode, Long departmentId, String specialty) {
        if (classCode == null || departmentId == null) {
            log.warn("Class code or department ID is null - class: {}, department: {}", classCode, departmentId);
            return new ArrayList<>();
        }

        try {
            log.info("Getting filtered subjects for class: {}, department: {}, specialty: {}",
                    classCode, departmentId, specialty);

            List<Subject> subjects = subjectService.getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
            log.info("Found {} filtered subjects for class: {}, department: {}, specialty: {}",
                    subjects.size(), classCode, departmentId, specialty);

            return subjects;
        } catch (Exception e) {
            log.error("Error getting filtered subjects for class: {}, department: {}, specialty: {}. Error: {}",
                    classCode, departmentId, specialty, e.getMessage());
            return new ArrayList<>();
        }
    }

    // NEW: Added getAvailableSubjects method for backward compatibility
    @Transactional(readOnly = true)
    public List<Subject> getAvailableSubjects(String classCode, Long departmentId, String specialty) {
        return getFilteredSubjectsForStudent(classCode, departmentId, specialty);
    }

    // NEW: Get grouped subjects for checkbox display
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupedSubjectsForSelection(String classCode, Long departmentId, String specialty) {
        Map<String, Object> result = new HashMap<>();

        if (classCode == null || departmentId == null) {
            result.put("success", false);
            result.put("error", "Class and department are required");
            return result;
        }

        try {
            Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(
                    classCode, departmentId, specialty);

            result.put("success", true);
            result.put("groupedSubjects", groupedSubjects);

            // Calculate counts for UI
            int totalSubjects = groupedSubjects.values().stream()
                    .mapToInt(List::size)
                    .sum();
            result.put("totalSubjects", totalSubjects);

            log.debug("Returning {} grouped subjects for selection", totalSubjects);

        } catch (Exception e) {
            log.error("Error getting grouped subjects for selection: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "Failed to load subjects: " + e.getMessage());
        }

        return result;
    }

    // Auto-assign subjects based on class, department, and specialty
    private List<Long> getAutoAssignedSubjectIds(Student student) {
        List<Subject> appropriateSubjects = getAppropriateSubjectsForStudent(student);
        return appropriateSubjects.stream()
                .map(Subject::getId)
                .collect(Collectors.toList());
    }

    // Get appropriate subjects for student
    private List<Subject> getAppropriateSubjectsForStudent(Student student) {
        if (student.getClassRoom() == null || student.getDepartment() == null) {
            return new ArrayList<>();
        }

        String classCode = student.getClassRoom().getCode().name();
        Long departmentId = student.getDepartment().getId();
        String specialty = student.getSpecialty();

        // Get grouped subjects
        Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(
                classCode, departmentId, specialty);

        List<Subject> autoAssignedSubjects = new ArrayList<>();

        // Always add compulsory subjects
        autoAssignedSubjects.addAll(groupedSubjects.getOrDefault("compulsory", new ArrayList<>()));

        // For Forms 4-5 and Sixth Form, add department core subjects
        ClassLevel classLevel = student.getClassRoom().getCode();
        if ((classLevel == ClassLevel.FORM_4 || classLevel == ClassLevel.FORM_5 || classLevel.isSixthForm())) {
            autoAssignedSubjects.addAll(groupedSubjects.getOrDefault("department", new ArrayList<>()));
        }

        // For Sixth Form with specialty, add specialty subjects
        if (classLevel.isSixthForm() && specialty != null && !specialty.trim().isEmpty()) {
            autoAssignedSubjects.addAll(groupedSubjects.getOrDefault("specialty", new ArrayList<>()));
        }

        log.debug("Auto-assigned {} subjects for student {}", autoAssignedSubjects.size(), student.getStudentId());
        return autoAssignedSubjects;
    }

    // ENHANCED: Student validation with department and specialty rules
    private void validateStudent(Student student) {
        validateClassDepartmentRules(student);
        validateSpecialtyRequirement(student);
        validateAcademicYears(student);
        validateEmailUniqueness(student);
    }

    // Enhanced class and department validation
    private void validateClassDepartmentRules(Student student) {
        if (student.getClassRoom() == null || student.getClassRoom().getCode() == null) {
            throw new BusinessRuleException("Class room is required");
        }

        if (student.getDepartment() == null) {
            throw new BusinessRuleException("Department is required");
        }

        ClassLevel classLevel = student.getClassRoom().getCode();
        DepartmentCode departmentCode = student.getDepartment().getCode();

        // Forms 1-3: All departments allowed
        if (classLevel == ClassLevel.FORM_1 || classLevel == ClassLevel.FORM_2 || classLevel == ClassLevel.FORM_3) {
            // No specialty allowed for Forms 1-3
            if (student.getSpecialty() != null && !student.getSpecialty().trim().isEmpty()) {
                throw new BusinessRuleException("Specialty is not allowed for Forms 1-3");
            }
        }
        // Forms 4-5: All departments allowed, specialties optional
        else if (classLevel == ClassLevel.FORM_4 || classLevel == ClassLevel.FORM_5) {
            // Specialty is optional but must be valid if provided
            if (student.getSpecialty() != null && !student.getSpecialty().trim().isEmpty()) {
                validateSpecialtyForDepartment(student.getSpecialty(), departmentCode);
            }
        }
        // Sixth Form: Strict department and specialty rules
        else if (classLevel.isSixthForm()) {
            // Science and Arts departments require specialties
            if ((departmentCode == DepartmentCode.SCI || departmentCode == DepartmentCode.ART) &&
                    (student.getSpecialty() == null || student.getSpecialty().trim().isEmpty())) {
                throw new BusinessRuleException("Specialty is required for Sixth Form Science/Arts students");
            }

            // Validate specialty for department
            if (student.getSpecialty() != null && !student.getSpecialty().trim().isEmpty()) {
                validateSpecialtyForDepartment(student.getSpecialty(), departmentCode);
            }
        }
    }

    // Validate specialty belongs to department
    private void validateSpecialtyForDepartment(String specialty, DepartmentCode departmentCode) {
        List<String> departmentSpecialties = specialtyService.getSpecialtiesByDepartment(departmentCode.name());
        if (!departmentSpecialties.contains(specialty)) {
            throw new BusinessRuleException("Specialty '" + specialty + "' is not valid for department " + departmentCode);
        }
    }

    // Specialty requirement validation
    private void validateSpecialtyRequirement(Student student) {
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null) {
            ClassLevel classLevel = student.getClassRoom().getCode();
            DepartmentCode departmentCode = student.getDepartment() != null ? student.getDepartment().getCode() : null;

            // Use the enhanced class department rules above
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

    // Rest of the existing methods...
    private String ensureUniqueStudentId(String baseId) {
        String newId = baseId;
        int attempt = 1;
        while (studentRepository.findByStudentId(newId).isPresent()) {
            newId = baseId + "-" + attempt++;
            log.warn("Duplicate studentId found. Regenerated as {}", newId);
        }
        return newId;
    }

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
            try {
                enrollmentService.enrollStudentInSubjects(updatedStudent, subjectIds);
                log.info("Updated student {} subject enrollments to {} subjects",
                        updatedStudent.getStudentId(), subjectIds.size());
            } catch (Exception e) {
                log.error("Failed to update student subject enrollments: {}", e.getMessage());
                throw new BusinessRuleException("Failed to update subject enrollments: " + e.getMessage());
            }
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

    @Transactional(readOnly = true)
    public List<String> getAllSpecialties() {
        return specialtyService.getAllSpecialties();
    }

    @Transactional(readOnly = true)
    public SpecialtyService.SpecialtyRequirement checkSpecialtyRequirement(String classCode, String departmentCode) {
        return specialtyService.checkSpecialtyRequirement(classCode, departmentCode);
    }

    @Transactional(readOnly = true)
    public List<StudentSubject> getStudentSubjects(Long studentId) {
        return enrollmentService.getStudentEnrollments(studentId);
    }

    @Transactional(readOnly = true)
    public List<Long> getSelectedSubjectIds(Long studentId) {
        List<StudentSubject> studentSubjects = getStudentSubjects(studentId);
        return studentSubjects.stream()
                .map(ss -> ss.getSubject().getId())
                .collect(Collectors.toList());
    }

    // Other existing methods...
    @Transactional(readOnly = true)
    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return studentRepository.findByNameContaining(query.trim());
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

    @Transactional(readOnly = true)
    public List<Subject> getAvailableSubjectsForStudent(Student student) {
        if (student.getClassRoom() == null || student.getDepartment() == null) {
            return new ArrayList<>();
        }

        String classCode = student.getClassRoom().getCode().name();
        Long departmentId = student.getDepartment().getId();
        String specialty = student.getSpecialty();

        return getFilteredSubjectsForStudent(classCode, departmentId, specialty);
    }

    // NEW: Get grouped available subjects
    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedAvailableSubjects(Student student) {
        if (student.getClassRoom() == null || student.getDepartment() == null) {
            log.warn("Student missing class or department information");
            return new HashMap<>();
        }

        String classCode = student.getClassRoom().getCode().name();
        Long departmentId = student.getDepartment().getId();
        String specialty = student.getSpecialty();

        log.info("Getting grouped subjects for student - class: {}, department: {}, specialty: {}",
                classCode, departmentId, specialty);

        Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(
                classCode, departmentId, specialty);

        log.info("Grouped subjects found - compulsory: {}, department: {}, specialty: {}, optional: {}",
                groupedSubjects.getOrDefault("compulsory", List.of()).size(),
                groupedSubjects.getOrDefault("department", List.of()).size(),
                groupedSubjects.getOrDefault("specialty", List.of()).size(),
                groupedSubjects.getOrDefault("optional", List.of()).size());

        return groupedSubjects;
    }
}