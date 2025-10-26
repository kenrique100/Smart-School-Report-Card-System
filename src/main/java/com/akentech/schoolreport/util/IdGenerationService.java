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

@Service
@Transactional(readOnly = true)
@Slf4j
public class IdGenerationService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;

    public IdGenerationService(StudentRepository studentRepository,
                               TeacherRepository teacherRepository,
                               SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
    }

    public String generateStudentId(Student student) {
        String departmentCode = extractDepartmentCode(student.getDepartment());
        String specialtyCode = extractSpecialtyCode(student.getSpecialty());

        // Use sequence-based approach to avoid duplicates
        long sequence = studentRepository.count() + 1;
        return String.format("STU%s%s%04d", departmentCode, specialtyCode, sequence);
    }

    public String generateTeacherId() {
        long sequence = teacherRepository.count() + 1;
        return String.format("TC%010d", sequence);
    }

    public String generateRollNumber(Student student) {
        ClassRoom classRoom = student.getClassRoom();
        Department department = student.getDepartment();

        String classCode = extractClassCode(classRoom);
        String deptCode = extractDepartmentCode(department);
        String specialtyCode = extractSpecialtyCode(student.getSpecialty());

        long count = studentRepository.countByClassRoomAndDepartmentAndSpecialty(
                classRoom, department, student.getSpecialty());
        String sequence = String.format("%03d", count + 1);

        return String.format("%s-%s-%s-%s", classCode, deptCode, specialtyCode, sequence);
    }

    public String generateSubjectCode(Subject subject) {
        String namePart = subject.getName().substring(0, Math.min(3, subject.getName().length())).toUpperCase();
        String deptCode = extractDepartmentCode(subject.getDepartment());
        String specialtyCode = extractSpecialtyCode(subject.getSpecialty());

        long count = subjectRepository.count() + 1;
        return String.format("%s-%s-%s-%03d", namePart, deptCode, specialtyCode, count);
    }

    private String extractClassCode(ClassRoom classRoom) {
        if (classRoom == null || classRoom.getCode() == null) {
            return "CL";
        }
        // Use the enum's code string
        return classRoom.getCode().getCode();
    }

    private String extractDepartmentCode(Department department) {
        if (department == null || department.getCode() == null) {
            return "GEN";
        }
        // Use the enum's code string
        return department.getCode().getCode();
    }

    private String extractSpecialtyCode(String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
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
            default -> "GEN";
        };
    }
}