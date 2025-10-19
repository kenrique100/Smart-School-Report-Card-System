package com.akentech.schoolreport.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index"; // Thymeleaf template src/main/resources/templates/index.html
    }
}
