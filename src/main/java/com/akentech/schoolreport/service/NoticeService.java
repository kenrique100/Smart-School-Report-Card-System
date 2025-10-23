package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Notice;
import com.akentech.schoolreport.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<Notice> getAllNotices() {
        return noticeRepository.findAll();
    }

    public List<Notice> getActiveNotices() {
        return noticeRepository.findByIsActiveTrueOrderByCreatedDateDesc();
    }

    public Optional<Notice> getNoticeById(Long id) {
        return noticeRepository.findById(id);
    }

    @Transactional
    public Notice saveNotice(Notice notice) {
        if (notice.getCreatedDate() == null) {
            notice.setCreatedDate(LocalDateTime.now());
        }
        if (notice.getIsActive() == null) {
            notice.setIsActive(true);
        }

        Notice saved = noticeRepository.save(notice);

        String teacherName = "System";
        if (saved.getTeacher() != null) {
            teacherName = saved.getTeacher().getFirstName() + " " + saved.getTeacher().getLastName();
        }

        log.info("Saved notice: {} by {}", saved.getTitle(), teacherName);
        return saved;
    }

    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
        log.info("Deleted notice id: {}", id);
    }

    @Transactional
    public Notice toggleNoticeStatus(Long id) {
        Optional<Notice> noticeOpt = noticeRepository.findById(id);
        if (noticeOpt.isPresent()) {
            Notice notice = noticeOpt.get();
            notice.setIsActive(!notice.getIsActive());
            Notice saved = noticeRepository.save(notice);
            log.info("Toggled notice status: {} - {}", saved.getTitle(), saved.getIsActive());
            return saved;
        }
        return null;
    }
}