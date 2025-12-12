package com.akentech.schoolreport.model.enums;

import lombok.Getter;

@Getter
public enum AssessmentType {
    ASSESSMENT_1(1, 1),  // Term 1, Assessment 1
    ASSESSMENT_2(2, 1),  // Term 1, Assessment 2
    ASSESSMENT_3(3, 2),  // Term 2, Assessment 3
    ASSESSMENT_4(4, 2),  // Term 2, Assessment 4
    ASSESSMENT_5(5, 3);  // Term 3, Assessment 5

    private final Integer assessmentNumber;
    private final Integer term;

    AssessmentType(Integer assessmentNumber, Integer term) {
        this.assessmentNumber = assessmentNumber;
        this.term = term;
    }

    public static AssessmentType fromTermAndNumber(Integer term, Integer assessmentNumber) {
        for (AssessmentType type : values()) {
            if (type.getTerm().equals(term) && type.getAssessmentNumber().equals(assessmentNumber)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid term or assessment number");
    }

    public static AssessmentType[] getAssessmentsForTerm(Integer term) {
        return switch (term) {
            case 1 -> new AssessmentType[]{ASSESSMENT_1, ASSESSMENT_2};
            case 2 -> new AssessmentType[]{ASSESSMENT_3, ASSESSMENT_4};
            case 3 -> new AssessmentType[]{ASSESSMENT_5};
            default -> throw new IllegalArgumentException("Invalid term: " + term);
        };
    }
}