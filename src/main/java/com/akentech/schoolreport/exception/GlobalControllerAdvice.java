package com.akentech.schoolreport.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.beans.PropertyEditorSupport;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    /**
     * Global model attributes available to all controllers
     */
    @ModelAttribute("appName")
    public String appName() {
        return "School Report System";
    }

    @ModelAttribute("currentYear")
    public Integer currentYear() {
        return java.time.Year.now().getValue();
    }

    /**
     * Init binder to handle custom type conversion
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Custom editor for Long parameters to handle "null" string
        binder.registerCustomEditor(Long.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text.trim())) {
                    setValue(null);
                } else {
                    try {
                        setValue(Long.parseLong(text.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to convert '{}' to Long, setting to null", text);
                        setValue(null);
                    }
                }
            }

            @Override
            public String getAsText() {
                Long value = (Long) getValue();
                return value != null ? value.toString() : "";
            }
        });

        // Custom editor for Integer parameters
        binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text.trim())) {
                    setValue(null);
                } else {
                    try {
                        setValue(Integer.parseInt(text.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to convert '{}' to Integer, setting to null", text);
                        setValue(null);
                    }
                }
            }

            @Override
            public String getAsText() {
                Integer value = (Integer) getValue();
                return value != null ? value.toString() : "";
            }
        });

        // Custom editor for Double parameters
        binder.registerCustomEditor(Double.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text.trim())) {
                    setValue(null);
                } else {
                    try {
                        setValue(Double.parseDouble(text.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to convert '{}' to Double, setting to null", text);
                        setValue(null);
                    }
                }
            }

            @Override
            public String getAsText() {
                Double value = (Double) getValue();
                return value != null ? value.toString() : "";
            }
        });

        // Custom editor for Boolean parameters
        binder.registerCustomEditor(Boolean.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text.trim())) {
                    setValue(null);
                } else {
                    setValue(Boolean.parseBoolean(text.trim()));
                }
            }

            @Override
            public String getAsText() {
                Boolean value = (Boolean) getValue();
                return value != null ? value.toString() : "";
            }
        });
    }
}