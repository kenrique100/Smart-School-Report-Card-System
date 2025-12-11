package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.BusinessRuleException;
import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.model.enums.Gender;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.StudentSubjectRepository;
import com.akentech.schoolreport.util.IdGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final IdGenerationService idGenerationService;
    private final StudentEnrollmentService enrollmentService;
    private final SpecialtyService specialtyService;
    private final StudentSubjectRepository studentSubjectRepository;
    @Lazy
    private final SubjectService subjectService;

    // ENHANCED: Student creation with subject selection
    public Student createStudent(Student student, List<Long> subjectIds) {
        log.info("Creating new student: {} {}", student.getFirstName(), student.getLastName());

        // SANITIZE EMAIL BEFORE VALIDATION - CRITICAL FIX
        if (student.getEmail() != null && student.getEmail().trim().isEmpty()) {
            student.setEmail(null);
        }

        if (student.getClassRoom() == null) {
            throw new BusinessRuleException("Class room is required");
        }

        // Validate class room has code
        validateClassRoom(student.getClassRoom());

        if (student.getDepartment() == null) {
            throw new BusinessRuleException("Department is required");
        }

        validateStudent(student);

        // Check specialty requirements based on department and class
        checkAndValidateSpecialty(student);

        // Generate IDs safely
        String studentId = idGenerationService.generateStudentId(student);
        String rollNumber = idGenerationService.generateRollNumber(student);

        // Ensure uniqueness before saving
        studentId = ensureUniqueStudentId(studentId);
        rollNumber = ensureUniqueRollNumber(rollNumber, student.getClassRoom());

        student.setStudentId(studentId);
        student.setRollNumber(rollNumber);

        // Final email sanitization
        if (student.getEmail() != null && student.getEmail().trim().isEmpty()) {
            student.setEmail(null);
        }

        // Set academic year if not provided
        if (student.getAcademicYearStart() == null || student.getAcademicYearEnd() == null) {
            setDefaultAcademicYear(student);
        }

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

        log.info("✅ Created student: {} {} (ID: {}, Roll: {}, Email: {})",
                savedStudent.getFirstName(), savedStudent.getLastName(),
                savedStudent.getStudentId(), savedStudent.getRollNumber(),
                savedStudent.getEmail() != null ? savedStudent.getEmail() : "None");

        return savedStudent;
    }

    // NEW: Check and validate specialty based on department and class
    private void checkAndValidateSpecialty(Student student) {
        if (student.getClassRoom() == null || student.getClassRoom().getCode() == null) {
            return;
        }

        if (student.getDepartment() == null || student.getDepartment().getCode() == null) {
            return;
        }

        String classCode = student.getClassRoom().getCode().name();
        String departmentCode = student.getDepartment().getCode().name();
        String specialty = student.getSpecialty();

        // Check if department has specialties
        boolean hasSpecialties = specialtyService.hasSpecialties(departmentCode);
        List<String> availableSpecialties = specialtyService.getSpecialtiesByDepartment(departmentCode);

        log.info("Checking specialty for class: {}, department: {}, hasSpecialties: {}, specialty: {}",
                classCode, departmentCode, hasSpecialties, specialty);

        // If department doesn't have specialties, clear any specialty value
        if (!hasSpecialties) {
            if (specialty != null && !specialty.trim().isEmpty()) {
                log.warn("Department {} doesn't have specialties, but student has specialty: {}. Clearing specialty.",
                        departmentCode, specialty);
                student.setSpecialty(null);
            }
            return;
        }

        // Department has specialties - validate based on class level
        boolean isSixthForm = classCode.equals("LOWER_SIXTH") || classCode.equals("UPPER_SIXTH");
        boolean isFormLevel = classCode.startsWith("FORM_");

        // For Forms 1-3: No specialties allowed
        if (isFormLevel && (classCode.equals("FORM_1") || classCode.equals("FORM_2") || classCode.equals("FORM_3"))) {
            if (specialty != null && !specialty.trim().isEmpty()) {
                throw new BusinessRuleException("Specialty is not allowed for Forms 1-3");
            }
            student.setSpecialty(null);
        }
        // For Forms 4-5: Specialty is optional
        else if (isFormLevel && (classCode.equals("FORM_4") || classCode.equals("FORM_5"))) {
            if (specialty != null && !specialty.trim().isEmpty()) {
                // Validate specialty exists for department
                if (!availableSpecialties.contains(specialty)) {
                    throw new BusinessRuleException("Specialty '" + specialty + "' is not valid for department " +
                            student.getDepartment().getName());
                }
            }
        }
        // For Sixth Form: Specialty is required for Science and Arts, optional for others
        else if (isSixthForm) {
            boolean requiresSpecialty = departmentCode.equals("SCI") || departmentCode.equals("ART");

            if (requiresSpecialty) {
                if (specialty == null || specialty.trim().isEmpty()) {
                    throw new BusinessRuleException("Specialty is required for Sixth Form " +
                            student.getDepartment().getName() + " students");
                }
                // Validate specialty exists for department
                if (!availableSpecialties.contains(specialty)) {
                    throw new BusinessRuleException("Specialty '" + specialty + "' is not valid for department " +
                            student.getDepartment().getName());
                }
            } else {
                // Other sixth form departments - specialty is optional but must be valid if provided
                if (specialty != null && !specialty.trim().isEmpty()) {
                    if (!availableSpecialties.contains(specialty)) {
                        throw new BusinessRuleException("Specialty '" + specialty + "' is not valid for department " +
                                student.getDepartment().getName());
                    }
                }
            }
        }
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

    private List<Long> getAutoAssignedSubjectIds(Student student) {
        List<Subject> appropriateSubjects = getAppropriateSubjectsForStudent(student);

        // Log the subjects being assigned
        log.info("Auto-assigning {} subjects to student {}:", appropriateSubjects.size(), student.getStudentId());
        for (Subject subject : appropriateSubjects) {
            log.info("  - {} (Dept: {}, Specialty: {})",
                    subject.getName(),
                    subject.getDepartment() != null ? subject.getDepartment().getName() : "General",
                    subject.getSpecialty() != null ? subject.getSpecialty() : "None");
        }

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

        // Always add compulsory subjects
        List<Subject> autoAssignedSubjects = new ArrayList<>(groupedSubjects.getOrDefault("compulsory", new ArrayList<>()));

        ClassLevel classLevel = student.getClassRoom().getCode();

        // For ALL class levels (including Form 1), add department core subjects
        // Form 1 students should get their department subjects too
        autoAssignedSubjects.addAll(groupedSubjects.getOrDefault("department", new ArrayList<>()));

        // For Sixth Form, add specialty subjects
        if (classLevel.isSixthForm() && specialty != null && !specialty.trim().isEmpty()) {
            autoAssignedSubjects.addAll(groupedSubjects.getOrDefault("specialty", new ArrayList<>()));
        }

        // Log what we're enrolling
        log.info("Auto-assigning {} subjects for student {} (Class: {}, Dept: {}, Specialty: {})",
                autoAssignedSubjects.size(), student.getStudentId(),
                classLevel.getDisplayName(),
                student.getDepartment().getName(),
                specialty != null ? specialty : "None");

        // Log breakdown
        log.info("Breakdown - Compulsory: {}, Department: {}, Specialty: {}",
                groupedSubjects.getOrDefault("compulsory", new ArrayList<>()).size(),
                groupedSubjects.getOrDefault("department", new ArrayList<>()).size(),
                groupedSubjects.getOrDefault("specialty", new ArrayList<>()).size());

        return autoAssignedSubjects;
    }

    // ENHANCED: Student validation with department and specialty rules
    private void validateStudent(Student student) {
        validateClassDepartmentRules(student);
        validateAcademicYears(student);
        validateEmailUniqueness(student);
        validateRollNumberUniqueness(student);
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

        // Validate date of birth
        if (student.getDateOfBirth() != null && student.getDateOfBirth().isAfter(LocalDate.now().minusYears(5))) {
            throw new BusinessRuleException("Student must be at least 5 years old");
        }
    }

    private void validateAcademicYears(Student student) {
        if (student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
            if (student.getAcademicYearStart() >= student.getAcademicYearEnd()) {
                throw new BusinessRuleException("Academic year start must be before academic year end");
            }
            // Validate reasonable academic years (e.g., not before 2000)
            if (student.getAcademicYearStart() < 2000 || student.getAcademicYearStart() > 2050) {
                throw new BusinessRuleException("Academic year start must be between 2000 and 2050");
            }
        }
    }

    // FIXED: Email uniqueness validation - properly handles null/empty emails
    private void validateEmailUniqueness(Student student) {
        String email = student.getEmail();

        if (email != null && !email.trim().isEmpty()) {
            // Check if email exists for other students
            Optional<Student> existingStudent = studentRepository.findByEmail(email.trim());
            if (existingStudent.isPresent() &&
                    (student.getId() == null || !existingStudent.get().getId().equals(student.getId()))) {
                throw new DataIntegrityException("Email '" + email + "' already exists");
            }
        }
        // If email is null or empty, it's fine - multiple nulls are allowed
    }

    // NEW: Validate roll number uniqueness
    private void validateRollNumberUniqueness(Student student) {
        if (student.getRollNumber() != null && !student.getRollNumber().trim().isEmpty() && student.getClassRoom() != null) {
            String rollNumber = student.getRollNumber().trim();
            Optional<Student> existingStudent = studentRepository.findByRollNumberAndClassRoom(rollNumber, student.getClassRoom());
            if (existingStudent.isPresent() &&
                    (student.getId() == null || !existingStudent.get().getId().equals(student.getId()))) {
                throw new DataIntegrityException("Roll number '" + rollNumber + "' already exists in class " +
                        student.getClassRoom().getName());
            }
        }
    }

    // Set default academic year based on current year
    private void setDefaultAcademicYear(Student student) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        // If it's after July, assume next academic year
        if (currentMonth >= 7) {
            student.setAcademicYearStart(currentYear);
            student.setAcademicYearEnd(currentYear + 1);
        } else {
            student.setAcademicYearStart(currentYear - 1);
            student.setAcademicYearEnd(currentYear);
        }
    }

    // FIXED: Ensure the return value is used by returning the result
    private String ensureUniqueStudentId(String baseId) {
        String newId = baseId;
        int attempt = 1;
        while (studentRepository.findByStudentId(newId).isPresent()) {
            newId = baseId + "-" + attempt++;
            log.warn("Duplicate studentId found. Regenerated as {}", newId);
        }
        return newId;
    }

    // FIXED: Ensure the return value is used by returning the result
    private String ensureUniqueRollNumber(String baseRoll, ClassRoom classRoom) {
        String newRoll = baseRoll;
        int attempt = 1;
        while (studentRepository.findByRollNumberAndClassRoom(newRoll, classRoom).isPresent()) {
            newRoll = baseRoll + "-" + attempt++;
            log.warn("Duplicate rollNumber found for class {}. Regenerated as {}", classRoom.getCode(), newRoll);
        }
        return newRoll;
    }

    public Student updateStudent(Long id, Student student, List<Long> subjectIds) {
        log.info("Updating student with ID: {} and {} subject IDs", id, subjectIds != null ? subjectIds.size() : 0);

        Student existingStudent = getStudentByIdOrThrow(id);

        // Update student fields
        existingStudent.setFirstName(student.getFirstName());
        existingStudent.setLastName(student.getLastName());
        existingStudent.setDateOfBirth(student.getDateOfBirth());
        existingStudent.setGender(student.getGender());
        existingStudent.setEmail(student.getEmail());
        existingStudent.setAddress(student.getAddress());
        existingStudent.setAcademicYearStart(student.getAcademicYearStart());
        existingStudent.setAcademicYearEnd(student.getAcademicYearEnd());
        existingStudent.setClassRoom(student.getClassRoom());
        existingStudent.setDepartment(student.getDepartment());
        existingStudent.setSpecialty(student.getSpecialty());

        // Validate the updated student
        validateStudent(existingStudent);
        checkAndValidateSpecialty(existingStudent);

        // Save the student first
        existingStudent = studentRepository.save(existingStudent);

        // Update subject enrollments through the enrollment service
        enrollmentService.enrollStudentInSubjects(existingStudent, subjectIds);

        log.info("✅ Successfully updated student with ID: {}", existingStudent.getStudentId());
        return existingStudent;
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
        return studentRepository.findByFilters(firstName, lastName, classRoomId,
                departmentId, specialty, pageable);
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
    public Optional<Student> getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<String> getAllSpecialties() {
        return specialtyService.getAllSpecialties();
    }

    @Transactional(readOnly = true)
    public SpecialtyService.SpecialtyRequirement checkSpecialtyRequirement(String classCode, String departmentCode) {
        log.debug("Checking specialty requirement for class: {}, department: {}", classCode, departmentCode);

        // Get specialties for department
        List<String> specialties = getSpecialtiesByDepartmentCode(departmentCode);
        boolean hasSpecialties = !specialties.isEmpty();

        // Check if sixth form
        boolean isSixthForm = classCode.equals("LOWER_SIXTH") || classCode.equals("UPPER_SIXTH");

        // Determine requirements
        boolean required = isSixthForm && (departmentCode.equals("SCI") || departmentCode.equals("ART"));
        boolean allowed = hasSpecialties && !classCode.equals("FORM_1") && !classCode.equals("FORM_2") && !classCode.equals("FORM_3");

        log.debug("Specialty requirement result: required={}, allowed={}, hasSpecialties={}, specialties count={}",
                required, allowed, hasSpecialties, specialties.size());

        return new SpecialtyService.SpecialtyRequirement(
                required, allowed, hasSpecialties, specialties
        );
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

    @Transactional(readOnly = true)
    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return studentRepository.findByNameContaining(query.trim());
    }

    @Transactional(readOnly = true)
    public Page<Student> searchStudents(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return Page.empty(pageable);
        }
        return studentRepository.findByNameContaining(query.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public long getStudentCount() {
        return studentRepository.count();
    }

    @Transactional(readOnly = true)
    public long getStudentCountByClass(Long classRoomId) {
        return studentRepository.countByClassRoomId(classRoomId);
    }

    @Transactional(readOnly = true)
    public long getStudentCountByDepartment(Long departmentId) {
        return studentRepository.countByDepartmentId(departmentId);
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
        Map<String, List<Subject>> result = new HashMap<>();
        result.put("compulsory", new ArrayList<>());
        result.put("department", new ArrayList<>());
        result.put("specialty", new ArrayList<>());
        result.put("optional", new ArrayList<>());

        if (student == null || student.getClassRoom() == null || student.getClassRoom().getCode() == null || student.getDepartment() == null) {
            log.warn("Cannot get grouped subjects: Student or class/department information is incomplete");
            return result;
        }

        try {
            String classCode = student.getClassRoom().getCode().name();
            Long departmentId = student.getDepartment().getId();
            String specialty = student.getSpecialty();

            log.info("📚 Getting grouped subjects for student - class: {}, department: {}, specialty: {}",
                    classCode, departmentId, specialty);

            Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(classCode, departmentId, specialty);

            // Log the grouping results
            log.info("📊 Grouped subjects result - Compulsory: {}, Department: {}, Specialty: {}, Optional: {}",
                    groupedSubjects.getOrDefault("compulsory", new ArrayList<>()).size(),
                    groupedSubjects.getOrDefault("department", new ArrayList<>()).size(),
                    groupedSubjects.getOrDefault("specialty", new ArrayList<>()).size(),
                    groupedSubjects.getOrDefault("optional", new ArrayList<>()).size());

            return groupedSubjects;
        } catch (Exception e) {
            log.error("❌ Error getting grouped subjects for student: {}", e.getMessage(), e);
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = getStudentCount();
        stats.put("totalStudents", totalStudents);

        // Get gender distribution
        List<Student> allStudents = studentRepository.findAll();
        long maleCount = allStudents.stream()
                .filter(s -> s.getGender() != null && s.getGender() == Gender.MALE)
                .count();
        long femaleCount = allStudents.stream()
                .filter(s -> s.getGender() != null && s.getGender() == Gender.FEMALE)
                .count();
        long unknownGender = totalStudents - maleCount - femaleCount;

        stats.put("maleCount", maleCount);
        stats.put("femaleCount", femaleCount);
        stats.put("unknownGenderCount", unknownGender);

        // Get class distribution
        Map<String, Long> classDistribution = new HashMap<>();
        List<ClassRoom> allClasses = classRoomRepository.findAll();
        for (ClassRoom classRoom : allClasses) {
            long count = studentRepository.countByClassRoom(classRoom);
            classDistribution.put(classRoom.getName(), count);
        }
        stats.put("classDistribution", classDistribution);

        // Get department distribution
        Map<String, Long> deptDistribution = new HashMap<>();
        List<Department> allDepartments = departmentRepository.findAll();
        for (Department dept : allDepartments) {
            long count = getStudentCountByDepartment(dept.getId());
            deptDistribution.put(dept.getName(), count);
        }
        stats.put("departmentDistribution", deptDistribution);

        // Get specialty distribution
        Map<String, Long> specialtyDistribution = new HashMap<>();
        List<String> specialties = getAllSpecialties();
        for (String specialty : specialties) {
            long count = studentRepository.findBySpecialty(specialty).size();
            specialtyDistribution.put(specialty, count);
        }
        stats.put("specialtyDistribution", specialtyDistribution);

        // Get email statistics
        long studentsWithEmail = allStudents.stream()
                .filter(s -> s.getEmail() != null && !s.getEmail().trim().isEmpty())
                .count();
        long studentsWithoutEmail = totalStudents - studentsWithEmail;

        stats.put("studentsWithEmail", studentsWithEmail);
        stats.put("studentsWithoutEmail", studentsWithoutEmail);
        stats.put("emailCompletionRate", totalStudents > 0 ?
                String.format("%.1f%%", (studentsWithEmail * 100.0 / totalStudents)) : "0%");

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsBySpecialty(String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return studentRepository.findBySpecialty(specialty);
    }

    @Transactional(readOnly = true)
    public Page<Student> getStudentsBySpecialty(String specialty, Pageable pageable) {
        if (specialty == null || specialty.trim().isEmpty()) {
            return Page.empty(pageable);
        }
        return studentRepository.findBySpecialty(specialty, pageable);
    }

    @Transactional(readOnly = true)
    public boolean isEmailUnique(String email, Long excludeStudentId) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }

        Optional<Student> student = studentRepository.findByEmail(email.trim());
        if (student.isPresent()) {
            // If excludeStudentId is provided, check if it's the same student
            if (excludeStudentId != null) {
                return student.get().getId().equals(excludeStudentId);
            }
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isRollNumberUnique(String rollNumber, ClassRoom classRoom, Long excludeStudentId) {
        if (rollNumber == null || rollNumber.trim().isEmpty() || classRoom == null) {
            return true;
        }

        Optional<Student> student = studentRepository.findByRollNumberAndClassRoom(rollNumber.trim(), classRoom);
        if (student.isPresent()) {
            // If excludeStudentId is provided, check if it's the same student
            if (excludeStudentId != null) {
                return student.get().getId().equals(excludeStudentId);
            }
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByAcademicYear(Integer startYear, Integer endYear) {
        return studentRepository.findAll().stream()
                .filter(s -> {
                    if (startYear != null && s.getAcademicYearStart() != null && s.getAcademicYearStart() < startYear) {
                        return false;
                    }
                    return endYear == null || s.getAcademicYearEnd() == null || s.getAcademicYearEnd() <= endYear;
                })
                .collect(Collectors.toList());
    }

    // NEW: Database cleanup method to fix existing empty emails
    @Transactional
    public void fixEmptyEmails() {
        try {
            List<Student> studentsWithEmptyEmails = studentRepository.findAll().stream()
                    .filter(s -> s.getEmail() != null && s.getEmail().trim().isEmpty())
                    .toList();

            if (!studentsWithEmptyEmails.isEmpty()) {
                log.info("Found {} students with empty emails, fixing...", studentsWithEmptyEmails.size());

                studentsWithEmptyEmails.forEach(student -> {
                    student.setEmail(null);
                    studentRepository.save(student);
                });

                log.info("✅ Fixed empty emails for {} students", studentsWithEmptyEmails.size());
            } else {
                log.info("No empty emails found to fix");
            }
        } catch (Exception e) {
            log.error("Error fixing empty emails: {}", e.getMessage());
        }
    }

    public List<String> getSpecialtiesByDepartmentCode(String departmentCode) {
        try {
            DepartmentCode deptCode = DepartmentCode.fromCode(departmentCode);
            return getSpecialtiesByDepartmentCode(deptCode);
        } catch (Exception e) {
            log.warn("Invalid department code: {}", departmentCode);
            return Collections.emptyList();
        }
    }

    public List<String> getSpecialtiesByDepartmentCode(DepartmentCode deptCode) {
        if (deptCode == null) {
            return Collections.emptyList();
        }

        return switch (deptCode) {
            case COM -> // Commercial
                    Arrays.asList("Accounting", "Administration & Communication Techniques");
            case SCI -> // Sciences (8 specialties as per DataInitializer)
                    Arrays.asList("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8");
            case ART -> // Arts (8 specialties as per DataInitializer)
                    Arrays.asList("A1", "A2", "A3", "A4", "A5");
            case BC, EPS, HE, GEN, CI -> // Departments with no specialties
                    Collections.emptyList();
        };
    }

    private void validateClassRoom(ClassRoom classRoom) {
        if (classRoom == null) {
            throw new BusinessRuleException("Class room is required");
        }

        if (classRoom.getCode() == null) {
            log.error("ClassRoom {} has null code. This is a data integrity issue.", classRoom.getName());
            throw new BusinessRuleException("Class room code is missing. Please contact administrator.");
        }
    }
}