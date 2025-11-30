package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
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
            initSubjects();
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

            // ============================================================
            // 3️⃣ ARTS DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            subjects.addAll(Arrays.asList(
                    // Ordinary Level Arts
                    Subject.builder().name("O-History").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("O-HIST-ART").description("Ordinary Level History").build(),

                    Subject.builder().name("O-Geography").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("O-GEO-ART").description("Ordinary Level Geography").build(),

                    Subject.builder().name("O-Economics").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("O-ECO-ART").description("Ordinary Level Economics").build(),

                    Subject.builder().name("O-Literature").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("O-LIT-ART").description("Ordinary Level Literature").build(),

                    Subject.builder().name("O-Religious Studies").coefficient(2)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("O-REL-ART").description("Ordinary Level Religious Studies").build(),

                    Subject.builder().name("O-Philosophy").coefficient(2)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("O-PHIL-ART").description("Ordinary Level Philosophy").build(),

                    // Advanced Level Arts - WITH SPECIALTY ASSIGNMENTS
                    Subject.builder().name("A-History").coefficient(4)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-HIST-ART").description("Advanced Level History")
                            .specialty("A1,A2,A4,A7").build(),

                    Subject.builder().name("A-Geography").coefficient(4)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-GEO-ART").description("Advanced Level Geography")
                            .specialty("A1,A3,A5,A7").build(),

                    Subject.builder().name("A-Economics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-ECO-ART").description("Advanced Level Economics")
                            .specialty("A1,A3,A5,A7").build(),

                    Subject.builder().name("A-Literature").coefficient(4)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-LIT-ART").description("Advanced Level Literature")
                            .specialty("A2,A4,A6,A8").build(),

                    Subject.builder().name("A-French Language").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-FREN-ART").description("Advanced Level French")
                            .specialty("A2,A6").build(),

                    Subject.builder().name("A-Religious Studies").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-REL-ART").description("Advanced Level Religious Studies")
                            .specialty("A4,A8").build(),

                    Subject.builder().name("A-Philosophy").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-PHIL-ART").description("Advanced Level Philosophy")
                            .specialty("A6,A8").build(),

                    Subject.builder().name("A-ICT").coefficient(3)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-ICT-ART").description("Advanced Level ICT")
                            .specialty("A5")
                            .optional(true).build(),

                    Subject.builder().name("A-Pure Mathematics With Stats").coefficient(4)
                            .department(deptMap.get(DepartmentCode.ART))
                            .subjectCode("A-PMATH-ART").description("Advanced Level Mathematics with Statistics")
                            .specialty("A3")
                            .optional(true).build()
            ));

            // ============================================================
            // 4️⃣ COMMERCIAL DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            subjects.addAll(Arrays.asList(
                    // Ordinary Level Commercial
                    Subject.builder().name("O-Accounting").coefficient(4)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-ACC-COM").description("Ordinary Level Accounting").build(),

                    Subject.builder().name("O-Commerce").coefficient(3)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-COM-COM").description("Ordinary Level Commerce").build(),

                    Subject.builder().name("O-Business Management").coefficient(3)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-BUS-COM").description("Ordinary Level Business Management").build(),

                    Subject.builder().name("O-Economics").coefficient(3)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-ECO-COM").description("Ordinary Level Economics").build(),

                    Subject.builder().name("O-Marketing").coefficient(3)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-MKT-COM").description("Ordinary Level Marketing").build(),

                    Subject.builder().name("O-Office Practice").coefficient(2)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-OFF-COM").description("Ordinary Level Office Practice").build(),

                    Subject.builder().name("O-Typewriting").coefficient(2)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("O-TYPE-COM").description("Ordinary Level Typewriting").build(),

                    // Advanced Level Commercial - WITH SPECIALTY ASSIGNMENTS
                    Subject.builder().name("A-Accounting").coefficient(5)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-ACC-COM").description("Advanced Level Accounting")
                            .specialty("C1,C2").build(),

                    Subject.builder().name("A-Business Management").coefficient(4)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-BUS-COM").description("Advanced Level Business Management")
                            .specialty("C1,C2,C3").build(),

                    Subject.builder().name("A-Economics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-ECO-COM").description("Advanced Level Economics")
                            .specialty("C1").build(),

                    Subject.builder().name("A-Commerce").coefficient(4)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-COM-COM").description("Advanced Level Commerce")
                            .specialty("C2,C3").build(),

                    Subject.builder().name("A-Marketing").coefficient(3)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-MKT-COM").description("Advanced Level Marketing")
                            .specialty("C3").build(),

                    Subject.builder().name("A-Office Practice").coefficient(2)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-OFF-COM").description("Advanced Level Office Practice")
                            .specialty("C4").build(),

                    Subject.builder().name("A-Typewriting").coefficient(2)
                            .department(deptMap.get(DepartmentCode.COM))
                            .subjectCode("A-TYPE-COM").description("Advanced Level Typewriting")
                            .specialty("C4")
                            .optional(true).build()
            ));

            // ============================================================
            // 5️⃣ TECHNICAL DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            subjects.addAll(Arrays.asList(
                    // Ordinary Level Technical
                    Subject.builder().name("O-Technical Drawing").coefficient(4)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-TD-TEC").description("Ordinary Level Technical Drawing").build(),

                    Subject.builder().name("O-Woodwork").coefficient(3)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-WOOD-TEC").description("Ordinary Level Woodwork").build(),

                    Subject.builder().name("O-Metalwork").coefficient(3)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-METAL-TEC").description("Ordinary Level Metalwork").build(),

                    Subject.builder().name("O-Electrical Technology").coefficient(4)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-ELEC-TEC").description("Ordinary Level Electrical Technology").build(),

                    Subject.builder().name("O-Building Construction").coefficient(4)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-BUILD-TEC").description("Ordinary Level Building Construction").build(),

                    Subject.builder().name("O-Automobile Mechanics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-AUTO-TEC").description("Ordinary Level Automobile Mechanics").build(),

                    Subject.builder().name("Workshop Practice").coefficient(3)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("O-WORK-TEC").description("Workshop Practice").build(),

                    // Advanced Level Technical - WITH SPECIALTY ASSIGNMENTS
                    Subject.builder().name("A-Electrical Technology").coefficient(5)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-ELEC-TEC").description("Advanced Level Electrical Technology")
                            .specialty("T1").build(),

                    Subject.builder().name("A-Building Construction").coefficient(5)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-BUILD-TEC").description("Advanced Level Building Construction")
                            .specialty("T2,T5").build(),

                    Subject.builder().name("A-Mechanical Engineering").coefficient(5)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-MECH-TEC").description("Advanced Level Mechanical Engineering")
                            .specialty("T3").build(),

                    Subject.builder().name("A-Automobile Mechanics").coefficient(5)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-AUTO-TEC").description("Advanced Level Automobile Mechanics")
                            .specialty("T4").build(),

                    Subject.builder().name("Technical Drawing").coefficient(4)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-TD-TEC").description("Advanced Technical Drawing")
                            .specialty("T1,T2,T3,T4,T5").build(),

                    Subject.builder().name("Workshop Practice").coefficient(3)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-WORK-TEC").description("Advanced Workshop Practice")
                            .specialty("T3,T4,T5")
                            .optional(true).build(),

                    Subject.builder().name("A-Physics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.TEC))
                            .subjectCode("A-PHY-TEC").description("Advanced Level Physics for Technical")
                            .specialty("T1")
                            .optional(true).build()
            ));

            // ============================================================
            // 6️⃣ HOME ECONOMICS DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            subjects.addAll(Arrays.asList(
                    // Ordinary Level Home Economics
                    Subject.builder().name("O-Home Economics").coefficient(3)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("O-HE-HE").description("Ordinary Level Home Economics").build(),

                    Subject.builder().name("O-Nutrition and Food Science").coefficient(4)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("O-NUT-HE").description("Ordinary Level Nutrition and Food Science").build(),

                    Subject.builder().name("O-Clothing Technology").coefficient(3)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("O-CLOTH-HE").description("Ordinary Level Clothing Technology").build(),

                    Subject.builder().name("O-Fashion Design").coefficient(3)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("O-FASH-HE").description("Ordinary Level Fashion Design").build(),

                    // Advanced Level Home Economics - WITH SPECIALTY ASSIGNMENTS
                    Subject.builder().name("A-Home Economics").coefficient(4)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("A-HE-HE").description("Advanced Level Home Economics")
                            .specialty("H1,H2,H3").build(),

                    Subject.builder().name("A-Nutrition and Food Science").coefficient(5)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("A-NUT-HE").description("Advanced Level Nutrition and Food Science")
                            .specialty("H1,H2").build(),

                    Subject.builder().name("A-Clothing Technology").coefficient(4)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("A-CLOTH-HE").description("Advanced Level Clothing Technology")
                            .specialty("H1,H3").build(),

                    Subject.builder().name("A-Fashion Design").coefficient(4)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("A-FASH-HE").description("Advanced Level Fashion Design")
                            .specialty("H3").build(),

                    Subject.builder().name("A-Food Science").coefficient(4)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("A-FOOD-HE").description("Advanced Level Food Science")
                            .specialty("H2")
                            .optional(true).build(),

                    Subject.builder().name("Textile Science").coefficient(3)
                            .department(deptMap.get(DepartmentCode.HE))
                            .subjectCode("A-TEXT-HE").description("Advanced Level Textile Science")
                            .specialty("H3")
                            .optional(true).build()
            ));

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

}