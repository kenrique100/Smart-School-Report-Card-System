package com.akentech.schoolreport.model.enums;

import lombok.Getter;

public enum DepartmentCode {
    GEN("GEN", "General Studies"),
    SCI("SCI", "Science"),
    ART("ART", "Arts"),
    COM("COM", "Commercial"),
    TEC("TEC", "Technical"),
    HE("HE", "Home Economics"),
    CI("CI", "Clothing Industry"),
    EPS("EPS", "Electrical Power Systems");

    @Getter
    private final String code;
    @Getter
    private final String description;

    DepartmentCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DepartmentCode fromCode(String code) {
        for (DepartmentCode dept : values()) {
            if (dept.code.equals(code)) {
                return dept;
            }
        }
        throw new IllegalArgumentException("Unknown department code: " + code);
    }
}
