package com.jobseek.service;

import com.jobseek.model.JobDescription;
import com.jobseek.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void addJob(JobDescription job) {
        logger.info("Saving job to DB: {}", job.getId());
        jobRepository.save(job);
    }

    public List<JobDescription> getAll() {
        return jobRepository.findAll();
    }

    public void deleteJobById(int id) {
        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job with ID " + id + " does not exist");
        }
        jobRepository.deleteById(id);
    }
}