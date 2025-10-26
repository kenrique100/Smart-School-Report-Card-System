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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // Clear existing enrollments
        studentSubjectRepository.deleteByStudentId(student.getId());

        if (subjectIds != null && !subjectIds.isEmpty()) {
            List<Subject> selectedSubjects = getSelectedSubjectsWithCompulsory(student, subjectIds);

            for (Subject subject : selectedSubjects) {
                boolean isCompulsory = isCompulsorySubject(subject.getName(), student.getClassRoom());

                StudentSubject enrollment = StudentSubject.builder()
                        .student(student)
                        .subject(subject)
                        .isCompulsory(isCompulsory)
                        .build();

                studentSubjectRepository.save(enrollment);
            }

            log.info("Enrolled student {} in {} subjects", student.getStudentId(), selectedSubjects.size());
        }
    }

    private List<Subject> getSelectedSubjectsWithCompulsory(Student student, List<Long> subjectIds) {
        List<Subject> selectedSubjects = subjectService.getAllSubjects().stream()
                .filter(subject -> subjectIds.contains(subject.getId()))
                .collect(Collectors.toList());

        // Add compulsory subjects for Forms 1-5
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null) {
            ClassLevel classLevel = student.getClassRoom().getCode();
            if (classLevel.isFormLevel()) {
                List<Subject> compulsorySubjects = subjectService.getCompulsorySubjectsForClass(
                        classLevel.getCode() // Use the string code
                );

                // Add compulsory subjects that are not already selected
                for (Subject compulsory : compulsorySubjects) {
                    if (selectedSubjects.stream().noneMatch(s -> s.getId().equals(compulsory.getId()))) {
                        selectedSubjects.add(compulsory);
                    }
                }
            }
        }

        return selectedSubjects;
    }

    private boolean isCompulsorySubject(String subjectName, ClassRoom classRoom) {
        if (classRoom != null && classRoom.getCode() != null) {
            ClassLevel classLevel = classRoom.getCode();
            if (classLevel.isFormLevel()) {
                List<String> compulsoryNames = Arrays.asList("Mathematics", "English Language", "French Language");
                return compulsoryNames.contains(subjectName);
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<StudentSubject> getStudentEnrollments(Long studentId) {
        return studentSubjectRepository.findByStudentId(studentId);
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

        summary.put("totalSubjects", totalSubjects);
        summary.put("subjectsWithScores", subjectsWithScores);
        summary.put("compulsorySubjects", compulsorySubjects);
        summary.put("enrollments", enrollments);

        return summary;
    }
}