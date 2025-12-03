package com.akentech.schoolreport.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ParameterUtils {

    /**
     * Safely parse a Long from a string value, handling null and "null" strings
     */
    public static Long safeParseLong(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long from value: {}", value);
            return null;
        }
    }

    /**
     * Safely parse an Integer from a string value, handling null and "null" strings
     */
    public static Integer safeParseInteger(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Integer from value: {}", value);
            return null;
        }
    }

    /**
     * Safely parse a Double from a string value, handling null and "null" strings
     */
    public static Double safeParseDouble(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Double from value: {}", value);
            return null;
        }
    }

    /**
     * Safely parse a Boolean from a string value
     */
    public static Boolean safeParseBoolean(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Clean string parameter - remove "null" string literal
     */
    public static String cleanString(String value) {
        if (value == null || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    /**
     * Check if a string represents a null value
     */
    public static boolean isNullValue(String value) {
        return value == null || "null".equalsIgnoreCase(value.trim()) || value.trim().isEmpty();
    }
}