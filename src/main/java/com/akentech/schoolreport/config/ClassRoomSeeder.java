package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.enums.ClassLevel;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassRoomSeeder implements CommandLineRunner {

    private final ClassRoomRepository classRoomRepository;

    @Override
    public void run(String... args) throws Exception {
        String academicYear = "2025/2026"; // Change as needed

        List<ClassLevel> classLevels = List.of(
                ClassLevel.FORM_1,
                ClassLevel.FORM_2,
                ClassLevel.FORM_3,
                ClassLevel.FORM_4,
                ClassLevel.FORM_5,
                ClassLevel.LOWER_SIXTH,
                ClassLevel.UPPER_SIXTH
        );

        for (ClassLevel level : classLevels) {
            classRoomRepository.findByCode(level).orElseGet(() -> {
                ClassRoom classroom = ClassRoom.builder()
                        .name(level.getDisplayName())
                        .code(level)
                        .academicYear(academicYear)
                        .build();

                log.info("Seeding classroom: {} ({})", classroom.getName(), classroom.getCode());
                return classRoomRepository.save(classroom);
            });
        }
    }
}
