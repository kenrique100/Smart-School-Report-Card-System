package com.akentech.schoolreport.model.enums;

import lombok.Getter;

@Getter
public enum ClassLevel {
    FORM_1("F1", "Form 1"),
    FORM_2("F2", "Form 2"),
    FORM_3("F3", "Form 3"),
    FORM_4("F4", "Form 4"),
    FORM_5("F5", "Form 5"),
    LOWER_SIXTH("LSX", "Lower Sixth"),
    UPPER_SIXTH("USX", "Upper Sixth");

    private final String code;
    private final String displayName;

    ClassLevel(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    // Helper method to check if it's a form level (F1-F5)
    public boolean isFormLevel() {
        return this.code != null && this.code.startsWith("F");
    }

    // Helper method to check if it's sixth form
    public boolean isSixthForm() {
        return this.code != null && (this.code.startsWith("L") || this.code.startsWith("U"));
    }

    // Static method to get enum from code
    public static ClassLevel fromCode(String code) {
        if (code == null) throw new IllegalArgumentException("Code cannot be null");
        for (ClassLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown class level code: " + code);
    }

    // NEW: Method to parse from various string formats
    public static ClassLevel fromString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return FORM_1; // default
        }

        String normalized = input.trim().toUpperCase().replace(" ", "_");

        // Try exact match first
        try {
            return ClassLevel.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try to match by display name or code
            for (ClassLevel level : values()) {
                if (level.getDisplayName().equalsIgnoreCase(input.trim()) ||
                        level.getCode().equalsIgnoreCase(input.trim())) {
                    return level;
                }
            }

            // Try partial matches for display names
            if (input.contains("Form 1") || input.contains("F1") || input.equals("1")) return FORM_1;
            if (input.contains("Form 2") || input.contains("F2") || input.equals("2")) return FORM_2;
            if (input.contains("Form 3") || input.contains("F3") || input.equals("3")) return FORM_3;
            if (input.contains("Form 4") || input.contains("F4") || input.equals("4")) return FORM_4;
            if (input.contains("Form 5") || input.contains("F5") || input.equals("5")) return FORM_5;
            if (input.contains("Lower Sixth") || input.contains("LSX") || input.contains("Lower")) return LOWER_SIXTH;
            if (input.contains("Upper Sixth") || input.contains("USX") || input.contains("Upper")) return UPPER_SIXTH;

            throw new IllegalArgumentException("Unknown class level: " + input);
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}