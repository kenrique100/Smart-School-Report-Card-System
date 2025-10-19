package com.akentech.schoolreport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
@Slf4j
public class SchoolReportCardSystemApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SchoolReportCardSystemApplication.class);
        app.run(args);
        // Attempt to open browser to app root - optional and non-fatal
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:8080"));
            }
        } catch (Exception ex) {
            log.warn("Could not open default browser automatically: {}", ex.getMessage());
        }
    }
}
