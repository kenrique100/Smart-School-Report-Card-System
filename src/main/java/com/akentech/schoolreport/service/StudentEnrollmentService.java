package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.model.enums.PerformanceLevel;
import com.akentech.schoolreport.repository.StudentSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentEnrollmentService {

    private final StudentSubjectRepository studentSubjectRepository;
    @Lazy
    private final SubjectService subjectService;

    private static final Map<ClassLevel, List<String>> COMPULSORY_SUBJECT_NAMES = createEnhancedCompulsoryMap();
    private static final Map<DepartmentCode, List<String>> DEPARTMENT_CORE_SUBJECTS = createDepartmentCoreMap();

    private static Map<ClassLevel, List<String>> createEnhancedCompulsoryMap() {
        Map<ClassLevel, List<String>> compulsoryMap = new HashMap<>();

        compulsoryMap.put(ClassLevel.FORM_1, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language", "O-Religious Studies"));
        compulsoryMap.put(ClassLevel.FORM_2, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language", "O-Religious Studies"));
        compulsoryMap.put(ClassLevel.FORM_3, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language", "O-Religious Studies"));

        compulsoryMap.put(ClassLevel.FORM_4, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language"));
        compulsoryMap.put(ClassLevel.FORM_5, Arrays.asList("O-Mathematics", "O-English Language", "O-French Language"));

        compulsoryMap.put(ClassLevel.LOWER_SIXTH, new ArrayList<>());
        compulsoryMap.put(ClassLevel.UPPER_SIXTH, new ArrayList<>());

        return compulsoryMap;
    }

    private static Map<DepartmentCode, List<String>> createDepartmentCoreMap() {
        Map<DepartmentCode, List<String>> deptCoreMap = new HashMap<>();

        deptCoreMap.put(DepartmentCode.SCI, Arrays.asList("O-Biology", "O-Chemistry", "O-Physics"));
        deptCoreMap.put(DepartmentCode.ART, Arrays.asList("O-History", "O-Geography", "O-Literature in English"));
        deptCoreMap.put(DepartmentCode.COM, Arrays.asList("O-Accounting", "O-Commerce", "O-Economics"));
        deptCoreMap.put(DepartmentCode.TEC, Arrays.asList("Technical Drawing", "Workshop Practice", "Engineering Science"));
        deptCoreMap.put(DepartmentCode.HE, Arrays.asList("Food and Nutrition", "Home Management", "Clothing and Textiles"));
        deptCoreMap.put(DepartmentCode.GEN, new ArrayList<>());

        return deptCoreMap;
    }

    // NEW: Method to enroll existing student in new subjects
    public void enrollStudentInAdditionalSubjects(Long studentId, List<Long> subjectIds) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }

        if (subjectIds == null || subjectIds.isEmpty()) {
            log.warn("No subject ID provided for student {}", studentId);
            return;
        }

        log.info("Enrolling student {} in {} additional subject's", studentId, subjectIds.size());

        // Get existing enrollments
        List<StudentSubject> existingEnrollments = studentSubjectRepository.findByStudentId(studentId);
        Set<Long> existingSubjectIds = existingEnrollments.stream()
                .map(ss -> ss.getSubject().getId())
                .collect(Collectors.toSet());

        // Filter out subjects the student is already enrolled in
        List<Long> newSubjectIds = subjectIds.stream()
                .filter(subjectId -> !existingSubjectIds.contains(subjectId))
                .collect(Collectors.toList());

        if (newSubjectIds.isEmpty()) {
            log.info("Student {} is already enrolled in all requested subjects", studentId);
            return;
        }

        // Get the subjects
        List<Subject> newSubjects = subjectService.getSubjectsByIds(newSubjectIds);

        // Get student from existing enrollment (or you could fetch it separately)
        Student student = existingEnrollments.isEmpty() ? null : existingEnrollments.getFirst().getStudent();
        if (student == null) {
            throw new EntityNotFoundException("Student", studentId);
        }

        // Enroll in new subjects
        for (Subject subject : newSubjects) {
            boolean isCompulsory = isCompulsorySubject(subject, student.getClassRoom()) ||
                    isDepartmentCoreSubject(subject, student.getDepartment(), student.getClassRoom());

            StudentSubject enrollment = StudentSubject.builder()
                    .student(student)
                    .subject(subject)
                    .isCompulsory(isCompulsory)
                    .build();

            studentSubjectRepository.save(enrollment);
            log.info("Enrolled student {} in new subject: {}", studentId, subject.getName());
        }

        log.info("Successfully enrolled student {} in {} new subject(s)", studentId, newSubjects.size());
    }

    // NEW: Method to get available subjects for enrollment (not already enrolled)
    @Transactional(readOnly = true)
    public List<Subject> getAvailableSubjectsForEnrollment(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }

        // Get student's current subjects
        List<StudentSubject> currentEnrollments = studentSubjectRepository.findByStudentId(studentId);
        Set<Long> currentSubjectIds = currentEnrollments.stream()
                .map(ss -> ss.getSubject().getId())
                .collect(Collectors.toSet());

        // Get student info
        Student student = currentEnrollments.isEmpty() ? null : currentEnrollments.getFirst().getStudent();
        if (student == null || student.getClassRoom() == null || student.getDepartment() == null) {
            return new ArrayList<>();
        }

        // Get all available subjects for student
        String classCode = student.getClassRoom().getCode().name();
        Long departmentId = student.getDepartment().getId();
        String specialty = student.getSpecialty();

        List<Subject> allAvailableSubjects = subjectService.getSubjectsByClassDepartmentAndSpecialty(
                classCode, departmentId, specialty);

        // Filter out already enrolled subjects
        return allAvailableSubjects.stream()
                .filter(subject -> !currentSubjectIds.contains(subject.getId()))
                .collect(Collectors.toList());
    }

    public void enrollStudentInSubjects(Student student, List<Long> subjectIds) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }

        log.info("Enrolling student {} in {} subjects", student.getStudentId(),
                subjectIds != null ? subjectIds.size() : 0);

        if (subjectIds == null || subjectIds.isEmpty()) {
            log.warn("No subject IDs provided for student {}", student.getStudentId());
            enrollAppropriateSubjects(student);
            return;
        }

        validateSubjectIds(subjectIds);
        Map<Long, StudentSubject> existingEnrollments = getExistingEnrollmentsWithScores(student.getId());

        studentSubjectRepository.deleteByStudentId(student.getId());
        log.info("Cleared existing enrollments for student {}", student.getStudentId());

        List<Subject> selectedSubjects = getSelectedSubjectsWithAutoEnrollment(student, subjectIds);

        log.info("Found {} subjects to enroll student in (including auto-enrolled)", selectedSubjects.size());

        for (Subject subject : selectedSubjects) {
            boolean isCompulsory = isCompulsorySubject(subject, student.getClassRoom()) ||
                    isDepartmentCoreSubject(subject, student.getDepartment(), student.getClassRoom());

            Double existingScore = null;
            StudentSubject existingEnrollment = existingEnrollments.get(subject.getId());
            if (existingEnrollment != null) {
                existingScore = existingEnrollment.getScore();
            }

            StudentSubject enrollment = StudentSubject.builder()
                    .student(student)
                    .subject(subject)
                    .score(existingScore)
                    .isCompulsory(isCompulsory)
                    .build();

            studentSubjectRepository.save(enrollment);
        }

        log.info("Successfully enrolled student {} in {} subjects", student.getStudentId(), selectedSubjects.size());
    }

    @Transactional(readOnly = true)
    public List<StudentSubject> getStudentEnrollments(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }

        // FIXED: Use repository method that fetches subject and department
        List<StudentSubject> enrollments = studentSubjectRepository.findByStudentIdWithSubject(studentId);
        enrollments.sort(Comparator.comparing(ss -> ss.getSubject().getName()));
        return enrollments;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentEnrollmentSummary(Long studentId) {
        List<StudentSubject> enrollments = getStudentEnrollments(studentId);
        Map<String, Object> summary = new HashMap<>();

        int totalSubjects = enrollments.size();
        int subjectsWithScores = (int) enrollments.stream().filter(ss -> ss.getScore() != null).count();
        int compulsorySubjects = (int) enrollments.stream()
                .filter(ss -> ss.getIsCompulsory() != null && ss.getIsCompulsory()).count();

        OptionalDouble averageScore = enrollments.stream()
                .filter(ss -> ss.getScore() != null)
                .mapToDouble(StudentSubject::getScore)
                .average();

        double totalCoefficient = enrollments.stream()
                .mapToDouble(ss -> {
                    Subject subject = ss.getSubject();
                    return subject != null && subject.getCoefficient() != null ? subject.getCoefficient() : 1.0;
                })
                .sum();

        double weightedSum = enrollments.stream()
                .filter(ss -> ss.getScore() != null)
                .mapToDouble(ss -> {
                    Subject subject = ss.getSubject();
                    double coefficient = subject != null && subject.getCoefficient() != null ? subject.getCoefficient() : 1.0;
                    return ss.getScore() * coefficient;
                })
                .sum();

        double weightedAverage = totalCoefficient > 0 ? weightedSum / totalCoefficient : 0.0;

        summary.put("totalSubjects", totalSubjects);
        summary.put("subjectsWithScores", subjectsWithScores);
        summary.put("compulsorySubjects", compulsorySubjects);
        summary.put("averageScore", averageScore.isPresent() ? Math.round(averageScore.getAsDouble() * 100.0) / 100.0 : 0.0);
        summary.put("weightedAverage", Math.round(weightedAverage * 100.0) / 100.0);
        summary.put("totalCoefficient", Math.round(totalCoefficient * 100.0) / 100.0);
        summary.put("studentSubjects", enrollments);

        return summary;
    }

    public void updateStudentScore(Long studentId, Long subjectId, Double score) {
        if (studentId == null || subjectId == null) {
            throw new IllegalArgumentException("Student ID and subject ID cannot be null");
        }

        if (score != null && (score < 0 || score > 100)) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }

        StudentSubject enrollment = studentSubjectRepository
                .findByStudentIdAndSubjectId(studentId, subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment",
                        "studentId: " + studentId + ", subjectId: " + subjectId));

        enrollment.setScore(score);
        enrollment.setPerformance(calculatePerformanceLevel(score));
        studentSubjectRepository.save(enrollment);

        log.info("Updated score for student {} in subject {} to {}", studentId, subjectId, score);
    }

    public PerformanceLevel calculatePerformanceLevel(Double score) {
        if (score == null) return null;
        if (score >= 80) return PerformanceLevel.EXCELLENT;
        if (score >= 70) return PerformanceLevel.GOOD;
        if (score >= 60) return PerformanceLevel.FAIR;
        if (score >= 50) return PerformanceLevel.AVERAGE;
        return PerformanceLevel.FAIL;
    }

    public void removeStudentFromSubject(Long studentId, Long subjectId) {
        studentSubjectRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .ifPresent(enrollment -> {
                    studentSubjectRepository.delete(enrollment);
                    log.info("Removed student {} from subject {}", studentId, subjectId);
                });
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolledInSubject(Long studentId, Long subjectId) {
        return studentSubjectRepository.existsByStudent_IdAndSubject_Id(studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public long countEnrollmentsByStudent(Long studentId) {
        return studentSubjectRepository.countByStudent_Id(studentId);
    }

    @Transactional(readOnly = true)
    public List<StudentSubject> getEnrollmentsBySubject(Long subjectId) {
        return studentSubjectRepository.findBySubject_Id(subjectId);
    }

    private void enrollAppropriateSubjects(Student student) {
        List<Subject> appropriateSubjects = getAppropriateSubjectsForStudent(student);

        if (appropriateSubjects.isEmpty()) {
            log.warn("No appropriate subjects found for student {}", student.getStudentId());
            return;
        }

        log.info("Auto-enrolling student {} in {} appropriate subjects",
                student.getStudentId(), appropriateSubjects.size());

        for (Subject subject : appropriateSubjects) {
            boolean isCompulsory = isCompulsorySubject(subject, student.getClassRoom()) ||
                    isDepartmentCoreSubject(subject, student.getDepartment(), student.getClassRoom());

            StudentSubject enrollment = StudentSubject.builder()
                    .student(student)
                    .subject(subject)
                    .isCompulsory(isCompulsory)
                    .build();

            studentSubjectRepository.save(enrollment);
        }
    }

    private List<Subject> getAppropriateSubjectsForStudent(Student student) {
        if (student.getClassRoom() == null || student.getDepartment() == null) {
            return new ArrayList<>();
        }

        String classCode = student.getClassRoom().getCode().name();
        Long departmentId = student.getDepartment().getId();
        String specialty = student.getSpecialty();

        Map<String, List<Subject>> groupedSubjects = subjectService.getGroupedSubjectsForEnrollment(
                classCode, departmentId, specialty);

        List<Subject> appropriateSubjects = new ArrayList<>(groupedSubjects.getOrDefault("compulsory", new ArrayList<>()));

        ClassLevel classLevel = student.getClassRoom().getCode();
        if ((classLevel == ClassLevel.FORM_4 || classLevel == ClassLevel.FORM_5 || classLevel.isSixthForm())) {
            appropriateSubjects.addAll(groupedSubjects.getOrDefault("department", new ArrayList<>()));
        }

        if (classLevel.isSixthForm() && specialty != null && !specialty.trim().isEmpty()) {
            appropriateSubjects.addAll(groupedSubjects.getOrDefault("specialty", new ArrayList<>()));
        }

        return appropriateSubjects;
    }

    private List<Subject> getSelectedSubjectsWithAutoEnrollment(Student student, List<Long> subjectIds) {
        Set<Subject> selectedSubjects = new HashSet<>();

        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> explicitlySelected = subjectService.getSubjectsByIds(subjectIds).stream()
                    .filter(subject -> isSubjectAppropriateForStudent(subject, student))
                    .toList();
            selectedSubjects.addAll(explicitlySelected);
        }

        List<Subject> autoEnrolledSubjects = getAppropriateSubjectsForStudent(student);
        selectedSubjects.addAll(autoEnrolledSubjects);

        return new ArrayList<>(selectedSubjects);
    }

    private boolean isCompulsorySubject(Subject subject, ClassRoom classRoom) {
        if (classRoom == null || classRoom.getCode() == null || subject == null) {
            return false;
        }

        ClassLevel classLevel = classRoom.getCode();
        List<String> compulsoryNames = COMPULSORY_SUBJECT_NAMES.get(classLevel);
        return compulsoryNames != null && compulsoryNames.contains(subject.getName());
    }

    private boolean isDepartmentCoreSubject(Subject subject, Department department, ClassRoom classRoom) {
        if (department == null || subject.getDepartment() == null || classRoom == null) {
            return false;
        }

        ClassLevel classLevel = classRoom.getCode();
        DepartmentCode deptCode = department.getCode();

        if (classLevel != ClassLevel.FORM_4 && classLevel != ClassLevel.FORM_5) {
            return false;
        }

        List<String> coreSubjects = DEPARTMENT_CORE_SUBJECTS.get(deptCode);
        return coreSubjects != null && coreSubjects.contains(subject.getName()) &&
                subject.getDepartment().getId().equals(department.getId());
    }

    private boolean isSubjectAppropriateForStudent(Subject subject, Student student) {
        if (student == null || student.getClassRoom() == null || subject == null) {
            return false;
        }

        ClassLevel classLevel = student.getClassRoom().getCode();
        if (classLevel == null) {
            return false;
        }

        String subjectName = subject.getName();
        if (subjectName == null) {
            return false;
        }

        if (classLevel.isSixthForm()) {
            boolean isAdvanced = subjectName.startsWith("A-") ||
                    (!subjectName.startsWith("O-") &&
                            subject.getDepartment() != null &&
                            !subject.getDepartment().getCode().name().equals("GEN"));

            if (!isAdvanced && subject.getDepartment() != null &&
                    subject.getDepartment().getCode() == DepartmentCode.GEN &&
                    (subjectName.equals("O-English Language") || subjectName.equals("O-French Language"))) {
                isAdvanced = true;
            }

            return isAdvanced;
        } else {
            return subjectName.startsWith("O-") ||
                    (!subjectName.startsWith("A-") &&
                            (subject.getDepartment() == null ||
                                    subject.getDepartment().getCode() == DepartmentCode.GEN));
        }
    }

    private void validateSubjectIds(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return;
        }

        List<Subject> existingSubjects = subjectService.getSubjectsByIds(subjectIds);
        Set<Long> existingSubjectIds = existingSubjects.stream()
                .map(Subject::getId)
                .collect(Collectors.toSet());

        List<Long> invalidSubjectIds = subjectIds.stream()
                .filter(id -> !existingSubjectIds.contains(id))
                .toList();

        if (!invalidSubjectIds.isEmpty()) {
            throw new EntityNotFoundException("Subjects", "IDs: " + invalidSubjectIds);
        }
    }

    private Map<Long, StudentSubject> getExistingEnrollmentsWithScores(Long studentId) {
        if (studentId == null) {
            return new HashMap<>();
        }

        List<StudentSubject> existing = studentSubjectRepository.findByStudent_Id(studentId);
        return existing.stream()
                .collect(Collectors.toMap(
                        ss -> ss.getSubject().getId(),
                        ss -> ss
                ));
    }
}