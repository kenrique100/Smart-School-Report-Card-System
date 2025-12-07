package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.service.SubjectService;
import jakarta.transaction.Transactional;
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
    private final SubjectRepository subjectRepository;
    private final SubjectService subjectService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            log.info("Starting data initialization...");
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
        long deptCount = departmentRepository.count();
        log.info("Found {} departments in database", deptCount);

        if (deptCount == 0) {
            log.info("Creating default departments...");
            List<Department> departments = Arrays.asList(
                    Department.builder().name("General").code(DepartmentCode.GEN).description("General Studies Department").build(),
                    Department.builder().name("Sciences").code(DepartmentCode.SCI).description("Science Department").build(),
                    Department.builder().name("Arts").code(DepartmentCode.ART).description("Arts Department").build(),
                    Department.builder().name("Commercial").code(DepartmentCode.COM).description("Commercial Department").build(),
                    Department.builder().name("Technical").code(DepartmentCode.TEC).description("Technical Department").build(),
                    Department.builder().name("Home Economics").code(DepartmentCode.HE).description("Home Economics Department").build()
            );
            List<Department> savedDepartments = departmentRepository.saveAll(departments);
            log.info("Default departments created: {}", savedDepartments.size());

            // Verify departments were created
            for (Department dept : savedDepartments) {
                log.info("Created department: {} (ID: {}, Code: {})",
                        dept.getName(), dept.getId(), dept.getCode());
            }
        } else {
            log.info("Departments already exist in database");
        }
    }

    private void initClassRooms() {
        long classCount = classRoomRepository.count();
        log.info("Found {} classrooms in database", classCount);

        if (classCount == 0) {
            log.info("Creating default classrooms...");
            List<ClassRoom> classRooms = Arrays.asList(
                    ClassRoom.builder().name("Form 1").code(ClassLevel.FORM_1).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 2").code(ClassLevel.FORM_2).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 3").code(ClassLevel.FORM_3).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 4").code(ClassLevel.FORM_4).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Form 5").code(ClassLevel.FORM_5).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Lower Sixth").code(ClassLevel.LOWER_SIXTH).academicYear("2025/2026").build(),
                    ClassRoom.builder().name("Upper Sixth").code(ClassLevel.UPPER_SIXTH).academicYear("2025/2026").build()
            );
            List<ClassRoom> savedClassrooms = classRoomRepository.saveAll(classRooms);
            log.info("Default classrooms created: {}", savedClassrooms.size());

            // Verify classrooms were created
            for (ClassRoom classroom : savedClassrooms) {
                log.info("Created classroom: {} (ID: {}, Code: {})",
                        classroom.getName(), classroom.getId(), classroom.getCode());
            }
        } else {
            log.info("Classrooms already exist in database");
        }
    }


    private void initSubjects() {
        long subjectCount = subjectRepository.count();
        log.info("Found {} subjects in database", subjectCount);

        if (subjectCount == 0) {
            log.info("Creating subjects...");

            // First, get all departments to ensure they exist
            List<Department> allDepartments = departmentRepository.findAll();
            if (allDepartments.isEmpty()) {
                log.error("No departments found! Cannot create subjects.");
                return;
            }

            log.info("Found {} departments for subject creation", allDepartments.size());

            // Create a map of department code to department
            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (Department dept : allDepartments) {
                deptMap.put(dept.getCode(), dept);
                log.info("Department mapped: {} -> {}", dept.getCode(), dept.getName());
            }

            // Check if we have the General department
            if (!deptMap.containsKey(DepartmentCode.GEN)) {
                log.error("General department not found! Subject creation may fail.");
            }

            // ============================================================
            // 1️⃣ GENERAL COMPULSORY SUBJECTS (All Departments - Forms 1-5)
            // ============================================================
            log.info("Creating General compulsory subjects...");
            List<Subject> subjects = new ArrayList<>(Arrays.asList(
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
            if (deptMap.containsKey(DepartmentCode.SCI)) {
                log.info("Creating Science subjects...");
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

                        // FIXED: A-Computer Science available to all Science specialties
                        Subject.builder().name("A-Computer Science").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-COMP-SCI")
                                .description("Optional Advanced Level Computer Science")
                                .specialty(null)  // Available to all Science specialties
                                .optional(true).build(),

                        // FIXED: A-ICT available to all Science specialties
                        Subject.builder().name("A-ICT").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-ICT-SCI")
                                .description("Optional Advanced Level ICT")
                                .specialty(null)  // Available to all Science specialties
                                .optional(true).build(),

                        Subject.builder().name("Food Science").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-FOOD-SCI")
                                .description("Optional Advanced Level Food Science")
                                .optional(true).build(),

                        Subject.builder().name("A-Geology").coefficient(4)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-GEO-SCI").description("Advanced Level Geology")
                                .specialty("S4").build()
                ));
            }

            // ============================================================
            // 3️⃣ ARTS DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.ART)) {
                log.info("Creating Arts subjects...");
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

                        Subject.builder().name("O-Logic").coefficient(2)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("O-LOG-ART").description("Ordinary Level Logic").build(),

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
                                .specialty("A4")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 4️⃣ COMMERCIAL DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.COM)) {
                log.info("Creating Commercial subjects...");
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
            }

            // ============================================================
            // 5️⃣ TECHNICAL DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.TEC)) {
                log.info("Creating Technical subjects...");
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
                                .optional(true).build(),

                        Subject.builder().name("A-Industrial-Computing").coefficient(4)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("A-COMP-TEC").description("Advanced Level Industrial computing")
                                .specialty("T1").build()
                ));
            }

            // ============================================================
            // 6️⃣ HOME ECONOMICS DEPARTMENT (GCE Ordinary & Advanced Level)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.HE)) {
                log.info("Creating Home Economics subjects...");
                subjects.addAll(Arrays.asList(
                        // Ordinary Level Home Economics
                        Subject.builder().name("O-Home Economics").coefficient(3)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("O-HE-HE").description("Ordinary Level Home Economics").build(),

                        Subject.builder().name("O-Nutrition and Food Science").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("O-NUT-HE").description("Ordinary Level Nutrition and Food Science").build(),

                        Subject.builder().name("O-Clothing Technology").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("O-CLOTH-HE").description("Ordinary Level Clothing Technology").build(),

                        Subject.builder().name("O-Fashion Design").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
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
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("A-CLOTH-HE").description("Advanced Level Clothing Technology")
                                .specialty("H1,H3").build(),

                        Subject.builder().name("A-Fashion Design").coefficient(4)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("A-FASH-HE").description("Advanced Level Fashion Design")
                                .specialty("H3").build(),

                        Subject.builder().name("A-Food Science").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("A-FOOD-HE").description("Advanced Level Food Science")
                                .specialty("H2")
                                .optional(true).build(),

                        Subject.builder().name("Textile Science").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("A-TEXT-HE").description("Advanced Level Textile Science")
                                .specialty("H3")
                                .optional(true).build()
                ));
            }

            log.info("Total subjects to create: {}", subjects.size());

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

                // Log each subject created
                log.info("Subjects created by department:");
                for (Map.Entry<String, Long> entry : subjectCountByDept.entrySet()) {
                    log.info("  {}: {} subjects", entry.getKey(), entry.getValue());
                }

            } catch (Exception e) {
                log.error("Critical error during subject initialization", e);
                throw new RuntimeException("Failed to initialize subjects", e);
            }
        } else {
            log.info("Subjects already initialized, skipping subject creation");
            // Log existing subject count for debugging
            List<Subject> existingSubjects = subjectRepository.findAll();
            log.info("Existing subjects count: {}", existingSubjects.size());

            Map<String, Long> existingCountByDept = existingSubjects.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getDepartment() != null ? s.getDepartment().getName() : "General",
                            Collectors.counting()
                    ));
            log.info("Existing subject distribution: {}", existingCountByDept);
        }
    }
}