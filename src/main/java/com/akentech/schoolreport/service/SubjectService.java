package com.akentech.schoolreport.service;

import com.akentech.schoolreport.model.Subject;
import com.akentech.schoolreport.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<Subject> getAll() {
        return subjectRepository.findAll();
    }

    public Subject save(Subject s) {
        Subject saved = subjectRepository.save(s);
        log.info("Saved subject: {} (coeff={})", saved.getName(), saved.getCoefficient());
        return saved;
    }

    public void delete(Long id) {
        subjectRepository.deleteById(id);
        log.info("Deleted subject id={}", id);
    }
}
