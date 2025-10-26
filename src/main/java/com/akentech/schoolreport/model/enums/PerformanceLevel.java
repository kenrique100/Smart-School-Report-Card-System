package com.akentech.schoolreport.model.enums;

public enum PerformanceLevel {
    EXCELLENT("Excellent", 90.0),
    VERY_GOOD("Very Good", 80.0),
    GOOD("Good", 70.0),
    AVERAGE("Average", 60.0),
    POOR("Poor", 50.0),
    FAIL("Fail", 0.0);

    private final String displayName;
    private final Double minimumScore;

    PerformanceLevel(String displayName, Double minimumScore) {
        this.displayName = displayName;
        this.minimumScore = minimumScore;
    }

    public String getDisplayName() { return displayName; }
    public Double getMinimumScore() { return minimumScore; }

    public static PerformanceLevel fromScore(Double score) {
        if (score == null) return FAIL;
        for (PerformanceLevel level : values()) {
            if (score >= level.minimumScore) {
                return level;
            }
        }
        return FAIL;
    }
}
