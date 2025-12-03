package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.DataIntegrityException;
import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
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
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final IdGenerationService idGenerationService;
    private final DepartmentRepository departmentRepository;

    // Enhanced compulsory subject definitions
    private final Map<ClassLevel, List<String>> COMPULSORY_SUBJECT_NAMES = createCompulsorySubjectMap();
    private final Map<DepartmentCode, List<String>> DEPARTMENT_CORE_SUBJECTS = createDepartmentCoreSubjects();

    public SubjectService(SubjectRepository subjectRepository, IdGenerationService idGenerationService, DepartmentRepository departmentRepository) {
        this.subjectRepository = subjectRepository;
        this.idGenerationService = idGenerationService;
        this.departmentRepository = departmentRepository;
    }

    private Map<ClassLevel, List<String>> createCompulsorySubjectMap() {
        Map<ClassLevel, List<String>> compulsoryMap = new HashMap<>();

        // Forms 1-3: Core subjects
        List<String> forms1to3Core = Arrays.asList("O-Mathematics", "O-English Language", "O-French Language", "Physical Education");
        compulsoryMap.put(ClassLevel.FORM_1, new ArrayList<>(forms1to3Core));
        compulsoryMap.put(ClassLevel.FORM_2, new ArrayList<>(forms1to3Core));
        compulsoryMap.put(ClassLevel.FORM_3, new ArrayList<>(forms1to3Core));

        // Forms 4-5: Reduced compulsory subjects
        List<String> forms4to5Core = Arrays.asList("O-Mathematics", "O-English Language", "O-French Language");
        compulsoryMap.put(ClassLevel.FORM_4, new ArrayList<>(forms4to5Core));
        compulsoryMap.put(ClassLevel.FORM_5, new ArrayList<>(forms4to5Core));

        // Sixth Form: No compulsory subjects (all are department/specialty specific)
        compulsoryMap.put(ClassLevel.LOWER_SIXTH, new ArrayList<>());
        compulsoryMap.put(ClassLevel.UPPER_SIXTH, new ArrayList<>());

        return compulsoryMap;
    }

    private Map<DepartmentCode, List<String>> createDepartmentCoreSubjects() {
        Map<DepartmentCode, List<String>> deptCoreMap = new HashMap<>();

        deptCoreMap.put(DepartmentCode.SCI, Arrays.asList("O-Biology", "O-Chemistry", "O-Physics", "O-Additional Mathematics"));
        deptCoreMap.put(DepartmentCode.ART, Arrays.asList("O-History", "O-Geography", "O-Literature in English", "O-Economics"));
        deptCoreMap.put(DepartmentCode.COM, Arrays.asList("O-Accounting", "O-Commerce", "O-Economics", "O-Business Studies"));
        deptCoreMap.put(DepartmentCode.TEC, Arrays.asList("Technical Drawing", "Workshop Practice", "Engineering Science", "O-Woodwork"));
        deptCoreMap.put(DepartmentCode.HE, Arrays.asList("Food and Nutrition", "Home Management", "Clothing and Textiles", "O-Child Development"));
        deptCoreMap.put(DepartmentCode.GEN, Arrays.asList("O-Computer Science", "O-ICT", "O-Religious Studies", "Citizenship Education"));

        return deptCoreMap;
    }

    /* ============================ CORE PUBLIC METHODS ============================ */

    @Transactional(readOnly = true)
    public Page<Subject> getSubjectsByFilters(String name, Long departmentId, String specialty, Pageable pageable) {
        return subjectRepository.findByFilters(name, departmentId, specialty, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getSubjectsGroupedByDepartmentAndSpecialty() {
        List<Subject> all = getAllSubjects();

        // Group by Department display name + specialty
        return all.stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            String dept = Optional.ofNullable(s.getDepartment()).map(Department::getName).orElse("General");
                            String spec = Optional.ofNullable(s.getSpecialty()).filter(st -> !st.isBlank()).orElse("(no specialty)");
                            return dept + " :: " + spec;
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByClassDepartmentAndSpecialty(String classCode, Long departmentId, String specialty) {
        log.info("🔍 Filtering subjects for class: {}, department: {}, specialty: {}", classCode, departmentId, specialty);

        List<Subject> allSubjects = getAllSubjects();
        List<Subject> filteredSubjects = new ArrayList<>();

        boolean isSixthForm = isSixthFormClass(classCode);
        ClassLevel classLevel = toClassLevel(classCode);

        log.info("📊 Total subjects in system: {}", allSubjects.size());
        log.info("🎓 Class level: {}, Is Sixth Form: {}", classLevel, isSixthForm);

        // Get department info for debugging
        Department department = null;
        if (departmentId != null) {
            department = departmentRepository.findById(departmentId).orElse(null);
            log.info("Requested department: {} (ID: {})", department != null ? department.getName() : "Not Found", departmentId);
        }

        for (Subject subject : allSubjects) {
            boolean isForClassLevel = isSubjectForClassLevel(subject, classLevel, isSixthForm);
            boolean isForDepartment = isSubjectForDepartment(subject, departmentId);
            boolean isForSpecialty = isSubjectForSpecialty(subject, specialty, isSixthForm);

            if (isForClassLevel && isForDepartment && isForSpecialty) {
                filteredSubjects.add(subject);
                log.debug("✅ INCLUDED: {} (Dept: {}, Specialty: {}, A-Level: {}, O-Level: {})",
                        subject.getName(),
                        subject.getDepartment() != null ? subject.getDepartment().getName() : "None",
                        subject.getSpecialty(),
                        subject.getName().startsWith("A-"),
                        subject.getName().startsWith("O-"));
            } else {
                log.debug("❌ EXCLUDED: {} - ClassLevel: {}, Department: {}, Specialty: {}, DeptName: {}",
                        subject.getName(), isForClassLevel, isForDepartment, isForSpecialty,
                        subject.getDepartment() != null ? subject.getDepartment().getName() : "None");
            }
        }

        log.info("📦 Filtered {} subjects for class: {}, department: {}, specialty: {}",
                filteredSubjects.size(), classCode, departmentId, specialty);

        return filteredSubjects;
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedSubjectsForEnrollment(String classCode, Long departmentId, String specialty) {
        log.info("Grouping subjects for enrollment for class: {}, department: {}, specialty: {}", classCode, departmentId, specialty);

        List<Subject> availableSubjects = getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
        ClassLevel classLevel = toClassLevel(classCode);

        Map<String, List<Subject>> grouped = new LinkedHashMap<>();
        List<Subject> compulsory = new ArrayList<>();
        List<Subject> departmentCore = new ArrayList<>();
        List<Subject> specialtySubjects = new ArrayList<>();
        List<Subject> optional = new ArrayList<>();

        for (Subject subject : availableSubjects) {
            if (isCompulsorySubject(subject, classLevel)) {
                compulsory.add(subject);
            } else if (isDepartmentCoreSubject(subject, departmentId, classLevel)) {
                departmentCore.add(subject);
            } else if (isSpecialtySubject(subject, specialty, classLevel)) {
                specialtySubjects.add(subject);
            } else if (Boolean.TRUE.equals(subject.getOptional())) {
                optional.add(subject);
            } else {
                // Default to department core for non-optional subjects
                departmentCore.add(subject);
            }
        }

        grouped.put("compulsory", compulsory);
        grouped.put("department", departmentCore);
        grouped.put("specialty", specialtySubjects);
        grouped.put("optional", optional);

        log.info("Grouped subjects - Compulsory: {}, Department: {}, Specialty: {}, Optional: {}",
                compulsory.size(), departmentCore.size(), specialtySubjects.size(), optional.size());

        return grouped;
    }

    /* ============================ CRUD OPERATIONS ============================ */

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

        // Check for duplicate subject code
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

    public List<Subject> createSubjects(List<Subject> subjects) {
        log.info("Creating {} subjects in bulk", subjects.size());

        List<Subject> subjectsToCreate = new ArrayList<>();
        List<Subject> existingSubjects = new ArrayList<>();

        // Validate and prepare all subjects
        for (Subject subject : subjects) {
            try {
                validateSubject(subject);

                // Check for duplicates
                if (subject.getSubjectCode() != null &&
                        subjectRepository.existsBySubjectCode(subject.getSubjectCode())) {
                    log.debug("Subject with code {} already exists, skipping creation", subject.getSubjectCode());
                    subjectRepository.findBySubjectCode(subject.getSubjectCode())
                            .ifPresent(existingSubjects::add);
                    continue;
                }

                if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                    subject.setSubjectCode(idGenerationService.generateSubjectCode(subject));
                }
                if (subject.getOptional() == null) {
                    subject.setOptional(false);
                }

                subjectsToCreate.add(subject);
            } catch (Exception e) {
                log.warn("Invalid subject {}: {}", subject.getName(), e.getMessage());
            }
        }

        List<Subject> savedSubjects = subjectRepository.saveAll(subjectsToCreate);
        log.info("Successfully created {} new subjects, {} already existed",
                savedSubjects.size(), existingSubjects.size());

        // Combine new and existing subjects
        List<Subject> allSubjects = new ArrayList<>(savedSubjects);
        allSubjects.addAll(existingSubjects);
        return allSubjects;
    }

    public Subject updateSubject(Long id, Subject subjectDetails) {
        log.info("Updating subject with id: {}", id);
        Subject existing = getSubjectByIdOrThrow(id);

        existing.setName(subjectDetails.getName());
        existing.setCoefficient(subjectDetails.getCoefficient());
        existing.setDepartment(subjectDetails.getDepartment());
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

    /* ============================ UTILITY METHODS ============================ */

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

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByIds(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        return subjectRepository.findAllById(subjectIds);
    }

    @Transactional(readOnly = true)
    public long getSubjectCount() {
        long count = subjectRepository.count();
        log.info("Total subjects in system: {}", count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<String> getAllSpecialties() {
        List<String> specialties = subjectRepository.findDistinctSpecialties();
        log.info("Found {} distinct specialties in system", specialties.size());
        return specialties;
    }

    /* ============================ PRIVATE HELPER METHODS ============================ */

    private boolean isCompulsorySubject(Subject subject, ClassLevel classLevel) {
        List<String> compulsoryNames = COMPULSORY_SUBJECT_NAMES.get(classLevel);
        return compulsoryNames != null && compulsoryNames.contains(subject.getName());
    }

    private boolean isDepartmentCoreSubject(Subject subject, Long departmentId, ClassLevel classLevel) {
        if (departmentId == null || subject.getDepartment() == null) {
            return false;
        }

        // Department core subjects apply to Forms 4-5 and Sixth Form
        if (classLevel == ClassLevel.FORM_4 || classLevel == ClassLevel.FORM_5 || classLevel.isSixthForm()) {
            DepartmentCode deptCode = subject.getDepartment().getCode();
            List<String> coreSubjects = DEPARTMENT_CORE_SUBJECTS.get(deptCode);
            return coreSubjects != null
                    && coreSubjects.contains(subject.getName())
                    && Objects.equals(subject.getDepartment().getId(), departmentId);
        }

        return false;
    }

    private boolean isSpecialtySubject(Subject subject, String specialty, ClassLevel classLevel) {
        if (specialty == null || specialty.isBlank() || !classLevel.isSixthForm()) {
            return false;
        }

        // For Sixth Form, include subjects that match the specialty
        // Handle comma-separated specialties
        if (subject.getSpecialty() == null || subject.getSpecialty().trim().isEmpty()) {
            return false;
        }

        List<String> subjectSpecialties = Arrays.asList(subject.getSpecialty().split(","));
        return subjectSpecialties.stream()
                .anyMatch(spec -> spec.trim().equalsIgnoreCase(specialty.trim()));
    }

    private boolean isSubjectForClassLevel(Subject subject, ClassLevel classLevel, boolean isSixthForm) {
        if (subject == null || subject.getName() == null) {
            return false;
        }

        String subjectName = subject.getName();

        // FORMS 1-5: Allow O-Level subjects and general subjects
        if (!isSixthForm) {
            // Forms 1-5 should see O-Level subjects and general subjects
            return subjectName.startsWith("O-") ||
                    (!subjectName.startsWith("A-") &&
                            subject.getDepartment() != null &&
                            subject.getDepartment().getCode() == DepartmentCode.GEN);
        }
        // SIXTH FORM: Allow A-Level subjects and general languages
        else {
            // Sixth Form should see A-Level subjects and English/French
            boolean isALevel = subjectName.startsWith("A-");
            boolean isGeneralLanguage = subjectName.equals("O-English Language") ||
                    subjectName.equals("O-French Language");

            return isALevel || isGeneralLanguage;
        }
    }

    private boolean isSubjectForForms1to5(String subjectName, Subject subject) {
        // For Forms 1-5, include:
        // 1. All O-Level subjects (starting with "O-")
        // 2. All subjects without A-Level prefix (general subjects)
        boolean isOLevel = subjectName.startsWith("O-");
        boolean isGeneralSubject = !subjectName.startsWith("A-");

        return isOLevel || isGeneralSubject;
    }

    private boolean isSubjectForSixthForm(String subjectName, Subject subject) {
        // For Sixth Form, include:
        // 1. All A-Level subjects (starting with "A-")
        // 2. General department languages (O-English, O-French)
        boolean isALevel = subjectName.startsWith("A-");
        boolean isGeneralLanguage = subjectName.equals("O-English Language") ||
                subjectName.equals("O-French Language");

        return isALevel || isGeneralLanguage;
    }

    private boolean isSubjectForDepartment(Subject subject, Long departmentId) {
        if (subject.getDepartment() == null) {
            log.debug("Subject {} has no department, excluding", subject.getName());
            return false;
        }

        if (departmentId == null) {
            return false;
        }

        // Include if:
        // 1. Subject belongs to General department (available to all)
        // 2. Subject belongs to the selected department
        boolean isGeneral = subject.getDepartment().getCode() == DepartmentCode.GEN;
        boolean matchesDepartment = subject.getDepartment().getId().equals(departmentId);

        log.debug("Department filter - Subject: {}, Dept: {} (ID: {}, Code: {}, IsGEN: {}), Requested: {}, Result: {}",
                subject.getName(),
                subject.getDepartment().getName(),
                subject.getDepartment().getId(),
                subject.getDepartment().getCode(),
                isGeneral,
                departmentId,
                isGeneral || matchesDepartment);

        return isGeneral || matchesDepartment;
    }

    // Enhanced specialty filtering
    private boolean isSubjectForSpecialty(Subject subject, String specialty, boolean isSixthForm) {
        // If no specialty specified, include all subjects that don't require specialty
        if (specialty == null || specialty.trim().isEmpty()) {
            // For Sixth Form, exclude subjects that require specific specialties
            return !isSixthForm || subject.getSpecialty() == null || subject.getSpecialty().trim().isEmpty();
        }

        // If specialty is specified, include subjects that match the specialty or have no specialty
        String subjectSpecialty = subject.getSpecialty();
        if (subjectSpecialty == null || subjectSpecialty.trim().isEmpty()) {
            return true; // Subjects without specialty are available to all
        }

        // Check if subject's specialty matches the requested specialty
        List<String> subjectSpecialties = Arrays.asList(subjectSpecialty.split(","));
        boolean matches = subjectSpecialties.stream()
                .anyMatch(spec -> spec.trim().equalsIgnoreCase(specialty.trim()));

        log.debug("Specialty match - Subject: {}, Specialty: {}, Requested: {}, Match: {}",
                subject.getName(), subjectSpecialty, specialty, matches);

        return matches;
    }

    private boolean isSixthFormClass(String classCode) {
        if (classCode == null) return false;
        return classCode.startsWith("LOWER_SIXTH") || classCode.startsWith("UPPER_SIXTH") || classCode.contains("SIXTH");
    }

    // FIXED: Improved class level parsing
    private ClassLevel toClassLevel(String classCode) {
        if (classCode == null) return ClassLevel.FORM_1;

        try {
            // Handle different class code formats
            if (classCode.startsWith("LOWER_SIXTH")) {
                return ClassLevel.LOWER_SIXTH;
            } else if (classCode.startsWith("UPPER_SIXTH")) {
                return ClassLevel.UPPER_SIXTH;
            } else {
                return ClassLevel.valueOf(classCode);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown class code: {}, defaulting to FORM_1", classCode);
            return ClassLevel.FORM_1;
        }
    }

    private void validateSubject(Subject subject) {
        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be empty");
        }
        if (subject.getCoefficient() == null || subject.getCoefficient() <= 0) {
            throw new IllegalArgumentException("Subject coefficient must be positive");
        }
    }

    // NEW: Method to force reinitialize subjects
    public List<Subject> reinitializeSubjects() {
        log.info("Reinitializing all subjects");
        subjectRepository.deleteAll();
        log.info("All subjects deleted, ready for reinitialization");
        return List.of();
    }

    // NEW: Method to get subject statistics
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
}