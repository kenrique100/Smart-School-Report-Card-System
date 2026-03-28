package com.akentech.schoolreport.util;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
@Slf4j
public class IdGenerationService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final AtomicLong studentSequence = new AtomicLong(0);
    private final AtomicLong subjectSequence = new AtomicLong(0);

    public IdGenerationService(StudentRepository studentRepository,
                               TeacherRepository teacherRepository,
                               SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        // Initialize with current count
        this.studentSequence.set(studentRepository.count());
        this.subjectSequence.set(subjectRepository.count());
    }

    @Transactional
    public String generateStudentId(Student student) {
        String departmentCode = extractDepartmentCode(student.getDepartment());
        String specialtyCode = extractSpecialtyCode(student.getSpecialty());

        // Generate unique sequence
        long sequence = studentSequence.incrementAndGet();
        String generatedId = String.format("STU%s%s%04d", departmentCode, specialtyCode, sequence);

        // Ensure uniqueness by checking database
        int attempt = 1;
        String finalId = generatedId;
        while (studentRepository.findByStudentId(finalId).isPresent()) {
            sequence = studentSequence.incrementAndGet();
            finalId = String.format("STU%s%s%04d-%d", departmentCode, specialtyCode, sequence, attempt);
            attempt++;
            log.warn("Duplicate student ID found. Generated alternative: {}", finalId);
        }

        log.debug("Generated student ID: {}", finalId);
        return finalId;
    }

    @Transactional
    public String generateRollNumber(Student student) {
        if (student.getClassRoom() == null) {
            throw new IllegalArgumentException("ClassRoom cannot be null for roll number generation");
        }
        if (student.getDepartment() == null) {
            throw new IllegalArgumentException("Department cannot be null for roll number generation");
        }

        String classCode = extractClassCode(student.getClassRoom());
        String deptCode = extractDepartmentCode(student.getDepartment());
        String specialtyCode = extractSpecialtyCode(student.getSpecialty());

        // Get count of students in same class, department, and specialty
        long count = studentRepository.countByClassRoomAndDepartmentAndSpecialty(
                student.getClassRoom(), student.getDepartment(), student.getSpecialty());

        // Generate base roll number
        String sequence = String.format("%03d", count + 1);

        // Ensure uniqueness
        String finalRollNumber = String.format("%s-%s-%s-%s", classCode, deptCode, specialtyCode, sequence);
        int attempt = 1;
        while (studentRepository.findByRollNumberAndClassRoom(finalRollNumber, student.getClassRoom()).isPresent()) {
            finalRollNumber = String.format("%s-%s-%s-%s-%d", classCode, deptCode, specialtyCode, sequence, attempt);
            attempt++;
            log.warn("Duplicate roll number found. Generated alternative: {}", finalRollNumber);
        }

        log.debug("Generated roll number: {}", finalRollNumber);
        return finalRollNumber;
    }

    public String generateTeacherId() {
        long sequence = teacherRepository.count() + 1;
        String baseId = String.format("TC%010d", sequence);

        // Ensure uniqueness
        int attempt = 1;
        String finalId = baseId;
        // Note: You'll need to add findByTeacherId method to TeacherRepository
        // For now, we'll assume it's unique based on sequence
        log.debug("Generated teacher ID: {}", finalId);
        return finalId;
    }

    public String generateSubjectCode(Subject subject) {
        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be null or empty");
        }

        String namePart = subject.getName().substring(0, Math.min(3, subject.getName().length())).toUpperCase();
        String deptCode = extractDepartmentCode(subject.getDepartment());
        String specialtyCode = extractSpecialtyCode(subject.getSpecialty());

        long count = subjectSequence.incrementAndGet();

        // FIXED: Changed from findByCode to findBySubjectCode
        String finalCode = String.format("%s-%s-%s-%03d", namePart, deptCode, specialtyCode, count);
        int attempt = 1;
        while (subjectRepository.findBySubjectCode(finalCode).isPresent()) {
            finalCode = String.format("%s-%s-%s-%03d-%d", namePart, deptCode, specialtyCode, count, attempt);
            attempt++;
            log.warn("Duplicate subject code found. Generated alternative: {}", finalCode);
        }

        log.debug("Generated subject code: {}", finalCode);
        return finalCode;
    }

    private String extractClassCode(ClassRoom classRoom) {
        if (classRoom == null || classRoom.getCode() == null) {
            log.warn("ClassRoom or its code is null, using default 'CL'");
            return "CL";
        }
        return classRoom.getCode().getCode() != null ? classRoom.getCode().getCode() : "CL";
    }

    private String extractDepartmentCode(Department department) {
        if (department == null || department.getCode() == null) {
            log.warn("Department or its code is null, using default 'GEN'");
            return "GEN";
        }
        return department.getCode().getCode() != null ? department.getCode().getCode() : "GEN";
    }

    private String extractSpecialtyCode(String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
            log.debug("Specialty is null or empty, using default 'GEN'");
            return "GEN";
        }

        String normalized = specialty.trim().toUpperCase();
        if (normalized.startsWith("S") && normalized.length() == 2) return normalized; // S1, S2, etc.
        if (normalized.startsWith("A") && normalized.length() == 2) return normalized; // A1, A2, etc.

        // Map longer specialty names to codes
        return switch (normalized) {
            case "ACCOUNTANCY" -> "ACC";
            case "MARKETING" -> "MKT";
            case "SAC(SECRETARIAL ADMINISTRATION AND COMMUNICATION)" -> "SAC";
            case "EPS(ELECTRICAL POWER SYSTEM)" -> "EPS";
            case "BC(BUILDING AND CONSTRUCTION)" -> "BC";
            case "CI(CLOTHING INDUSTRY)" -> "CI";
            default -> {
                log.warn("Unknown specialty: {}, using 'GEN'", normalized);
                yield "GEN";
            }
        };
    }
}