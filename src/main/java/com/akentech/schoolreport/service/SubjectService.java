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
        List<String> forms4to5Core = Arrays.asList("O-Mathematics", "O-English Language", "O-French Language", "Physical Education");
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
        deptCoreMap.put(DepartmentCode.HE, Arrays.asList("Food and Nutrition", "Home Management", "Clothing and Textiles"));
        deptCoreMap.put(DepartmentCode.GEN, Arrays.asList("O-Computer Science", "O-ICT", "O-Religious Studies", "Citizenship Education"));
        deptCoreMap.put(DepartmentCode.EPS, Arrays.asList("Technical drawing", "Circuit System"));

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
        log.info("🔍 Filtering subjects for classCode: {}, departmentId: {}, specialty: {}",
                classCode, departmentId, specialty);

        List<Subject> allSubjects = subjectRepository.findAll();
        log.info("📊 Total subjects in system: {}", allSubjects.size());

        // FIXED: Use ClassLevel.fromString instead of valueOf
        ClassLevel classLevel = ClassLevel.fromString(classCode);
        boolean isSixthForm = classLevel.isSixthForm();
        boolean isForm1 = classLevel == ClassLevel.FORM_1;
        log.info("🎓 Class level: {} (code: {}, display: {}), Is Sixth Form: {}, Is Form 1: {}",
                classLevel, classLevel.getCode(), classLevel.getDisplayName(), isSixthForm, isForm1);

        // Special handling for Form 1
        if (isForm1) {
            List<Subject> filteredSubjects = allSubjects.stream()
                    .filter(subject -> {
                        boolean departmentMatch = subject.getDepartment() != null &&
                                subject.getDepartment().getId().equals(departmentId);

                        // For Form 1, include all subjects from General department
                        // Form 1 students don't have specialties yet
                        boolean specialtyMatch = subject.getSpecialty() == null ||
                                subject.getSpecialty().isEmpty();

                        // Exclude advanced subjects
                        boolean isAdvanced = subject.getName() != null &&
                                (subject.getName().contains("(Advanced)") ||
                                        subject.getName().startsWith("A-"));

                        // Exclude subjects marked for higher forms
                        boolean isForHigherForm = subject.getSubjectCode() != null &&
                                (subject.getSubjectCode().matches(".*-F[2-5]$") ||
                                        subject.getSubjectCode().matches(".*-(LSX|USX)$"));

                        boolean result = departmentMatch && specialtyMatch && !isAdvanced && !isForHigherForm;

                        if (result) {
                            log.debug("✅ INCLUDED: {} (Dept: {}, Specialty: {}, SubjectCode: {})",
                                    subject.getName(),
                                    subject.getDepartment() != null ? subject.getDepartment().getName() : "None",
                                    subject.getSpecialty(),
                                    subject.getSubjectCode());
                        } else {
                            log.debug("❌ EXCLUDED: {} - DeptMatch: {}, SpecialtyMatch: {}, IsAdvanced: {}, IsForHigherForm: {}",
                                    subject.getName(),
                                    departmentMatch,
                                    specialtyMatch,
                                    isAdvanced,
                                    isForHigherForm);
                        }

                        return result;
                    })
                    .sorted(Comparator.comparing(Subject::getName))
                    .collect(Collectors.toList());

            log.info("📚 Found {} subjects for Form 1", filteredSubjects.size());
            return filteredSubjects;
        }

        // Non-Form 1 cases
        List<Subject> filteredSubjects = allSubjects.stream()
                .filter(subject -> {
                    // Filter by class level
                    boolean classLevelMatch = isSubjectForClassLevel(subject, classLevel, isSixthForm);

                    // Filter by department
                    boolean departmentMatch = isSubjectForDepartment(subject, departmentId, isSixthForm);

                    // Filter by specialty
                    boolean specialtyMatch = isSubjectForSpecialty(subject, specialty, isSixthForm);

                    boolean result = classLevelMatch && departmentMatch && specialtyMatch;

                    if (result) {
                        log.debug("✅ INCLUDED: {} (Dept: {}, Specialty: {}, SubjectCode: {})",
                                subject.getName(),
                                subject.getDepartment() != null ? subject.getDepartment().getName() : "None",
                                subject.getSpecialty(),
                                subject.getSubjectCode());
                    }

                    return result;
                })
                .sorted(Comparator.comparing(Subject::getName))
                .collect(Collectors.toList());

        log.info("📚 Found {} subjects for {} with department {} and specialty {}",
                filteredSubjects.size(), classLevel.getDisplayName(), departmentId, specialty);
        return filteredSubjects;
    }

    @Transactional(readOnly = true)
    public Map<String, List<Subject>> getGroupedSubjectsForEnrollment(String classCode, Long departmentId, String specialty) {
        log.info("Grouping subjects for enrollment for class: {}, department: {}, specialty: {}",
                classCode, departmentId, specialty);

        List<Subject> availableSubjects = getSubjectsByClassDepartmentAndSpecialty(classCode, departmentId, specialty);
        ClassLevel classLevel = ClassLevel.fromString(classCode);

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
        if (subject.getSpecialty() == null || subject.getSpecialty().trim().isEmpty()) {
            return false;
        }

        // Handle comma-separated specialties
        List<String> subjectSpecialties = Arrays.asList(subject.getSpecialty().split(","));
        return subjectSpecialties.stream()
                .anyMatch(spec -> spec.trim().equalsIgnoreCase(specialty.trim()));
    }

    private boolean isSubjectForClassLevel(Subject subject, ClassLevel classLevel, boolean isSixthForm) {
        if (subject == null || subject.getName() == null) {
            return false;
        }

        String subjectName = subject.getName();
        boolean isOLevel = subjectName.startsWith("O-");
        boolean isALevel = subjectName.startsWith("A-");

        // Special handling for languages and mathematics
        boolean isCompulsoryLanguage = subjectName.equals("O-English Language") ||
                subjectName.equals("O-French Language") ||
                subjectName.equals("O-Mathematics");

        // FORMS 1-5: Show O-Level subjects
        if (!isSixthForm) {
            // Forms 1-5 should see compulsory languages and O-Level subjects
            if (isCompulsoryLanguage) {
                return true; // Languages are compulsory for Forms 1-5
            }
            return isOLevel || !isALevel;
        }
        // SIXTH FORM: Show A-Level subjects only
        else {
            // Sixth Form should NOT see O-English, O-French, or O-Mathematics
            if (isCompulsoryLanguage) {
                return false; // O-languages and O-maths are NOT for sixth form
            }

            // Check if it's an optional subject for Sixth Form
            boolean isOptionalForSixthForm = subjectName.equals("A-Further Mathematics") ||
                    subjectName.equals("A-Computer Science") ||
                    subjectName.equals("A-ICT");

            if (isOptionalForSixthForm) {
                return true; // These are optional for sixth form
            }

            // Include A-Level subjects and subjects without O-/A- prefix that are not from General department
            if (isALevel) {
                return true;
            }

            // Include subjects without O-/A- prefix that are in Science department
            if (!isOLevel && subject.getDepartment() != null &&
                    subject.getDepartment().getCode() == DepartmentCode.SCI) {
                return true;
            }

            // Exclude O-Level subjects for Sixth Form
            return !isOLevel;
        }
    }

    private boolean isSubjectForDepartment(Subject subject, Long departmentId, boolean isSixthForm) {
        if (subject.getDepartment() == null) {
            log.debug("Subject {} has no department, excluding", subject.getName());
            return false;
        }

        if (departmentId == null) {
            return false;
        }

        // Always include subjects from General department (except O-languages for Sixth Form)
        if (subject.getDepartment().getCode() == DepartmentCode.GEN) {
            // For sixth form, exclude O-French, O-English, and O-Maths from GEN department
            if (isSixthForm) {
                String subjectName = subject.getName();
                boolean isCompulsoryLanguage = subjectName.equals("O-English Language") ||
                        subjectName.equals("O-French Language") ||
                        subjectName.equals("O-Mathematics");

                if (isCompulsoryLanguage) {
                    return false; // O-languages and O-maths are NOT for sixth form
                }

                // A-ICT from GEN department is optional for all Sixth Form
                boolean isICT = subjectName.equals("A-ICT");
                if (isICT) {
                    return true; // A-ICT is optional for all Sixth Form
                }
            }
            return true; // Other GEN subjects are available to all
        }

        // For sixth form Science: A-Computer Science and A-ICT are optional for all specialties
        if (isSixthForm && subject.getDepartment().getCode() == DepartmentCode.SCI) {
            String subjectName = subject.getName();
            boolean isComputerScience = subjectName.equals("A-Computer Science");
            boolean isICT = subjectName.equals("A-ICT");

            // These are available to all Science specialties (specialty is null)
            if (isComputerScience || isICT) {
                return true;
            }
        }

        // Check if subject belongs to the student's department
        boolean matchesDepartment = subject.getDepartment().getId().equals(departmentId);

        return matchesDepartment;
    }

    private boolean isSubjectForSpecialty(Subject subject, String specialty, boolean isSixthForm) {
        // If no specialty specified, include subjects that don't require specific specialty
        if (specialty == null || specialty.trim().isEmpty()) {
            // For Sixth Form, include subjects with null specialty or subjects that are optional
            if (isSixthForm) {
                String subjectName = subject.getName();
                boolean isOptionalForAll = subjectName.equals("A-Computer Science") ||
                        subjectName.equals("A-ICT") ||
                        subjectName.equals("A-Further Mathematics");

                if (isOptionalForAll) {
                    return true; // These are optional for all Sixth Form students
                }

                // Include subjects without specialty for Sixth Form
                return subject.getSpecialty() == null || subject.getSpecialty().trim().isEmpty();
            }
            // For Forms 1-5, include all subjects without specialty
            return subject.getSpecialty() == null || subject.getSpecialty().trim().isEmpty();
        }

        // If specialty is specified, include subjects that match the specialty or have no specialty
        String subjectSpecialty = subject.getSpecialty();
        if (subjectSpecialty == null || subjectSpecialty.trim().isEmpty()) {
            // Check if this is an optional subject for all specialties
            String subjectName = subject.getName();
            boolean isOptionalForAll = subjectName.equals("A-Computer Science") ||
                    subjectName.equals("A-ICT");

            if (isOptionalForAll && isSixthForm) {
                return true; // These are optional for all Sixth Form specialties
            }

            // Subjects without specialty are available to all specialties
            return true;
        }

        // Check if subject's specialty matches the requested specialty
        List<String> subjectSpecialties = Arrays.asList(subjectSpecialty.split(","));
        boolean matches = subjectSpecialties.stream()
                .anyMatch(spec -> spec.trim().equalsIgnoreCase(specialty.trim()));

        return matches;
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