package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.*;
import com.akentech.schoolreport.repository.*;
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
                    Department.builder().name("Sciences").code("SCI").description("Science Department").build(),
                    Department.builder().name("Arts").code("ART").description("Arts Department").build(),
                    Department.builder().name("Commercial").code("COM").description("Commercial Department").build(),
                    Department.builder().name("Technical").code("TEC").description("Technical Department").build()
            );
            departmentRepository.saveAll(departments);
            log.info("Default departments created");
        }
    }

    private void initClassRooms() {
        if (classRoomRepository.count() == 0) {
            List<ClassRoom> classRooms = Arrays.asList(
                    ClassRoom.builder().name("1-A").code("1A").academicYear("2024").build(),
                    ClassRoom.builder().name("1-B").code("1B").academicYear("2024").build(),
                    ClassRoom.builder().name("2-A").code("2A").academicYear("2024").build(),
                    ClassRoom.builder().name("2-B").code("2B").academicYear("2024").build(),
                    ClassRoom.builder().name("3-A").code("3A").academicYear("2024").build(),
                    ClassRoom.builder().name("3-B").code("3B").academicYear("2024").build(),
                    ClassRoom.builder().name("4-A").code("4A").academicYear("2024").build(),
                    ClassRoom.builder().name("4-B").code("4B").academicYear("2024").build()
            );
            classRoomRepository.saveAll(classRooms);
            log.info("Default classrooms created");
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
            ClassRoom class4B = classRoomRepository.findByName("4-B").orElseThrow();

            List<Student> students = Arrays.asList(
                    Student.builder()
                            .studentId("ST1000010001")
                            .firstName("Kasun")
                            .lastName("Chamara")
                            .rollNumber("4B-SCI-001")
                            .classRoom(class4B)
                            .gender("Male")
                            .dateOfBirth(LocalDate.of(2001, 6, 26))
                            .email("kasun@student.com")
                            .address("Colombo Road Kandy")
                            .build(),
                    Student.builder()
                            .studentId("ST1000010002")
                            .firstName("Dasun")
                            .lastName("Shanuka")
                            .rollNumber("4B-SCI-002")
                            .classRoom(class4B)
                            .gender("Male")
                            .dateOfBirth(LocalDate.of(2020, 5, 31))
                            .email("dasun@student.com")
                            .address("Ampara Road Uhana")
                            .build()
            );
            studentRepository.saveAll(students);
            log.info("Default students created");
        }
    }

    /**
     * Initializes the database with a comprehensive list of subjects if none exist.
     * This includes core, science, language, art, and technical subjects.
     */
    private void initSubjects() {
        if (subjectRepository.count() == 0) {
            List<Subject> subjects = Arrays.asList(
                    // Core Subjects
                    Subject.builder().name("Mathematics").coefficient(4).build(),
                    Subject.builder().name("English Language").coefficient(3).build(),
                    Subject.builder().name("Science").coefficient(4).build(),
                    Subject.builder().name("History").coefficient(2).build(),
                    Subject.builder().name("Geography").coefficient(2).build(),

                    // Sciences
                    Subject.builder().name("Physics").coefficient(4).build(),
                    Subject.builder().name("Chemistry").coefficient(4).build(),
                    Subject.builder().name("Biology").coefficient(3).build(),
                    Subject.builder().name("Computer Science").coefficient(3).build(),
                    Subject.builder().name("Health Science").coefficient(2).build(),

                    // Languages
                    Subject.builder().name("French").coefficient(2).build(),
                    Subject.builder().name("Literature in English").coefficient(3).build(),

                    // Arts and Humanities
                    Subject.builder().name("Civic Education").coefficient(2).build(),
                    Subject.builder().name("Religious Studies").coefficient(2).build(),
                    Subject.builder().name("Economics").coefficient(3).build(),
                    Subject.builder().name("Commerce").coefficient(2).build(),
                    Subject.builder().name("Philosophy").coefficient(2).build(),

                    // Technical / Vocational
                    Subject.builder().name("Agricultural Science").coefficient(3).build(),
                    Subject.builder().name("Food and Nutrition").coefficient(2).build(),
                    Subject.builder().name("Home Economics").coefficient(2).build(),
                    Subject.builder().name("Business Studies").coefficient(3).build(),
                    Subject.builder().name("Accounting").coefficient(3).build(),
                    Subject.builder().name("Technical Drawing").coefficient(3).build(),
                    Subject.builder().name("Woodwork").coefficient(2).build(),
                    Subject.builder().name("Metalwork").coefficient(2).build(),
                    Subject.builder().name("Physical Education").coefficient(2).build()
            );

            subjectRepository.saveAll(subjects);
            log.info("Default subjects created successfully ({} subjects)", subjects.size());
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