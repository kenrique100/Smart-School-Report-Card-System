package com.akentech.schoolreport.util;

public final class GradeCalculator {

    private GradeCalculator() {}

    public static String toLetterGrade(double scoreOutOf20) {
        // Score is between 0 and 20
        if (scoreOutOf20 >= 18.0) return "A";
        if (scoreOutOf20 >= 15.0) return "B";
        if (scoreOutOf20 >= 10.0) return "C";
        if (scoreOutOf20 <= 9.0) return "D";
        return "U";
    }

    public static String remarkForAverage(double avg) {
        if (avg >= 18.0) return "Excellent";
        if (avg >= 15.0) return "Very Good";
        if (avg >= 10.0) return "Average";
        if (avg <= 9.0) return "Failed";
        return "Poor";
    }
}
