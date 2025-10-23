package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.model.Notice;
import com.akentech.schoolreport.repository.TeacherRepository;
import com.akentech.schoolreport.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;
    private final TeacherRepository teacherRepository;

    @GetMapping
    public String listNotices(Model model) {
        model.addAttribute("notices", noticeService.getAllNotices());
        model.addAttribute("teachers", teacherRepository.findAll());
        return "notices";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("notice", new Notice());
        model.addAttribute("teachers", teacherRepository.findAll());
        return "add-notice";
    }

    @PostMapping("/add")
    public String addNotice(@ModelAttribute Notice notice) {
        noticeService.saveNotice(notice);
        return "redirect:/notices";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        noticeService.getNoticeById(id).ifPresent(notice -> model.addAttribute("notice", notice));
        model.addAttribute("teachers", teacherRepository.findAll());
        return "edit-notice";
    }

    @PostMapping("/update")
    public String updateNotice(@ModelAttribute Notice notice) {
        noticeService.saveNotice(notice);
        return "redirect:/notices";
    }

    @GetMapping("/toggle/{id}")
    public String toggleNotice(@PathVariable Long id) {
        noticeService.toggleNoticeStatus(id);
        return "redirect:/notices";
    }

    @GetMapping("/delete/{id}")
    public String deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/notices";
    }
}