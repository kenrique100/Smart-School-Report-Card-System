package com.akentech.schoolreport.service;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Subject;
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

    public SubjectService(SubjectRepository subjectRepository, IdGenerationService idGenerationService) {
        this.subjectRepository = subjectRepository;
        this.idGenerationService = idGenerationService;
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Subject> getAllSubjects(Pageable pageable) {
        return subjectRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Subject> getSubjectsByFilters(String name, Long departmentId, String specialty, Pageable pageable) {
        return subjectRepository.findByFilters(name, departmentId, specialty, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Subject> getSubjectById(Long id) {
        return subjectRepository.findById(id);
    }

    // NEW: Get subject by ID or throw exception
    @Transactional(readOnly = true)
    public Subject getSubjectByIdOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject", id));
    }

    public Subject createSubject(Subject subject) {
        log.info("Creating new subject: {}", subject.getName());

        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            String subjectCode = idGenerationService.generateSubjectCode(subject);
            subject.setSubjectCode(subjectCode);
        }

        Subject saved = subjectRepository.save(subject);
        log.info("Created subject: {} (Code: {})", saved.getName(), saved.getSubjectCode());
        return saved;
    }

    public Subject updateSubject(Long id, Subject subjectDetails) {
        log.info("Updating subject with id: {}", id);

        Subject existingSubject = getSubjectByIdOrThrow(id);

        existingSubject.setName(subjectDetails.getName());
        existingSubject.setCoefficient(subjectDetails.getCoefficient());
        existingSubject.setDepartment(subjectDetails.getDepartment());
        existingSubject.setSpecialty(subjectDetails.getSpecialty());
        existingSubject.setDescription(subjectDetails.getDescription());

        Subject updated = subjectRepository.save(existingSubject);
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
    public List<Subject> getSubjectsByDepartment(Long departmentId) {
        return subjectRepository.findByDepartmentId(departmentId);
    }

    @Transactional(readOnly = true)
    public Page<Subject> getSubjectsByDepartment(Long departmentId, Pageable pageable) {
        return subjectRepository.findByDepartmentId(departmentId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByDepartmentAndSpecialty(Long departmentId, String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
            return getSubjectsByDepartment(departmentId);
        }
        return subjectRepository.findByDepartmentIdAndSpecialty(departmentId, specialty);
    }

    @Transactional(readOnly = true)
    public List<Subject> getCompulsorySubjectsForClass(String classCode) {
        List<Subject> compulsorySubjects = new ArrayList<>();

        if (classCode != null && classCode.matches("F[1-5]")) {
            List<String> compulsoryNames = Arrays.asList("Mathematics", "English Language", "French Language");
            compulsorySubjects = subjectRepository.findByNameIn(compulsoryNames);
        }

        return compulsorySubjects;
    }

    // NEW: Enhanced method to get subjects by class, department, and specialty
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByClassDepartmentAndSpecialty(String classCode, Long departmentId, String specialty) {
        List<Subject> subjects = new ArrayList<>();

        // Add compulsory subjects for Forms 1-5
        if (classCode != null && classCode.matches("F[1-5]")) {
            subjects.addAll(getCompulsorySubjectsForClass(classCode));
        }

        // Add department subjects
        if (departmentId != null) {
            if (specialty != null && !specialty.trim().isEmpty()) {
                subjects.addAll(getSubjectsByDepartmentAndSpecialty(departmentId, specialty));
            } else {
                subjects.addAll(getSubjectsByDepartment(departmentId));
            }
        }

        // Remove duplicates
        return subjects.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, List<Subject>>> getSubjectsGroupedByDepartmentAndSpecialty() {
        List<Subject> subjects = getAllSubjects();
        return subjects.stream()
                .collect(Collectors.groupingBy(
                        subject -> subject.getDepartment() != null ? subject.getDepartment().getName() : "No Department",
                        Collectors.groupingBy(
                                subject -> subject.getSpecialty() != null ? subject.getSpecialty() : "General"
                        )
                ));
    }

    @Transactional(readOnly = true)
    public List<String> getSpecialtiesByDepartment(Long departmentId) {
        return getSubjectsByDepartment(departmentId).stream()
                .map(Subject::getSpecialty)
                .filter(specialty -> specialty != null && !specialty.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // NEW: Method to find subjects by names
    @Transactional(readOnly = true)
    public List<Subject> findSubjectsByName(List<String> names) {
        return subjectRepository.findByNameIn(names);
    }

    // ADDED: Method for dashboard statistics
    @Transactional(readOnly = true)
    public long getSubjectCount() {
        return subjectRepository.count();
    }

    // NEW: Method to get subjects by multiple IDs
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByIds(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return new ArrayList<>();
        }
        return subjectRepository.findAllById(subjectIds);
    }
}