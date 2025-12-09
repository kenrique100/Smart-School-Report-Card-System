package com.akentech.schoolreport.util;

import java.util.Arrays;
import java.util.List;

public class DepartmentUtil {

    public static List<String> getSpecialtiesForDepartment(String departmentCode) {
        if (departmentCode == null) {
            return List.of();
        }

        return switch (departmentCode) {
            case "COM" -> Arrays.asList("Accounting", "Administration & Communication Techniques");
            case "SCI" -> Arrays.asList("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8");
            case "ART" -> Arrays.asList("A1", "A2", "A3", "A4", "A5");
            default -> List.of();
        };
    }

    public static boolean hasSpecialties(String departmentCode) {
        return !getSpecialtiesForDepartment(departmentCode).isEmpty();
    }

    public static String getDepartmentDisplayName(String departmentCode) {
        if (departmentCode == null) {
            return "General";
        }

        return switch (departmentCode) {
            case "COM" -> "Commercial";
            case "SCI" -> "Sciences";
            case "ART" -> "Arts";
            case "TEC" -> "Technical";
            case "HE" -> "Home Economics";
            case "EPS" -> "Electrical Power System";
            case "CI" -> "Clothing Industry";
            default -> "General";
        };
    }
}