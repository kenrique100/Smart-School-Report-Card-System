package com.akentech.schoolreport.util;

public final class GradeCalculator {

    private GradeCalculator() {}

    public static String toLetterGrade(double scoreOutOf20, String className) {
        // Determine grading system based on class level
        boolean isAdvancedLevel = className.contains("Sixth") || className.contains("Upper") || className.contains("Lower");

        if (isAdvancedLevel) {
            // A-Level grading: A-E pass, F fail, O complimentary
            if (scoreOutOf20 >= 18.0) return "A";
            if (scoreOutOf20 >= 16.0) return "B";
            if (scoreOutOf20 >= 14.0) return "C";
            if (scoreOutOf20 >= 12.0) return "D";
            if (scoreOutOf20 >= 10.0) return "E";
            if (scoreOutOf20 >= 8.0) return "O"; // Complimentary
            return "F"; // Failed
        } else {
            // Ordinary Level (Forms 1-5): A-C pass, D/U fail
            if (scoreOutOf20 >= 18.0) return "A";
            if (scoreOutOf20 >= 15.0) return "B";
            if (scoreOutOf20 >= 10.0) return "C";
            if (scoreOutOf20 >= 5.0) return "D";
            return "U"; // Unclassified (Failed)
        }
    }

    public static String remarkForAverage(double avg) {
        if (avg >= 18.0) return "Excellent";
        if (avg >= 15.0) return "Very Good";
        if (avg >= 10.0) return "Good";
        if (avg >= 5.0) return "Poor";
        return "Very Poor";
    }

    public static boolean isPassingGrade(String letterGrade, String className) {
        boolean isAdvancedLevel = className.contains("Sixth") || className.contains("Upper") || className.contains("Lower");

        if (isAdvancedLevel) {
            // A-Level: A-E and O are passing
            return !letterGrade.equals("F");
        } else {
            // Ordinary Level: A-C are passing
            return letterGrade.equals("A") || letterGrade.equals("B") || letterGrade.equals("C");
        }
    }

    // Additional helper method to get grade points
    public static double getGradePoints(String letterGrade, String className) {
        boolean isAdvancedLevel = className.contains("Sixth") || className.contains("Upper") || className.contains("Lower");

        if (isAdvancedLevel) {
            return switch (letterGrade) {
                case "A" -> 5.0;
                case "B" -> 4.0;
                case "C" -> 3.0;
                case "D" -> 2.0;
                case "E" -> 1.0;
                case "O" -> 0.5;
                default -> 0.0;
            };
        } else {
            return switch (letterGrade) {
                case "A" -> 4.0;
                case "B" -> 3.0;
                case "C" -> 2.0;
                case "D" -> 1.0;
                default -> 0.0;
            };
        }
    }
}