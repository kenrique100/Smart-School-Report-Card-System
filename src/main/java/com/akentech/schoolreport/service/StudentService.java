package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Student> getByClass(ClassRoom classRoom) {
        return studentRepository.findByClassRoom(classRoom);
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student saveStudent(Student student) {
        log.info("Saving student: {} {}", student.getFirstName(), student.getLastName());

        // For new students, generate IDs
        if (student.getId() == null) {
            log.info("Creating new student");

            if (student.getStudentId() == null) {
                String studentId = generateStudentId(student);
                student.setStudentId(studentId);
                log.info("Generated student ID: {}", studentId);
            }

            if (student.getRollNumber() == null || student.getRollNumber().isEmpty()) {
                String rollNumber = generateRollNumber(student);
                student.setRollNumber(rollNumber);
                log.info("Generated roll number: {}", rollNumber);
            }

        } else {
            // Preserve IDs for existing students
            log.info("Updating existing student ID: {}", student.getId());
            studentRepository.findById(student.getId()).ifPresent(existing -> {
                student.setStudentId(existing.getStudentId());
                student.setRollNumber(existing.getRollNumber());
                log.info("Preserved existing IDs for student {}", existing.getId());
            });
        }

        // Validate specialty based on class and department
        validateSpecialtyRequirement(student);

        // Validate academic year
        if (student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
            if (student.getAcademicYearStart() >= student.getAcademicYearEnd()) {
                throw new IllegalArgumentException("Academic year start must be before academic year end");
            }
        }

        Student savedStudent = studentRepository.save(student);
        log.info("Saved student: {} {} (ID: {}, Roll: {})",
                savedStudent.getFirstName(),
                savedStudent.getLastName(),
                savedStudent.getStudentId(),
                savedStudent.getRollNumber());

        return savedStudent;
    }

    private void validateSpecialtyRequirement(Student student) {
        if (student.getClassRoom() != null && student.getClassRoom().getCode() != null) {
            String classCode = student.getClassRoom().getCode();

            // For Forms 1-3 with General department, no specialty allowed
            if ((classCode.equals("F1") || classCode.equals("F2") || classCode.equals("F3")) &&
                    student.getDepartment() != null && "GEN".equals(student.getDepartment().getCode())) {
                student.setSpecialty(null);
            }

            // For Sixth Form with Science/Arts departments, specialty is required
            if ((classCode.equals("LSX") || classCode.equals("USX")) &&
                    student.getDepartment() != null &&
                    ("SCI".equals(student.getDepartment().getCode()) || "ART".equals(student.getDepartment().getCode()))) {
                if (student.getSpecialty() == null || student.getSpecialty().trim().isEmpty()) {
                    throw new IllegalArgumentException("Specialty is required for Sixth Form Science/Arts students");
                }
            }
        }
    }

    public boolean isSpecialtyRequired(String classCode, String departmentCode) {
        if (classCode == null || departmentCode == null) return false;

        // Specialty is required for Sixth Form Science and Arts
        return (classCode.equals("LSX") || classCode.equals("USX")) &&
                (departmentCode.equals("SCI") || departmentCode.equals("ART"));
    }

    public boolean isSpecialtyAllowed(String classCode, String departmentCode) {
        if (classCode == null || departmentCode == null) return false;

        // Specialty not allowed for Forms 1-3 with General department
        if ((classCode.equals("F1") || classCode.equals("F2") || classCode.equals("F3")) &&
                departmentCode.equals("GEN")) {
            return false;
        }

        // Specialty allowed for all other combinations
        return true;
    }

    private String generateStudentId(Student student) {
        long totalCount = studentRepository.count() + 1;

        String deptCode = "GEN";
        String specialtyCode = getSpecialtyCode(student.getSpecialty());

        if (student.getDepartment() != null) {
            Department department = student.getDepartment();
            if (department.getCode() != null && !department.getCode().isEmpty()) {
                deptCode = department.getCode().toUpperCase();
            } else if (department.getName() != null && department.getName().length() >= 3) {
                deptCode = department.getName().substring(0, 3).toUpperCase();
            } else if (department.getName() != null && !department.getName().isEmpty()) {
                deptCode = department.getName().toUpperCase();
            }
        }

        return String.format("STU%s%s%04d", deptCode, specialtyCode, totalCount);
    }

    private String generateRollNumber(Student student) {
        ClassRoom classRoom = student.getClassRoom();
        Department department = student.getDepartment();

        String classCode = "CL";
        String deptCode = "GEN";
        String specialtyCode = getSpecialtyCode(student.getSpecialty());

        // Handle class code safely
        if (classRoom != null) {
            if (classRoom.getCode() != null && !classRoom.getCode().isEmpty()) {
                classCode = classRoom.getCode().replaceAll("\\s+", "").toUpperCase();
            } else if (classRoom.getName() != null && classRoom.getName().length() >= 2) {
                classCode = classRoom.getName().substring(0, 2).toUpperCase();
            } else if (classRoom.getName() != null && !classRoom.getName().isEmpty()) {
                classCode = classRoom.getName().toUpperCase();
            }
        }

        // Handle department code safely
        if (department != null) {
            if (department.getCode() != null && !department.getCode().isEmpty()) {
                deptCode = department.getCode().toUpperCase();
            } else if (department.getName() != null && department.getName().length() >= 3) {
                deptCode = department.getName().substring(0, 3).toUpperCase();
            } else if (department.getName() != null && !department.getName().isEmpty()) {
                deptCode = department.getName().toUpperCase();
            }
        }

        long count = studentRepository.countByClassRoomAndDepartmentAndSpecialty(classRoom, department, student.getSpecialty());
        String sequence = String.format("%03d", count + 1);

        return classCode + "-" + deptCode + "-" + specialtyCode + "-" + sequence;
    }

    private String getSpecialtyCode(String specialty) {
        if (specialty == null) return "GEN";

        switch (specialty.trim().toUpperCase()) {
            case "ACCOUNTANCY": return "ACC";
            case "MARKETTING": return "MKT";
            case "SAC(SECRETARIAL ADMINISTRATION AND COMMUNICATION)": return "SAC";
            case "EPS(ELECTRICAL POWER SYSTEM)": return "EPS";
            case "BC(BUILDING AND CONSTRUCTION)": return "BC";
            case "CI(CLOTHING INDUSTRY)": return "CI";
            case "S1": case "S2": case "S3": case "S4": case "S5": case "S6": return specialty.toUpperCase();
            case "A1": case "A2": case "A3": case "A4": case "A5": return specialty.toUpperCase();
            default: return "GEN";
        }
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
            log.info("Deleted student id: {}", id);
        } else {
            throw new IllegalArgumentException("Student not found with id: " + id);
        }
    }

    public long getStudentCount() {
        return studentRepository.count();
    }

    public String getStudentFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    public List<String> getCommercialSpecialties() {
        return List.of("Accountancy", "Marketting", "SAC(Secretarial Administration and Communication)");
    }

    public List<String> getTechnicalSpecialties() {
        return List.of("EPS(Electrical Power System)", "BC(Building and Construction)", "CI(Clothing Industry)");
    }

    public List<String> getScienceSpecialties() {
        return List.of("S1", "S2", "S3", "S4", "S5", "S6");
    }

    public List<String> getArtsSpecialties() {
        return List.of("A1", "A2", "A3", "A4", "A5");
    }

    public List<String> getHomeEconomicsSpecialties() {
        return List.of("Home Economics", "Food and Nutrition", "Textiles");
    }

    public List<String> getGeneralSpecialties() {
        return List.of(); // Empty list for General department
    }

    public List<String> getAllSpecialties() {
        List<String> all = new ArrayList<>();
        all.addAll(getCommercialSpecialties());
        all.addAll(getTechnicalSpecialties());
        all.addAll(getScienceSpecialties());
        all.addAll(getArtsSpecialties());
        all.addAll(getHomeEconomicsSpecialties());
        return all;
    }

    public List<String> getSpecialtiesByDepartment(String departmentCode) {
        if (departmentCode == null) return new ArrayList<>();

        switch (departmentCode) {
            case "COM": return getCommercialSpecialties();
            case "TEC": return getTechnicalSpecialties();
            case "SCI": return getScienceSpecialties();
            case "ART": return getArtsSpecialties();
            case "HE": return getHomeEconomicsSpecialties();
            case "GEN": return getGeneralSpecialties();
            default: return new ArrayList<>();
        }
    }

    // AJAX endpoint to check specialty requirements
    public SpecialtyRequirement checkSpecialtyRequirement(String classCode, String departmentCode) {
        boolean required = isSpecialtyRequired(classCode, departmentCode);
        boolean allowed = isSpecialtyAllowed(classCode, departmentCode);
        List<String> specialties = getSpecialtiesByDepartment(departmentCode);

        return new SpecialtyRequirement(required, allowed, specialties);
    }

    // Helper class for AJAX response
    public static class SpecialtyRequirement {
        private boolean required;
        private boolean allowed;
        private List<String> specialties;

        public SpecialtyRequirement(boolean required, boolean allowed, List<String> specialties) {
            this.required = required;
            this.allowed = allowed;
            this.specialties = specialties;
        }

        // Getters and setters
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public boolean isAllowed() { return allowed; }
        public void setAllowed(boolean allowed) { this.allowed = allowed; }
        public List<String> getSpecialties() { return specialties; }
        public void setSpecialties(List<String> specialties) { this.specialties = specialties; }
    }
}