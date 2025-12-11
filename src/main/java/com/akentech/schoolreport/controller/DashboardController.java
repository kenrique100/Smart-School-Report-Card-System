package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.ClassRoom;
import com.akentech.schoolreport.model.DashboardStatistics;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final StatisticsService statisticsService;
    private final ClassRoomRepository classRoomRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            // Get complete dashboard statistics including distributions
            DashboardStatistics stats = statisticsService.getCompleteDashboardStatistics();
            List<ClassRoom> classes = classRoomRepository.findAll();

            model.addAttribute("statistics", stats);
            model.addAttribute("classes", classes);

            log.info("Dashboard loaded successfully with {} classes", classes.size());
            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("statistics", DashboardStatistics.builder().build());
            model.addAttribute("classes", List.of());
            model.addAttribute("error", "Unable to load dashboard statistics: " + e.getMessage());
            return "dashboard";
        }
    }
}