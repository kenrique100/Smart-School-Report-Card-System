package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.util.IdGenerationService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final IdGenerationService idGenerationService;
    private final DepartmentRepository departmentRepository;
    private final ClassRoomRepository classRoomRepository;

    // ADD THIS METHOD: Bulk create subjects
    @Transactional
    public List<Subject> createSubjects(List<Subject> subjects) {
        log.info("Creating {} subjects in bulk", subjects.size());

        List<Subject> subjectsToCreate = new ArrayList<>();
        List<Subject> existingSubjects = new ArrayList<>();

        // Validate and prepare all subjects
        for (Subject subject : subjects) {
            try {
                validateSubject(subject);

                // Check for duplicates by subject code
                if (subject.getSubjectCode() != null &&
                        subjectRepository.existsBySubjectCode(subject.getSubjectCode())) {
                    log.debug("Subject with code {} already exists, skipping creation", subject.getSubjectCode());
                    subjectRepository.findBySubjectCode(subject.getSubjectCode())
                            .ifPresent(existingSubjects::add);
                    continue;
                }

                // Generate subject code if not provided
                if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                    subject.setSubjectCode(idGenerationService.generateSubjectCode(subject));
                }

                // Set optional flag if not provided
                if (subject.getOptional() == null) {
                    subject.setOptional(false);
                }

                subjectsToCreate.add(subject);
            } catch (Exception e) {
                log.warn("Invalid subject {}: {}", subject.getName(), e.getMessage());
            }
        }

        // Save all valid subjects
        List<Subject> savedSubjects = subjectRepository.saveAll(subjectsToCreate);
        log.info("Successfully created {} new subjects, {} already existed",
                savedSubjects.size(), existingSubjects.size());

        // Combine new and existing subjects
        List<Subject> allSubjects = new ArrayList<>(savedSubjects);
        allSubjects.addAll(existingSubjects);

        return allSubjects;
    }

    // Also add this validation method that's used in createSubjects
    private void validateSubject(Subject subject) {
        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be empty");
        }
        if (subject.getCoefficient() == null || subject.getCoefficient() <= 0) {
            throw new IllegalArgumentException("Subject coefficient must be positive");
        }
        if (subject.getClassRoom() == null) {
            throw new IllegalArgumentException("Subject must be assigned to a classroom");
        }
        if (subject.getDepartment() == null) {
            throw new IllegalArgumentException("Subject must be assigned to a department");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getSubjectsGroupedByDepartmentAndSpecialty() {
        List<Subject> allSubjects = getAllSubjects();

        // Group by Department display name + specialty
        return allSubjects.stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            String dept = s.getDepartment() != null ?
                                    s.getDepartment().getName() : "General";
                            String spec = s.getSpecialty() != null && !s.getSpecialty().isBlank() ?
                                    s.getSpecialty() : "(no specialty)";
                            return dept + " :: " + spec;
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSubjectStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalSubjects = getSubjectCount();
        stats.put("totalSubjects", totalSubjects);

        List<Subject> allSubjects = getAllSubjects();

        // Group by department
        Map<String, Long> deptCounts = allSubjects.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getDepartment() != null ? s.getDepartment().getName() : "Unknown",
                        Collectors.counting()
                ));
        stats.put("departmentCounts", deptCounts);

        // Count by specialty
        Map<String, Long> specialtyCounts = allSubjects.stream()
                .filter(s -> s.getSpecialty() != null && !s.getSpecialty().isEmpty())
                .collect(Collectors.groupingBy(
                        Subject::getSpecialty,
                        Collectors.counting()
                ));
        stats.put("specialtyCounts", specialtyCounts);

        // Count optional vs compulsory
        long optionalCount = allSubjects.stream()
                .filter(s -> Boolean.TRUE.equals(s.getOptional()))
                .count();
        stats.put("optionalCount", optionalCount);
        stats.put("compulsoryCount", totalSubjects - optionalCount);

        return stats;
    }

    @Transactional(readOnly = true)
    public long getSubjectCount() {
        return subjectRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByClassDepartmentAndSpecialty(String classCode, Long departmentId, String specialty) {
        log.info("🔍 Getting subjects for class: {}, department: {}, specialty: {}",
                classCode, departmentId, specialty);

        try {
            // First, find the classroom by code
            ClassLevel classLevel = ClassLevel.fromString(classCode);

            // Get the classroom ID from the class level
            Long classroomId = getClassroomIdByClassLevel(classLevel);

            if (classroomId == null) {
                log.warn("No classroom found for class level: {}", classLevel);
                return new ArrayList<>();
            }

            // Filter based on all criteria
            List<Subject> filteredSubjects;

            if (departmentId != null && specialty != null && !specialty.trim().isEmpty()) {
                // All three criteria specified
                filteredSubjects = subjectRepository.findByClassRoomIdAndDepartmentIdAndSpecialty(
                        classroomId, departmentId, specialty);
            } else if (departmentId != null) {
                // Classroom and department only
                filteredSubjects = subjectRepository.findByClassRoomIdAndDepartmentId(
                        classroomId, departmentId);
            } else {
                // Classroom only
                filteredSubjects = subjectRepository.findByClassroomIdWithDepartment(classroomId);
            }

            log.info("✅ Found {} subjects for class {}, department {}, specialty {}",
                    filteredSubjects.size(), classCode, departmentId, specialty);

            return filteredSubjects;

        } catch (Exception e) {
            log.error("❌ Error getting subjects for class: {}, department: {}, specialty: {}. Error: {}",
                    classCode, departmentId, specialty, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedSubjectsForEnrollment(String classCode, Long departmentId, String specialty) {
        log.info("📚 Grouping subjects for enrollment - class: {}, department: {}, specialty: {}",
                classCode, departmentId, specialty);

        List<Subject> availableSubjects = getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
        ClassLevel classLevel = ClassLevel.fromString(classCode);

        Map<String, List<Subject>> grouped = new LinkedHashMap<>();
        List<Subject> compulsory = new ArrayList<>();
        List<Subject> departmentCore = new ArrayList<>();
        List<Subject> specialtySubjects = new ArrayList<>();
        List<Subject> optional = new ArrayList<>();

        // Get department to check department-specific rules
        Optional<Department> departmentOpt = departmentRepository.findById(departmentId);
        DepartmentCode deptCode = departmentOpt.map(Department::getCode).orElse(DepartmentCode.GEN);

        // Get compulsory subjects based on class level
        List<String> compulsorySubjectNames = getCompulsorySubjectNamesForClass(classLevel);

        for (Subject subject : availableSubjects) {
            // Check if subject is optional
            if (Boolean.TRUE.equals(subject.getOptional())) {
                optional.add(subject);
                continue;
            }

            // Check if subject is compulsory based on name
            if (compulsorySubjectNames.contains(subject.getName())) {
                compulsory.add(subject);
                continue;
            }

            // Check if subject matches specialty (for sixth form only)
            if (classLevel.isSixthForm() &&
                    specialty != null && !specialty.trim().isEmpty() &&
                    subject.getSpecialty() != null &&
                    subject.getSpecialty().equals(specialty)) {
                specialtySubjects.add(subject);
                continue;
            }

            // Check if subject belongs to the department (for all classes)
            if (subject.getDepartment() != null &&
                    subject.getDepartment().getId().equals(departmentId)) {
                departmentCore.add(subject);
                continue;
            }

            // General subjects (no specific department or specialty) are compulsory
            if (subject.getDepartment() == null ||
                    subject.getDepartment().getCode() == DepartmentCode.GEN) {
                compulsory.add(subject);
            }
        }

        grouped.put("compulsory", compulsory);
        grouped.put("department", departmentCore);
        grouped.put("specialty", specialtySubjects);
        grouped.put("optional", optional);

        log.info("📊 Grouped subjects - Compulsory: {}, Department: {}, Specialty: {}, Optional: {}",
                compulsory.size(), departmentCore.size(), specialtySubjects.size(), optional.size());

        return grouped;
    }

    private List<String> getCompulsorySubjectNamesForClass(ClassLevel classLevel) {
        Map<ClassLevel, List<String>> compulsoryMap = Map.of(
                ClassLevel.FORM_1, Arrays.asList("Mathematics", "English", "French"),
                ClassLevel.FORM_2, Arrays.asList("Mathematics", "English", "French"),
                ClassLevel.FORM_3, Arrays.asList("Mathematics", "English", "French"),
                ClassLevel.FORM_4, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language"),
                ClassLevel.FORM_5, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language"),
                ClassLevel.LOWER_SIXTH, Collections.emptyList(),
                ClassLevel.UPPER_SIXTH, Collections.emptyList()
        );
        return compulsoryMap.getOrDefault(classLevel, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsForStudentEnrollment(Student student) {
        if (student == null || student.getClassRoom() == null || student.getDepartment() == null) {
            log.warn("Cannot get subjects: Student or class/department information is incomplete");
            return new ArrayList<>();
        }

        Long classroomId = student.getClassRoom().getId();
        Long departmentId = student.getDepartment().getId();
        String specialty = student.getSpecialty();

        log.info("📚 Getting subjects for student enrollment - Classroom: {}, Department: {}, Specialty: {}",
                classroomId, departmentId, specialty);

        // Get all subjects for the student's classroom
        List<Subject> classroomSubjects = subjectRepository.findByClassroomIdWithDepartment(classroomId);

        // Filter by department and specialty
        List<Subject> filteredSubjects = classroomSubjects.stream()
                .filter(subject -> {
                    // For Forms 1-2, include subjects from student's department
                    ClassLevel classLevel = student.getClassRoom().getCode();
                    if (classLevel == ClassLevel.FORM_1 || classLevel == ClassLevel.FORM_2) {
                        // Forms 1-2: Include subjects from student's department OR general subjects
                        return subject.getDepartment() == null ||
                                subject.getDepartment().getId().equals(departmentId) ||
                                subject.getDepartment().getCode() == DepartmentCode.GEN;
                    }

                    // For other classes: Subject must belong to student's department OR be a general subject
                    boolean departmentMatch = subject.getDepartment() == null ||
                            subject.getDepartment().getId().equals(departmentId) ||
                            subject.getDepartment().getCode() == DepartmentCode.GEN;

                    // Check specialty match
                    boolean specialtyMatch = true;
                    if (specialty != null && !specialty.trim().isEmpty()) {
                        // If student has specialty, include subjects without specialty or with matching specialty
                        if (subject.getSpecialty() != null && !subject.getSpecialty().isEmpty()) {
                            specialtyMatch = subject.getSpecialty().equals(specialty);
                        }
                    } else {
                        // If student has no specialty, exclude subjects that require specialty
                        specialtyMatch = subject.getSpecialty() == null || subject.getSpecialty().isEmpty();
                    }

                    return departmentMatch && specialtyMatch;
                })
                .sorted(Comparator.comparing(Subject::getName))
                .collect(Collectors.toList());

        log.info("✅ Found {} subjects for student enrollment (Class: {}, Dept: {})",
                filteredSubjects.size(),
                student.getClassRoom().getName(),
                student.getDepartment().getName());

        return filteredSubjects;
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedSubjectsForStudent(Student student) {
        List<Subject> subjects = getSubjectsForStudentEnrollment(student);
        return groupSubjectsForStudent(student, subjects);
    }

    private Map<String, List<Subject>> groupSubjectsForStudent(Student student, List<Subject> subjects) {
        Map<String, List<Subject>> grouped = new LinkedHashMap<>();
        List<Subject> compulsory = new ArrayList<>();
        List<Subject> departmentCore = new ArrayList<>();
        List<Subject> specialtySubjects = new ArrayList<>();
        List<Subject> optional = new ArrayList<>();

        String specialty = student.getSpecialty();

        for (Subject subject : subjects) {
            if (Boolean.TRUE.equals(subject.getOptional())) {
                optional.add(subject);
            } else if (specialty != null && !specialty.trim().isEmpty() &&
                    subject.getSpecialty() != null && subject.getSpecialty().equals(specialty)) {
                specialtySubjects.add(subject);
            } else if (subject.getDepartment() != null &&
                    subject.getDepartment().getId().equals(student.getDepartment().getId())) {
                departmentCore.add(subject);
            } else {
                compulsory.add(subject);
            }
        }

        grouped.put("compulsory", compulsory);
        grouped.put("department", departmentCore);
        grouped.put("specialty", specialtySubjects);
        grouped.put("optional", optional);

        return grouped;
    }

    private Long getClassroomIdByClassLevel(ClassLevel classLevel) {
        return classRoomRepository.findByCode(classLevel)
                .map(ClassRoom::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No classroom found for class level: " + classLevel));
    }

    @Transactional(readOnly = true)
    public Page<Subject> getSubjectsByFilters(String name, Long departmentId, String specialty, Pageable pageable) {
        return subjectRepository.findByFilters(name, departmentId, specialty, pageable);
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Subject> getSubjectById(Long id) {
        return subjectRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Subject getSubjectByIdOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject", id));
    }

    public Subject createSubject(Subject subject) {
        log.info("Creating new subject: {}", subject.getName());
        validateSubject(subject);

        if (subject.getSubjectCode() != null &&
                subjectRepository.existsBySubjectCode(subject.getSubjectCode())) {
            log.warn("Subject with code {} already exists, returning existing subject", subject.getSubjectCode());
            return subjectRepository.findBySubjectCode(subject.getSubjectCode())
                    .orElseThrow(() -> new DataIntegrityException("Subject with code " + subject.getSubjectCode() + " already exists"));
        }

        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            subject.setSubjectCode(idGenerationService.generateSubjectCode(subject));
        }

        if (subject.getOptional() == null) {
            subject.setOptional(false);
        }

        Subject saved = subjectRepository.save(subject);
        log.info("Created subject: {} (Code: {})", saved.getName(), saved.getSubjectCode());
        return saved;
    }

    public Subject updateSubject(Long id, Subject subjectDetails) {
        log.info("Updating subject with id: {}", id);
        Subject existing = getSubjectByIdOrThrow(id);

        existing.setName(subjectDetails.getName());
        existing.setCoefficient(subjectDetails.getCoefficient());
        existing.setDepartment(subjectDetails.getDepartment());
        existing.setClassRoom(subjectDetails.getClassRoom());
        existing.setSpecialty(subjectDetails.getSpecialty());
        existing.setDescription(subjectDetails.getDescription());
        existing.setOptional(Boolean.TRUE.equals(subjectDetails.getOptional()));

        Subject updated = subjectRepository.save(existing);
        log.info("Updated subject: {} (ID: {})", updated.getName(), updated.getId());
        return updated;
    }

    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new EntityNotFoundException("Subject", id);
        }
        subjectRepository.deleteById(id);
        log.info("Deleted subject with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<String> getSpecialtiesByDepartment(Long departmentId) {
        List<Subject> subjects = subjectRepository.findByDepartmentId(departmentId);
        return subjects.stream()
                .map(Subject::getSpecialty)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public List<Subject> getSubjectsByIds(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            log.warn("Empty subject IDs list provided");
            return new ArrayList<>();
        }

        // Filter out null values
        List<Long> validIds = subjectIds.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validIds.isEmpty()) {
            log.warn("No valid subject IDs in the list");
            return new ArrayList<>();
        }

        log.info("Fetching subjects with IDs: {}", validIds);
        List<Subject> subjects = subjectRepository.findAllById(validIds);

        if (subjects.size() != validIds.size()) {
            Set<Long> foundIds = subjects.stream()
                    .map(Subject::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = validIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            log.warn("Some subject IDs not found: {}", missingIds);
        }

        return subjects;
    }
}