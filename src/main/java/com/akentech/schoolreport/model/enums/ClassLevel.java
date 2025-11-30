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

    @Override
    public String toString() {
        return displayName;
    }
}
