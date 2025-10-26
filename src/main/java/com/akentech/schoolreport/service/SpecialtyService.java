package com.akentech.schoolreport.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SpecialtyService {

    public SpecialtyService() {
    }

    public List<String> getCommercialSpecialties() {
        return List.of("Accountancy", "Marketing", "SAC(Secretarial Administration and Communication)");
    }

    public List<String> getTechnicalSpecialties() {
        return List.of("EPS(Electrical Power System)", "BC(Building and Construction)", "CI(Clothing Industry)");
    }

    public List<String> getScienceSpecialties() {
        return List.of("S1", "S2", "S3", "S4", "S5", "S6");
    }

    public List<String> getArtsSpecialties() {
        return List.of("A1", "A2", "A3", "A4", "A5");
    }

    public List<String> getHomeEconomicsSpecialties() {
        return List.of("Home Economics", "Food and Nutrition", "Textiles");
    }

    public List<String> getGeneralSpecialties() {
        return List.of();
    }

    public List<String> getAllSpecialties() {
        List<String> all = new ArrayList<>();
        all.addAll(getCommercialSpecialties());
        all.addAll(getTechnicalSpecialties());
        all.addAll(getScienceSpecialties());
        all.addAll(getArtsSpecialties());
        all.addAll(getHomeEconomicsSpecialties());
        return all;
    }

    public List<String> getSpecialtiesByDepartment(String departmentCode) {
        if (departmentCode == null) return new ArrayList<>();

        return switch (departmentCode) {
            case "COM" -> getCommercialSpecialties();
            case "TEC" -> getTechnicalSpecialties();
            case "SCI" -> getScienceSpecialties();
            case "ART" -> getArtsSpecialties();
            case "HE" -> getHomeEconomicsSpecialties();
            case "GEN" -> getGeneralSpecialties();
            default -> new ArrayList<>();
        };
    }

    // KEEP: This method works with string codes for AJAX compatibility
    public SpecialtyRequirement checkSpecialtyRequirement(String classCode, String departmentCode) {
        boolean required = isSpecialtyRequired(classCode, departmentCode);
        boolean allowed = isSpecialtyAllowed(classCode, departmentCode);
        List<String> specialties = getSpecialtiesByDepartment(departmentCode);

        return new SpecialtyRequirement(required, allowed, specialties);
    }

    public boolean isSpecialtyRequired(String classCode, String departmentCode) {
        if (classCode == null || departmentCode == null) return false;
        return (classCode.equals("LSX") || classCode.equals("USX")) &&
                (departmentCode.equals("SCI") || departmentCode.equals("ART"));
    }

    public boolean isSpecialtyAllowed(String classCode, String departmentCode) {
        if (classCode == null || departmentCode == null) return false;
        return (!classCode.equals("F1") && !classCode.equals("F2") && !classCode.equals("F3")) ||
                !departmentCode.equals("GEN");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class SpecialtyRequirement {
        private boolean required;
        private boolean allowed;
        private List<String> specialties;
    }
}