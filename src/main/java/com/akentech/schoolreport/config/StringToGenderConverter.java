package com.akentech.schoolreport.config;

import com.akentech.schoolreport.model.enums.Gender;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToGenderConverter implements Converter<String, Gender> {

    @Override
    public Gender convert(String source) {
        if (source.trim().isEmpty()) {
            return null;
        }
        return Gender.fromString(source);
    }
}