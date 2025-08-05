package com.jobseek.controller;

import com.jobseek.model.JobDescription;
import com.jobseek.qdrant.QdrantMatchService;
import com.jobseek.repository.JobRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job")
public class JobController {

    private final JobRepository jobRepository;
    private final QdrantMatchService qdrantMatchService;

    @Autowired
    public JobController(JobRepository jobRepository, @Autowired(required = false) QdrantMatchService qdrantMatchService) {
        this.jobRepository = jobRepository;
        this.qdrantMatchService = qdrantMatchService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Integer id) {
        if (!jobRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        jobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add")
    public String addJob(@RequestBody JobDescription job) {
        jobRepository.save(job);
        if (qdrantMatchService != null) {
            try {
                qdrantMatchService.indexJobDescription(job);
                return "Job saved and indexed in Qdrant.";
            } catch (Exception e) {
                return "Job saved but failed to index in Qdrant: " + e.getMessage();
            }
        }
        return "Job saved. Qdrant integration is disabled.";
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<JobDescription> getJobById(@PathVariable Integer id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs")
    public List<JobDescription> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/search")
    public List<JobDescription> searchJobs(@RequestParam String keyword) {
        return jobRepository.findByContentContainingIgnoreCase(keyword);
    }
}