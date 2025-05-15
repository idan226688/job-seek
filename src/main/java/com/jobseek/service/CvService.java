package com.jobseek.service;

import com.jobseek.model.Cv;
import com.jobseek.repository.CvRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class CvService {

    private final CvRepository cvRepository;

    public CvService(CvRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    public void addCv(Cv cv) {
        cvRepository.save(cv);
    }

    public Collection<Cv> getAll() {
        return cvRepository.findAll();
    }
}
