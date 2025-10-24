package com.akentech.schoolreport.repository;

import com.akentech.schoolreport.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByIsActiveTrueOrderByCreatedDateDesc();
    long countByIsActiveTrue();
    long countByIsActive(boolean isActive);
}