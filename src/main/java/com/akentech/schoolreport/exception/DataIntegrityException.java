package com.akentech.schoolreport.exception;

public class DataIntegrityException extends SchoolManagementException {
    public DataIntegrityException(String message) {
        super(message);
    }

    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
