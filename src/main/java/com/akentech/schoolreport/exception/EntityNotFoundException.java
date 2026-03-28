package com.akentech.schoolreport.exception;

public class EntityNotFoundException extends SchoolManagementException {
    public EntityNotFoundException(String entityName, Long id) {
        super(String.format("%s not found with id: %d", entityName, id));
    }

    public EntityNotFoundException(String entityName, String identifier) {
        super(String.format("%s not found with identifier: %s", entityName, identifier));
    }

    // ADDED: Overload for enum types
    public EntityNotFoundException(String entityName, Enum<?> enumValue) {
        super(String.format("%s not found with identifier: %s", entityName, enumValue.name()));
    }

    public EntityNotFoundException(String entityName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", entityName, fieldName, fieldValue));
    }

    public EntityNotFoundException(String entityName, String fieldName, Object fieldValue, String additionalField, Object additionalValue) {
        super(String.format("%s not found with %s: '%s' in %s: '%s'",
                entityName, fieldName, fieldValue, additionalField, additionalValue));
    }
    public EntityNotFoundException(String message) {
        super(message);
    }
}
