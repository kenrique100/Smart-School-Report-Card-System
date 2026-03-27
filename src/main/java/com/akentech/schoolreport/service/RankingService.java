package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.StudentRank;
import com.akentech.schoolreport.model.AverageRecord;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.repository.AverageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final AverageRecordRepository averageRecordRepository;

    /**
     * Compute ranking for students in this list for the given term.
     * Returns a list sorted by rank ascending (1 best).
     * Also computes department ranking for students in the same department within the classroom.
     */
    public List<StudentRank> computeRanking(List<Student> students, Integer term) {
        if (students.isEmpty()) return Collections.emptyList();

        List<AverageRecord> records = averageRecordRepository.findByStudentInAndTerm(students, term);
        Map<Long, AverageRecord> byStudent = records.stream().collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));

        List<StudentRank> ranks = new ArrayList<>();
        for (Student s : students) {
            AverageRecord ar = byStudent.get(s.getId());
            if (ar != null) {
                ranks.add(StudentRank.builder()
                        .student(s)
                        .average(ar.getAverage())
                        .remarks(ar.getRemarks())
                        .build());
            } else {
                ranks.add(StudentRank.builder()
                        .student(s)
                        .average(0.0)
                        .remarks("No data")
                        .build());
            }
        }

        // sort descending by average
        ranks.sort(Comparator.comparing(StudentRank::getAverage, Comparator.nullsFirst(Comparator.reverseOrder())));

        // assign rank numbers (handling ties by equal average -> same rank)
        int currentRank = 0;
        Double lastAvg = null;
        for (int i = 0; i < ranks.size(); i++) {
            Double avg = ranks.get(i).getAverage();
            if (lastAvg == null || Double.compare(avg, lastAvg) != 0) {
                currentRank = i + 1;
                lastAvg = avg;
            }
            ranks.get(i).setRank(currentRank);
        }

        // Compute department rankings
        computeDepartmentRanks(ranks);

        log.info("Computed ranking for term {}: {} entries, top student: {} {}",
                term,
                ranks.size(),
                ranks.get(0).getStudent().getFirstName(),
                ranks.get(0).getStudent().getLastName());
        return ranks;
    }

    /**
     * Compute department rankings for students grouped by department.
     * Students without a department are skipped for department ranking.
     */
    private void computeDepartmentRanks(List<StudentRank> ranks) {
        // Group students by department
        Map<Long, List<StudentRank>> byDepartment = new HashMap<>();
        for (StudentRank rank : ranks) {
            if (rank.getStudent().getDepartment() != null) {
                Long deptId = rank.getStudent().getDepartment().getId();
                byDepartment.computeIfAbsent(deptId, k -> new ArrayList<>()).add(rank);
            }
        }

        // Compute ranking within each department
        for (List<StudentRank> deptRanks : byDepartment.values()) {
            // Already sorted by average descending from class ranking
            int currentRank = 0;
            Double lastAvg = null;
            for (int i = 0; i < deptRanks.size(); i++) {
                Double avg = deptRanks.get(i).getAverage();
                if (lastAvg == null || Double.compare(avg, lastAvg) != 0) {
                    currentRank = i + 1;
                    lastAvg = avg;
                }
                deptRanks.get(i).setRankInDepartment(currentRank);
            }
        }
    }
}