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
                    Department.builder().name("Home Economics").code(DepartmentCode.HE).description("Home Economics Department").build(),
                    Department.builder().name("Electrical Power System").code(DepartmentCode.EPS).description("Electrical Power System Department").build(),
                    Department.builder().name("Clothing Industry").code(DepartmentCode.CI).description("Clothing Industry Department").build()
            );
            List<Department> savedDepartments = departmentRepository.saveAll(departments);
            log.info("Default departments created: {}", savedDepartments.size());

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

            List<Department> allDepartments = departmentRepository.findAll();
            if (allDepartments.isEmpty()) {
                log.error("No departments found! Cannot create subjects.");
                return;
            }

            log.info("Found {} departments for subject creation", allDepartments.size());

            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (Department dept : allDepartments) {
                deptMap.put(dept.getCode(), dept);
                log.info("Department mapped: {} -> {}", dept.getCode(), dept.getName());
            }

            List<Subject> subjects = new ArrayList<>();

            // ============================================================
            // 1️⃣ GENERAL DEPARTMENT - FORMS 1-3 (ALL COMPULSORY)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.GEN)) {
                log.info("Creating General department subjects for Forms 1-3...");

                // FORM 1 SUBJECTS (All Compulsory)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("English Language").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-ENG-F1").description("English Language - Form 1").build(),

                        Subject.builder().name("French").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-FREN-F1").description("French - Form 1").build(),

                        Subject.builder().name("Mathematics").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-MATH-F1").description("Mathematics - Form 1").build(),

                        Subject.builder().name("History").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-HIST-F1").description("History - Form 1").build(),

                        Subject.builder().name("Geography").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-GEO-F1").description("Geography - Form 1").build(),

                        Subject.builder().name("Literature in English").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-LIT-F1").description("Literature in English - Form 1").build(),

                        Subject.builder().name("Integrated Science").coefficient(4)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-INTSCI-F1").description("Integrated Science - Form 1").build(),

                        Subject.builder().name("Computer Studies / ICT").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-COMP-F1").description("Computer Studies - Form 1").build(),

                        Subject.builder().name("Citizenship Education").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-CIT-F1").description("Citizenship Education - Form 1").build(),

                        Subject.builder().name("Religious Studies").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-REL-F1").description("Religious Studies - Form 1").build(),

                        Subject.builder().name("Physical Education").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-PE-F1").description("Physical Education - Form 1").build()
                ));

                // FORM 2 SUBJECTS (All Compulsory)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("English Language").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-ENG-F2").description("English Language - Form 2").build(),

                        Subject.builder().name("French").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-FREN-F2").description("French - Form 2").build(),

                        Subject.builder().name("Mathematics").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-MATH-F2").description("Mathematics - Form 2").build(),

                        Subject.builder().name("History").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-HIST-F2").description("History - Form 2").build(),

                        Subject.builder().name("Geography").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-GEO-F2").description("Geography - Form 2").build(),

                        Subject.builder().name("Literature in English").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-LIT-F2").description("Literature in English - Form 2").build(),

                        Subject.builder().name("Integrated Science").coefficient(4)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-INTSCI-F2").description("Integrated Science - Form 2").build(),

                        Subject.builder().name("ICT").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-ICT-F2").description("ICT - Form 2").build(),

                        Subject.builder().name("Citizenship").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-CIT-F2").description("Citizenship - Form 2").build(),

                        Subject.builder().name("Religious Studies").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-REL-F2").description("Religious Studies - Form 2").build(),

                        Subject.builder().name("Physical Education").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-PE-F2").description("Physical Education - Form 2").build()
                ));

                // FORM 3 SUBJECTS (All Compulsory)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("English Language").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-ENG-F3").description("English Language - Form 3").build(),

                        Subject.builder().name("French").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-FREN-F3").description("French - Form 3").build(),

                        Subject.builder().name("Mathematics").coefficient(5)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-MATH-F3").description("Mathematics - Form 3").build(),

                        Subject.builder().name("Biology").coefficient(4)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-BIO-F3").description("Biology - Form 3").build(),

                        Subject.builder().name("Chemistry").coefficient(4)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-CHEM-F3").description("Chemistry - Form 3").build(),

                        Subject.builder().name("Physics").coefficient(4)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-PHY-F3").description("Physics - Form 3").build(),

                        Subject.builder().name("History").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-HIST-F3").description("History - Form 3").build(),

                        Subject.builder().name("Geography").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-GEO-F3").description("Geography - Form 3").build(),

                        Subject.builder().name("Literature in English").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-LIT-F3").description("Literature in English - Form 3").build(),

                        Subject.builder().name("Citizenship / Social Studies").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-CIT-F3").description("Citizenship - Form 3").build(),

                        Subject.builder().name("ICT").coefficient(3)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-ICT-F3").description("ICT - Form 3").build(),

                        Subject.builder().name("Religious Studies").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-REL-F3").description("Religious Studies - Form 3").build(),

                        Subject.builder().name("Physical Education").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-PE-F3").description("Physical Education - Form 3").build()
                ));

                // OPTIONAL SUBJECTS FOR GENERAL DEPARTMENT (Forms 1-5)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Religious Studies (Optional)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-REL-OPT")
                                .description("Optional Religious Studies")
                                .optional(true).build(),

                        Subject.builder().name("Citizenship Education (Optional)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-CIT-OPT")
                                .description("Optional Citizenship Education")
                                .optional(true).build(),

                        Subject.builder().name("Computer Science (Optional)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-COMP-OPT")
                                .description("Optional Computer Science")
                                .optional(true).build(),

                        Subject.builder().name("ICT (Optional)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("GEN-ICT-OPT")
                                .description("Optional ICT")
                                .optional(true).build()
                ));

                // COMPLEMENTARY EDUCATION SUBJECTS (Forms 1-5)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Sports").coefficient(1)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("COMP-SPORTS").description("Sports and Physical Activities").build(),

                        Subject.builder().name("Physical Education").coefficient(2)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("COMP-PE").description("Physical Education and Health").build(),

                        Subject.builder().name("Manual Labour").coefficient(1)
                                .department(deptMap.get(DepartmentCode.GEN))
                                .subjectCode("COMP-ML").description("Manual Labour and Practical Skills").build()
                ));
            }

            // ============================================================
            // 2️⃣ SCIENCE DEPARTMENT - FORMS 4-5 (O-LEVEL)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.SCI)) {
                log.info("Creating Science department subjects for Forms 4-5 (O-Level)...");

                // CORE SCIENCE SUBJECTS (Forms 4-5)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("English Language (Science)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-ENG-F4-5").description("English Language for Science - Form 4-5").build(),

                        Subject.builder().name("French (Science)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-FREN-F4-5").description("French for Science - Form 4-5").build(),

                        Subject.builder().name("Mathematics (Science)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-MATH-F4-5").description("Mathematics for Science - Form 4-5").build(),

                        Subject.builder().name("Biology").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-BIO-F4-5").description("Biology - Form 4-5").build(),

                        Subject.builder().name("Chemistry").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-CHEM-F4-5").description("Chemistry - Form 4-5").build(),

                        Subject.builder().name("Physics").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-PHY-F4-5").description("Physics - Form 4-5").build(),

                        Subject.builder().name("Additional Mathematics").coefficient(4)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-ADD-MATH-F4-5").description("Additional Mathematics - Form 4-5").build()
                ));

                // OPTIONAL SUBJECTS FOR SCIENCE (Forms 4-5)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Geography (Science)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-GEO-F4-5")
                                .description("Optional Geography for Science - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Computer Science (Science)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-COMP-F4-5")
                                .description("Optional Computer Science for Science - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Economics (Science)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-ECO-F4-5")
                                .description("Optional Economics for Science - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Literature in English (Science)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-LIT-F4-5")
                                .description("Optional Literature for Science - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Religious Studies (Science)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-REL-F4-5")
                                .description("Optional Religious Studies for Science - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Human Biology").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("SCI-HUM-BIO-F4-5")
                                .description("Optional Human Biology - Form 4-5")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 3️⃣ SCIENCE DEPARTMENT - SIXTH FORM (A-LEVEL WITH SPECIALTIES S1-S8)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.SCI)) {
                log.info("Creating Science department subjects for Sixth Form (A-Level)...");

                // S1: Chemistry, Physics, Pure Mathematics with Mechanics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Chemistry (S1)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S1")
                                .description("Chemistry for S1 specialty")
                                .specialty("S1").build(),

                        Subject.builder().name("Physics (S1)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PHY-S1")
                                .description("Physics for S1 specialty")
                                .specialty("S1").build(),

                        Subject.builder().name("Pure Mathematics with Mechanics (S1)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PMATH-MECH-S1")
                                .description("Pure Mathematics with Mechanics for S1")
                                .specialty("S1").build()
                ));

                // S2: Chemistry, Physics, Biology
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Chemistry (S2)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S2")
                                .description("Chemistry for S2 specialty")
                                .specialty("S2").build(),

                        Subject.builder().name("Physics (S2)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PHY-S2")
                                .description("Physics for S2 specialty")
                                .specialty("S2").build(),

                        Subject.builder().name("Biology (S2)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-BIO-S2")
                                .description("Biology for S2 specialty")
                                .specialty("S2").build()
                ));

                // S3: Biology, Chemistry, Pure Mathematics with Statistics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Biology (S3)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-BIO-S3")
                                .description("Biology for S3 specialty")
                                .specialty("S3").build(),

                        Subject.builder().name("Chemistry (S3)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S3")
                                .description("Chemistry for S3 specialty")
                                .specialty("S3").build(),

                        Subject.builder().name("Pure Mathematics with Statistics (S3)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PMATH-STAT-S3")
                                .description("Pure Mathematics with Statistics for S3")
                                .specialty("S3").build()
                ));

                // S4: Biology, Chemistry, Geology
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Biology (S4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-BIO-S4")
                                .description("Biology for S4 specialty")
                                .specialty("S4").build(),

                        Subject.builder().name("Chemistry (S4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S4")
                                .description("Chemistry for S4 specialty")
                                .specialty("S4").build(),

                        Subject.builder().name("Geology (S4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-GEOL-S4")
                                .description("Geology for S4 specialty")
                                .specialty("S4").build()
                ));

                // S5: Chemistry, Biology, Mathematics with Mechanics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Chemistry (S5)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S5")
                                .description("Chemistry for S5 specialty")
                                .specialty("S5").build(),

                        Subject.builder().name("Biology (S5)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-BIO-S5")
                                .description("Biology for S5 specialty")
                                .specialty("S5").build(),

                        Subject.builder().name("Mathematics with Mechanics (S5)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-MATH-MECH-S5")
                                .description("Mathematics with Mechanics for S5")
                                .specialty("S5").build()
                ));

                // S6: Chemistry, Physics, Mathematics with Mechanics, Further Mathematics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Chemistry (S6)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S6")
                                .description("Chemistry for S6 specialty")
                                .specialty("S6").build(),

                        Subject.builder().name("Physics (S6)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PHY-S6")
                                .description("Physics for S6 specialty")
                                .specialty("S6").build(),

                        Subject.builder().name("Mathematics with Mechanics (S6)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-MATH-MECH-S6")
                                .description("Mathematics with Mechanics for S6")
                                .specialty("S6").build(),

                        Subject.builder().name("Further Mathematics (S6)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-FMATH-S6")
                                .description("Further Mathematics for S6")
                                .specialty("S6").build()
                ));

                // S7: Chemistry, Biology, Physics, Mathematics with Mechanics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Chemistry (S7)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S7")
                                .description("Chemistry for S7 specialty")
                                .specialty("S7").build(),

                        Subject.builder().name("Biology (S7)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-BIO-S7")
                                .description("Biology for S7 specialty")
                                .specialty("S7").build(),

                        Subject.builder().name("Physics (S7)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PHY-S7")
                                .description("Physics for S7 specialty")
                                .specialty("S7").build(),

                        Subject.builder().name("Mathematics with Mechanics (S7)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-MATH-MECH-S7")
                                .description("Mathematics with Mechanics for S7")
                                .specialty("S7").build()
                ));

                // S8: Biology, Chemistry, Physics, Mathematics with Mechanics, Further Mathematics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Biology (S8)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-BIO-S8")
                                .description("Biology for S8 specialty")
                                .specialty("S8").build(),

                        Subject.builder().name("Chemistry (S8)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-CHEM-S8")
                                .description("Chemistry for S8 specialty")
                                .specialty("S8").build(),

                        Subject.builder().name("Physics (S8)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-PHY-S8")
                                .description("Physics for S8 specialty")
                                .specialty("S8").build(),

                        Subject.builder().name("Mathematics with Mechanics (S8)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-MATH-MECH-S8")
                                .description("Mathematics with Mechanics for S8")
                                .specialty("S8").build(),

                        Subject.builder().name("Further Mathematics (S8)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-FMATH-S8")
                                .description("Further Mathematics for S8")
                                .specialty("S8").build()
                ));

                // OPTIONAL SUBJECTS FOR SCIENCE SIXTH FORM (Available to all specialties)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Computer Science (Science Advanced)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-COMP-SCI")
                                .description("Optional Computer Science for Science - A-Level")
                                .optional(true).build(),

                        Subject.builder().name("ICT (Science Advanced)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-ICT-SCI")
                                .description("Optional ICT for Science - A-Level")
                                .optional(true).build(),

                        Subject.builder().name("Food Science").coefficient(3)
                                .department(deptMap.get(DepartmentCode.SCI))
                                .subjectCode("A-FOOD-SCI")
                                .description("Optional Food Science for Science - A-Level")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 4️⃣ ARTS DEPARTMENT - FORMS 4-5 (O-LEVEL)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.ART)) {
                log.info("Creating Arts department subjects for Forms 4-5 (O-Level)...");

                // CORE ARTS SUBJECTS (Forms 4-5)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("English Language (Arts)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-ENG-F4-5").description("English Language for Arts - Form 4-5").build(),

                        Subject.builder().name("Mathematics (General)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-MATH-F4-5").description("General Mathematics for Arts - Form 4-5").build()
                ));

                // OPTIONAL ARTS SUBJECTS (Forms 4-5)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("French (Arts)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-FREN-F4-5")
                                .description("Optional French for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Literature in English (Arts)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-LIT-F4-5")
                                .description("Literature in English for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("History (Arts)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-HIST-F4-5")
                                .description("History for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Geography (Arts)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-GEO-F4-5")
                                .description("Geography for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Economics (Arts)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-ECO-F4-5")
                                .description("Economics for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Religious Studies (Arts)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-REL-F4-5")
                                .description("Religious Studies for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Commerce (Arts)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-COM-F4-5")
                                .description("Optional Commerce for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Food & Nutrition (Arts)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-FOOD-F4-5")
                                .description("Optional Food & Nutrition for Arts - Form 4-5")
                                .optional(true).build(),

                        Subject.builder().name("Clothing & Textiles (Arts)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("ART-CLOTH-F4-5")
                                .description("Optional Clothing & Textiles for Arts - Form 4-5")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 5️⃣ ARTS DEPARTMENT - SIXTH FORM (A-LEVEL WITH SPECIALTIES A1-A5)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.ART)) {
                log.info("Creating Arts department subjects for Sixth Form (A-Level)...");

                // A1: Literature, History, French
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Literature (A1)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-LIT-A1")
                                .description("Literature for A1 specialty")
                                .specialty("A1").build(),

                        Subject.builder().name("History (A1)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-HIST-A1")
                                .description("History for A1 specialty")
                                .specialty("A1").build(),

                        Subject.builder().name("French (A1)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-FREN-A1")
                                .description("French for A1 specialty")
                                .specialty("A1").build()
                ));

                // A2: History, Geography, Economics
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("History (A2)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-HIST-A2")
                                .description("History for A2 specialty")
                                .specialty("A2").build(),

                        Subject.builder().name("Geography (A2)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-GEO-A2")
                                .description("Geography for A2 specialty")
                                .specialty("A2").build(),

                        Subject.builder().name("Economics (A2)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-ECO-A2")
                                .description("Economics for A2 specialty")
                                .specialty("A2").build()
                ));

                // A3: Literature, Economics, History
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Literature (A3)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-LIT-A3")
                                .description("Literature for A3 specialty")
                                .specialty("A3").build(),

                        Subject.builder().name("Economics (A3)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-ECO-A3")
                                .description("Economics for A3 specialty")
                                .specialty("A3").build(),

                        Subject.builder().name("History (A3)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-HIST-A3")
                                .description("History for A3 specialty")
                                .specialty("A3").build()
                ));

                // A4: Economics, Geography, Pure Mathematics (Mechanics or Statistics)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Economics (A4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-ECO-A4")
                                .description("Economics for A4 specialty")
                                .specialty("A4").build(),

                        Subject.builder().name("Geography (A4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-GEO-A4")
                                .description("Geography for A4 specialty")
                                .specialty("A4").build(),

                        Subject.builder().name("Pure Mathematics (Mechanics) (A4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-PMATH-MECH-A4")
                                .description("Pure Mathematics (Mechanics) for A4")
                                .specialty("A4").build(),

                        Subject.builder().name("Pure Mathematics (Statistics) (A4)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-PMATH-STAT-A4")
                                .description("Pure Mathematics (Statistics) for A4")
                                .specialty("A4").build()
                ));

                // A5: Literature, History, Philosophy
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Literature (A5)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-LIT-A5")
                                .description("Literature for A5 specialty")
                                .specialty("A5").build(),

                        Subject.builder().name("History (A5)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-HIST-A5")
                                .description("History for A5 specialty")
                                .specialty("A5").build(),

                        Subject.builder().name("Philosophy (A5)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-PHIL-A5")
                                .description("Philosophy for A5 specialty")
                                .specialty("A5").build()
                ));

                // OPTIONAL SUBJECTS FOR ARTS SIXTH FORM (Available to all specialties)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Computer Science (Arts Advanced)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-COMP-ART")
                                .description("Optional Computer Science for Arts - A-Level")
                                .optional(true).build(),

                        Subject.builder().name("ICT (Arts Advanced)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.ART))
                                .subjectCode("A-ICT-ART")
                                .description("Optional ICT for Arts - A-Level")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 6️⃣ COMMERCIAL DEPARTMENT
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.COM)) {
                log.info("Creating Commercial subjects...");

                // FORM 1-2 SUBJECTS (NO SPECIALTY)
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Mathematics (Commercial)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-MATH-F1-2").description("Mathematics for Commercial Studies - Form 1-2").build(),

                        Subject.builder().name("English (Commercial)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ENG-F1-2").description("English for Commercial Studies - Form 1-2").build(),

                        Subject.builder().name("French (Commercial)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-FREN-F1-2").description("French for Commercial Studies - Form 1-2").build(),

                        Subject.builder().name("Accounting (Form 1-2)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ACC-F1-2").description("Accounting for Forms 1-2").build(),

                        Subject.builder().name("Commerce (Form 1-2)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-COM-F1-2").description("Commerce for Forms 1-2").build(),

                        Subject.builder().name("Office and Administrative Management (Form 1-2)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OAM-F1-2").description("Office and Administrative Management - Form 1-2").build(),

                        Subject.builder().name("Computer Science (Commercial)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-COMP-F1-2").description("Computer Science for Commercial - Form 1-2").build(),

                        Subject.builder().name("Marketing (Form 1-2)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-MKT-F1-2").description("Marketing for Forms 1-2").build(),

                        Subject.builder().name("History (Commercial)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-HIST-F1-2").description("History for Commercial Studies - Form 1-2").build(),

                        Subject.builder().name("Geography (Commercial)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-GEO-F1-2").description("Geography for Commercial Studies - Form 1-2").build(),

                        Subject.builder().name("Citizenship (Commercial)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-CIT-F1-2").description("Citizenship for Commercial Studies - Form 1-2").build(),

                        Subject.builder().name("Religious Studies (Commercial)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-REL-F1-2")
                                .description("Optional Religious Studies - Form 1-2")
                                .optional(true).build(),

                        Subject.builder().name("Office Practice (Form 1-2)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OP-F1-2")
                                .description("Optional Office Practice - Form 1-2")
                                .optional(true).build()
                ));

                // FORM 3-5 ACCOUNTING SPECIALTY
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("OHADA Financial Accounting").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OHADA-FA")
                                .description("OHADA Financial Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("OHADA Financial Reporting").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OHADA-FR")
                                .description("OHADA Financial Reporting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("International Financial Accounting").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-IFA")
                                .description("International Financial Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Business Mathematics (Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-BMATH-ACC")
                                .description("Business Mathematics for Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Commerce (Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-COM-ACC")
                                .description("Commerce for Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Professional Communication Technique (Accounting)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-PCT-ACC")
                                .description("Professional Communication Technique for Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Entrepreneurship (Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ENT-ACC")
                                .description("Entrepreneurship for Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Economics (Accounting)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ECO-ACC")
                                .description("Economics for Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Law and Government (Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-LG-ACC")
                                .description("Law and Government for Accounting")
                                .specialty("Accounting").build(),

                        Subject.builder().name("ICT (Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ICT-ACC")
                                .description("Optional ICT for Accounting")
                                .specialty("Accounting")
                                .optional(true).build(),

                        Subject.builder().name("Computer Science (Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-COMP-ACC")
                                .description("Optional Computer Science for Accounting")
                                .specialty("Accounting")
                                .optional(true).build()
                ));

                // FORM 3-5 ADMINISTRATION & COMMUNICATION TECHNIQUES SPECIALTY
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Office and Administration Management (ACT)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OAM-ACT")
                                .description("Office and Administration Management for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Information Processing").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-IP")
                                .description("Information Processing for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Professional Communication Technique (ACT)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-PCT-ACT")
                                .description("Professional Communication Technique for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Graphic Designing").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-GD")
                                .description("Graphic Designing for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Commerce (ACT)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-COM-ACT")
                                .description("Commerce for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Business Mathematics (ACT)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-BMATH-ACT")
                                .description("Business Mathematics for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Law and Government (ACT)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-LG-ACT")
                                .description("Law and Government for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Economics (ACT)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ECO-ACT")
                                .description("Economics for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("OHADA Financial Accounting (ACT)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OHADA-ACT")
                                .description("OHADA Financial Accounting for ACT")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("ICT (ACT)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ICT-ACT")
                                .description("Optional ICT for ACT")
                                .specialty("Administration & Communication Techniques")
                                .optional(true).build(),

                        Subject.builder().name("Computer Science (ACT)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-COMP-ACT")
                                .description("Optional Computer Science for ACT")
                                .specialty("Administration & Communication Techniques")
                                .optional(true).build()
                ));

                // LOWER/UPPER SIXTH ADMINISTRATION & COMMUNICATION TECHNIQUES
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Automated Clerical Management").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ACM-L6-U6")
                                .description("Automated Clerical Management - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Professional English (ACT)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-PENG-ACT-L6-U6")
                                .description("Professional English for ACT - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Applied Work").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-AW-L6-U6")
                                .description("Applied Work - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Graphic Designing (Advanced)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-GD-ADV-L6-U6")
                                .description("Advanced Graphic Designing - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Office Technology").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-OT-L6-U6")
                                .description("Office Technology - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Information Processing (Advanced)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-IP-ADV-L6-U6")
                                .description("Advanced Information Processing - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Professional Communication Technique (Advanced)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-PCT-ADV-L6-U6")
                                .description("Advanced Professional Communication - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build(),

                        Subject.builder().name("Commerce and Finance (ACT)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-CF-ACT-L6-U6")
                                .description("Commerce and Finance for ACT - Lower/Upper Sixth")
                                .specialty("Administration & Communication Techniques").build()
                ));

                // LOWER/UPPER SIXTH ACCOUNTING SPECIALTY
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Cost and Management Accounting").coefficient(5)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-CMA-L6-U6")
                                .description("Cost and Management Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Financial Accounting (Advanced)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-FA-ADV-L6-U6")
                                .description("Advanced Financial Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Corporate Accounting").coefficient(5)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-CA-L6-U6")
                                .description("Corporate Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Business Mathematics (Advanced Accounting)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-BMATH-ADV-ACC-L6-U6")
                                .description("Advanced Business Mathematics for Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Entrepreneurship (Advanced Accounting)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ENT-ADV-ACC-L6-U6")
                                .description("Advanced Entrepreneurship for Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Economics (Advanced Accounting)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ECO-ADV-ACC-L6-U6")
                                .description("Advanced Economics for Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Commerce and Finance (Accounting)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-CF-ACC-L6-U6")
                                .description("Commerce and Finance for Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("Business Management (Accounting)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-BM-ACC-L6-U6")
                                .description("Business Management for Accounting - Lower/Upper Sixth")
                                .specialty("Accounting").build(),

                        Subject.builder().name("ICT (Advanced Accounting)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.COM))
                                .subjectCode("COM-ICT-ADV-ACC-L6-U6")
                                .description("Optional Advanced ICT for Accounting - Lower/Upper Sixth")
                                .specialty("Accounting")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 7️⃣ TECHNICAL DEPARTMENT (NO SPECIALTY)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.TEC)) {
                log.info("Creating Technical (Building and Construction) subjects...");

                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Quantities and Estimate").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-QE").description("Quantities and Estimate").build(),

                        Subject.builder().name("Soils and Survey").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-SS").description("Soils and Survey").build(),

                        Subject.builder().name("Practicals (Technical)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-PR").description("Building and Construction Practicals").build(),

                        Subject.builder().name("Technical Drawing").coefficient(4)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-TD").description("Technical Drawing").build(),

                        Subject.builder().name("Applied Mechanics").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-AM").description("Applied Mechanics").build(),

                        Subject.builder().name("Construction Processes").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-CP").description("Construction Processes").build(),

                        Subject.builder().name("Project Management").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-PM").description("Project Management").build(),

                        Subject.builder().name("Trade and Training").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-TT").description("Trade and Training").build(),

                        Subject.builder().name("English (Technical)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-ENG").description("English for Technical Studies").build(),

                        Subject.builder().name("French (Technical)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-FREN").description("French for Technical Studies").build(),

                        Subject.builder().name("Mathematics (Technical)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-MATH").description("Mathematics for Technical Studies").build(),

                        Subject.builder().name("Physics (Technical)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-PHY").description("Physics for Technical Studies").build(),

                        Subject.builder().name("Chemistry (Technical)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-CHEM").description("Chemistry for Technical Studies").build(),

                        Subject.builder().name("Citizenship (Technical)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-CIT").description("Citizenship for Technical Studies").build(),

                        Subject.builder().name("Law and Government (Technical)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-LG").description("Law and Government for Technical Studies").build(),

                        Subject.builder().name("History (Technical)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-HIST").description("History for Technical Studies").build(),

                        Subject.builder().name("Geography (Technical)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-GEO").description("Geography for Technical Studies").build(),

                        Subject.builder().name("Engineering Science").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-ES").description("Engineering Science").build(),

                        Subject.builder().name("Computer Science (Technical)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.TEC))
                                .subjectCode("TEC-COMP").description("Computer Science for Technical Studies").build()
                ));
            }

            // ============================================================
            // 8️⃣ HOME ECONOMICS DEPARTMENT (NO SPECIALTY)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.HE)) {
                log.info("Creating Home Economics subjects...");

                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Food Nutrition and Health (FNH)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-FNH-F1-5").description("Food Nutrition and Health - Form 1-5").build(),

                        Subject.builder().name("Practicals on Food Nutrition and Health").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-FNH-PR-F1-5").description("Practicals on Food Nutrition and Health - Form 1-5").build(),

                        Subject.builder().name("Resource Management on Home Studies (RMHS)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-RMHS-F1-5").description("Resource Management on Home Studies - Form 1-5").build(),

                        Subject.builder().name("Practicals on RMHS").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-RMHS-PR-F1-5").description("Practicals on Resource Management - Form 1-5").build(),

                        Subject.builder().name("Family Life Education and Gerontology (FLEG)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-FLEG-F1-5").description("Family Life Education and Gerontology - Form 1-5").build(),

                        Subject.builder().name("Natural Science (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-NS-F1-5").description("Natural Science for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Business Mathematics (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-BMATH-F1-5").description("Business Mathematics for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Entrepreneurship (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-ENT-F1-5").description("Entrepreneurship for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Economic Geography (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-EGEO-F1-5").description("Economic Geography for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Law and Government (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-LG-F1-5").description("Law and Government for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Citizenship (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-CIT-F1-5").description("Citizenship for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Management Aided in Computer (HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-MAC-F1-5").description("Management Aided in Computer for Home Economics - Form 1-5").build(),

                        Subject.builder().name("French (HE)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-FREN-F1-5").description("French for Home Economics - Form 1-5").build(),

                        Subject.builder().name("English (HE)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-ENG-F1-5").description("English for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Mathematics (HE)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-MATH-F1-5").description("Mathematics for Home Economics - Form 1-5").build(),

                        Subject.builder().name("Catering Management and Dietetics").coefficient(5)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-CATERING-L6-U6").description("Catering Management and Dietetics - Lower/Upper Sixth").build(),

                        Subject.builder().name("Culinary Practice on Catering Management and Dietetics").coefficient(3)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-CULINARY-L6-U6").description("Culinary Practice - Lower/Upper Sixth").build(),

                        Subject.builder().name("Family Life Education and Gerontology (Theory)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-FLEG-T-L6-U6").description("FLEG Theory - Lower/Upper Sixth").build(),

                        Subject.builder().name("Family Life Education and Gerontology (Practicals)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-FLEG-P-L6-U6").description("FLEG Practicals - Lower/Upper Sixth").build(),

                        Subject.builder().name("Resource Management on Home Studies (RMHS)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-RMHS-L6-U6").description("Resource Management - Lower/Upper Sixth").build(),

                        Subject.builder().name("Practicals on RMHS").coefficient(3)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-RMHS-P-L6-U6").description("RMHS Practicals - Lower/Upper Sixth").build(),

                        Subject.builder().name("Social Life").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-SL-L6-U6").description("Social Life Studies - Lower/Upper Sixth").build(),

                        Subject.builder().name("Entrepreneurship (Advanced HE)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-ENT-ADV-L6-U6").description("Advanced Entrepreneurship for Home Economics - Lower/Upper Sixth").build(),

                        Subject.builder().name("Natural Science (Advanced HE)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-NS-ADV-L6-U6").description("Advanced Natural Science for Home Economics - Lower/Upper Sixth").build(),

                        Subject.builder().name("Economics (HE Advanced)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-ECO-ADV")
                                .description("Optional Economics for Home Economics - Lower/Upper Sixth")
                                .optional(true).build(),

                        Subject.builder().name("Management Aided in Computer (Advanced HE)").coefficient(1)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-MAC-ADV")
                                .description("Optional Advanced Management Aided in Computer - Lower/Upper Sixth")
                                .optional(true).build(),

                        Subject.builder().name("Professional English (HE)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.HE))
                                .subjectCode("HE-PENG-ADV")
                                .description("Optional Professional English for Home Economics - Lower/Upper Sixth")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 9️⃣ EPS DEPARTMENT (NO SPECIALTY)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.EPS)) {
                log.info("Creating EPS subjects...");

                subjects.addAll(Arrays.asList(
                        Subject.builder().name("French (EPS)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-FREN-F1-5").description("French for EPS - Form 1-5").build(),

                        Subject.builder().name("English (EPS)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ENG-F1-5").description("English for EPS - Form 1-5").build(),

                        Subject.builder().name("Mathematics (EPS)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-MATH-F1-5").description("Mathematics for EPS - Form 1-5").build(),

                        Subject.builder().name("Human and Economic Geography (EPS)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-Geo-F1-5").description("Human and Economic Geography for EPS - Form 1-5").build(),

                        Subject.builder().name("Citizenship (EPS)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-CITI-F1-5").description("Citizenship for EPS - Form 1-5").build(),

                        Subject.builder().name("Entrepreneurship (EPS)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ENTRE-F1-5").description("Entrepreneurship for EPS - Form 1-5").build(),

                        Subject.builder().name("Engineering Science (EPS)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ENG-SCI-F1-5").description("Engineering Science for EPS - Form 1-5").build(),

                        Subject.builder().name("Computer Science (EPS)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-Computer-SCI-F1-5").description("Computer Science for EPS - Form 1-5").build(),

                        Subject.builder().name("Electrical Technology (EPS)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ELEC-TECH-F1-5").description("Electrical Technology for EPS - Form 1-5").build(),

                        Subject.builder().name("Electrical Diagram (EPS)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ELEC-DIA-F1-5").description("Electrical Diagram for EPS - Form 1-5").build(),

                        Subject.builder().name("EPS Practicals (EPS)").coefficient(6)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-PRAC-F1-5").description("Practicals EPS - Form 1-5").build(),

                        Subject.builder().name("Engineering Drawing (EPS)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ENG-DRA-F1-5").description("Engineering Drawing for EPS - Form 1-5").build(),

                        Subject.builder().name("Electrical Circuit (Form 2-5)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-OP-F2-5")
                                .description("Optional Electrical Circuit - Form 2-5")
                                .optional(true).build(),

                        Subject.builder().name("Law and Government (Form 3-5)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-LAW-F3-5")
                                .description("Optional Law and Government - Form 3-5")
                                .optional(true).build(),

                        Subject.builder().name("Electrical Technology (Form 3-5)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ELEC-TECH-F3-5")
                                .description("Optional Electrical Technology - Form 3-5")
                                .optional(true).build(),

                        Subject.builder().name("Electrical Machine (Form 3-5)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-ELEC-MACH-F3-5")
                                .description("Optional Electrical Machine - Form 3-5")
                                .optional(true).build(),

                        Subject.builder().name("Test and Measurement (Form 3-5)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.EPS))
                                .subjectCode("EPS-TEST-MEA-F3-5")
                                .description("Optional Test and Measurement - Form 3-5")
                                .optional(true).build()
                ));
            }

            // ============================================================
            // 🔟 CI DEPARTMENT (NO SPECIALTY)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.CI)) {
                log.info("Creating Clothing Industry subjects...");

                subjects.addAll(Arrays.asList(
                        Subject.builder().name("French (CI)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-FREN-F1-5").description("French for CI - Form 1-5").build(),

                        Subject.builder().name("English (CI)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-ENG-F1-5").description("English for CI - Form 1-5").build(),

                        Subject.builder().name("Mathematics (CI)").coefficient(5)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-MATH-F1-5").description("Mathematics for CI - Form 1-5").build(),

                        Subject.builder().name("Technology (CI)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-TECH-F1-5").description("Technology for CI - Form 1-5").build(),

                        Subject.builder().name("Practical (CI)").coefficient(4)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-PRAC-F1-5").description("Practical for CI - Form 1-5").build(),

                        Subject.builder().name("Technical Drawing (CI)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-TECH-DRAW-F1-5").description("Technical Drawing for CI - Form 1-5").build(),

                        Subject.builder().name("Computer Science (CI)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-COMP-F1-5")
                                .description("Optional Computer Science - Form 1-5")
                                .optional(true).build(),

                        Subject.builder().name("Trade and mining (CI)").coefficient(3)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-TRADE-MIN-F1-5")
                                .description("Optional Trade and Mining - Form 1-5")
                                .optional(true).build(),

                        Subject.builder().name("Materials (CI)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-MATERIALS-F2-5")
                                .description("Optional Materials - Form 2-5")
                                .optional(true).build(),

                        Subject.builder().name("Survey (CI)").coefficient(2)
                                .department(deptMap.get(DepartmentCode.CI))
                                .subjectCode("CI-SURVEY-F1-5")
                                .description("Optional Survey - Form 2-5")
                                .optional(true).build()
                ));
            }

            log.info("Total subjects to create: {}", subjects.size());

            // ============================================================
            // ✅ SAVE ALL SUBJECTS
            // ============================================================
            try {
                List<Subject> savedSubjects = subjectService.createSubjects(subjects);
                log.info("✅ All subjects initialized successfully - {} subjects processed", savedSubjects.size());

                Map<String, Long> subjectCountByDept = savedSubjects.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getDepartment() != null ? s.getDepartment().getName() : "General",
                                Collectors.counting()
                        ));

                log.info("Subject distribution by department: {}", subjectCountByDept);

                for (Map.Entry<String, Long> entry : subjectCountByDept.entrySet()) {
                    log.info("  {}: {} subjects", entry.getKey(), entry.getValue());
                }

            } catch (Exception e) {
                log.error("Critical error during subject initialization", e);
                throw new RuntimeException("Failed to initialize subjects", e);
            }
        } else {
            log.info("Subjects already initialized, skipping subject creation");
        }
    }
}