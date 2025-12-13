package com.akentech.schoolreport.service;

import com.akentech.schoolreport.dto.ReportDTO;
import com.akentech.schoolreport.dto.SubjectReport;
import com.akentech.schoolreport.model.Assessment;
import com.akentech.schoolreport.model.Student;
import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.model.enums.AssessmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final GradeService gradeService;

    public ReportDTO toReportDTO(Student student, List<SubjectReport> subjectReports,
                                 Integer term, Map<String, Object> statistics) {
        String className = student.getClassRoom() != null ? student.getClassRoom().getName() : "";

        Double termAverage = (Double) statistics.get("termAverage");

        // Get pass rate and subjects passed from statistics (already calculated in ReportServiceImpl)
        Double passRate = (Double) statistics.get("passRate");
        long subjectsPassed = ((Integer) statistics.get("subjectsPassed")).longValue();
        Integer totalSubjects = (Integer) statistics.get("totalSubjects");

        return ReportDTO.builder()
                .id(student.getId())
                .student(student)
                .studentFullName(student.getFullName())
                .rollNumber(student.getRollNumber())
                .className(className)
                .department(student.getDepartment() != null ? student.getDepartment().getName() : "")
                .specialty(student.getSpecialty())
                .term(term)
                .termAverage(termAverage)
                .formattedAverage(formatScore(termAverage))
                .rankInClass((Integer) statistics.get("rankInClass"))
                .totalStudentsInClass((Integer) statistics.get("totalStudentsInClass"))
                .remarks((String) statistics.get("remarks"))
                .subjectReports(subjectReports)
                .academicYear(getAcademicYear(student))
                .classTeacher(student.getClassRoom() != null ?
                        (student.getClassRoom().getClassTeacher() != null ?
                                student.getClassRoom().getClassTeacher() : "Not Assigned") : "Not Assigned")
                .passRate(passRate != null ? passRate : 0.0)
                .subjectsPassed((int) subjectsPassed)
                .totalSubjects(totalSubjects != null ? totalSubjects : 0)
                .build();
    }

    public SubjectReport toSubjectReport(Subject subject, List<Assessment> assessments,
                                         Integer term, String className) {

        AssessmentType[] types = AssessmentType.getAssessmentsForTerm(term);

        Double assessment1 = null;
        Double assessment2 = null;

        // Extract scores based on term
        if (term == 1) {
            assessment1 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_1);
            assessment2 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_2);
        } else if (term == 2) {
            assessment1 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_3);
            assessment2 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_4);
        } else if (term == 3) {
            assessment1 = extractAssessmentScore(assessments, AssessmentType.ASSESSMENT_5);
            // Term 3 only has exam
        }

        // Calculate subject average using GradeService
        Double subjectAverage = gradeService.calculateSubjectAverage(assessment1, assessment2, term);
        String letterGrade = gradeService.calculateLetterGrade(subjectAverage, className);

        return SubjectReport.builder()
                .subjectName(subject.getName())
                .coefficient(subject.getCoefficient())
                .assessment1(assessment1)
                .assessment2(assessment2)
                .subjectAverage(subjectAverage)
                .letterGrade(letterGrade)
                .className(className) // Add className to SubjectReport
                .build();
    }

    private Double extractAssessmentScore(List<Assessment> assessments, AssessmentType type) {
        return assessments.stream()
                .filter(a -> a.getType() == type)
                .findFirst()
                .map(Assessment::getScore)
                .orElse(null);
    }

    private String formatScore(Double score) {
        return score != null ? String.format("%.2f/20", score) : "0.00/20";
    }

    private String getAcademicYear(Student student) {
        if (student.getAcademicYearStart() != null && student.getAcademicYearEnd() != null) {
            return student.getAcademicYearStart() + "-" + student.getAcademicYearEnd();
        }
        int currentYear = Year.now().getValue();
        return currentYear + "-" + (currentYear + 1);
    }
}