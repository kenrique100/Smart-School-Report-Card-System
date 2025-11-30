package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.model.enums.Gender;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.service.StudentService;
import com.akentech.schoolreport.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final NoticeRepository noticeRepository;
    private final StudentService studentService;
    private final SubjectService subjectService;

    // Enhanced Specialty definitions with subject mappings
    private final Map<String, List<String>> departmentSpecialties = createDepartmentSpecialties();
    private final Map<String, String> specialtyFullNames = createSpecialtyFullNames();
    private final Map<String, List<String>> specialtySubjectMappings = createSpecialtySubjectMappings();

    // Helper method to create department specialties
    private Map<String, List<String>> createDepartmentSpecialties() {
        Map<String, List<String>> specialties = new HashMap<>();
        specialties.put("SCI", Arrays.asList("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8"));
        specialties.put("ART", Arrays.asList("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8"));
        specialties.put("COM", Arrays.asList("C1", "C2", "C3", "C4"));
        specialties.put("TEC", Arrays.asList("T1", "T2", "T3", "T4", "T5"));
        specialties.put("HE", Arrays.asList("H1", "H2", "H3"));
        specialties.put("GEN", List.of());
        return specialties;
    }

    // Helper method to create specialty full names
    private Map<String, String> createSpecialtyFullNames() {
        Map<String, String> fullNames = new HashMap<>();
        // Science specialties
        fullNames.put("S1", "Mathematics With Mechanics, A-Physics, A-Chemistry");
        fullNames.put("S2", "A-Biology, A-Chemistry, A-Physics");
        fullNames.put("S3", "Mathematics With Statistics, A-Chemistry, A-Biology");
        fullNames.put("S4", "A-Biology, A-Chemistry, Geology");
        fullNames.put("S5", "Mathematics With Statistics, A-Chemistry, A-Biology, Computer Sciences");
        fullNames.put("S6", "A-Chemistry, A-Physics, Mathematics With Mechanics, A-Further Mathematics");
        fullNames.put("S7", "A-Chemistry, A-Biology, A-Physics, Mathematics With Mechanics");
        fullNames.put("S8", "A-Biology, A-Chemistry, A-Physics, Mathematics With Mechanics, Further Mathematics");

        // Arts specialties
        fullNames.put("A1", "A-History, A-Geography, A-Economics");
        fullNames.put("A2", "A-Literature in English, A-History, A-French");
        fullNames.put("A3", "A-Geography, A-Economics, Mathematics With Statistics");
        fullNames.put("A4", "A-History, A-Literature in English, A-Religious Studies");
        fullNames.put("A5", "A-Geography, A-Economics, A-ICT");
        fullNames.put("A6", "A-French, A-Literature in English, Philosophy");
        fullNames.put("A7", "A-History, A-Economics, A-Geography");
        fullNames.put("A8", "A-Religious Studies, Philosophy, A-Literature in English");

        // Commercial specialties
        fullNames.put("C1", "A-Accounting, A-Business Mathematics, A-Economics");
        fullNames.put("C2", "A-Commerce, A-Accounting, A-Business Studies");
        fullNames.put("C3", "A-Marketing, A-Commerce, A-Business Studies");
        fullNames.put("C4", "A-Office Practice, A-Shorthand, A-Typewriting");

        // Technical specialties
        fullNames.put("T1", "Electrical Technology, Technical Drawing, Physics");
        fullNames.put("T2", "Building Construction, Technical Drawing, Woodwork");
        fullNames.put("T3", "Mechanical Engineering, Workshop Practice, Technical Drawing");
        fullNames.put("T4", "Automobile Mechanics, Technical Drawing, Workshop Practice");
        fullNames.put("T5", "Plumbing & Pipefitting, Technical Drawing, Building Construction");

        // Home Economics specialties
        fullNames.put("H1", "Home Management, Food & Nutrition, Clothing & Textiles");
        fullNames.put("H2", "Food Science, Nutrition, Dietetics");
        fullNames.put("H3", "Clothing Technology, Fashion Design, Textile Science");

        return fullNames;
    }

    // ENHANCED: Specialty to Subject mapping with proper subject names
    private Map<String, List<String>> createSpecialtySubjectMappings() {
        Map<String, List<String>> mappings = new HashMap<>();

        // Science specialties - using exact subject names from initialization
        mappings.put("S1", Arrays.asList("A-Pure Mathematics with Mech", "A-Physics", "A-Chemistry"));
        mappings.put("S2", Arrays.asList("A-Biology", "A-Chemistry", "A-Physics"));
        mappings.put("S3", Arrays.asList("A-Pure Mathematics With Stats", "A-Chemistry", "A-Biology"));
        mappings.put("S4", Arrays.asList("A-Biology", "A-Chemistry", "A-Geology"));
        mappings.put("S5", Arrays.asList("A-Pure Mathematics With Stats", "A-Chemistry", "A-Biology", "A-Computer Science"));
        mappings.put("S6", Arrays.asList("A-Chemistry", "A-Physics", "A-Pure Mathematics with Mech", "A-Further Mathematics"));
        mappings.put("S7", Arrays.asList("A-Chemistry", "A-Biology", "A-Physics", "A-Pure Mathematics with Mech"));
        mappings.put("S8", Arrays.asList("A-Biology", "A-Chemistry", "A-Physics", "A-Pure Mathematics with Mech", "A-Further Mathematics"));

        // Arts specialties
        mappings.put("A1", Arrays.asList("A-History", "A-Geography", "A-Economics"));
        mappings.put("A2", Arrays.asList("A-Literature", "A-History", "A-French Language"));
        mappings.put("A3", Arrays.asList("A-Geography", "A-Economics", "A-Pure Mathematics With Stats"));
        mappings.put("A4", Arrays.asList("A-History", "A-Literature", "A-Religious Studies"));
        mappings.put("A5", Arrays.asList("A-Geography", "A-Economics", "A-ICT"));
        mappings.put("A6", Arrays.asList("A-French Language", "A-Literature", "A-Philosophy"));
        mappings.put("A7", Arrays.asList("A-History", "A-Economics", "A-Geography"));
        mappings.put("A8", Arrays.asList("A-Religious Studies", "A-Philosophy", "A-Literature"));

        // Commercial specialties
        mappings.put("C1", Arrays.asList("A-Accounting", "A-Business Management", "A-Economics"));
        mappings.put("C2", Arrays.asList("A-Commerce", "A-Accounting", "A-Business Management"));
        mappings.put("C3", Arrays.asList("A-Marketing", "A-Commerce", "A-Business Management"));
        mappings.put("C4", Arrays.asList("A-Office Practice", "A-Typewriting"));

        // Technical specialties
        mappings.put("T1", Arrays.asList("A-Electrical Technology", "Technical Drawing", "A-Physics"));
        mappings.put("T2", Arrays.asList("A-Building Construction", "Technical Drawing", "O-Woodwork"));
        mappings.put("T3", Arrays.asList("A-Mechanical Engineering", "Workshop Practice", "Technical Drawing"));
        mappings.put("T4", Arrays.asList("A-Automobile Mechanics", "Technical Drawing", "Workshop Practice"));
        mappings.put("T5", Arrays.asList("A-Building Construction", "Technical Drawing", "Workshop Practice"));

        // Home Economics specialties
        mappings.put("H1", Arrays.asList("A-Home Economics", "A-Nutrition and Food Science", "A-Clothing Technology"));
        mappings.put("H2", Arrays.asList("A-Nutrition and Food Science", "A-Home Economics", "A-Food Science"));
        mappings.put("H3", Arrays.asList("A-Clothing Technology", "A-Fashion Design", "A-Home Economics"));

        return mappings;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            initDepartments();
            initClassRooms();
            initTeachers();
            initSubjects();
            initStudents();
            initNotices();
            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Data initialization failed", e);
            throw e;
        }
    }

    private void initDepartments() {
        if (departmentRepository.count() == 0) {
            List<Department> departments = Arrays.asList(
                    Department.builder().name("General").code(DepartmentCode.GEN).description("General Studies Department").build(),
                    Department.builder().name("Sciences").code(DepartmentCode.SCI).description("Science Department").build(),
                    Department.builder().name("Arts").code(DepartmentCode.ART).description("Arts Department").build(),
                    Department.builder().name("Commercial").code(DepartmentCode.COM).description("Commercial Department").build(),
                    Department.builder().name("Technical").code(DepartmentCode.TEC).description("Technical Department").build(),
                    Department.builder().name("Home Economics").code(DepartmentCode.HE).description("Home Economics Department").build()
            );
            departmentRepository.saveAll(departments);
            log.info("Default departments created");
        }
    }

    private void initClassRooms() {
        if (classRoomRepository.count() == 0) {
            List<ClassRoom> classRooms = Arrays.asList(
                    ClassRoom.builder().name("Form 1").code(ClassLevel.FORM_1).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 2").code(ClassLevel.FORM_2).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 3").code(ClassLevel.FORM_3).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 4").code(ClassLevel.FORM_4).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 5").code(ClassLevel.FORM_5).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Lower Sixth").code(ClassLevel.LOWER_SIXTH).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Upper Sixth").code(ClassLevel.UPPER_SIXTH).academicYear("2025/2026").build()
            );
            classRoomRepository.saveAll(classRooms);
            log.info("Default classrooms created (Form 1–5 + Sixth Form)");
        }
    }

    // ... (rest of the initTeachers method remains the same)

    private void initTeachers() {
        if (teacherRepository.count() == 0) {
            List<Teacher> teachers = new ArrayList<>();

            // Science Department Teachers
            Teacher physicsTeacher = Teacher.builder()
                    .teacherId("TC100001")
                    .firstName("Dr. Samuel")
                    .lastName("Ngassa")
                    .gender(Gender.MALE)
                    .contact("677112233")
                    .skills("A-Physics,Mathematics With Mechanics,Additional Mathematics,Further Mathematics")
                    .build();
            teachers.add(physicsTeacher);

            Teacher biologyTeacher = Teacher.builder()
                    .teacherId("TC100002")
                    .firstName("Dr. Amina")
                    .lastName("Mohammed")
                    .gender(Gender.FEMALE)
                    .contact("677445566")
                    .skills("A-Biology,A-Chemistry,Geology,Environmental Science")
                    .build();
            teachers.add(biologyTeacher);

            // ... (rest of teacher initialization remains the same)

            teacherRepository.saveAll(teachers);
            log.info("{} teachers created with specialized subject assignments", teachers.size());
        }
    }

    // ENHANCED: Subject initialization with specialty assignments
    private void initSubjects() {
        if (subjectRepository.count() == 0) {
            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (DepartmentCode code : DepartmentCode.values()) {
                departmentRepository.findByCode(code).ifPresent(dept -> deptMap.put(code, dept));
            }

            List<Subject> subjects = new ArrayList<>();

            // ============================================================
            // 1️⃣ GENERAL COMPULSORY SUBJECTS (All Departments - Forms 1-5)
            // ============================================================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("O-English Language").coefficient(3)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-ENG").description("Compulsory English Language").build(),

                    Subject.builder().name("O-French Language").coefficient(2)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-FREN").description("Compulsory French Language").build(),

                    Subject.builder().name("O-Mathematics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-MATH").description("Core Mathematics").build(),

                    Subject.builder().name("Physical Education").coefficient(1)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-PE").description("Physical Education").build(),

                    // Optional (applies to all departments)
                    Subject.builder().name("O-Computer Science").coefficient(2)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-COMP-SCI")
                            .description("Optional Computer Studies (Ordinary Level)")
                            .optional(true).build(),

                    Subject.builder().name("O-ICT").coefficient(2)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-ICT-SCI")
                            .description("Optional ICT (Ordinary Level)")
                            .optional(true).build(),

                    Subject.builder().name("O-Religious Studies").coefficient(2)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-REL-STUD")
                            .description("Optional Religious Studies (Ordinary Level)")
                            .optional(true).build(),

                    Subject.builder().name("Citizenship Education").coefficient(2)
                            .department(deptMap.get(DepartmentCode.GEN))
                            .subjectCode("O-CITIZEN")
                            .description("Citizenship and Moral Education")
                            .optional(true).build()
            ));

            // ============================================================
            // 2️⃣ SCIENCE DEPARTMENT (GCE Ordinary Level - Forms 1-5)
            // ============================================================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("O-Biology").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("O-BIO-SCI").description("Ordinary Level Biology").build(),

                    Subject.builder().name("O-Human Biology").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("O-HUM-BIO").description("Ordinary Level Human Biology").build(),

                    Subject.builder().name("O-Chemistry").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("O-CHEM-SCI").description("Ordinary Level Chemistry").build(),

                    Subject.builder().name("O-Physics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("O-PHY-SCI").description("Ordinary Level Physics").build(),

                    Subject.builder().name("O-Additional Mathematics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("O-ADD-MATH-SCI").description("Ordinary Level Additional Mathematics").build(),

                    // Advanced Level Sciences (Sixth Form Only) - WITH SPECIALTY ASSIGNMENTS
                    Subject.builder().name("A-Pure Mathematics with Mech").coefficient(5)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-PMATH-Mech").description("Advanced Level Pure Mathematics with Mechanics")
                            .specialty("S1,S6,S7,S8").build(),

                    Subject.builder().name("A-Pure Mathematics With Stats").coefficient(5)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-PMATH-Stats").description("Advanced Level Pure Mathematics with Statistics")
                            .specialty("S3,S5").build(),

                    Subject.builder().name("A-Physics").coefficient(5)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-PHY-SCI").description("Advanced Level Physics")
                            .specialty("S1,S2,S6,S7,S8").build(),

                    Subject.builder().name("A-Chemistry").coefficient(5)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-CHEM-SCI").description("Advanced Level Chemistry")
                            .specialty("S1,S2,S3,S4,S5,S6,S7,S8").build(),

                    Subject.builder().name("A-Biology").coefficient(5)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-BIO-SCI").description("Advanced Level Biology")
                            .specialty("S2,S3,S4,S5,S7,S8").build(),

                    Subject.builder().name("A-Further Mathematics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-FMATH-SCI").description("Advanced Level Further Mathematics")
                            .specialty("S6,S8")
                            .optional(true).build(),

                    Subject.builder().name("A-Computer Science").coefficient(3)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-COMP-SCI")
                            .description("Optional Advanced Level Computer Science")
                            .specialty("S5")
                            .optional(true).build(),

                    Subject.builder().name("A-ICT").coefficient(3)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-ICT-SCI")
                            .description("Optional Advanced Level ICT")
                            .optional(true).build(),

                    Subject.builder().name("Food Science").coefficient(3)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-FOOD-SCI")
                            .description("Optional Advanced Level Food Science")
                            .optional(true).build(),

                    Subject.builder().name("A-Geology").coefficient(4)
                            .department(deptMap.get(DepartmentCode.SCI))
                            .subjectCode("A-GEO-SCI").description("Advanced Level Geology")
                            .specialty("S4")
                            .optional(true).build()
            ));

            // ... (rest of subject initialization for other departments with specialty assignments)

            // ============================================================
            // ✅ SAVE ALL SUBJECTS
            // ============================================================
            try {
                List<Subject> savedSubjects = subjectService.createSubjects(subjects);
                log.info("✅ Enhanced Cameroon GCE subjects initialized with proper O-Level/Advanced Level separation and specialty assignments - {} subjects processed", savedSubjects.size());

                // Log subject distribution by department and specialty
                Map<String, Long> subjectCountByDept = savedSubjects.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getDepartment() != null ? s.getDepartment().getName() : "General",
                                Collectors.counting()
                        ));

                Map<String, Long> specialtySubjectCount = savedSubjects.stream()
                        .filter(s -> s.getSpecialty() != null && !s.getSpecialty().isEmpty())
                        .collect(Collectors.groupingBy(
                                Subject::getSpecialty,
                                Collectors.counting()
                        ));

                log.info("Subject distribution: {}", subjectCountByDept);
                log.info("Specialty subject distribution: {}", specialtySubjectCount);

            } catch (Exception e) {
                log.error("Critical error during subject initialization", e);
                throw new RuntimeException("Failed to initialize subjects", e);
            }
        } else {
            log.info("Subjects already initialized, skipping subject creation");
        }
    }

    // ENHANCED: Student initialization with proper specialty handling
    private void initStudents() {
        if (studentRepository.count() == 0) {
            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (DepartmentCode code : DepartmentCode.values()) {
                departmentRepository.findByCode(code).ifPresent(dept -> deptMap.put(code, dept));
            }

            Map<ClassLevel, ClassRoom> classMap = new HashMap<>();
            for (ClassLevel level : ClassLevel.values()) {
                classRoomRepository.findByCode(level).ifPresent(cls -> classMap.put(level, cls));
            }

            List<Student> students = new ArrayList<>();

            // =========================
            // SIXTH FORM STUDENTS (Advanced Level with strict specialties)
            // =========================
            ClassRoom lowerSixth = classMap.get(ClassLevel.LOWER_SIXTH);
            ClassRoom upperSixth = classMap.get(ClassLevel.UPPER_SIXTH);

            // Create students for each specialty in Science department (including S7)
            Department scienceDept = deptMap.get(DepartmentCode.SCI);
            List<String> scienceSpecialties = departmentSpecialties.get("SCI");

            for (String specialty : scienceSpecialties) {
                // Lower Sixth Students
                students.add(createStudentWithGender(
                        "L6SCI" + specialty + "M",
                        getAdvancedLastName(specialty),
                        lowerSixth,
                        scienceDept,
                        specialty,
                        2005,
                        2006,
                        Gender.MALE
                ));

                students.add(createStudentWithGender(
                        "L6SCI" + specialty + "F",
                        getAdvancedLastName(specialty),
                        lowerSixth,
                        scienceDept,
                        specialty,
                        2005,
                        2006,
                        Gender.FEMALE
                ));

                // Upper Sixth Students
                students.add(createStudentWithGender(
                        "U6SCI" + specialty + "M",
                        getAdvancedLastName(specialty),
                        upperSixth,
                        scienceDept,
                        specialty,
                        2004,
                        2005,
                        Gender.MALE
                ));

                students.add(createStudentWithGender(
                        "U6SCI" + specialty + "F",
                        getAdvancedLastName(specialty),
                        upperSixth,
                        scienceDept,
                        specialty,
                        2004,
                        2005,
                        Gender.FEMALE
                ));
            }

            // Save all students
            students.forEach(s -> {
                try {
                    // Get appropriate subjects based on class, department, and specialty
                    List<Subject> appropriateSubjects = getAppropriateSubjectsForStudent(s);
                    List<Long> subjectIds = appropriateSubjects.stream()
                            .map(Subject::getId)
                            .collect(Collectors.toList());

                    studentService.createStudent(s, subjectIds);
                    log.info("Created student {} with specialty {} and {} subjects",
                            s.getFirstName(), s.getSpecialty(), subjectIds.size());
                } catch (Exception e) {
                    log.warn("Failed to create student {}: {}", s.getFirstName(), e.getMessage());
                }
            });
            log.info("Complete student data initialized with proper department and specialty assignments - {} students", students.size());

            // Log student distribution
            logStudentDistribution(students);
        }
    }

    // ENHANCED: Get appropriate subjects for student based on class, department, and specialty
    private List<Subject> getAppropriateSubjectsForStudent(Student student) {
        List<Subject> appropriateSubjects = new ArrayList<>();

        if (student.getClassRoom() == null || student.getDepartment() == null) {
            return appropriateSubjects;
        }

        ClassLevel classLevel = student.getClassRoom().getCode();
        DepartmentCode deptCode = student.getDepartment().getCode();
        String specialty = student.getSpecialty();

        // Get all subjects
        List<Subject> allSubjects = subjectRepository.findAll();

        for (Subject subject : allSubjects) {
            if (isSubjectAppropriateForStudent(subject, student, classLevel, deptCode, specialty)) {
                appropriateSubjects.add(subject);
            }
        }

        log.debug("Found {} appropriate subjects for student {} with specialty {}",
                appropriateSubjects.size(), student.getFirstName(), specialty);

        return appropriateSubjects;
    }

    // ENHANCED: Improved subject appropriateness check
    private boolean isSubjectAppropriateForStudent(Subject subject, Student student,
                                                   ClassLevel classLevel, DepartmentCode deptCode,
                                                   String specialty) {
        if (subject == null || subject.getDepartment() == null) {
            return false;
        }

        String subjectName = subject.getName();
        DepartmentCode subjectDeptCode = subject.getDepartment().getCode();

        // Check class level compatibility
        if (classLevel.isFormLevel()) {
            // Forms 1-5: Only O-Level subjects or subjects without level prefix
            if (subjectName.startsWith("A-")) {
                return false; // Advanced subjects not allowed in Forms 1-5
            }
        } else {
            // Sixth Form: Only A-Level subjects and general languages
            if (subjectName.startsWith("O-") &&
                    !subjectName.equals("O-English Language") &&
                    !subjectName.equals("O-French Language")) {
                return false; // Ordinary subjects not allowed in Sixth Form (except languages)
            }
        }

        // Check department compatibility
        if (subjectDeptCode != DepartmentCode.GEN) {
            // Department-specific subject must match student's department
            if (subjectDeptCode != deptCode) {
                return false;
            }
        }

        // Check specialty compatibility for Sixth Form
        if (classLevel.isSixthForm() && specialty != null && !specialty.isBlank()) {
            String subjectSpecialty = subject.getSpecialty();
            if (subjectSpecialty != null && !subjectSpecialty.isEmpty()) {
                // Subject has specific specialty requirements
                List<String> allowedSpecialties = Arrays.asList(subjectSpecialty.split(","));
                if (!allowedSpecialties.contains(specialty)) {
                    return Boolean.TRUE.equals(subject.getOptional());
                }
            }
        }

        return true;
    }

    // Helper method to create student with specific gender
    private Student createStudentWithGender(String firstName, String lastName, ClassRoom cls, Department dept,
                                            String specialty, int birthYearStart, int birthYearEnd, Gender gender) {
        return Student.builder()
                .firstName(firstName)
                .lastName(lastName)
                .classRoom(cls)
                .department(dept)
                .specialty(specialty)
                .gender(gender)
                .dateOfBirth(generateRandomBirthDate(birthYearStart, birthYearEnd))
                .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@student.com")
                .address(getRandomCameroonianAddress())
                .academicYearStart(2025)
                .academicYearEnd(2026)
                .build();
    }

    private LocalDate generateRandomBirthDate(int startYear, int endYear) {
        int year = ThreadLocalRandom.current().nextInt(startYear, endYear + 1);
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, 29);
        return LocalDate.of(year, month, day);
    }

    // Helper method to get advanced level last names
    private String getAdvancedLastName(String specialty) {
        return switch (specialty) {
            case "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8" -> "Scientist";
            case "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8" -> "Academic";
            case "C1", "C2", "C3", "C4" -> "Executive";
            case "T1", "T2", "T3", "T4", "T5" -> "Technologist";
            case "H1", "H2", "H3" -> "Expert";
            default -> "Graduate";
        };
    }

    // Helper method to generate random Cameroonian addresses
    private String getRandomCameroonianAddress() {
        String[] areas = {"Bastos, Yaoundé", "Bonapriso, Douala", "Melen, Yaoundé", "Bonanjo, Douala",
                "Akwa, Douala", "Nsimeyong, Yaoundé", "Makepe, Douala", "Bamend, Bamenda",
                "Molyko, Buea", "Commercial Ave, Limbe"};
        return areas[ThreadLocalRandom.current().nextInt(areas.length)];
    }

    // Helper method to log student distribution
    private void logStudentDistribution(List<Student> students) {
        Map<String, Long> classCount = students.stream()
                .collect(Collectors.groupingBy(s -> s.getClassRoom().getName(), Collectors.counting()));

        Map<String, Long> genderCount = students.stream()
                .collect(Collectors.groupingBy(s -> s.getGender().toString(), Collectors.counting()));

        Map<String, Long> deptCount = students.stream()
                .collect(Collectors.groupingBy(s -> s.getDepartment().getName(), Collectors.counting()));

        Map<String, Long> specialtyCount = students.stream()
                .filter(s -> s.getSpecialty() != null)
                .collect(Collectors.groupingBy(Student::getSpecialty, Collectors.counting()));

        log.info("Student Distribution by Class: {}", classCount);
        log.info("Student Distribution by Gender: {}", genderCount);
        log.info("Student Distribution by Department: {}", deptCount);
        log.info("Student Distribution by Specialty: {}", specialtyCount);

        // Log specialty details with full names
        log.info("Available Specialties by Department:");
        departmentSpecialties.forEach((dept, specs) -> {
            if (!specs.isEmpty()) {
                List<String> specialtyDetails = specs.stream()
                        .map(spec -> spec + " (" + specialtyFullNames.getOrDefault(spec, "General") + ")")
                        .collect(Collectors.toList());
                log.info("  {}: {}", dept, String.join("; ", specialtyDetails));
            }
        });
    }

    private void initNotices() {
        if (noticeRepository.count() == 0) {
            Teacher teacher = teacherRepository.findAll().getFirst();
            Notice notice = Notice.builder()
                    .title("School Reopening - 2025/2026 Academic Year")
                    .content("School will reopen on September 2nd, 2025 for the new academic year. All students are expected to be present in full uniform. GCE registration forms for both Ordinary and Advanced Levels are available at the administration office.")
                    .createdDate(LocalDateTime.now())
                    .teacher(teacher)
                    .isActive(true)
                    .build();
            noticeRepository.save(notice);
            log.info("Default notice created");
        }
    }
}