package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Attendance;
import com.akentech.schoolreport.repository.AttendanceRepository;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.repository.StudentRepository;
import com.akentech.schoolreport.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final AttendanceRepository attendanceRepository;

    @GetMapping
    public String viewAttendance(Model model) {
        model.addAttribute("attendanceList", attendanceRepository.findAll());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("classRooms", classRoomRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "attendance";
    }

    @GetMapping("/take")
    public String showTakeAttendanceForm(Model model) {
        model.addAttribute("attendance", new Attendance());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("classRooms", classRoomRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "take-attendance";
    }

    @PostMapping("/take")
    public String takeAttendance(@ModelAttribute Attendance attendance) {
        attendanceService.saveAttendance(attendance);
        return "redirect:/attendance";
    }

    @GetMapping("/delete/{id}")
    public String deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return "redirect:/attendance";
    }
}