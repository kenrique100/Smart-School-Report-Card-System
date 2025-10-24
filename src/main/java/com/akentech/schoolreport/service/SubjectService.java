package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<Subject> getAll() {
        return subjectRepository.findAll();
    }

    public Optional<Subject> getById(Long id) {
        return subjectRepository.findById(id);
    }

    @Transactional
    public Subject save(Subject subject) {
        // Auto-generate subject code if not provided
        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            String subjectCode = generateSubjectCode(subject);
            subject.setSubjectCode(subjectCode);
        }

        Subject saved = subjectRepository.save(subject);
        log.info("Saved subject: {} (Code: {}, Dept: {}, Specialty: {})",
                saved.getName(), saved.getSubjectCode(),
                saved.getDepartment() != null ? saved.getDepartment().getName() : "None",
                saved.getSpecialty() != null ? saved.getSpecialty() : "General");
        return saved;
    }

    public void delete(Long id) {
        subjectRepository.deleteById(id);
        log.info("Deleted subject id={}", id);
    }

    public List<Subject> getSubjectsByDepartment(Long departmentId) {
        return subjectRepository.findByDepartmentId(departmentId);
    }

    public List<Subject> getSubjectsByDepartmentAndSpecialty(Long departmentId, String specialty) {
        return subjectRepository.findByDepartmentIdAndSpecialty(departmentId, specialty);
    }

    public Map<String, List<Subject>> getSubjectsGroupedByDepartment() {
        List<Subject> subjects = getAll();
        return subjects.stream()
                .collect(Collectors.groupingBy(
                        subject -> subject.getDepartment() != null ? subject.getDepartment().getName() : "No Department"
                ));
    }

    public Map<String, Map<String, List<Subject>>> getSubjectsGroupedByDepartmentAndSpecialty() {
        List<Subject> subjects = getAll();
        return subjects.stream()
                .collect(Collectors.groupingBy(
                        subject -> subject.getDepartment() != null ? subject.getDepartment().getName() : "No Department",
                        Collectors.groupingBy(
                                subject -> subject.getSpecialty() != null ? subject.getSpecialty() : "General"
                        )
                ));
    }

    private String generateSubjectCode(Subject subject) {
        String namePart = subject.getName().substring(0, Math.min(3, subject.getName().length())).toUpperCase();
        String deptPart = subject.getDepartment() != null ?
                subject.getDepartment().getCode() : "GEN";
        String specialtyPart = subject.getSpecialty() != null ?
                subject.getSpecialty().substring(0, Math.min(2, subject.getSpecialty().length())).toUpperCase() : "GEN";

        long count = subjectRepository.count() + 1;
        return String.format("%s-%s-%s-%03d", namePart, deptPart, specialtyPart, count);
    }

    // Get all unique specialties across departments
    public List<String> getAllSpecialties() {
        return getAll().stream()
                .map(Subject::getSpecialty)
                .filter(specialty -> specialty != null && !specialty.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // Get specialties by department
    public List<String> getSpecialtiesByDepartment(Long departmentId) {
        return getSubjectsByDepartment(departmentId).stream()
                .map(Subject::getSpecialty)
                .filter(specialty -> specialty != null && !specialty.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}