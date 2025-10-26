package com.akentech.schoolreport.exception;

public abstract class SchoolManagementException extends RuntimeException {
    public SchoolManagementException(String message) {
        super(message);
    }

    public SchoolManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}