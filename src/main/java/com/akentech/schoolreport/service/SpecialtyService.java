package com.akentech.schoolreport.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class SpecialtyService {

    private final Map<String, List<String>> departmentSpecialties = createDepartmentSpecialties();

    public SpecialtyService() {
    }

    private Map<String, List<String>> createDepartmentSpecialties() {
        Map<String, List<String>> specialties = new HashMap<>();
        specialties.put("COM", Arrays.asList("C1", "C2", "C3", "C4"));
        specialties.put("TEC", Arrays.asList("T1", "T2", "T3", "T4", "T5"));
        specialties.put("SCI", Arrays.asList("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8"));
        specialties.put("ART", Arrays.asList("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8"));
        specialties.put("HE", Arrays.asList("H1", "H2", "H3"));
        specialties.put("GEN", new ArrayList<>());
        return specialties;
    }

    public List<String> getCommercialSpecialties() {
        return List.of("C1", "C2", "C3", "C4");
    }

    public List<String> getTechnicalSpecialties() {
        return List.of("T1", "T2", "T3", "T4", "T5");
    }

    public List<String> getScienceSpecialties() {
        return List.of("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8");
    }

    public List<String> getArtsSpecialties() {
        return List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8");
    }

    public List<String> getHomeEconomicsSpecialties() {
        return List.of("H1", "H2", "H3");
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

        log.info("Getting specialties for department: {}", departmentCode);
        List<String> specialties = departmentSpecialties.getOrDefault(departmentCode, new ArrayList<>());
        log.info("Found {} specialties for department {}", specialties.size(), departmentCode);
        return specialties;
    }

    public List<String> getSpecialtiesByDepartmentCode(String departmentCode) {
        return getSpecialtiesByDepartment(departmentCode);
    }

    // KEEP: This method works with string codes for AJAX compatibility
    public SpecialtyRequirement checkSpecialtyRequirement(String classCode, String departmentCode) {
        log.info("Checking specialty requirement for class: {}, department: {}", classCode, departmentCode);

        boolean required = isSpecialtyRequired(classCode, departmentCode);
        boolean allowed = isSpecialtyAllowed(classCode, departmentCode);
        List<String> specialties = getSpecialtiesByDepartment(departmentCode);

        log.info("Specialty requirement - required: {}, allowed: {}, specialties: {}",
                required, allowed, specialties.size());

        return new SpecialtyRequirement(required, allowed, specialties);
    }

    public boolean isSpecialtyRequired(String classCode, String departmentCode) {
        if (classCode == null || departmentCode == null) return false;

        // Specialty is required for Sixth Form Science and Arts students
        boolean isSixthForm = classCode.equals("LOWER_SIXTH") || classCode.equals("UPPER_SIXTH");
        boolean requiresSpecialty = departmentCode.equals("SCI") || departmentCode.equals("ART");

        boolean result = isSixthForm && requiresSpecialty;
        log.debug("Specialty required for class {} department {}: {}", classCode, departmentCode, result);

        return result;
    }

    public boolean isSpecialtyAllowed(String classCode, String departmentCode) {
        if (classCode == null || departmentCode == null) return false;

        // Specialty not allowed for Forms 1-3
        if (classCode.equals("FORM_1") || classCode.equals("FORM_2") || classCode.equals("FORM_3")) {
            return false;
        }

        // Specialty allowed for Forms 4-5 and Sixth Form in all departments except General
        boolean result = !departmentCode.equals("GEN");
        log.debug("Specialty allowed for class {} department {}: {}", classCode, departmentCode, result);

        return result;
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