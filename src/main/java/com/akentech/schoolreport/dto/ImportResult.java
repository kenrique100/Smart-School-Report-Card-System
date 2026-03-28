package com.akentech.schoolreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {

    @Builder.Default
    private int successCount = 0;

    @Builder.Default
    private int errorCount = 0;

    @Builder.Default
    private int warningCount = 0;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> successMessages = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
        this.errorCount++;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
        this.warningCount++;
    }

    public void addSuccess(String message) {
        this.successMessages.add(message);
        this.successCount++;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public boolean hasWarnings() {
        return warningCount > 0;
    }

    public boolean isSuccess() {
        return successCount > 0 && errorCount == 0;
    }
}
