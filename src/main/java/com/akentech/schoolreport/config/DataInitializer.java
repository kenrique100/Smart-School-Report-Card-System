package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.repository.*;
import com.akentech.schoolreport.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    @Override
    public void run(String... args) throws Exception {
        initDepartments();
        initClassRooms();
        initTeachers();
        initStudents();
        initSubjects();
        initNotices();
    }

    private void initDepartments() {
        if (departmentRepository.count() == 0) {
            List<Department> departments = Arrays.asList(
                    Department.builder().name("General").code("GEN").description("General Studies Department").build(),
                    Department.builder().name("Sciences").code("SCI").description("Science Department").build(),
                    Department.builder().name("Arts").code("ART").description("Arts Department").build(),
                    Department.builder().name("Commercial").code("COM").description("Commercial Department").build(),
                    Department.builder().name("Technical").code("TEC").description("Technical Department").build(),
                    Department.builder().name("Home Economics").code("HE").description("Home Economics Department").build()
            );
            departmentRepository.saveAll(departments);
            log.info("Default departments created including General department");
        }
    }

    private void initClassRooms() {
        if (classRoomRepository.count() == 0) {
            List<ClassRoom> classRooms = Arrays.asList(
                    ClassRoom.builder().name("Form 1").code("F1").academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 2").code("F2").academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 3").code("F3").academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 4").code("F4").academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Form 5").code("F5").academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Lower Sixth").code("LSX").academicYear("2024/2025").build(),
                    ClassRoom.builder().name("Upper Sixth").code("USX").academicYear("2024/2025").build()
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
                    .gender("Male")
                    .email("nimal@school.com")
                    .address("Kandy Road Nittambuwa")
                    .contact("0339988554")
                    .skills("Science,Mathematics,History,Geography")
                    .build();
            teacherRepository.save(teacher);
            log.info("Default teacher created");
        }
    }

    private void initStudents() {
        if (studentRepository.count() == 0) {
            // Get departments
            Department generalDept = departmentRepository.findByCode("GEN")
                    .orElseThrow(() -> new RuntimeException("General department not found"));
            Department commercialDept = departmentRepository.findByCode("COM")
                    .orElseThrow(() -> new RuntimeException("Commercial department not found"));
            Department technicalDept = departmentRepository.findByCode("TEC")
                    .orElseThrow(() -> new RuntimeException("Technical department not found"));
            Department scienceDept = departmentRepository.findByCode("SCI")
                    .orElseThrow(() -> new RuntimeException("Science department not found"));
            Department artsDept = departmentRepository.findByCode("ART")
                    .orElseThrow(() -> new RuntimeException("Arts department not found"));

            // Get classrooms
            ClassRoom form1 = classRoomRepository.findByCode("F1")
                    .orElseThrow(() -> new RuntimeException("Form 1 class not found"));
            ClassRoom form2 = classRoomRepository.findByCode("F2")
                    .orElseThrow(() -> new RuntimeException("Form 2 class not found"));
            ClassRoom form3 = classRoomRepository.findByCode("F3")
                    .orElseThrow(() -> new RuntimeException("Form 3 class not found"));
            ClassRoom form4 = classRoomRepository.findByCode("F4")
                    .orElseThrow(() -> new RuntimeException("Form 4 class not found"));
            ClassRoom form5 = classRoomRepository.findByCode("F5")
                    .orElseThrow(() -> new RuntimeException("Form 5 class not found"));
            ClassRoom lowerSixth = classRoomRepository.findByCode("LSX")
                    .orElseThrow(() -> new RuntimeException("Lower Sixth class not found"));
            ClassRoom upperSixth = classRoomRepository.findByCode("USX")
                    .orElseThrow(() -> new RuntimeException("Upper Sixth class not found"));

            List<Student> students = Arrays.asList(
                    // Form 1-3 (General department, no specialty)
                    createStudent("John", "Doe", form1, generalDept, null, 2024, 2025),
                    createStudent("Jane", "Smith", form1, generalDept, null, 2024, 2025),
                    createStudent("Mike", "Johnson", form2, generalDept, null, 2024, 2025),
                    createStudent("Sarah", "Wilson", form3, generalDept, null, 2024, 2025),

                    // Form 4-5 (Various departments with specialties)
                    createStudent("David", "Brown", form4, commercialDept, "Accountancy", 2024, 2025),
                    createStudent("Lisa", "Davis", form4, technicalDept, "EPS(Electrical Power System)", 2024, 2025),
                    createStudent("Robert", "Miller", form5, commercialDept, "Marketting", 2023, 2024),
                    createStudent("Emily", "Taylor", form5, technicalDept, "BC(Building and Construction)", 2023, 2024),

                    // Sixth Form Science (With S1-S6 specialties)
                    createStudent("James", "Anderson", lowerSixth, scienceDept, "S1", 2024, 2025),
                    createStudent("Maria", "Thomas", lowerSixth, scienceDept, "S2", 2024, 2025),
                    createStudent("Daniel", "Martinez", upperSixth, scienceDept, "S3", 2023, 2024),
                    createStudent("Karen", "Garcia", upperSixth, scienceDept, "S4", 2023, 2024),

                    // Sixth Form Arts (With A1-A5 specialties)
                    createStudent("Paul", "Rodriguez", lowerSixth, artsDept, "A1", 2024, 2025),
                    createStudent("Nancy", "Lee", lowerSixth, artsDept, "A2", 2024, 2025),
                    createStudent("Kevin", "White", upperSixth, artsDept, "A3", 2023, 2024),
                    createStudent("Amy", "Harris", upperSixth, artsDept, "A4", 2023, 2024)
            );

            for (Student student : students) {
                studentService.saveStudent(student);
            }
            log.info("Default students created with new department structure");
        }
    }

    private Student createStudent(String firstName, String lastName, ClassRoom classRoom,
                                  Department department, String specialty, int startYear, int endYear) {
        return Student.builder()
                .firstName(firstName)
                .lastName(lastName)
                .classRoom(classRoom)
                .department(department)
                .specialty(specialty)
                .gender("Male")
                .dateOfBirth(LocalDate.of(2001, 6, 26))
                .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@student.com")
                .address("Sample Address")
                .academicYearStart(startYear)
                .academicYearEnd(endYear)
                .build();
    }

    private void initSubjects() {
        if (subjectRepository.count() == 0) {
            // Get departments
            Department generalDept = departmentRepository.findByCode("GEN")
                    .orElseThrow(() -> new RuntimeException("General department not found"));
            Department scienceDept = departmentRepository.findByCode("SCI")
                    .orElseThrow(() -> new RuntimeException("Science department not found"));
            Department artsDept = departmentRepository.findByCode("ART")
                    .orElseThrow(() -> new RuntimeException("Arts department not found"));
            Department commercialDept = departmentRepository.findByCode("COM")
                    .orElseThrow(() -> new RuntimeException("Commercial department not found"));
            Department technicalDept = departmentRepository.findByCode("TEC")
                    .orElseThrow(() -> new RuntimeException("Technical department not found"));
            Department homeEconDept = departmentRepository.findByCode("HE")
                    .orElseThrow(() -> new RuntimeException("Home Economics department not found"));

            List<Subject> subjects = Arrays.asList(
                    // General Department Subjects (No specialty)
                    Subject.builder().name("Mathematics").coefficient(4).department(generalDept).subjectCode("MATH-GEN").description("General Mathematics").build(),
                    Subject.builder().name("English Language").coefficient(3).department(generalDept).subjectCode("ENG-GEN").description("English Language").build(),
                    Subject.builder().name("Civic Education").coefficient(2).department(generalDept).subjectCode("CIVIC-GEN").description("Civic Education").build(),
                    Subject.builder().name("Physical Education").coefficient(1).department(generalDept).subjectCode("PE-GEN").description("Physical Education").build(),

                    // Science Department - General Science
                    Subject.builder().name("General Science").coefficient(4).department(scienceDept).subjectCode("SCI-GEN").description("General Science").build(),
                    Subject.builder().name("Biology").coefficient(4).department(scienceDept).subjectCode("BIO-GEN").description("Biology").build(),
                    Subject.builder().name("Chemistry").coefficient(4).department(scienceDept).subjectCode("CHEM-GEN").description("Chemistry").build(),
                    Subject.builder().name("Physics").coefficient(4).department(scienceDept).subjectCode("PHY-GEN").description("Physics").build(),

                    // Science Department - S1 Specialty (Pure Sciences)
                    Subject.builder().name("Advanced Mathematics").coefficient(5).department(scienceDept).specialty("S1").subjectCode("MATH-S1").description("Advanced Mathematics for S1").build(),
                    Subject.builder().name("Advanced Physics").coefficient(5).department(scienceDept).specialty("S1").subjectCode("PHY-S1").description("Advanced Physics for S1").build(),
                    Subject.builder().name("Advanced Chemistry").coefficient(5).department(scienceDept).specialty("S1").subjectCode("CHEM-S1").description("Advanced Chemistry for S1").build(),

                    // Science Department - S2 Specialty (Biological Sciences)
                    Subject.builder().name("Human Biology").coefficient(5).department(scienceDept).specialty("S2").subjectCode("HBIO-S2").description("Human Biology for S2").build(),
                    Subject.builder().name("Geology").coefficient(4).department(scienceDept).specialty("S2").subjectCode("BOT-S2").description("Botany for S2").build(),

                    // Arts Department - General Arts
                    Subject.builder().name("History").coefficient(3).department(artsDept).subjectCode("HIST-GEN").description("History").build(),
                    Subject.builder().name("Geography").coefficient(3).department(artsDept).subjectCode("GEO-GEN").description("Geography").build(),
                    Subject.builder().name("Literature in English").coefficient(3).department(artsDept).subjectCode("LIT-GEN").description("Literature in English").build(),

                    // Arts Department - A1 Specialty (Languages)
                    Subject.builder().name("French").coefficient(4).department(artsDept).specialty("A1").subjectCode("FREN-A1").description("French Language for A1").build(),

                    // Arts Department - A2 Specialty (Humanities)
                    Subject.builder().name("Philosophy").coefficient(4).department(artsDept).specialty("A2").subjectCode("PHIL-A2").description("Philosophy for A2").build(),

                    // Commercial Department
                    Subject.builder().name("Commerce").coefficient(3).department(commercialDept).subjectCode("COMM-GEN").description("Commerce").build(),
                    Subject.builder().name("Accounting").coefficient(4).department(commercialDept).subjectCode("ACC-GEN").description("Accounting").build(),
                    Subject.builder().name("Business Studies").coefficient(3).department(commercialDept).subjectCode("BUS-GEN").description("Business Studies").build(),
                    Subject.builder().name("Economics").coefficient(3).department(commercialDept).subjectCode("ECO-GEN").description("Economics").build(),

                    // Commercial Department - Accountancy Specialty
                    Subject.builder().name("Advanced Accounting").coefficient(5).department(commercialDept).specialty("Accountancy").subjectCode("ACC-ADV").description("Advanced Accounting").build(),
                    Subject.builder().name("Cost Accounting").coefficient(4).department(commercialDept).specialty("Accountancy").subjectCode("COST-ACC").description("Cost Accounting").build(),

                    // Commercial Department - Marketing Specialty
                    Subject.builder().name("Marketing Principles").coefficient(4).department(commercialDept).specialty("Marketting").subjectCode("MKT-PRIN").description("Marketing Principles").build(),
                    Subject.builder().name("Consumer Behavior").coefficient(3).department(commercialDept).specialty("Marketting").subjectCode("CONS-BEH").description("Consumer Behavior").build(),

                    // Technical Department
                    Subject.builder().name("Technical Drawing").coefficient(3).department(technicalDept).subjectCode("TD-GEN").description("Technical Drawing").build(),
                    Subject.builder().name("Woodwork").coefficient(3).department(technicalDept).subjectCode("WOOD-GEN").description("Woodwork").build(),
                    Subject.builder().name("Metalwork").coefficient(3).department(technicalDept).subjectCode("METAL-GEN").description("Metalwork").build(),

                    // Technical Department - EPS Specialty
                    Subject.builder().name("Electrical Circuits").coefficient(4).department(technicalDept).specialty("EPS(Electrical Power System)").subjectCode("ELEC-CIR").description("Electrical Circuits").build(),
                    Subject.builder().name("Power Systems").coefficient(4).department(technicalDept).specialty("EPS(Electrical Power System)").subjectCode("PWR-SYS").description("Power Systems").build(),

                    // Technical Department - BC Specialty
                    Subject.builder().name("Building Construction").coefficient(4).department(technicalDept).specialty("BC(Building and Construction)").subjectCode("BLD-CON").description("Building Construction").build(),
                    Subject.builder().name("Structural Design").coefficient(4).department(technicalDept).specialty("BC(Building and Construction)").subjectCode("STR-DES").description("Structural Design").build(),

                    // Home Economics Department
                    Subject.builder().name("Home Management").coefficient(3).department(homeEconDept).subjectCode("HOME-MGT").description("Home Management").build(),
                    Subject.builder().name("Nutrition").coefficient(3).department(homeEconDept).subjectCode("NUTR-GEN").description("Nutrition").build(),
                    Subject.builder().name("Textiles").coefficient(3).department(homeEconDept).subjectCode("TEXT-GEN").description("Textiles").build()
            );

            subjectRepository.saveAll(subjects);
            log.info("Default subjects created successfully with departments and specialties ({} subjects)", subjects.size());
        }
    }

    private void initNotices() {
        if (noticeRepository.count() == 0) {
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