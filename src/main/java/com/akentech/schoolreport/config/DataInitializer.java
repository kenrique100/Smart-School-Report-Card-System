package com.akentech.schoolreport.config;

import com.akentech.schoolreport.exception.EntityNotFoundException;
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
            Teacher teacher = Teacher.builder()
                    .teacherId("TC1000020000")
                    .firstName("Nimal")
                    .lastName("Soyza")
                    .dateOfBirth(LocalDate.of(1990, 6, 19))
                    .gender(Gender.MALE)
                    .email("nimal@school.com")
                    .address("Kandy Road Nittambuwa")
                    .contact("0339988554")
                    .skills("Science,Mathematics,History,Geography")
                    .build();
            teacherRepository.save(teacher);
            log.info("Default teacher created");
        }
    }

    private void initSubjects() {
        if (subjectRepository.count() == 0) {
            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (DepartmentCode code : DepartmentCode.values()) {
                Department dept = departmentRepository.findByCode(code)
                        .orElseThrow(() -> new EntityNotFoundException("Department", code));
                deptMap.put(code, dept);
            }

            List<Subject> subjects = new ArrayList<>();

            // ====================================
            // 1. General Compulsory for Forms 1–5
            // ====================================
            subjects.addAll(Arrays.asList(
                    Subject.builder().name("English Language").coefficient(3).department(deptMap.get(DepartmentCode.GEN)).subjectCode("ENG-COMP").description("Compulsory English").build(),
                    Subject.builder().name("French Language").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("FREN-COMP").description("Compulsory French").build(),
                    Subject.builder().name("Mathematics").coefficient(4).department(deptMap.get(DepartmentCode.GEN)).subjectCode("MATH-COMP").description("Compulsory Math").build(),
                    Subject.builder().name("Civic Education").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("CIVIC-COMP").description("Civic Education").build(),
                    Subject.builder().name("Physical Education").coefficient(1).department(deptMap.get(DepartmentCode.GEN)).subjectCode("PE-COMP").description("Physical Education").build(),
                    Subject.builder().name("Computer Science").coefficient(2).department(deptMap.get(DepartmentCode.GEN)).subjectCode("COMP-SCI").description("Optional Computer Science").build()
            ));

            // =====================
            // 2. Science Department
            // =====================
            String[] sciSeries = {"S1", "S2", "S3", "S4", "S5", "S6"};
            for (String series : sciSeries) {
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Biology").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty(series).subjectCode("BIO-" + series).description("Biology for " + series).build(),
                        Subject.builder().name("Chemistry").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty(series).subjectCode("CHEM-" + series).description("Chemistry for " + series).build(),
                        Subject.builder().name("Physics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty(series).subjectCode("PHY-" + series).description("Physics for " + series).build(),
                        Subject.builder().name("Mathematics").coefficient(4).department(deptMap.get(DepartmentCode.SCI)).specialty(series).subjectCode("MATH-" + series).description("Mathematics for " + series).build()
                ));
            }

            // =====================
            // 3. Arts Department
            // =====================
            String[] artsSeries = {"A1", "A2", "A3", "A4", "A5"};
            for (String series : artsSeries) {
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("History").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty(series).subjectCode("HIST-" + series).description("History for " + series).build(),
                        Subject.builder().name("Geography").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty(series).subjectCode("GEO-" + series).description("Geography for " + series).build(),
                        Subject.builder().name("Literature in English").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty(series).subjectCode("LIT-" + series).description("Literature for " + series).build(),
                        Subject.builder().name("French").coefficient(3).department(deptMap.get(DepartmentCode.ART)).specialty(series).subjectCode("FREN-" + series).description("French for " + series).build()
                ));
            }

            // =====================
            // 4. Commercial Department
            // =====================
            String[] comSeries = {"Accountancy", "Marketing", "Economics"};
            for (String series : comSeries) {
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Commerce").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty(series).subjectCode("COMM-" + series).description("Commerce for " + series).build(),
                        Subject.builder().name("Accounting").coefficient(4).department(deptMap.get(DepartmentCode.COM)).specialty(series).subjectCode("ACC-" + series).description("Accounting for " + series).build(),
                        Subject.builder().name("Business Studies").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty(series).subjectCode("BUS-" + series).description("Business Studies for " + series).build(),
                        Subject.builder().name("Economics").coefficient(3).department(deptMap.get(DepartmentCode.COM)).specialty(series).subjectCode("ECO-" + series).description("Economics for " + series).build()
                ));
            }

            // =====================
            // 5. Technical Department
            // =====================
            String[] tecSeries = {"BC", "EPS", "MECH", "CIVIL"};
            for (String series : tecSeries) {
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Technical Drawing").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).specialty(series).subjectCode("TD-" + series).description("Technical Drawing for " + series).build(),
                        Subject.builder().name("Woodwork").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).specialty(series).subjectCode("WOOD-" + series).description("Woodwork for " + series).build(),
                        Subject.builder().name("Metalwork").coefficient(3).department(deptMap.get(DepartmentCode.TEC)).specialty(series).subjectCode("METAL-" + series).description("Metalwork for " + series).build()
                ));
            }

            // =====================
            // 6. Home Economics
            // =====================
            String[] heSeries = {"HE1", "HE2"};
            for (String series : heSeries) {
                subjects.addAll(Arrays.asList(
                        Subject.builder().name("Home Management").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty(series).subjectCode("HM-" + series).description("Home Management for " + series).build(),
                        Subject.builder().name("Nutrition").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty(series).subjectCode("NUTR-" + series).description("Nutrition for " + series).build(),
                        Subject.builder().name("Textiles").coefficient(3).department(deptMap.get(DepartmentCode.HE)).specialty(series).subjectCode("TEXT-" + series).description("Textiles for " + series).build()
                ));
            }

            // Save all subjects
            subjects.forEach(subjectService::createSubject);
            log.info("Core subjects for Forms 1–5 initialized successfully ({} subjects)", subjects.size());
        }
    }

        private void initStudents() {
        if (studentRepository.count() == 0) {
            Map<DepartmentCode, Department> deptMap = new HashMap<>();
            for (DepartmentCode code : DepartmentCode.values()) {
                Department dept = departmentRepository.findByCode(code)
                        .orElseThrow(() -> new EntityNotFoundException("Department", code));
                deptMap.put(code, dept);
            }

            Map<ClassLevel, ClassRoom> classMap = new HashMap<>();
            for (ClassLevel level : ClassLevel.values()) {
                ClassRoom cls = classRoomRepository.findByCode(level)
                        .orElseThrow(() -> new EntityNotFoundException("ClassRoom", level));
                classMap.put(level, cls);
            }

            List<Student> students = new ArrayList<>();

            // =========================
            // 2 students per specialty/series
            // =========================

            // Forms 1-3 general
            for(int i=1;i<=3;i++){
                ClassRoom cls = classMap.get(ClassLevel.valueOf("FORM_"+i));
                students.add(createStudent("John"+i,"Doe",cls,deptMap.get(DepartmentCode.GEN),null));
                students.add(createStudent("Jane"+i,"Smith",cls,deptMap.get(DepartmentCode.GEN),null));
            }

            // Forms 4-5 with departments
            List<String> f4f5Series = Arrays.asList("Accountancy","Marketing","EPS","BC");
            ClassRoom form4 = classMap.get(ClassLevel.FORM_4);
            ClassRoom form5 = classMap.get(ClassLevel.FORM_5);
            for(String series : f4f5Series){
                students.add(createStudent("StudentF4"+series,"A",form4,series.startsWith("AC")||series.startsWith("MK")?deptMap.get(DepartmentCode.COM):deptMap.get(DepartmentCode.TEC),series));
                students.add(createStudent("StudentF4"+series,"B",form4,series.startsWith("AC")||series.startsWith("MK")?deptMap.get(DepartmentCode.COM):deptMap.get(DepartmentCode.TEC),series));
                students.add(createStudent("StudentF5"+series,"A",form5,series.startsWith("AC")||series.startsWith("MK")?deptMap.get(DepartmentCode.COM):deptMap.get(DepartmentCode.TEC),series));
                students.add(createStudent("StudentF5"+series,"B",form5,series.startsWith("AC")||series.startsWith("MK")?deptMap.get(DepartmentCode.COM):deptMap.get(DepartmentCode.TEC),series));
            }

            // Sixth Form Science & Arts
            ClassRoom lowerSixth = classMap.get(ClassLevel.LOWER_SIXTH);
            ClassRoom upperSixth = classMap.get(ClassLevel.UPPER_SIXTH);

            List<String> sciSeries = Arrays.asList("S1","S2","S3","S4","S5","S6");
            List<String> artsSeries = Arrays.asList("A1","A2","A3","A4","A5");

            sciSeries.forEach(series -> {
                students.add(createStudent("SciLower"+series,"A",lowerSixth,deptMap.get(DepartmentCode.SCI),series));
                students.add(createStudent("SciLower"+series,"B",lowerSixth,deptMap.get(DepartmentCode.SCI),series));
                students.add(createStudent("SciUpper"+series,"A",upperSixth,deptMap.get(DepartmentCode.SCI),series));
                students.add(createStudent("SciUpper"+series,"B",upperSixth,deptMap.get(DepartmentCode.SCI),series));
            });

            artsSeries.forEach(series -> {
                students.add(createStudent("ArtsLower"+series,"A",lowerSixth,deptMap.get(DepartmentCode.ART),series));
                students.add(createStudent("ArtsLower"+series,"B",lowerSixth,deptMap.get(DepartmentCode.ART),series));
                students.add(createStudent("ArtsUpper"+series,"A",upperSixth,deptMap.get(DepartmentCode.ART),series));
                students.add(createStudent("ArtsUpper"+series,"B",upperSixth,deptMap.get(DepartmentCode.ART),series));
            });

            // Save all
            students.forEach(s -> studentService.createStudent(s,null));
            log.info("Students initialized ({} students)", students.size());
        }
    }

    private Student createStudent(String firstName,String lastName,ClassRoom cls,Department dept,String specialty){
        return Student.builder()
                .firstName(firstName)
                .lastName(lastName)
                .classRoom(cls)
                .department(dept)
                .specialty(specialty)
                .gender(ThreadLocalRandom.current().nextBoolean()?Gender.MALE:Gender.FEMALE)
                .dateOfBirth(generateRandomBirthDate())
                .email(firstName.toLowerCase()+"."+lastName.toLowerCase()+"@student.com")
                .address("Sample Address")
                .academicYearStart(2024)
                .academicYearEnd(2025)
                .build();
    }

    private LocalDate generateRandomBirthDate(){
        int year = ThreadLocalRandom.current().nextInt(2000,2008);
        int month = ThreadLocalRandom.current().nextInt(1,13);
        int day = ThreadLocalRandom.current().nextInt(1,29);
        return LocalDate.of(year,month,day);
    }

    private void initNotices(){
        if(noticeRepository.count()==0){
            Teacher teacher = teacherRepository.findAll().get(0);
            Notice notice = Notice.builder()
                    .title("School Reopening")
                    .content("School will reopen on January 15th after the holidays. All students are expected to be present.")
                    .createdDate(LocalDateTime.now())
                    .teacher(teacher)
                    .isActive(true)
                    .build();
            noticeRepository.save(notice);
            log.info("Default notice created");
        }
    }
}
