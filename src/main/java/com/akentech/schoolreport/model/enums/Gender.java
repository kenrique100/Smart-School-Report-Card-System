package com.akentech.schoolreport.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    MALE("Male"),
    FEMALE("Female");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Gender fromDisplayName(String displayName) {
        for (Gender gender : values()) {
            if (gender.displayName.equalsIgnoreCase(displayName)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown gender: " + displayName);
    }

    // Helper method to convert from string
    public static Gender fromString(String value) {
        if (value == null) return null;

        try {
            // First try to match by enum name
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If that fails, try to match by display name
            return fromDisplayName(value);
        }
    }
}