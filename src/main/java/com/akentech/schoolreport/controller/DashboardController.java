package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final StatisticsService statisticsService;

    @GetMapping("/")
    public String home(Model model) {
        Map<String, Long> stats = statisticsService.getDashboardStatistics();
        model.addAttribute("statistics", stats);
        log.info("Loading dashboard with statistic: {}", stats);
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Long> stats = statisticsService.getDashboardStatistics();
        model.addAttribute("statistics", stats);
        log.info("Loading dashboard with statistics: {}", stats);
        return "dashboard";
    }
}