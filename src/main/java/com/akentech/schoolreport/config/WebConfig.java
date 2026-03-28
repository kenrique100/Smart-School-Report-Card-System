package com.akentech.schoolreport.config;

import com.akentech.schoolreport.util.ParameterUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Additional MVC configuration (static resources, converters, etc.)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources from /static (classpath)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {

        // String → Long
        registry.addConverter(new Converter<String, Long>() {
            @Override
            public Long convert(String source) {
                return ParameterUtils.safeParseLong(source);
            }
        });

        // String → Integer
        registry.addConverter(new Converter<String, Integer>() {
            @Override
            public Integer convert(String source) {
                return ParameterUtils.safeParseInteger(source);
            }
        });

        // String → Double
        registry.addConverter(new Converter<String, Double>() {
            @Override
            public Double convert(String source) {
                return ParameterUtils.safeParseDouble(source);
            }
        });

        // String → Boolean
        registry.addConverter(new Converter<String, Boolean>() {
            @Override
            public Boolean convert(String source) {
                return ParameterUtils.safeParseBoolean(source);
            }
        });
    }
}
