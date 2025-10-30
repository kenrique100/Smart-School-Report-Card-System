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

    // Specialty definitions - FIXED: Use HashMap instead of Map.of for large maps
    private final Map<String, List<String>> departmentSpecialties = createDepartmentSpecialties();

    private final Map<String, String> specialtyFullNames = createSpecialtyFullNames();

    // Helper method to create department specialties
    private Map<String, List<String>> createDepartmentSpecialties() {
        Map<String, List<String>> specialties = new HashMap<>();
        specialties.put("SCI", Arrays.asList("S1", "S2", "S3", "S4", "S5", "S6"));
        specialties.put("ART", Arrays.asList("A1", "A2", "A3", "A4", "A5"));
        specialties.put("COM", Arrays.asList("Accountancy", "Marketing", "SAC"));
        specialties.put("TEC", Arrays.asList("EPS", "BC", "CI", "MECH"));
        specialties.put("HE", Arrays.asList("Home Economics", "Food and Nutrition", "Textiles"));
        specialties.put("GEN", Arrays.asList());
        return specialties;
    }

    // Helper method to create specialty full names - FIXED: Use HashMap for large maps
    private Map<String, String> createSpecialtyFullNames() {
        Map<String, String> fullNames = new HashMap<>();
        // Science specialties
        fullNames.put("S1", "Mathematics With Mechanics, Physics, Chemistry");
        fullNames.put("S2", "Biology, Chemistry, Physics");
        fullNames.put("S3", "Mathematics, Physics, Computer Science");
        fullNames.put("S4", "Biology, Chemistry, Geography");
        fullNames.put("S5", "Mathematics, Economics, Geography");
        fullNames.put("S6", "Biology, Agriculture, Chemistry");

        // Arts specialties
        fullNames.put("A1", "History, Geography, Economics");
        fullNames.put("A2", "Literature, History, French");
        fullNames.put("A3", "Economics, Geography, Mathematics");
        fullNames.put("A4", "History, Religious Studies, Literature");
        fullNames.put("A5", "Geography, Economics, Computer Science");

        // Commercial specialties
        fullNames.put("Accountancy", "Accounting, Business Mathematics, Economics");
        fullNames.put("Marketing", "Marketing, Commerce, Business Studies");
        fullNames.put("SAC", "Office Practice, Shorthand, Typewriting");

        // Technical specialties
        fullNames.put("EPS", "Electrical Technology, Technical Drawing, Physics");
        fullNames.put("BC", "Building Construction, Technical Drawing, Woodwork");
        fullNames.put("CI", "Clothing Technology, Textiles, Fashion Design");
        fullNames.put("MECH", "Mechanical Engineering, Workshop Practice, Technical Drawing");

        // Home Economics specialties
        fullNames.put("Home Economics", "Home Management, Nutrition, Childcare");
        fullNames.put("Food and Nutrition", "Food Science, Nutrition, Dietetics");
        fullNames.put("Textiles", "Clothing Technology, Fashion Design, Textile Science");

        return fullNames;
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
                    ClassRoom.builder().name("Form 1").code(ClassLevel.FORM_1).academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 2").code(ClassLevel.FORM_2).academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 3").code(ClassLevel.FORM_3).academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 4").code(ClassLevel.FORM_4).academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 5").code(ClassLevel.FORM_5).academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Lower Sixth").code(ClassLevel.LOWER_SIXTH).academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Upper Sixth").code(ClassLevel.UPPER_SIXTH).academicYear("2024/2025").build()
            );
            classRoomRepository.saveAll(classRooms);
            log.info("Default classrooms created (Form 1–5 + Sixth Form)");
        }
    }

    private void initTeachers() {
        if (teacherRepository.count() == 0) {
            List<Teacher> teachers = new ArrayList<>();

            // 1. Science Department Teachers
            Teacher physicsTeacher = Teacher.builder()
                    .teacherId("TC100001")
                    .firstName("Dr. Samuel")
                    .lastName("Ngassa")
                    .gender(Gender.MALE)
                    .contact("677112233")
                    .skills("Physics,Mathematics,Additional Mathematics,Further Mathematics")
                    .build();
            teachers.add(physicsTeacher);

            Teacher biologyTeacher = Teacher.builder()
                    .teacherId("TC100002")
                    .firstName("Dr. Amina")
                    .lastName("Mohammed")
                    .gender(Gender.FEMALE)
                    .contact("677445566")
                    .skills("Biology,Chemistry,Geology,Environmental Science")
                    .build();
            teachers.add(biologyTeacher);

            // 2. Arts Department Teachers
            Teacher historyTeacher = Teacher.builder()
                    .teacherId("TC100003")
                    .firstName("Mr. Jean")
                    .lastName("Fotso")
                    .gender(Gender.MALE)
                    .contact("677778899")
                    .skills("History,Geography,Government,Philosophy")
                    .build();
            teachers.add(historyTeacher);

            Teacher languagesTeacher = Teacher.builder()
                    .teacherId("TC100004")
                    .firstName("Mrs. Grace")
                    .lastName("Tabi")
                    .gender(Gender.FEMALE)
                    .contact("677001122")
                    .skills("English Literature,French Literature,English Language,French Language,Religious Studies")
                    .build();
            teachers.add(languagesTeacher);

            // 3. Commercial Department Teacher
            Teacher commercialTeacher = Teacher.builder()
                    .teacherId("TC100005")
                    .firstName("Mr. Paul")
                    .lastName("Ndjodo")
                    .gender(Gender.MALE)
                    .contact("677334455")
                    .skills("Accounting,Commerce,Economics,Business Studies,Statistics")
                    .build();
            teachers.add(commercialTeacher);

            // 4. Technical Department Teacher
            Teacher technicalTeacher = Teacher.builder()
                    .teacherId("TC100006")
                    .firstName("Eng. Michel")
                    .lastName("Kamtchouang")
                    .gender(Gender.MALE)
                    .contact("677556677")
                    .skills("Technical Drawing,Engineering Science,Building Construction,Electrical Technology,Mechanical Engineering")
                    .build();
            teachers.add(technicalTeacher);

            // 5. Home Economics Teacher
            Teacher homeEconTeacher = Teacher.builder()
                    .teacherId("TC100007")
                    .firstName("Mrs. Marthe")
                    .lastName("Ngo")
                    .gender(Gender.FEMALE)
                    .contact("677889900")
                    .skills("Home Management,Food and Nutrition,Clothing and Textiles,Biology,Chemistry")
                    .build();
            teachers.add(homeEconTeacher);

            // 6. General Subjects Teacher
            Teacher generalTeacher = Teacher.builder()
                    .teacherId("TC100008")
                    .firstName("Mr. Nimal")
                    .lastName("Soyza")
                    .gender(Gender.MALE)
                    .contact("0339988554")
                    .skills("Mathematics,English Language,French Language,Computer Science,Citizenship Education")
                    .build();
            teachers.add(generalTeacher);

            teacherRepository.saveAll(teachers);
            log.info("{} teachers created", teachers.size());
        }
    }

    private void initSubjects() {
        if (subjectRepository.count() == 0) {
            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (DepartmentCode code : DepartmentCode.values()) {
                departmentRepository.findByCode(code).ifPresent(dept -> deptMap.put(code, dept));
            }

            List<Subject> subjects = new ArrayList<>();

            // ====================================
            // 1. GENERAL COMPULSORY SUBJECTS (All Forms 1-5)
            // ====================================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("English Language").coefficient(3).department(deptMap.get(DepartmentCode.GEN)).subjectCode("ENG-COMP").description("Compulsory English").build(),
                    Subject.builder().name("French Language").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("FREN-COMP").description("Compulsory French").build(),
                    Subject.builder().name("Mathematics").coefficient(4).department(deptMap.get(DepartmentCode.GEN)).subjectCode("MATH-COMP").description("Compulsory Mathematics").build(),
                    Subject.builder().name("Citizenship Education").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("CITIZEN-COMP").description("Civic Education").build(),
                    Subject.builder().name("Physical Education").coefficient(1).department(deptMap.get(DepartmentCode.GEN)).subjectCode("PE-COMP").description("Physical Education").build(),
                    Subject.builder().name("Computer Science").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("COMP-SCI").description("Computer Studies").build(),
                    Subject.builder().name("Religious Studies").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("REL-STUD").description("Religious Education").build()
            ));

            // =====================
            // 2. SCIENCE DEPARTMENT - ORDINARY LEVEL
            // =====================

            // Science Core for Forms 1-3 (No specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Biology").coefficient(3).department(deptMap.get(DepartmentCode.SCI)).subjectCode("BIO-CORE").description("Core Biology").build(),
                    Subject.builder().name("Chemistry").coefficient(3).department(deptMap.get(DepartmentCode.SCI)).subjectCode("CHEM-CORE").description("Core Chemistry").build(),
                    Subject.builder().name("Physics").coefficient(3).department(deptMap.get(DepartmentCode.SCI)).subjectCode("PHY-CORE").description("Core Physics").build()
            ));

            // Science Advanced for Forms 4-5 (Science specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Biology").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("BIO-ADV").description("Advanced Biology").build(),
                    Subject.builder().name("Chemistry").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("CHEM-ADV").description("Advanced Chemistry").build(),
                    Subject.builder().name("Physics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("PHY-ADV").description("Advanced Physics").build(),
                    Subject.builder().name("Additional Mathematics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("ADD-MATH").description("Advanced Mathematics").build(),
                    Subject.builder().name("Geography").coefficient(3).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("GEO-SCI").description("Geography for Scientists").build()
            ));

            // =====================
            // 3. ARTS DEPARTMENT - ORDINARY LEVEL
            // =====================

            // Arts Core for Forms 1-3 (No specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("History").coefficient(3).department(deptMap.get(DepartmentCode.ART)).subjectCode("HIST-CORE").description("Core History").build(),
                    Subject.builder().name("Geography").coefficient(3).department(deptMap.get(DepartmentCode.ART)).subjectCode("GEO-CORE").description("Core Geography").build(),
                    Subject.builder().name("Literature in English").coefficient(3).department(deptMap.get(DepartmentCode.ART)).subjectCode("LIT-CORE").description("Core Literature").build(),
                    Subject.builder().name("Art & Design").coefficient(2).department(deptMap.get(DepartmentCode.ART)).subjectCode("ART-DESIGN").description("Creative Arts").build(),
                    Subject.builder().name("Logic").coefficient(2).department(deptMap.get(DepartmentCode.ART)).subjectCode("LOGIC").description("Logical Reasoning").build()
            ));

            // Arts Advanced for Forms 4-5 (Arts specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("History").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("HIST-ADV").description("Advanced History").build(),
                    Subject.builder().name("Geography").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("GEO-ADV").description("Advanced Geography").build(),
                    Subject.builder().name("Literature in English").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("LIT-ADV").description("Advanced Literature").build(),
                    Subject.builder().name("Economics").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("ECO-ART").description("Economic Principles").build(),
                    Subject.builder().name("French Literature").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("FREN-LIT").description("French Literary Studies").build()
            ));

            // =====================
            // 4. COMMERCIAL DEPARTMENT - ORDINARY LEVEL
            // =====================

            // Commercial Core for Forms 1-3 (No specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Commerce").coefficient(3).department(deptMap.get(DepartmentCode.COM)).subjectCode("COMM-CORE").description("Basic Commerce").build(),
                    Subject.builder().name("Accounting").coefficient(3).department(deptMap.get(DepartmentCode.COM)).subjectCode("ACC-CORE").description("Basic Accounting").build(),
                    Subject.builder().name("Economics").coefficient(3).department(deptMap.get(DepartmentCode.COM)).subjectCode("ECO-CORE").description("Basic Economics").build()
            ));

            // Commercial Advanced for Forms 4-5 (Commercial specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Commerce").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("COMM-ADV").description("Advanced Commerce").build(),
                    Subject.builder().name("Accounting").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("ACC-ADV").description("Advanced Accounting").build(),
                    Subject.builder().name("Economics").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("ECO-ADV").description("Advanced Economics").build(),
                    Subject.builder().name("Business Studies").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("BUS-STUD").description("Business Management").build(),
                    Subject.builder().name("Business Mathematics").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("BUS-MATH").description("Commercial Mathematics").build(),
                    Subject.builder().name("Office Practice").coefficient(2).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("OFF-PRAC").description("Office Administration").build()
            ));

            // =====================
            // 5. TECHNICAL DEPARTMENT - ORDINARY LEVEL
            // =====================

            // Technical Core for Forms 1-3 (No specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Basic Technology").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).subjectCode("BASIC-TECH").description("Fundamental Technology").build(),
                    Subject.builder().name("Technical Drawing").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).subjectCode("TD-CORE").description("Basic Technical Drawing").build()
            ));

            // Technical Advanced for Forms 4-5 (Technical specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Technical Drawing").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("TD-ADV").description("Advanced Technical Drawing").build(),
                    Subject.builder().name("Workshop Practice").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("WORKSHOP").description("Practical Workshop Skills").build(),
                    Subject.builder().name("Engineering Science").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("ENG-SCI").description("Engineering Principles").build(),
                    Subject.builder().name("Industrial Computing").coefficient(2).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("IND-COMP").description("Computer Applications").build(),
                    Subject.builder().name("Building Construction").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("BLD-CONST").description("Construction Technology").build(),
                    Subject.builder().name("Electrical Technology").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("ELEC-TECH").description("Electrical Engineering").build(),
                    Subject.builder().name("Mechanical Engineering").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("MECH-ENG").description("Mechanical Principles").build(),
                    Subject.builder().name("Woodwork Technology").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("WOOD-TECH").description("Wood Technology").build()
            ));

            // =====================
            // 6. HOME ECONOMICS - ORDINARY LEVEL
            // =====================

            // Home Economics Core for Forms 1-3 (No specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Food and Nutrition").coefficient(3).department(deptMap.get(DepartmentCode.HE)).subjectCode("FOOD-CORE").description("Basic Nutrition").build(),
                    Subject.builder().name("Home Management").coefficient(3).department(deptMap.get(DepartmentCode.HE)).subjectCode("HOME-CORE").description("Basic Home Care").build(),
                    Subject.builder().name("Art and Design").coefficient(2).department(deptMap.get(DepartmentCode.HE)).subjectCode("ART-HE").description("Creative Design").build()
            ));

            // Home Economics Advanced for Forms 4-5 (Home Economics specialty)
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Food and Nutrition").coefficient(4).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("FOOD-ADV").description("Advanced Nutrition").build(),
                    Subject.builder().name("Home Management").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("HOME-ADV").description("Advanced Home Care").build(),
                    Subject.builder().name("Clothing and Textiles").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("CLOTH-TEX").description("Fashion & Textiles").build()
            ));

            // ====================================
            // ADVANCED LEVEL SUBJECTS (SIXTH FORM)
            // ====================================

            // =====================
            // 7. SCIENCE DEPARTMENT - ADVANCED LEVEL
            // =====================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Mathematics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("MATH-AL").description("Pure Mathematics").build(),
                    Subject.builder().name("Mathematics With Mechanics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("MATH-ALM").description("Pure Mathematics With Mechanics").build(),
                    Subject.builder().name("Mathematics With Statistics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("MATH-ALS").description("Pure Mathematics With Statistics").build(),
                    Subject.builder().name("Further Mathematics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("FURTHER-MATH").description("Advanced Mathematics").build(),
                    Subject.builder().name("Physics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("PHYSICS-AL").description("Advanced Physics").build(),
                    Subject.builder().name("Chemistry").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("CHEMISTRY-AL").description("Advanced Chemistry").build(),
                    Subject.builder().name("Biology").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("BIOLOGY-AL").description("Advanced Biology").build(),
                    Subject.builder().name("Geology").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("GEOLOGY-AL").description("Earth Sciences").build(),
                    Subject.builder().name("Computer Science").coefficient(3).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("COMP-SCI-AL").description("Advanced Computing").build(),
                    Subject.builder().name("Geography").coefficient(3).department(deptMap.get(DepartmentCode.SCI)).specialty("Science").subjectCode("GEO-SCI-AL").description("Physical Geography").build()
            ));

            // =====================
            // 8. ARTS DEPARTMENT - ADVANCED LEVEL
            // =====================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Literature in English").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("LIT-ENG-AL").description("Advanced Literature").build(),
                    Subject.builder().name("History").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("HISTORY-AL").description("Advanced History").build(),
                    Subject.builder().name("Geography").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("GEOGRAPHY-AL").description("Human Geography").build(),
                    Subject.builder().name("Economics").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("ECONOMICS-AL").description("Advanced Economics").build(),
                    Subject.builder().name("French").coefficient(4).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("FRENCH-AL").description("Advanced French").build(),
                    Subject.builder().name("Religious Studies").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("REL-STUD-AL").description("Theology & Ethics").build(),
                    Subject.builder().name("Philosophy").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty("Arts").subjectCode("PHILOSOPHY-AL").description("Philosophical Studies").build()
            ));

            // =====================
            // 9. COMMERCIAL DEPARTMENT - ADVANCED LEVEL
            // =====================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Accounting").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("ACCOUNTING-AL").description("Advanced Accounting").build(),
                    Subject.builder().name("Business Studies").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("BUSINESS-AL").description("Business Management").build(),
                    Subject.builder().name("Economics").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("ECO-COMM-AL").description("Economic Theory").build(),
                    Subject.builder().name("Mathematics").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("MATH-COMM-AL").description("Commercial Mathematics").build(),
                    Subject.builder().name("Computer Science").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("COMP-COMM-AL").description("Business Computing").build(),
                    Subject.builder().name("Statistics").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty("Commercial").subjectCode("STATISTICS-AL").description("Statistical Analysis").build()
            ));

            // =====================
            // 10. TECHNICAL DEPARTMENT - ADVANCED LEVEL
            // =====================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Technical Drawing").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("TD-AL").description("Engineering Drawing").build(),
                    Subject.builder().name("Engineering Science").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("ENG-SCI-AL").description("Advanced Engineering").build(),
                    Subject.builder().name("Building Construction").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("BLD-CONST-AL").description("Advanced Construction").build(),
                    Subject.builder().name("Electrical Technology").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("ELEC-TECH-AL").description("Electrical Engineering").build(),
                    Subject.builder().name("Mechanical Engineering").coefficient(4).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("MECH-ENG-AL").description("Mechanical Science").build(),
                    Subject.builder().name("Industrial Computer Studies").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).specialty("Technical").subjectCode("COMP-TEC-AL").description("Technical Computing").build()
            ));

            // =====================
            // 11. HOME ECONOMICS - ADVANCED LEVEL
            // =====================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("Food and Nutrition").coefficient(4).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("FOOD-NUT-AL").description("Advanced Nutrition").build(),
                    Subject.builder().name("Clothing and Textiles").coefficient(4).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("CLOTH-TEX-AL").description("Fashion Technology").build(),
                    Subject.builder().name("Home Management").coefficient(4).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("HOME-MGMT-AL").description("Advanced Home Science").build(),
                    Subject.builder().name("Biology").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("BIO-HE-AL").description("Biological Sciences").build(),
                    Subject.builder().name("Chemistry").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty("Home Economics").subjectCode("CHEM-HE-AL").description("Food Chemistry").build()
            ));

            // Save all subjects
            subjects.forEach(subjectService::createSubject);
            log.info("Complete Cameroon GCE subjects initialized (Ordinary & Advanced Levels) - {} subjects", subjects.size());
        }
    }

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
            // FORMS 1-3 GENERAL STUDENTS (2 students per class, different genders)
            // =========================
            for (int i = 1; i <= 3; i++) {
                ClassRoom cls = classMap.get(ClassLevel.valueOf("FORM_" + i));

                // Male student for Form 1-3 (General department)
                students.add(createStudentWithGender(
                        "Form" + i + "Male",
                        "Student",
                        cls,
                        deptMap.get(DepartmentCode.GEN),
                        null,
                        2009 - i,
                        2010 - i,
                        Gender.MALE
                ));

                // Female student for Form 1-3 (General department)
                students.add(createStudentWithGender(
                        "Form" + i + "Female",
                        "Student",
                        cls,
                        deptMap.get(DepartmentCode.GEN),
                        null,
                        2009 - i,
                        2010 - i,
                        Gender.FEMALE
                ));
            }

            // =========================
            // FORMS 4-5 SPECIALTY STUDENTS (2 students per department per class, different genders)
            // =========================
            String[] form45Departments = {"SCI", "ART", "COM", "TEC", "HE"};

            for (String deptCode : form45Departments) {
                Department dept = deptMap.get(DepartmentCode.valueOf(deptCode));
                List<String> specialties = departmentSpecialties.get(deptCode);

                if (specialties.isEmpty()) continue;

                // Take first specialty for Forms 4-5
                String specialty = specialties.get(0);

                // Form 4 Students (Male and Female)
                students.add(createStudentWithGender(
                        "F4" + deptCode + "M",
                        getSpecialtyLastName(specialty),
                        classMap.get(ClassLevel.FORM_4),
                        dept,
                        specialty,
                        2007,
                        2008,
                        Gender.MALE
                ));

                students.add(createStudentWithGender(
                        "F4" + deptCode + "F",
                        getSpecialtyLastName(specialty),
                        classMap.get(ClassLevel.FORM_4),
                        dept,
                        specialty,
                        2007,
                        2008,
                        Gender.FEMALE
                ));

                // Form 5 Students (Male and Female)
                students.add(createStudentWithGender(
                        "F5" + deptCode + "M",
                        getSpecialtyLastName(specialty),
                        classMap.get(ClassLevel.FORM_5),
                        dept,
                        specialty,
                        2006,
                        2007,
                        Gender.MALE
                ));

                students.add(createStudentWithGender(
                        "F5" + deptCode + "F",
                        getSpecialtyLastName(specialty),
                        classMap.get(ClassLevel.FORM_5),
                        dept,
                        specialty,
                        2006,
                        2007,
                        Gender.FEMALE
                ));
            }

            // =========================
            // SIXTH FORM STUDENTS (ADVANCED LEVEL) - 2 students per specialty per level, different genders
            // =========================
            ClassRoom lowerSixth = classMap.get(ClassLevel.LOWER_SIXTH);
            ClassRoom upperSixth = classMap.get(ClassLevel.UPPER_SIXTH);

            // Create students for each specialty in Science and Arts (where specialties are required)
            for (String deptCode : Arrays.asList("SCI", "ART")) {
                Department dept = deptMap.get(DepartmentCode.valueOf(deptCode));
                List<String> specialties = departmentSpecialties.get(deptCode);

                for (String specialty : specialties) {
                    // Lower Sixth Students (Male and Female)
                    students.add(createStudentWithGender(
                            "L6" + deptCode + specialty + "M",
                            getAdvancedLastName(specialty),
                            lowerSixth,
                            dept,
                            specialty,
                            2005,
                            2006,
                            Gender.MALE
                    ));

                    students.add(createStudentWithGender(
                            "L6" + deptCode + specialty + "F",
                            getAdvancedLastName(specialty),
                            lowerSixth,
                            dept,
                            specialty,
                            2005,
                            2006,
                            Gender.FEMALE
                    ));

                    // Upper Sixth Students (Male and Female)
                    students.add(createStudentWithGender(
                            "U6" + deptCode + specialty + "M",
                            getAdvancedLastName(specialty),
                            upperSixth,
                            dept,
                            specialty,
                            2004,
                            2005,
                            Gender.MALE
                    ));

                    students.add(createStudentWithGender(
                            "U6" + deptCode + specialty + "F",
                            getAdvancedLastName(specialty),
                            upperSixth,
                            dept,
                            specialty,
                            2004,
                            2005,
                            Gender.FEMALE
                    ));
                }
            }

            // For other departments in Sixth Form (Commercial, Technical, Home Economics)
            for (String deptCode : Arrays.asList("COM", "TEC", "HE")) {
                Department dept = deptMap.get(DepartmentCode.valueOf(deptCode));
                List<String> specialties = departmentSpecialties.get(deptCode);

                if (specialties.isEmpty()) continue;

                // Take first specialty for other departments
                String specialty = specialties.get(0);

                // Lower Sixth Students (Male and Female)
                students.add(createStudentWithGender(
                        "L6" + deptCode + "M",
                        getAdvancedLastName(specialty),
                        lowerSixth,
                        dept,
                        specialty,
                        2005,
                        2006,
                        Gender.MALE
                ));

                students.add(createStudentWithGender(
                        "L6" + deptCode + "F",
                        getAdvancedLastName(specialty),
                        lowerSixth,
                        dept,
                        specialty,
                        2005,
                        2006,
                        Gender.FEMALE
                ));

                // Upper Sixth Students (Male and Female)
                students.add(createStudentWithGender(
                        "U6" + deptCode + "M",
                        getAdvancedLastName(specialty),
                        upperSixth,
                        dept,
                        specialty,
                        2004,
                        2005,
                        Gender.MALE
                ));

                students.add(createStudentWithGender(
                        "U6" + deptCode + "F",
                        getAdvancedLastName(specialty),
                        upperSixth,
                        dept,
                        specialty,
                        2004,
                        2005,
                        Gender.FEMALE
                ));
            }

            // Save all students
            students.forEach(s -> {
                try {
                    studentService.createStudent(s, null);
                } catch (Exception e) {
                    log.warn("Failed to create student {}: {}", s.getFirstName(), e.getMessage());
                }
            });
            log.info("Complete student data initialized (Forms 1-5 + Sixth Form) - {} students", students.size());

            // Log student distribution
            logStudentDistribution(students);
        }
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
                .academicYearStart(2024)
                .academicYearEnd(2025)
                .build();
    }

    private LocalDate generateRandomBirthDate(int startYear, int endYear) {
        int year = ThreadLocalRandom.current().nextInt(startYear, endYear + 1);
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, 29);
        return LocalDate.of(year, month, day);
    }

    // Helper method to get appropriate last names based on specialty
    private String getSpecialtyLastName(String specialty) {
        return switch (specialty) {
            case "S1", "S2", "S3", "S4", "S5", "S6" -> "Researcher";
            case "A1", "A2", "A3", "A4", "A5" -> "Scholar";
            case "Accountancy", "Marketing", "SAC" -> "Entrepreneur";
            case "EPS", "BC", "CI", "MECH" -> "Engineer";
            case "Home Economics", "Food and Nutrition", "Textiles" -> "Specialist";
            default -> "Student";
        };
    }

    // Helper method to get advanced level last names
    private String getAdvancedLastName(String specialty) {
        return switch (specialty) {
            case "S1", "S2", "S3", "S4", "S5", "S6" -> "Scientist";
            case "A1", "A2", "A3", "A4", "A5" -> "Academic";
            case "Accountancy", "Marketing", "SAC" -> "Executive";
            case "EPS", "BC", "CI", "MECH" -> "Technologist";
            case "Home Economics", "Food and Nutrition", "Textiles" -> "Expert";
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

    // Helper method to log student distribution - FIXED: Use specialtyFullNames for better logging
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

        // Log specialty details with full names - FIXED: Now using specialtyFullNames
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
            Teacher teacher = teacherRepository.findAll().get(0);
            Notice notice = Notice.builder()
                    .title("School Reopening - 2024/2025 Academic Year")
                    .content("School will reopen on September 2nd, 2024 for the new academic year. All students are expected to be present in full uniform. GCE registration forms for both Ordinary and Advanced Levels are available at the administration office.")
                    .createdDate(LocalDateTime.now())
                    .teacher(teacher)
                    .isActive(true)
                    .build();
            noticeRepository.save(notice);
            log.info("Default notice created");
        }
    }
}