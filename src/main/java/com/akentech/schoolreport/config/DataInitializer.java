package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.Department;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.model.enums.DepartmentCode;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.DepartmentRepository;
import com.akentech.schoolreport.repository.SubjectRepository;
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
                    Department.builder().name("Building and Construction").code(DepartmentCode.BC).description("Building and Construction").build(),
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
                if (dept.getCode() != null) {
                    deptMap.put(dept.getCode(), dept);
                    log.info("Department mapped: {} -> {}", dept.getCode(), dept.getName());
                } else {
                    log.warn("Skipping department {}: null code", dept.getName());
                }
            }

            List<Subject> subjects = new ArrayList<>();

            // Get all classrooms for reference
            List<ClassRoom> allClassRooms = classRoomRepository.findAll();
            Map<ClassLevel, ClassRoom> classRoomMap = allClassRooms.stream()
                    .filter(cr -> cr.getCode() != null)
                    .collect(Collectors.toMap(ClassRoom::getCode, cr -> cr));

            // ============================================================
            // 1️⃣ GENERAL DEPARTMENT - FORMS 1-3 (ALL COMPULSORY)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.GEN)) {
                log.info("Creating General department subjects for Forms 1-3...");

                // FORM 1 SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_1)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Biology").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-BIO").description("Biology - Form 1").build(),

                            Subject.builder().name("Chemistry").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-CHEM").description("Chemistry - Form 1").build(),

                            Subject.builder().name("Physics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-PHY").description("Physics - Form 1").build(),

                            Subject.builder().name("History").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-HIST").description("History - Form 1").build(),

                            Subject.builder().name("Geography").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-GEO").description("Geography - Form 1").build(),

                            Subject.builder().name("Literature in English").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-LIT").description("Literature in English - Form 1").build(),

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-MATH").description("Mathematics - Form 1").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-ENG").description("English - Form 1").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_1))
                                    .subjectCode("F1-GEN-FREN").description("French - Form 1").build()

                    ));
                }

                // FORM 2 SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_2)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-MATH").description("Mathematics - Form 2").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-ENG").description("English - Form 2").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-FREN").description("French - Form 2").build(),

                            Subject.builder().name("Geography").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-GEO").description("Geography - Form 2").build(),


                            Subject.builder().name("Biology").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-BIO").description("Biology - Form 2").build(),

                            Subject.builder().name("Chemistry").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-CHEM").description("Chemistry - Form 2").build(),

                            Subject.builder().name("Physics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-PHY").description("Physics - Form 2").build(),

                            Subject.builder().name("History").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-HIST").description("History - Form 2").build(),

                            Subject.builder().name("Literature in English").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_2))
                                    .subjectCode("F2-GEN-LIT").description("Literature in English - Form 2").build()

                    ));
                }

                // FORM 3 SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_3)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-MATH").description("Mathematics - Form 3").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-ENG").description("English - Form 3").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-FREN").description("French - Form 3").build(),

                            Subject.builder().name("Biology").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-BIO").description("Biology - Form 3").build(),

                            Subject.builder().name("Chemistry").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-CHEM").description("Chemistry - Form 3").build(),

                            Subject.builder().name("Physics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-PHY").description("Physics - Form 3").build(),

                            Subject.builder().name("History").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-HIST").description("History - Form 3").build(),

                            Subject.builder().name("Geography").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-GEO").description("Geography - Form 3").build(),

                            Subject.builder().name("Literature in English").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.GEN))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_3))
                                    .subjectCode("F3-GEN-LIT").description("Literature in English - Form 3").build()

                    ));
                }

                // OPTIONAL SUBJECTS FOR GENERAL DEPARTMENT (Forms 1-5)
                subjects.addAll(createOptionalSubjects(deptMap.get(DepartmentCode.GEN), classRoomMap));
            }

            // ============================================================
            // 2️⃣ SCIENCE DEPARTMENT - FORMS 4-5 (O-LEVEL)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.SCI)) {
                log.info("Creating Science department subjects for Forms 4-5...");

                // FORM 4 SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_4)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-MATH").description("Mathematics - Form 4 Science").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-ENG").description("English - Form 4 Science").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-FREN").description("French - Form 4 Science").build(),

                            Subject.builder().name("Biology").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-BIO").description("Biology - Form 4 Science").build(),

                            Subject.builder().name("Chemistry").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-CHEM").description("Chemistry - Form 4 Science").build(),

                            Subject.builder().name("Physics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-PHY").description("Physics - Form 4 Science").build(),

                            Subject.builder().name("Additional Mathematics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-ADDMATH")
                                    .description("Additional Mathematics - Form 4 Science")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Human Biology").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-HBIO")
                                    .description("Human Biology - Form 4 Science (Optional)")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Geography").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-GEOGRAPHY")
                                    .description("Geography - Form 4 Science (Optional)")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Economics").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-ECONS")
                                    .description("Economics -Form 4 Science (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Computer Science").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-COMP")
                                    .description("Computer Science -Form 4 Science (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Religious Studies").coefficient(2)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-RS")
                                    .description("Religious Studies - Form 4 Science")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Citizenship").coefficient(2)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-CIT")
                                    .description("Citizenship - Form 4 Science")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("ICT").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-SCI-ICT")
                                    .description("ICT -Form 4 Science (Optional")
                                    .optional(true)
                                    .build()
                    ));
                }

                // FORM 5 SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_5)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-MATH").description("Mathematics - Form 5 Science").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-ENG").description("English - Form 5 Science").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-FREN").description("French - Form 5 Science").build(),

                            Subject.builder().name("Biology").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-BIO").description("Biology - Form 5 Science").build(),

                            Subject.builder().name("Chemistry").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-CHEM").description("Chemistry - Form 5 Science").build(),

                            Subject.builder().name("Physics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-PHY").description("Physics - Form 5 Science").build(),

                            Subject.builder().name("Additional Mathematics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-ADDMATH")
                                    .description("Additional Mathematics - Form 5 Science")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Religious Studies").coefficient(2)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-RS")
                                    .description("Religious Studies - Form 5 Science")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Citizenship").coefficient(2)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-CIT")
                                    .description("Citizenship - Form 5 Science")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Human Biology").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-HBIO")
                                    .description("Human Biology - Form 5 Science (Optional)")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Geography").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-GEOGRAPHY")
                                    .description("Geography - Form 5 Science (Optional)")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Economics").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-ECONOMICS")
                                    .description("Economics - Form 5 Science (Optional)")
                                    .optional(true)
                                    .build(),


                            Subject.builder().name("Computer Science").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-COMP")
                                    .description("Computer Science -Form 5 Science (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("ICT").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.SCI))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-SCI-ICT")
                                    .description("ICT -Form 5 Science (Optional")
                                    .optional(true)
                                    .build()

                    ));
                }

                // ============================================================
                // SCIENCE SIXTH FORM SUBJECTS BY SPECIALTY
                // ============================================================
                createScienceSixthFormSubjects(subjects, deptMap.get(DepartmentCode.SCI), classRoomMap);
            }

            // ============================================================
            // 3️⃣ ARTS DEPARTMENT - FORMS 4-5 (O-LEVEL)
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.ART)) {
                log.info("Creating Arts department subjects for Forms 4-5...");

                // FORM 4 ARTS SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_4)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-MATH").description("Mathematics - Form 4 Arts").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-ENG").description("English - Form 4 Arts").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-FREN").description("French - Form 4 Arts").build(),

                            Subject.builder().name("Literature in English").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-LIT").description("Literature in English - Form 4 Arts").build(),

                            Subject.builder().name("History").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-HIST").description("History - Form 4 Arts").build(),

                            Subject.builder().name("Geography").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-GEO").description("Geography - Form 4 Arts").build(),

                            Subject.builder().name("Economics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-ECONS").description("Economics - Form 4 Arts").build(),

                            Subject.builder().name("Computer Science").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-COMP")
                                    .description("Computer Science -Form 4 Arts (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("ICT").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-ICT")
                                    .description("ICT -Form 4 Arts (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Religious Studies").coefficient(2)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-RS")
                                    .description("Religious Studies - Form 5 Arts")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Citizenship").coefficient(2)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-CIT")
                                    .description("Citizenship - Form 4 Arts")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Biology").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_4))
                                    .subjectCode("F4-ART-BIO")
                                    .description("Biology -Form 4 Arts (Optional")
                                    .optional(true)
                                    .build()
                    ));
                }

                // FORM 5 ARTS SUBJECTS
                if (classRoomMap.containsKey(ClassLevel.FORM_5)) {
                    subjects.addAll(Arrays.asList(

                            Subject.builder().name("Mathematics").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-MATH").description("Mathematics - Form 5 Arts").build(),

                            Subject.builder().name("English").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-ENG").description("English - Form 5 Arts").build(),

                            Subject.builder().name("French").coefficient(5)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-FREN").description("French - Form 5 Arts").build(),

                            Subject.builder().name("Literature in English").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-LIT").description("Literature in English - Form 5 Arts").build(),

                            Subject.builder().name("History").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-HIST").description("History - Form 5 Arts").build(),

                            Subject.builder().name("Geography").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-GEO").description("Geography - Form 5 Arts").build(),


                            Subject.builder().name("Economics").coefficient(4)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-ECONS").description("Economics - Form 5 Arts").build(),

                            Subject.builder().name("Computer Science").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-COMP")
                                    .description("Computer Science -Form 5 Arts (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("ICT").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-ICT")
                                    .description("ICT -Form 5 Arts (Optional")
                                    .optional(true)
                                    .build(),

                            Subject.builder().name("Biology").coefficient(3)
                                    .department(deptMap.get(DepartmentCode.ART))
                                    .classRoom(classRoomMap.get(ClassLevel.FORM_5))
                                    .subjectCode("F5-ART-BIO")
                                    .description("Biology -Form 5 Arts (Optional")
                                    .optional(true)
                                    .build()
                    ));
                }

                // ============================================================
                // ARTS SIXTH FORM SUBJECTS BY SPECIALTY
                // ============================================================
                createArtsSixthFormSubjects(subjects, deptMap.get(DepartmentCode.ART), classRoomMap);
            }

            // ============================================================
            // 4️⃣ COMMERCIAL DEPARTMENT SUBJECTS BY CLASS
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.COM)) {
                log.info("Creating Commercial department subject...");
                createCommercialSubjects(subjects, deptMap.get(DepartmentCode.COM), classRoomMap);
            }

            // ============================================================
            // 5️⃣ TECHNICAL DEPARTMENT SUBJECTS BY CLASS
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.BC)) {
                log.info("Creating Technical department subjects...");
                createBuildingConstructionSubjects(subjects, deptMap.get(DepartmentCode.BC), classRoomMap);
            }

            // ============================================================
            // 6️⃣ HOME ECONOMICS DEPARTMENT SUBJECTS BY CLASS
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.HE)) {

                Department heDepartment = deptMap.get(DepartmentCode.HE);

                log.info("Creating Home Economics department subjects...");

                // 1st Cycle (Forms 1–5)
                createHomeEconomicsSubjects(
                        subjects,
                        heDepartment,
                        classRoomMap
                );

                // 2nd Cycle (Lower & Upper Sixth)
                createHomeEconomicsSixthFormSubjects(
                        subjects,
                        heDepartment,
                        classRoomMap
                );
            }


            // ============================================================
            // 7️⃣ EPS DEPARTMENT SUBJECTS BY CLASS
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.EPS)) {
                log.info("Creating EPS department subjects...");
                createEPSSubjects(subjects, deptMap.get(DepartmentCode.EPS), classRoomMap);
            }

            // ============================================================
            // 8️⃣ CI DEPARTMENT SUBJECTS BY CLASS
            // ============================================================
            if (deptMap.containsKey(DepartmentCode.CI)) {
                log.info("Creating CI department subjects...");
                createCISubjects(subjects, deptMap.get(DepartmentCode.CI), classRoomMap);
            }

            log.info("Total subjects to create: {}", subjects.size());

            // ============================================================
            // ✅ SAVE ALL SUBJECTS
            // ============================================================
            try {
                List<Subject> savedSubjects = subjectService.createSubjects(subjects);
                log.info("✅ All subjects initialized successfully - {} subjects processed", savedSubjects.size());

                // Log subject distribution by department and class
                Map<String, Map<String, Long>> distribution = savedSubjects.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getDepartment() != null ? s.getDepartment().getName() : "Unknown",
                                Collectors.groupingBy(
                                        s -> s.getClassRoom() != null ? s.getClassRoom().getName() : "No Class",
                                        Collectors.counting()
                                )
                        ));

                log.info("Subject distribution by department and class:");
                distribution.forEach((dept, classMap) -> {
                    log.info("  {}:", dept);
                    classMap.forEach((className, count) -> log.info("    {}: {} subjects", className, count));
                });

                // Count subjects with specialties
                long specialtySubjects = savedSubjects.stream()
                        .filter(s -> s.getSpecialty() != null && !s.getSpecialty().isEmpty())
                        .count();
                log.info("Subjects with specialties: {}", specialtySubjects);

            } catch (Exception e) {
                log.error("Critical error during subject initialization", e);
                throw new RuntimeException("Failed to initialize subjects", e);
            }
        } else {
            log.info("Subjects already initialized, skipping subject creation");
        }
    }

    // ============================================================
    // HELPER METHODS FOR SUBJECT CREATION
    // ============================================================

    private List<Subject> createOptionalSubjects(Department department, Map<ClassLevel, ClassRoom> classRoomMap) {
        List<Subject> optionalSubjects = new ArrayList<>();

        for (ClassLevel level : Arrays.asList(ClassLevel.FORM_1, ClassLevel.FORM_2, ClassLevel.FORM_3,
                ClassLevel.FORM_4, ClassLevel.FORM_5)) {
            if (classRoomMap.containsKey(level)) {
                String formCode = level.name().replace("FORM_", "F");
                optionalSubjects.addAll(Arrays.asList(
                        Subject.builder().name("Religious Studies (Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-REL-OPT")
                                .description("Optional Religious Studies - " + classRoomMap.get(level).getName())
                                .optional(true).build(),

                        Subject.builder().name("Citizenship Education (Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-CIT-OPT")
                                .description("Optional Citizenship Education - " + classRoomMap.get(level).getName())
                                .optional(true).build(),

                        Subject.builder().name("Computer Science (Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-COMP-OPT")
                                .description("Optional Computer Science - " + classRoomMap.get(level).getName())
                                .optional(true).build(),

                        Subject.builder().name("ICT (Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-ICT-OPT")
                                .description("Optional ICT - " + classRoomMap.get(level).getName())
                                .optional(true).build(),

                        Subject.builder().name("Integrated Science (Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-INT-OPT")
                                .description("Optional Integrated Science - " + classRoomMap.get(level).getName())
                                .optional(true).build(),

                        Subject.builder().name("Economics (Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-ECONS-OPT")
                                .description("Optional Economics - " + classRoomMap.get(level).getName())
                                .optional(true).build(),

                        Subject.builder().name("Food And Nutrition(Optional)").coefficient(2)
                                .department(department)
                                .classRoom(classRoomMap.get(level))
                                .subjectCode(formCode + "-GEN-FN-OPT")
                                .description("Optional Food And Nutrition - " + classRoomMap.get(level).getName())
                                .optional(true).build()
                ));
            }
        }

        return optionalSubjects;
    }

    private void createScienceSixthFormSubjects(List<Subject> subjects, Department department,
                                                Map<ClassLevel, ClassRoom> classRoomMap) {
        if (!classRoomMap.containsKey(ClassLevel.LOWER_SIXTH) ||
                !classRoomMap.containsKey(ClassLevel.UPPER_SIXTH)) {
            return;
        }

        ClassRoom lowerSixth = classRoomMap.get(ClassLevel.LOWER_SIXTH);
        ClassRoom upperSixth = classRoomMap.get(ClassLevel.UPPER_SIXTH);

        // S1: Chemistry, Physics, Pure Mathematics with Mechanics
        createScienceSpecialty(subjects, department, lowerSixth, "S1", "Chemistry, Physics, Pure Mathematics with Mechanics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S1-CHEM"),
                        new SubjectSpec("Physics", 5, "L6-SCI-S1-PHY"),
                        new SubjectSpec("Pure Mathematics with Mechanics", 5, "L6-SCI-S1-MATH-MECH"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S1-COMP")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S1", "Chemistry, Physics, Pure Mathematics with Mechanics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S1-CHEM"),
                        new SubjectSpec("Physics", 5, "U6-SCI-S1-PHY"),
                        new SubjectSpec("Pure Mathematics with Mechanics", 5, "U6-SCI-S1-MATH-MECH"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S1-COMP")
                ));

        // S2: Chemistry, Physics, Biology
        createScienceSpecialty(subjects, department, lowerSixth, "S2", "Chemistry, Physics, Biology",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S2-CHEM"),
                        new SubjectSpec("Physics", 5, "L6-SCI-S2-PHY"),
                        new SubjectSpec("Biology", 5, "L6-SCI-S2-BIO"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S2-COMP"),
                        new SubjectSpec("ICT", 5, "L6-SCI-S2-ICT")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S2", "Chemistry, Physics, Biology",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S2-CHEM"),
                        new SubjectSpec("Physics", 5, "U6-SCI-S2-PHY"),
                        new SubjectSpec("Biology", 5, "U6-SCI-S2-BIO"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S2-COMP"),
                        new SubjectSpec("ICT", 5, "U6-SCI-S2-ICT")
                ));

        // S3: Biology, Chemistry, Pure Mathematics with Statistics
        createScienceSpecialty(subjects, department, lowerSixth, "S3", "Biology, Chemistry, Pure Mathematics with Statistics",
                Arrays.asList(
                        new SubjectSpec("Biology", 5, "L6-SCI-S3-BIO"),
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S3-CHEM"),
                        new SubjectSpec("Pure Mathematics with Statistics", 5, "L6-SCI-S3-MATH-STAT"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S3-COMP"),
                        new SubjectSpec("ICT", 5, "L6-SCI-S3-ICT")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S3", "Biology, Chemistry, Pure Mathematics with Statistics",
                Arrays.asList(
                        new SubjectSpec("Biology", 5, "U6-SCI-S3-BIO"),
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S3-CHEM"),
                        new SubjectSpec("Pure Mathematics with Statistics", 5, "U6-SCI-S3-MATH-STAT"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S3-COMP"),
                        new SubjectSpec("ICT", 5, "U6-SCI-S3-ICT")
                ));

        // S4: Biology, Chemistry, Geology
        createScienceSpecialty(subjects, department, lowerSixth, "S4", "Biology, Chemistry, Geology",
                Arrays.asList(
                        new SubjectSpec("Biology", 5, "L6-SCI-S4-BIO"),
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S4-CHEM"),
                        new SubjectSpec("Geology", 5, "L6-SCI-S4-GEOL"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S4-COMP"),
                        new SubjectSpec("ICT", 5, "L6-SCI-S4-ICT")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S4", "Biology, Chemistry, Geology",
                Arrays.asList(
                        new SubjectSpec("Biology", 5, "U6-SCI-S4-BIO"),
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S4-CHEM"),
                        new SubjectSpec("Geology", 5, "U6-SCI-S4-GEOL"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S4-COMP"),
                        new SubjectSpec("ICT", 5, "U6-SCI-S4-ICT")
                ));

        // S5: Chemistry, Biology, Mathematics with Mechanics
        createScienceSpecialty(subjects, department, lowerSixth, "S5", "Chemistry, Biology, Mathematics with Mechanics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S5-CHEM"),
                        new SubjectSpec("Biology", 5, "L6-SCI-S5-BIO"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "L6-SCI-S5-MATH-MECH"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S5-COMP"),
                        new SubjectSpec("ICT", 5, "L6-SCI-S5-ICT")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S5", "Chemistry, Biology, Mathematics with Mechanics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S5-CHEM"),
                        new SubjectSpec("Biology", 5, "U6-SCI-S5-BIO"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "U6-SCI-S5-MATH-MECH"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S5-COMP"),
                        new SubjectSpec("ICT", 5, "U6-SCI-S5-ICT")
                ));

        // S6: Chemistry, Physics, Mathematics with Mechanics, Further Mathematics
        createScienceSpecialty(subjects, department, lowerSixth, "S6", "Chemistry, Physics, Mathematics with Mechanics, Further Mathematics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S6-CHEM"),
                        new SubjectSpec("Physics", 5, "L6-SCI-S6-PHY"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "L6-SCI-S6-MATH-MECH"),
                        new SubjectSpec("Further Mathematics", 4, "L6-SCI-S6-FMATH"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S6-COMP"),
                        new SubjectSpec("ICT", 5, "L6-SCI-S6-ICT")

                ));

        createScienceSpecialty(subjects, department, upperSixth, "S6", "Chemistry, Physics, Mathematics with Mechanics, Further Mathematics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S6-CHEM"),
                        new SubjectSpec("Physics", 5, "U6-SCI-S6-PHY"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "U6-SCI-S6-MATH-MECH"),
                        new SubjectSpec("Further Mathematics", 4, "U6-SCI-S6-FMATH"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S6-COMP"),
                        new SubjectSpec("ICT", 5, "U6-SCI-S6-ICT")
                ));

        // S7: Chemistry, Biology, Physics, Mathematics with Mechanics
        createScienceSpecialty(subjects, department, lowerSixth, "S7", "Chemistry, Biology, Physics, Mathematics with Mechanics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S7-CHEM"),
                        new SubjectSpec("Biology", 5, "L6-SCI-S7-BIO"),
                        new SubjectSpec("Physics", 5, "L6-SCI-S7-PHY"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "L6-SCI-S7-MATH-MECH"),
                        new SubjectSpec("Computer Science", 5, "L6-SCI-S7-COMP"),
                        new SubjectSpec("ICT", 5, "L6-SCI-S7-ICT")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S7", "Chemistry, Biology, Physics, Mathematics with Mechanics",
                Arrays.asList(
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S7-CHEM"),
                        new SubjectSpec("Biology", 5, "U6-SCI-S7-BIO"),
                        new SubjectSpec("Physics", 5, "U6-SCI-S7-PHY"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "U6-SCI-S7-MATH-MECH"),
                        new SubjectSpec("Computer Science", 5, "U6-SCI-S7-COMP"),
                        new SubjectSpec("ICT", 5, "U6-SCI-S7-ICT")
                ));

        // S8: Biology, Chemistry, Physics, Mathematics with Mechanics, Further Mathematics
        createScienceSpecialty(subjects, department, lowerSixth, "S8", "Biology, Chemistry, Physics, Mathematics with Mechanics, Further Mathematics",
                Arrays.asList(
                        new SubjectSpec("Biology", 5, "L6-SCI-S8-BIO"),
                        new SubjectSpec("Chemistry", 5, "L6-SCI-S8-CHEM"),
                        new SubjectSpec("Physics", 5, "L6-SCI-S8-PHY"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "L6-SCI-S8-MATH-MECH"),
                        new SubjectSpec("Further Mathematics", 4, "L6-SCI-S8-FMATH")
                ));

        createScienceSpecialty(subjects, department, upperSixth, "S8", "Biology, Chemistry, Physics, Mathematics with Mechanics, Further Mathematics",
                Arrays.asList(
                        new SubjectSpec("Biology", 5, "U6-SCI-S8-BIO"),
                        new SubjectSpec("Chemistry", 5, "U6-SCI-S8-CHEM"),
                        new SubjectSpec("Physics", 5, "U6-SCI-S8-PHY"),
                        new SubjectSpec("Mathematics with Mechanics", 5, "U6-SCI-S8-MATH-MECH"),
                        new SubjectSpec("Further Mathematics", 4, "U6-SCI-S8-FMATH")
                ));
    }

    private void createScienceSpecialty(List<Subject> subjects, Department department, ClassRoom classRoom,
                                        String specialtyCode, String specialtyDesc, List<SubjectSpec> subjectSpecs) {
        for (SubjectSpec spec : subjectSpecs) {
            subjects.add(
                    Subject.builder()
                            .name(spec.name + " (" + specialtyCode + ")")
                            .coefficient(spec.coefficient)
                            .department(department)
                            .classRoom(classRoom)
                            .subjectCode(spec.code)
                            .specialty(specialtyCode)
                            .description(spec.name + " for " + specialtyCode + " specialty - " +
                                    classRoom.getName() + " - " + specialtyDesc)
                            .build()
            );
        }
    }

    private void createArtsSixthFormSubjects(List<Subject> subjects, Department department,
                                             Map<ClassLevel, ClassRoom> classRoomMap) {
        if (!classRoomMap.containsKey(ClassLevel.LOWER_SIXTH) ||
                !classRoomMap.containsKey(ClassLevel.UPPER_SIXTH)) {
            return;
        }

        ClassRoom lowerSixth = classRoomMap.get(ClassLevel.LOWER_SIXTH);
        ClassRoom upperSixth = classRoomMap.get(ClassLevel.UPPER_SIXTH);

        // A1: Literature, History, French
        createArtsSpecialty(subjects, department, lowerSixth, "A1", "Literature, History, French",
                Arrays.asList(
                        new SubjectSpec("Literature", 5, "L6-ART-A1-LIT"),
                        new SubjectSpec("History", 5, "L6-ART-A1-HIST"),
                        new SubjectSpec("French", 5, "L6-ART-A1-FREN"),
                        new SubjectSpec("ICT", 5, "L6-ART-A1-ICT")
                ));

        createArtsSpecialty(subjects, department, upperSixth, "A1", "Literature, History, French",
                Arrays.asList(
                        new SubjectSpec("Literature", 5, "U6-ART-A1-LIT"),
                        new SubjectSpec("History", 5, "U6-ART-A1-HIST"),
                        new SubjectSpec("French", 5, "U6-ART-A1-FREN"),
                        new SubjectSpec("ICT", 5, "U6-ART-A1-ICT")
                ));

        // A2: History, Geography, Economics
        createArtsSpecialty(subjects, department, lowerSixth, "A2", "History, Geography, Economics",
                Arrays.asList(
                        new SubjectSpec("History", 5, "L6-ART-A2-HIST"),
                        new SubjectSpec("Geography", 5, "L6-ART-A2-GEO"),
                        new SubjectSpec("Economics", 5, "L6-ART-A2-ECO"),
                        new SubjectSpec("ICT", 5, "L6-ART-A2-ICT")
                ));

        createArtsSpecialty(subjects, department, upperSixth, "A2", "History, Geography, Economics",
                Arrays.asList(
                        new SubjectSpec("History", 5, "U6-ART-A2-HIST"),
                        new SubjectSpec("Geography", 5, "U6-ART-A2-GEO"),
                        new SubjectSpec("Economics", 5, "U6-ART-A2-ECO"),
                        new SubjectSpec("ICT", 5, "U6-ART-A2-ICT")
                ));

        // A3: Literature, Economics, History
        createArtsSpecialty(subjects, department, lowerSixth, "A3", "Literature, Economics, History",
                Arrays.asList(
                        new SubjectSpec("Literature", 5, "L6-ART-A3-LIT"),
                        new SubjectSpec("Economics", 5, "L6-ART-A3-ECO"),
                        new SubjectSpec("History", 5, "L6-ART-A3-HIST"),
                        new SubjectSpec("ICT", 5, "L6-ART-A3-ICT")
                ));

        createArtsSpecialty(subjects, department, upperSixth, "A3", "Literature, Economics, History",
                Arrays.asList(
                        new SubjectSpec("Literature", 5, "U6-ART-A3-LIT"),
                        new SubjectSpec("Economics", 5, "U6-ART-A3-ECO"),
                        new SubjectSpec("History", 5, "U6-ART-A3-HIST"),
                        new SubjectSpec("ICT", 5, "U6-ART-A3-ICT")
                ));

        // A4: Economics, Geography, Pure Mathematics
        createArtsSpecialty(subjects, department, lowerSixth, "A4", "Economics, Geography, Pure Mathematics",
                Arrays.asList(
                        new SubjectSpec("Economics", 5, "L6-ART-A4-ECO"),
                        new SubjectSpec("Geography", 5, "L6-ART-A4-GEO"),
                        new SubjectSpec("Pure Mathematics (Statistics)", 5, "L6-ART-A4-MATH-STAT"),
                        new SubjectSpec("ICT", 5, "L6-ART-A4-ICT")
                ));

        createArtsSpecialty(subjects, department, upperSixth, "A4", "Economics, Geography, Pure Mathematics",
                Arrays.asList(
                        new SubjectSpec("Economics", 5, "U6-ART-A4-ECO"),
                        new SubjectSpec("Geography", 5, "U6-ART-A4-GEO"),
                        new SubjectSpec("Pure Mathematics (Statistics)", 5, "U6-ART-A4-MATH-STAT"),
                        new SubjectSpec("ICT", 5, "U6-ART-A4-ICT")
                ));

        // A5: Literature, History, Philosophy
        createArtsSpecialty(subjects, department, lowerSixth, "A5", "Literature, History, Philosophy",
                Arrays.asList(
                        new SubjectSpec("Literature", 5, "L6-ART-A5-LIT"),
                        new SubjectSpec("History", 5, "L6-ART-A5-HIST"),
                        new SubjectSpec("Philosophy", 5, "L6-ART-A5-PHIL"),
                        new SubjectSpec("ICT", 5, "L6-ART-A5-ICT")
                ));

        createArtsSpecialty(subjects, department, upperSixth, "A5", "Literature, History, Philosophy",
                Arrays.asList(
                        new SubjectSpec("Literature", 5, "U6-ART-A5-LIT"),
                        new SubjectSpec("History", 5, "U6-ART-A5-HIST"),
                        new SubjectSpec("Philosophy", 5, "U6-ART-A5-PHIL"),
                        new SubjectSpec("ICT", 5, "U6-ART-A5-ICT")
                ));
    }

    private void createArtsSpecialty(List<Subject> subjects, Department department, ClassRoom classRoom,
                                     String specialtyCode, String specialtyDesc, List<SubjectSpec> subjectSpecs) {
        for (SubjectSpec spec : subjectSpecs) {
            subjects.add(
                    Subject.builder()
                            .name(spec.name + " (" + specialtyCode + ")")
                            .coefficient(spec.coefficient)
                            .department(department)
                            .classRoom(classRoom)
                            .subjectCode(spec.code)
                            .specialty(specialtyCode)
                            .description(spec.name + " for " + specialtyCode + " specialty - " +
                                    classRoom.getName() + " - " + specialtyDesc) // Now using specialtyDesc
                            .build()
            );
        }
    }

    private void createCommercialSubjects(List<Subject> subjects, Department department,
                                          Map<ClassLevel, ClassRoom> classRoomMap) {
        log.info("Creating Commercial department subjects...");

        // Forms 1-2 Commercial Subjects (no specialties)
        createFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_1, "F1-COM", Arrays.asList(
                // Core / Compulsory Subjects
                new SubjectSpec("O-Mathematics", 5, "F1-COM-MATH"),
                new SubjectSpec("O-English Language", 5, "F1-COM-ENG"),
                new SubjectSpec("O-French Language", 5, "F1-COM-FREN"),
                new SubjectSpec("O-Physics", 4, "F1-COM-PHY"),
                new SubjectSpec("O-Chemistry", 4, "F1-COM-CHEM"),
                new SubjectSpec("O-Biology", 3, "F1-COM-BIO"),
                new SubjectSpec("O-Geography", 3, "F1-COM-GEO"),
                new SubjectSpec("O-History", 3, "F1-COM-HIS"),
                new SubjectSpec("O-Citizenship Education", 2, "F1-COM-CIT"),
                new SubjectSpec("O-Physical Education", 2, "F1-COM-PE"),

                // Trade / Commercial Subjects
                new SubjectSpec("Accounting", 3, "F1-COM-ACC"),
                new SubjectSpec("Commerce", 3, "F1-COM-COM"),
                new SubjectSpec("Office and Administrative Management", 3, "F1-COM-OAM"),
                new SubjectSpec("Information Technology", 2, "F1-COM-IT"),
                new SubjectSpec("Computer Science", 2, "F1-COM-COMP")
        ));

        createFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_2, "F2-COM", Arrays.asList(
                // Core / Compulsory Subjects
                new SubjectSpec("O-Mathematics", 5, "F2-COM-MATH"),
                new SubjectSpec("O-English Language", 5, "F2-COM-ENG"),
                new SubjectSpec("O-French Language", 5, "F2-COM-FREN"),
                new SubjectSpec("O-Physics", 4, "F2-COM-PHY"),
                new SubjectSpec("O-Chemistry", 4, "F2-COM-CHEM"),
                new SubjectSpec("O-Biology", 3, "F2-COM-BIO"),
                new SubjectSpec("O-Geography", 3, "F2-COM-GEO"),
                new SubjectSpec("O-History", 3, "F2-COM-HIS"),
                new SubjectSpec("O-Citizenship Education", 2, "F2-COM-CIT"),
                new SubjectSpec("O-Physical Education", 2, "F2-COM-PE"),
                new SubjectSpec("Computer Science", 2, "F2-COM-COMP"),

                // Trade / Commercial Subjects
                new SubjectSpec("Accounting", 3, "F2-COM-ACC"),
                new SubjectSpec("Commerce", 3, "F2-COM-COM"),
                new SubjectSpec("Office and Administrative Management", 3, "F2-COM-OAM"),
                new SubjectSpec("Information Technology", 2, "F2-COM-IT")
        ));

        // ============================================================
        // FORMS 3-5: CREATE COMPULSORY SUBJECTS WITHOUT SPECIALTY
        // ============================================================
        for (ClassLevel level : Arrays.asList(ClassLevel.FORM_3, ClassLevel.FORM_4, ClassLevel.FORM_5)) {
            if (classRoomMap.containsKey(level)) {
                String formCode = level.name().replace("FORM_", "F");

                // Create compulsory subjects WITHOUT specialty for Forms 3-5
                createFormSubjects(subjects, department, classRoomMap, level, formCode + "-COM", Arrays.asList(
                        // ✅ Core Compulsory Subjects (NO SPECIALTY)
                        new SubjectSpec("Mathematics", 5, formCode + "-COM-MATH"),
                        new SubjectSpec("English", 5, formCode + "-COM-ENG"),
                        new SubjectSpec("French", 5, formCode + "-COM-FREN"),

                        // ✅ Optional Subjects (marked as optional)
                        new SubjectSpec("Economics", 3, formCode + "-COM-ECONS", true),
                        new SubjectSpec("Law and Government", 2, formCode + "-COM-LAW", true),
                        new SubjectSpec("Physical Education", 2, formCode + "-COM-PE", true),
                        new SubjectSpec("Citizenship", 2, formCode + "-COM-CIT", true),
                        new SubjectSpec("Computer Science", 2, formCode + "-COM-COMP", true),
                        new SubjectSpec("ICT", 2, formCode + "-COM-ICT", true)
                ));
            }
        }

        // ============================================================
        // FORM 3 SPECIALTY SUBJECTS (ONLY SPECIALTY-SPECIFIC SUBJECTS)
        // ============================================================
        if (classRoomMap.containsKey(ClassLevel.FORM_3)) {
            // ACCOUNTING SPECIALTY (Form 3) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_3,
                    "F3-COM-ACC", "Accounting", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("OHADA Financial Accounting", 3, "F3-COM-ACC-FA"),
                            new SubjectSpec("OHADA Financial Reporting", 3, "F3-COM-ACC-RE"),
                            new SubjectSpec("International Financial Accounting", 3, "F3-COM-ACC-INT-FA"),
                            new SubjectSpec("Business Mathematics", 2, "F3-COM-ACC-BUSMATH"),
                            new SubjectSpec("Commerce", 2, "F3-COM-ACC-COM"),
                            new SubjectSpec("Professional Communication Technique", 3, "F3-COM-ACC-PCT"),
                            new SubjectSpec("Entrepreneurship", 2, "F3-COM-ACC-ENT")
                    ));

            // ACT SPECIALTY (Form 3) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_3,
                    "F3-COM-ACT", "Administration & Communication Techniques", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("Office and Administration Management", 3, "F3-COM-ACT-OAM"),
                            new SubjectSpec("Information Processing", 3, "F3-COM-ACT-IP"),
                            new SubjectSpec("Professional Communication Technique", 3, "F3-COM-ACT-PCT"),
                            new SubjectSpec("Introduction to Accounting", 3, "F3-COM-ACT-ACC-INTRO"),
                            new SubjectSpec("Information Technology", 3, "F3-COM-ACT-IT"),
                            new SubjectSpec("Graphic Designing", 2, "F3-COM-ACT-GRAPHIC"),
                            new SubjectSpec("Business Mathematics", 2, "F3-COM-ACT-BUSMATH")
                    ));

            // MARKETING SPECIALTY (Form 3) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_3,
                    "F3-COM-MKT", "Marketing", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("Professional Marketing Practice", 4, "F3-COM-MKT-PMP"),
                            new SubjectSpec("Marketing Skills", 4, "F3-COM-MKT-MSK"),
                            new SubjectSpec("Digital Marketing Practice", 4, "F3-COM-MKT-DMP"),
                            new SubjectSpec("Business Mathematics", 3, "F3-COM-MKT-BUSMATH"),
                            new SubjectSpec("Entrepreneurship", 3, "F3-COM-MKT-ENT"),
                            new SubjectSpec("Commerce & Finance", 3, "F3-COM-MKT-CFN")
                    ));
        }

        // ============================================================
        // FORM 4 SPECIALTY SUBJECTS (ONLY SPECIALTY-SPECIFIC SUBJECTS)
        // ============================================================
        if (classRoomMap.containsKey(ClassLevel.FORM_4)) {
            // ACCOUNTING SPECIALTY (Form 4) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_4,
                    "F4-COM-ACC", "Accounting", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("OHADA Financial Accounting", 3, "F4-COM-ACC-FA"),
                            new SubjectSpec("OHADA Financial Reporting", 3, "F4-COM-ACC-RE"),
                            new SubjectSpec("International Financial Accounting", 3, "F4-COM-ACC-INT-FA"),
                            new SubjectSpec("Business Mathematics", 2, "F4-COM-ACC-BUSMATH"),
                            new SubjectSpec("Commerce", 2, "F4-COM-ACC-COM"),
                            new SubjectSpec("Professional Communication Technique", 3, "F4-COM-ACC-PCT"),
                            new SubjectSpec("Entrepreneurship", 2, "F4-COM-ACC-ENT")
                    ));

            // ACT SPECIALTY (Form 4) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_4,
                    "F4-COM-ACT", "Administration & Communication Techniques", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("Office and Administration Management", 3, "F4-COM-ACT-OAM"),
                            new SubjectSpec("Information Processing", 3, "F4-COM-ACT-IP"),
                            new SubjectSpec("Professional Communication Technique", 3, "F4-COM-ACT-PCT"),
                            new SubjectSpec("Financial Accounting for Administrators", 3, "F4-COM-ACT-FA"),
                            new SubjectSpec("Information Technology", 3, "F4-COM-ACT-IT"),
                            new SubjectSpec("Graphic Designing", 2, "F4-COM-ACT-GRAPHIC"),
                            new SubjectSpec("Business Mathematics", 2, "F4-COM-ACT-BUSMATH")
                    ));

            // MARKETING SPECIALTY (Form 4) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_4,
                    "F4-COM-MKT", "Marketing", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("Professional Marketing Practice", 4, "F4-COM-MKT-PMP"),
                            new SubjectSpec("Marketing Skills", 4, "F4-COM-MKT-MSK"),
                            new SubjectSpec("Digital Marketing Practice", 4, "F4-COM-MKT-DMP"),
                            new SubjectSpec("Business Mathematics", 3, "F4-COM-MKT-BUSMATH"),
                            new SubjectSpec("Entrepreneurship", 3, "F4-COM-MKT-ENT"),
                            new SubjectSpec("Commerce & Finance", 3, "F4-COM-MKT-CFN")
                    ));
        }

        // ============================================================
        // FORM 5 SPECIALTY SUBJECTS (ONLY SPECIALTY-SPECIFIC SUBJECTS)
        // ============================================================
        if (classRoomMap.containsKey(ClassLevel.FORM_5)) {
            // ACCOUNTING SPECIALTY (Form 5) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_5,
                    "F5-COM-ACC", "Accounting", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("OHADA Financial Accounting", 3, "F5-COM-ACC-FA"),
                            new SubjectSpec("OHADA Financial Reporting", 3, "F5-COM-ACC-RE"),
                            new SubjectSpec("International Financial Accounting", 3, "F5-COM-ACC-INT-FA"),
                            new SubjectSpec("Business Mathematics", 2, "F5-COM-ACC-BUSMATH"),
                            new SubjectSpec("Commerce", 2, "F5-COM-ACC-COM"),
                            new SubjectSpec("Professional Communication Technique", 3, "F5-COM-ACC-PCT"),
                            new SubjectSpec("Entrepreneurship", 2, "F5-COM-ACC-ENT")
                    ));

            // ACT SPECIALTY (Form 5) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_5,
                    "F5-COM-ACT", "Administration & Communication Techniques", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("Advanced Office and Administration Management", 3, "F5-COM-ACT-OAM"),
                            new SubjectSpec("Advanced Information Processing", 3, "F5-COM-ACT-IP"),
                            new SubjectSpec("Professional Communication Technique", 3, "F5-COM-ACT-PCT"),
                            new SubjectSpec("Financial Management for Administrators", 3, "F5-COM-ACT-FM"),
                            new SubjectSpec("Advanced Information Technology", 3, "F5-COM-ACT-IT"),
                            new SubjectSpec("Advanced Graphic Designing", 2, "F5-COM-ACT-GRAPHIC"),
                            new SubjectSpec("Advanced Business Mathematics", 2, "F5-COM-ACT-ADV-BUSMATH")
                    ));

            // MARKETING SPECIALTY (Form 5) - ONLY specialty subjects
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.FORM_5,
                    "F5-COM-MKT", "Marketing", Arrays.asList(
                            // ✅ ONLY Specialty Subjects
                            new SubjectSpec("Advanced Professional Marketing Practice", 4, "F5-COM-MKT-ADV-PMP"),
                            new SubjectSpec("Advanced Marketing Skills", 4, "F5-COM-MKT-ADV-MSK"),
                            new SubjectSpec("Advanced Digital Marketing Practice", 4, "F5-COM-MKT-ADV-DMP"),
                            new SubjectSpec("Advanced Business Mathematics", 3, "F5-COM-MKT-ADV-BUSMATH"),
                            new SubjectSpec("Advanced Entrepreneurship", 3, "F5-COM-MKT-ADV-ENT"),
                            new SubjectSpec("Advanced Commerce & Finance", 3, "F5-COM-MKT-ADV-CFN")
                    ));
        }

        // ============================================================
        // SIXTH FORM SUBJECTS (Keep as is - Sixth Form works differently)
        // ============================================================
        if (classRoomMap.containsKey(ClassLevel.LOWER_SIXTH)) {
            // Lower Sixth Accounting
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.LOWER_SIXTH,
                    "L6-COM-ACC", "Accounting", Arrays.asList(
                            new SubjectSpec("Cost and Management Accounting", 5, "L6-COM-ACC-CMA"),
                            new SubjectSpec("Financial Accounting", 5, "L6-COM-ACC-FA"),
                            new SubjectSpec("Corporate Accounting", 5, "L6-COM-ACC-CA"),
                            new SubjectSpec("Business Mathematics", 5, "L6-COM-ACC-BUSMATH"),
                            new SubjectSpec("Entrepreneurship", 5, "L6-COM-ACC-ENT"),
                            new SubjectSpec("Economics", 5, "L6-COM-ACC-ECONS"),
                            new SubjectSpec("Commerce and Finance", 5, "L6-COM-ACC-FIN"),
                            new SubjectSpec("Business Management", 5, "L6-COM-ACC-MGMT"),
                            new SubjectSpec("Food Science", 5, "L6-COM-ACC-FS")
                    ));

            // Lower Sixth ACT
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.LOWER_SIXTH,
                    "L6-COM-ACT", "Administration & Communication Techniques", Arrays.asList(
                            new SubjectSpec("Automated Clerical Management", 5, "L6-COM-ACT-ACM"),
                            new SubjectSpec("Professional English", 5, "L6-COM-ACT-PENG"),
                            new SubjectSpec("Applied Office Work", 5, "L6-COM-ACT-APPWORK"),
                            new SubjectSpec("Graphic Designing", 5, "L6-COM-ACT-GRAPHIC"),
                            new SubjectSpec("Office Technology", 5, "L6-COM-ACT-OFFTECH"),
                            new SubjectSpec("Information Processing", 5, "L6-COM-ACT-INFO"),
                            new SubjectSpec("Professional Communication Technique", 5, "L6-COM-ACT-PCT"),
                            new SubjectSpec("Business Finance for Administrators", 5, "L6-COM-ACT-FIN"),
                            new SubjectSpec("Food Science", 5, "L6-COM-ACT-FS")
                    ));

            // Lower Sixth Marketing
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.LOWER_SIXTH,
                    "L6-COM-MKT", "Marketing", Arrays.asList(
                            new SubjectSpec("Professional Marketing Practice", 5, "L6-COM-MKT-PMP"),
                            new SubjectSpec("Marketing Skills", 5, "L6-COM-MKT-MSK"),
                            new SubjectSpec("Digital Marketing Practice", 5, "L6-COM-MKT-DMP"),
                            new SubjectSpec("Business Mathematics", 5, "L6-COM-MKT-BMA"),
                            new SubjectSpec("Commerce & Finance", 5, "L6-COM-MKT-CFN"),
                            new SubjectSpec("Entrepreneurship", 5, "L6-COM-MKT-ENT"),
                            new SubjectSpec("Economics", 5, "L6-COM-MKT-ECO"),
                            new SubjectSpec("Law", 5, "L6-COM-MKT-LAW"),
                            new SubjectSpec("ICT", 5, "L6-COM-MKT-ICT"),
                            new SubjectSpec("Religious Studies", 5, "L6-COM-MKT-REL")
                    ));
        }

        if (classRoomMap.containsKey(ClassLevel.UPPER_SIXTH)) {
            // Upper Sixth Accounting
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.UPPER_SIXTH,
                    "U6-COM-ACC", "Accounting", Arrays.asList(
                            new SubjectSpec("Advanced Cost and Management Accounting", 5, "U6-COM-ACC-CMA"),
                            new SubjectSpec("Advanced Financial Accounting", 5, "U6-COM-ACC-FA"),
                            new SubjectSpec("Advanced Corporate Accounting", 5, "U6-COM-ACC-CA"),
                            new SubjectSpec("Advanced Business Mathematics", 5, "U6-COM-ACC-ADV-BUSMATH"),
                            new SubjectSpec("Advanced Entrepreneurship", 5, "U6-COM-ACC-ENT"),
                            new SubjectSpec("Advanced Economics", 5, "U6-COM-ACC-ECONS"),
                            new SubjectSpec("International Finance", 5, "U6-COM-ACC-FIN"),
                            new SubjectSpec("Strategic Business Management", 5, "U6-COM-ACC-MGMT"),
                            new SubjectSpec("Food Science", 5, "U6-COM-ACC-FS")
                    ));

            // Upper Sixth ACT
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.UPPER_SIXTH,
                    "U6-COM-ACT", "Administration & Communication Techniques", Arrays.asList(
                            new SubjectSpec("Advanced Automated Clerical Management", 5, "U6-COM-ACT-ACM"),
                            new SubjectSpec("Advanced Professional English", 5, "U6-COM-ACT-PENG"),
                            new SubjectSpec("Advanced Applied Office Work", 5, "U6-COM-ACT-APPWORK"),
                            new SubjectSpec("Advanced Graphic Designing", 5, "U6-COM-ACT-GRAPHIC"),
                            new SubjectSpec("Advanced Office Technology", 5, "U6-COM-ACT-OFFTECH"),
                            new SubjectSpec("Advanced Information Processing", 5, "U6-COM-ACT-INFO"),
                            new SubjectSpec("Advanced Professional Communication Technique", 5, "U6-COM-ACT-PCT"),
                            new SubjectSpec("Corporate Finance Management", 5, "U6-COM-ACT-FIN"),
                            new SubjectSpec("Food Science", 5, "U6-COM-ACT-FS")
                    ));

            // Upper Sixth Marketing
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.UPPER_SIXTH,
                    "U6-COM-MKT", "Marketing", Arrays.asList(
                            new SubjectSpec("Advanced Professional Marketing Practice", 5, "U6-COM-MKT-ADV-PMP"),
                            new SubjectSpec("Advanced Marketing Skills", 5, "U6-COM-MKT-ADV-MSK"),
                            new SubjectSpec("Advanced Digital Marketing Practice", 5, "U6-COM-MKT-ADV-DMP"),
                            new SubjectSpec("Advanced Business Mathematics", 5, "U6-COM-MKT-ADV-BMA"),
                            new SubjectSpec("Advanced Commerce & Finance", 5, "U6-COM-MKT-ADV-CFN"),
                            new SubjectSpec("Advanced Entrepreneurship", 5, "U6-COM-MKT-ADV-ENT"),
                            new SubjectSpec("Advanced Economics", 5, "U6-COM-MKT-ADV-ECO"),
                            new SubjectSpec("Advanced Law", 5, "U6-COM-MKT-ADV-LAW"),
                            new SubjectSpec("Advanced ICT", 5, "U6-COM-MKT-ADV-ICT"),
                            new SubjectSpec("Advanced Religious Studies", 5, "U6-COM-MKT-ADV-REL")
                    ));
        }
    }

    private void createBuildingConstructionSubjects(List<Subject> subjects, Department department,
                                                    Map<ClassLevel, ClassRoom> classRoomMap) {

        for (ClassLevel level : Arrays.asList(
                ClassLevel.FORM_1, ClassLevel.FORM_2, ClassLevel.FORM_3,
                ClassLevel.FORM_4, ClassLevel.FORM_5)) {

            if (classRoomMap.containsKey(level)) {
                String formCode = level.name().replace("FORM_", "F");

                createFormSubjects(subjects, department, classRoomMap, level, formCode + "-BC", Arrays.asList(
                        // ✅ COMPULSORY CORE SUBJECTS
                        new SubjectSpec("Mathematics", 5, formCode + "-BC-MATH"),
                        new SubjectSpec("English", 5, formCode + "-BC-ENG"),
                        new SubjectSpec("French", 5, formCode + "-BC-FREN"),
                        new SubjectSpec("Physics", 5, formCode + "-BC-PHY"),
                        new SubjectSpec("Chemistry", 5, formCode + "-BC-CHEM"),

                        // ✅ TRADE SUBJECTS (COMPULSORY)
                        new SubjectSpec("Technical Drawing", 4, formCode + "-BC-TD"),
                        new SubjectSpec("Practicals", 5, formCode + "-BC-PRAC"),
                        new SubjectSpec("Applied Mechanics", 3, formCode + "-BC-AM"),
                        new SubjectSpec("Construction Processes", 3, formCode + "-BC-CP"),

                        // ✅ OPTIONAL SUBJECTS (Students choose some)
                        new SubjectSpec("Quantities and Estimate", 3, formCode + "-BC-QE", true),
                        new SubjectSpec("Soils / Surveying", 3, formCode + "-BC-SURV", true),
                        new SubjectSpec("Project Management", 3, formCode + "-BC-PM", true),
                        new SubjectSpec("Trade and Training", 3, formCode + "-BC-TT", true),
                        new SubjectSpec("Citizenship", 2, formCode + "-BC-CIT", true),
                        new SubjectSpec("Law and Government", 2, formCode + "-BC-LAW", true),
                        new SubjectSpec("History / Geography", 3, formCode + "-BC-HISTGEO", true),
                        new SubjectSpec("Computer Science", 3, formCode + "-BC-COMPSCI", true),
                        new SubjectSpec("Engineering Science", 3, formCode + "-BC-ENGSCI", true)
                ));
            }
        }

        // LOWER SIXTH BC (Specialty subjects)
        if (classRoomMap.containsKey(ClassLevel.LOWER_SIXTH)) {
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.LOWER_SIXTH,
                    "L6-BC", "Building Construction", Arrays.asList(
                            // ✅ SIXTH FORM SPECIALTY SUBJECTS
                            new SubjectSpec("Technical Drawing", 4, "L6-BC-TD"),
                            new SubjectSpec("Practicals", 5, "L6-BC-PRAC"),
                            new SubjectSpec("Applied Mechanics", 3, "L6-BC-AM"),
                            new SubjectSpec("Construction Processes", 3, "L6-BC-CP"),
                            new SubjectSpec("Mathematics With Mechanics", 5, "L6-BC-MATH-MECH"),
                            new SubjectSpec("Industrial Computing", 5, "L6-BC-INT-COMP"),

                            // ✅ OPTIONAL SUBJECTS
                            new SubjectSpec("Quantities and Estimate", 3, "L6-BC-QE", true),
                            new SubjectSpec("Soils / Surveying", 3, "L6-BC-SURV", true),
                            new SubjectSpec("Project Management", 3, "L6-BC-PM", true),
                            new SubjectSpec("Trade and Training", 3, "L6-BC-TT", true)
                    ));
        }

        // UPPER SIXTH BC (Specialty subjects)
        if (classRoomMap.containsKey(ClassLevel.UPPER_SIXTH)) {
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.UPPER_SIXTH,
                    "U6-BC", "Building Construction", Arrays.asList(
                            // ✅ ADVANCED SIXTH FORM SPECIALTY SUBJECTS
                            new SubjectSpec("Technical Drawing", 4, "U6-BC-TD"),
                            new SubjectSpec("Practicals", 5, "U6-BC-PRAC"),
                            new SubjectSpec("Applied Mechanics", 3, "U6-BC-AM"),
                            new SubjectSpec("Construction Processes", 3, "U6-BC-CP"),
                            new SubjectSpec("Mathematics With Mechanics", 5, "U6-BC-MATH-MECH"),
                            new SubjectSpec("Industrial Computing", 5, "U6-BC-INT-COMP"),

                            // ✅ ADVANCED OPTIONAL SUBJECTS
                            new SubjectSpec("Advanced Quantities and Estimate", 3, "U6-BC-ADV-QE", true),
                            new SubjectSpec("Advanced Soils / Surveying", 3, "U6-BC-ADV-SURV", true),
                            new SubjectSpec("Advanced Project Management", 3, "U6-BC-ADV-PM", true),
                            new SubjectSpec("Advanced Trade and Training", 3, "U6-BC-ADV-TT", true)
                    ));
        }
    }

    private void createHomeEconomicsSubjects(
            List<Subject> subjects,
            Department department,
            Map<ClassLevel, ClassRoom> classRoomMap) {

        for (ClassLevel level : Arrays.asList(
                ClassLevel.FORM_1,
                ClassLevel.FORM_2,
                ClassLevel.FORM_3,
                ClassLevel.FORM_4,
                ClassLevel.FORM_5)) {

            if (!classRoomMap.containsKey(level)) {
                continue;
            }

            String levelCode = level.name().replace("FORM_", "F");

            createFormSubjects(
                    subjects,
                    department,
                    classRoomMap,
                    level,
                    levelCode + "-HE",
                    Arrays.asList(
                            // ✅ COMPULSORY CORE SUBJECTS
                            new SubjectSpec("French", 4, levelCode + "-HE-FREN"),
                            new SubjectSpec("English", 4, levelCode + "-HE-ENG"),
                            new SubjectSpec("Mathematics", 4, levelCode + "-HE-MATH"),

                            // ✅ PROFESSIONAL SUBJECTS (COMPULSORY)
                            new SubjectSpec("Food Nutrition and Health (FNH)", 4, levelCode + "-HE-FNH"),
                            new SubjectSpec("Resource Management on Home Studies (RMHS)", 4, levelCode + "-HE-RMHS"),
                            new SubjectSpec("Family Life Education and Gerontology (FLEG)", 4, levelCode + "-HE-FLEG"),

                            // ✅ PRACTICALS (COMPULSORY)
                            new SubjectSpec("Practicals on Food Nutrition and Health", 2, levelCode + "-HE-FNH-PRAC"),
                            new SubjectSpec("Practicals on RMHS", 2, levelCode + "-HE-RMHS-PRAC"),

                            // ✅ OPTIONAL RELATED PROFESSIONAL SUBJECTS
                            new SubjectSpec("Natural Science", 2, levelCode + "-HE-NSCI", true),
                            new SubjectSpec("Business Mathematics", 2, levelCode + "-HE-BUSMATH", true),
                            new SubjectSpec("Entrepreneurship", 2, levelCode + "-HE-ENT", true),
                            new SubjectSpec("Economic Geography", 2, levelCode + "-HE-ECOGEO", true),
                            new SubjectSpec("Law and Government", 2, levelCode + "-HE-LAW", true),
                            new SubjectSpec("Citizenship", 2, levelCode + "-HE-CIT", true),
                            new SubjectSpec("Management Aided in Computer", 2, levelCode + "-HE-MGT-COMP", true),
                            new SubjectSpec("Computer Science", 2, levelCode + "-HE-COMP", true),
                            new SubjectSpec("ICT", 2, levelCode + "-HE-ICT", true)
                    )
            );
        }
    }

    private void createHomeEconomicsSixthFormSubjects(
            List<Subject> subjects,
            Department department,
            Map<ClassLevel, ClassRoom> classRoomMap) {

        for (ClassLevel level : Arrays.asList(
                ClassLevel.LOWER_SIXTH,
                ClassLevel.UPPER_SIXTH)) {

            if (!classRoomMap.containsKey(level)) {
                continue;
            }

            String levelCode = level == ClassLevel.LOWER_SIXTH ? "L6" : "U6";

            createFormSubjects(
                    subjects,
                    department,
                    classRoomMap,
                    level,
                    levelCode + "-HE",
                    Arrays.asList(
                            // ✅ COMPULSORY PROFESSIONAL SUBJECTS
                            new SubjectSpec("Catering Management and Dietetics", 5, levelCode + "-HE-CMD"),
                            new SubjectSpec("Family Life Education and Gerontology (Theory)", 5, levelCode + "-HE-FLEG"),
                            new SubjectSpec("Resource Management on Home Studies (RMHS)", 5, levelCode + "-HE-RMHS"),
                            new SubjectSpec("Professional English", 4, levelCode + "-HE-PENG"),

                            // ✅ PRACTICALS (COMPULSORY)
                            new SubjectSpec("Culinary Practicals on Catering Management and Dietetics", 3, levelCode + "-HE-CMD-PRAC"),
                            new SubjectSpec("Family Life Education and Gerontology (Practicals)", 3, levelCode + "-HE-FLEG-PRAC"),
                            new SubjectSpec("Practicals on RMHS", 3, levelCode + "-HE-RMHS-PRAC"),

                            // ✅ OPTIONAL RELATED PROFESSIONAL SUBJECTS
                            new SubjectSpec("Social Life", 2, levelCode + "-HE-SLIFE", true),
                            new SubjectSpec("Entrepreneurship", 2, levelCode + "-HE-ENT", true),
                            new SubjectSpec("Natural Science", 3, levelCode + "-HE-NSCI", true),
                            new SubjectSpec("Economics", 3, levelCode + "-HE-ECON", true),
                            new SubjectSpec("Management Aided in Computer", 1, levelCode + "-HE-MGT-COMP", true),
                            new SubjectSpec("Advanced Computer Science", 2, levelCode + "-HE-ADV-COMP", true),
                            new SubjectSpec("Advanced ICT", 2, levelCode + "-HE-ADV-ICT", true)
                    )
            );
        }
    }

    private void createEPSSubjects(List<Subject> subjects, Department department,
                                   Map<ClassLevel, ClassRoom> classRoomMap) {

        // Forms 1 Building Construction Subjects
        for (ClassLevel level : Arrays.asList(ClassLevel.FORM_1, ClassLevel.FORM_2, ClassLevel.FORM_3,
                ClassLevel.FORM_4, ClassLevel.FORM_5)) {
            if (classRoomMap.containsKey(level)) {
                String formCode = level.name().replace("FORM_", "F");

                createFormSubjects(subjects, department, classRoomMap, level, formCode + "-EPS", Arrays.asList(
                        // ✅ COMPULSORY CORE SUBJECTS
                        new SubjectSpec("Mathematics", 5, formCode + "-EPS-MATH"),
                        new SubjectSpec("English", 5, formCode + "-EPS-ENG"),
                        new SubjectSpec("French", 5, formCode + "-EPS-FREN"),

                        // ✅ COMPULSORY EPS SUBJECTS
                        new SubjectSpec("EPS Practical", 6, formCode + "-EPS-PRAC"),
                        new SubjectSpec("Engineering Drawing", 4, formCode + "-EPS-ENG-DRAW"),
                        new SubjectSpec("Electrical Technology", 2, formCode + "-EPS-ELEC-TECH"),
                        new SubjectSpec("Electrical Diagram", 2, formCode + "-EPS-ELEC-DIA"),

                        // ✅ OPTIONAL SUBJECTS
                        new SubjectSpec("Citizenship", 2, formCode + "-EPS-CIT", true),
                        new SubjectSpec("Human And Economic Geography", 2, formCode + "-EPS-GEO", true),
                        new SubjectSpec("Engineering Science", 2, formCode + "-EPS-ENG-SCI", true),
                        new SubjectSpec("Computer Science", 3, formCode + "-EPS-COMP", true),
                        new SubjectSpec("Entrepreneurship", 2, formCode + "-EPS-ENT", true),
                        new SubjectSpec("Electrical Circuit", 2, formCode + "-EPS-ELEC-CIR", true),
                        new SubjectSpec("Electrical Machine", 2, formCode + "-EPS-ELEC-MACH", true),
                        new SubjectSpec("Test and Measurement", 2, formCode + "-EPS-TEST-MEA", true),
                        new SubjectSpec("Law And Government", 2, formCode + "-EPS-LAW-GOV", true),
                        new SubjectSpec("Industrial Computing", 3, formCode + "-EPS-INT-COMP", true)
                ));
            }
        }

        // LOWER SIXTH EPS (Specialty subjects)
        if (classRoomMap.containsKey(ClassLevel.LOWER_SIXTH)) {
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.LOWER_SIXTH,
                    "L6-EPS", "Electrical Power System", Arrays.asList(
                            // ✅ SIXTH FORM SPECIALTY SUBJECTS
                            new SubjectSpec("Electrical Technology", 5, "L6-EPS-ELEC-TECH"),
                            new SubjectSpec("Electrical Diagram", 5, "L6-EPS-ELEC-DIA"),
                            new SubjectSpec("Engineering Drawing", 5, "L6-EPS-ENG-DRAW"),
                            new SubjectSpec("EPS Practical", 6, "L6-EPS-PRAC"),
                            new SubjectSpec("Electrical Circuit", 5, "L6-EPS-ELEC-CIR"),
                            new SubjectSpec("Electrical Machine", 5, "L6-EPS-ELEC-MACH"),
                            new SubjectSpec("Professional English", 4, "L6-EPS-PENG"),
                            new SubjectSpec("Professional Mathematics", 4, "L6-EPS-PMATH"),

                            // ✅ OPTIONAL SUBJECTS
                            new SubjectSpec("Test and Measurement", 5, "L6-EPS-TEST-MEA", true),
                            new SubjectSpec("Power Systems", 5, "L6-EPS-POWER-SYS", true),
                            new SubjectSpec("Control Systems", 5, "L6-EPS-CONTROL-SYS", true),
                            new SubjectSpec("Electronics", 5, "L6-EPS-ELECTRONICS", true),
                            new SubjectSpec("Engineering Science", 4, "L6-EPS-ENG-SCI", true),
                            new SubjectSpec("Entrepreneurship", 3, "L6-EPS-ENT", true),
                            new SubjectSpec("Computer Applications", 3, "L6-EPS-COMP-APP", true)
                    ));
        }

        // UPPER SIXTH EPS (Specialty subjects)
        if (classRoomMap.containsKey(ClassLevel.UPPER_SIXTH)) {
            createSpecialtyFormSubjects(subjects, department, classRoomMap, ClassLevel.UPPER_SIXTH,
                    "U6-EPS", "Electrical Power System", Arrays.asList(
                            // ✅ ADVANCED SIXTH FORM SPECIALTY SUBJECTS
                            new SubjectSpec("Advanced Electrical Technology", 5, "U6-EPS-ADV-ELEC-TECH"),
                            new SubjectSpec("Advanced Electrical Diagram", 5, "U6-EPS-ADV-ELEC-DIA"),
                            new SubjectSpec("Advanced Engineering Drawing", 5, "U6-EPS-ADV-ENG-DRAW"),
                            new SubjectSpec("Advanced EPS Practical", 6, "U6-EPS-ADV-PRAC"),
                            new SubjectSpec("Advanced Electrical Circuit", 5, "U6-EPS-ADV-ELEC-CIR"),
                            new SubjectSpec("Advanced Electrical Machine", 5, "U6-EPS-ADV-ELEC-MACH"),
                            new SubjectSpec("Professional English", 4, "U6-EPS-PENG"),
                            new SubjectSpec("Advanced Professional Mathematics", 4, "U6-EPS-ADV-PMATH"),

                            // ✅ ADVANCED OPTIONAL SUBJECTS
                            new SubjectSpec("Advanced Test and Measurement", 5, "U6-EPS-ADV-TEST-MEA", true),
                            new SubjectSpec("Advanced Power Systems", 5, "U6-EPS-ADV-POWER-SYS", true),
                            new SubjectSpec("Advanced Control Systems", 5, "U6-EPS-ADV-CONTROL-SYS", true),
                            new SubjectSpec("Advanced Electronics", 5, "U6-EPS-ADV-ELECTRONICS", true),
                            new SubjectSpec("Power Electronics", 5, "U6-EPS-POWER-ELECTRONICS", true),
                            new SubjectSpec("Renewable Energy Systems", 5, "U6-EPS-RENEW-ENERGY", true),
                            new SubjectSpec("Advanced Engineering Science", 4, "U6-EPS-ADV-ENG-SCI", true),
                            new SubjectSpec("Advanced Entrepreneurship", 3, "U6-EPS-ADV-ENT", true),
                            new SubjectSpec("Computer Applications", 3, "U6-EPS-COMP-APP", true)
                    ));
        }
    }

    private void createCISubjects(
            List<Subject> subjects,
            Department department,
            Map<ClassLevel, ClassRoom> classRoomMap) {

        for (ClassLevel level : Arrays.asList(
                ClassLevel.FORM_1,
                ClassLevel.FORM_2,
                ClassLevel.FORM_3,
                ClassLevel.FORM_4,
                ClassLevel.FORM_5)) {

            if (!classRoomMap.containsKey(level)) {
                continue;
            }

            String levelCode = level.name().replace("FORM_", "F");

            createFormSubjects(
                    subjects,
                    department,
                    classRoomMap,
                    level,
                    levelCode + "-CI",
                    Arrays.asList(
                            // ✅ COMPULSORY CORE SUBJECTS
                            new SubjectSpec("English Language", 3, levelCode + "-CI-ENG"),
                            new SubjectSpec("Mathematics", 3, levelCode + "-CI-MATH"),
                            new SubjectSpec("French Language", 3, levelCode + "-CI-FREN"),

                            // ✅ COMPULSORY PROFESSIONAL SUBJECTS
                            new SubjectSpec("Sewing", 4, levelCode + "-CI-SEW"),
                            new SubjectSpec("Pattern Drafting", 4, levelCode + "-CI-PATTERN"),
                            new SubjectSpec("Textile Technology", 3, levelCode + "-CI-TEXT"),

                            // ✅ OPTIONAL SUBJECTS
                            new SubjectSpec("Citizenship", 3, levelCode + "-CI-CIT", true),
                            new SubjectSpec("Physical Science", 4, levelCode + "-CI-PSCI", true),
                            new SubjectSpec("Legislation", 4, levelCode + "-CI-LAW", true),
                            new SubjectSpec("Work Organization", 2, levelCode + "-CI-WORKORG", true),
                            new SubjectSpec("Fashion Drawing", 3, levelCode + "-CI-FDRAW", true),
                            new SubjectSpec("Computer Science", 4, levelCode + "-CI-CS", true),
                            new SubjectSpec("Information and Communication Technology", 4, levelCode + "-CI-ICT", true)
                    )
            );
        }
    }

    private void createFormSubjects(List<Subject> subjects, Department department,
                                    Map<ClassLevel, ClassRoom> classRoomMap, ClassLevel level,
                                    String prefix, List<SubjectSpec> specs) {
        if (!classRoomMap.containsKey(level)) return;

        ClassRoom classRoom = classRoomMap.get(level);
        for (SubjectSpec spec : specs) {
            subjects.add(
                    Subject.builder()
                            .name(spec.name)
                            .coefficient(spec.coefficient)
                            .department(department)
                            .classRoom(classRoom)
                            .subjectCode(spec.code)
                            .optional(spec.optional)
                            .description(prefix + " - " + spec.name + " - " + classRoom.getName())
                            .build()
            );
        }
    }

    private void createSpecialtyFormSubjects(List<Subject> subjects, Department department,
                                             Map<ClassLevel, ClassRoom> classRoomMap, ClassLevel level,
                                             String prefix, String specialty, List<SubjectSpec> specs) {
        if (!classRoomMap.containsKey(level)) return;

        ClassRoom classRoom = classRoomMap.get(level);
        for (SubjectSpec spec : specs) {
            subjects.add(
                    Subject.builder()
                            .name(spec.name)
                            .coefficient(spec.coefficient)
                            .department(department)
                            .classRoom(classRoom)
                            .subjectCode(spec.code)
                            .specialty(specialty)
                            .description(prefix + ": " + spec.name + " for " + specialty + " - " + classRoom.getName()) // Using prefix
                            .build()
            );
        }
    }

    // Helper class for subject specifications
    private static class SubjectSpec {
        String name;
        int coefficient;
        String code;
        boolean optional;

        // Constructor for compulsory subjects
        SubjectSpec(String name, int coefficient, String code) {
            this(name, coefficient, code, false);
        }

        // Constructor with optional flag
        SubjectSpec(String name, int coefficient, String code, boolean optional) {
            this.name = name;
            this.coefficient = coefficient;
            this.code = code;
            this.optional = optional;
        }
    }
}