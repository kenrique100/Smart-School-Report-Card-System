package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.StudentSubject;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.repository.StudentSubjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class StudentEnrollmentService {

    private final StudentSubjectRepository studentSubjectRepository;
    private final SubjectService subjectService;

    public StudentEnrollmentService(StudentSubjectRepository studentSubjectRepository,
                                    SubjectService subjectService) {
        this.studentSubjectRepository = studentSubjectRepository;
        this.subjectService = subjectService;
    }

    public void enrollStudentInSubjects(Student student, List<Long> subjectIds) {
        log.info("Enrolling student {} in {} subjects", student.getStudentId(),
                subjectIds != null ? subjectIds.size() : 0);

        // Get current enrollments to preserve scores
        Map<Long, StudentSubject> existingEnrollments = getExistingEnrollmentsWithScores(student.getId());

        // Clear existing enrollments
        studentSubjectRepository.deleteByStudentId(student.getId());

        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> selectedSubjects = getSelectedSubjectsWithCompulsory(student, subjectIds);

            for (Subject subject : selectedSubjects) {
                boolean isCompulsory = isCompulsorySubject(subject, student.getClassRoom());

                // Preserve existing score if available
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
                log.debug("Enrolled student {} in subject: {} (Compulsory: {})",
                        student.getStudentId(), subject.getName(), isCompulsory);
            }

            log.info("Enrolled student {} in {} subjects", student.getStudentId(), selectedSubjects.size());
        } else {
            log.info("No subjects selected for student {}", student.getStudentId());
        }
    }

    private Map<Long, StudentSubject> getExistingEnrollmentsWithScores(Long studentId) {
        List<StudentSubject> existing = studentSubjectRepository.findByStudentIdWithSubject(studentId);
        return existing.stream()
                .collect(Collectors.toMap(
                        ss -> ss.getSubject().getId(),
                        ss -> ss
                ));
    }

    private List<Subject> getSelectedSubjectsWithCompulsory(Student student, List<Long> subjectIds) {
        Set<Subject> selectedSubjects = new HashSet<>();

        // Add explicitly selected subjects
        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> explicitlySelected = subjectService.getAllSubjects().stream()
                    .filter(subject -> subjectIds.contains(subject.getId()))
                    .collect(Collectors.toList());
            selectedSubjects.addAll(explicitlySelected);
        }

        // Add compulsory subjects for Forms 1-5
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null) {
            ClassLevel classLevel = student.getClassRoom().getCode();
            if (classLevel.isFormLevel()) {
                List<Subject> compulsorySubjects = getCompulsorySubjects(student, classLevel);
                selectedSubjects.addAll(compulsorySubjects);
            }
        }

        return new ArrayList<>(selectedSubjects);
    }

    private List<Subject> getCompulsorySubjects(Student student, ClassLevel classLevel) {
        List<Subject> compulsorySubjects = new ArrayList<>();

        // Core compulsory subjects for all forms
        List<String> compulsoryNames = Arrays.asList("Mathematics", "English Language", "French Language");
        List<Subject> allCompulsory = subjectService.getAllSubjects().stream()
                .filter(subject -> compulsoryNames.contains(subject.getName()))
                .collect(Collectors.toList());
        compulsorySubjects.addAll(allCompulsory);

        // Add department-specific compulsory subjects for Forms 4-5
        if (classLevel == ClassLevel.FORM_4 || classLevel == ClassLevel.FORM_5) {
            if (student.getDepartment() != null) {
                List<Subject> deptCompulsory = getDepartmentCompulsorySubjects(student);
                compulsorySubjects.addAll(deptCompulsory);
            }
        }

        return compulsorySubjects;
    }

    private List<Subject> getDepartmentCompulsorySubjects(Student student) {
        List<Subject> deptSubjects = new ArrayList<>();
        String deptCode = student.getDepartment().getCode().name();

        switch (deptCode) {
            case "SCI":
                deptSubjects.addAll(subjectService.getAllSubjects().stream()
                        .filter(s -> Arrays.asList("Biology", "Chemistry", "Physics").contains(s.getName()))
                        .collect(Collectors.toList()));
                break;
            case "ART":
                deptSubjects.addAll(subjectService.getAllSubjects().stream()
                        .filter(s -> Arrays.asList("History", "Geography").contains(s.getName()))
                        .collect(Collectors.toList()));
                break;
            case "COM":
                deptSubjects.addAll(subjectService.getAllSubjects().stream()
                        .filter(s -> Arrays.asList("Commerce", "Accounting", "Economics").contains(s.getName()))
                        .collect(Collectors.toList()));
                break;
        }

        return deptSubjects;
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
    public List<StudentSubject> getStudentEnrollments(Long studentId) {
        List<StudentSubject> enrollments = studentSubjectRepository.findByStudentIdWithSubject(studentId);
        log.debug("Found {} enrollments for student ID: {}", enrollments.size(), studentId);

        // Sort by subject name for consistent display
        enrollments.sort(Comparator.comparing(ss -> ss.getSubject().getName()));

        return enrollments;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentEnrollmentSummary(Long studentId) {
        List<StudentSubject> enrollments = getStudentEnrollments(studentId);
        Map<String, Object> summary = new HashMap<>();

        int totalSubjects = enrollments.size();
        int subjectsWithScores = (int) enrollments.stream()
                .filter(ss -> ss.getScore() != null)
                .count();
        int compulsorySubjects = (int) enrollments.stream()
                .filter(StudentSubject::getIsCompulsory)
                .count();

        // Calculate average score
        OptionalDouble averageScore = enrollments.stream()
                .filter(ss -> ss.getScore() != null)
                .mapToDouble(StudentSubject::getScore)
                .average();

        summary.put("totalSubjects", totalSubjects);
        summary.put("subjectsWithScores", subjectsWithScores);
        summary.put("compulsorySubjects", compulsorySubjects);
        summary.put("averageScore", averageScore.isPresent() ?
                Math.round(averageScore.getAsDouble() * 100.0) / 100.0 : 0.0);
        summary.put("studentSubjects", enrollments); // CRITICAL: Include the actual list

        log.debug("Student enrollment summary for ID {}: {}", studentId, summary);
        return summary;
    }

    // NEW: Method to add individual subject enrollment
    public StudentSubject enrollStudentInSubject(Student student, Subject subject) {
        boolean isCompulsory = isCompulsorySubject(subject, student.getClassRoom());

        StudentSubject enrollment = StudentSubject.builder()
                .student(student)
                .subject(subject)
                .isCompulsory(isCompulsory)
                .build();

        return studentSubjectRepository.save(enrollment);
    }

    // NEW: Method to remove individual subject enrollment
    public void removeStudentFromSubject(Long studentId, Long subjectId) {
        studentSubjectRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .ifPresent(studentSubjectRepository::delete);
    }
}