package com.jobseek.controller;

import com.jobseek.model.MatchResult;
import com.jobseek.repository.MatchResultRepository;
import com.jobseek.service.MatchService;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import com.jobseek.model.Cv;
import com.jobseek.model.JobDescription;
import com.jobseek.repository.CvRepository;
import com.jobseek.qdrant.QdrantMatchService;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;
    private final MatchResultRepository matchResultRepository;
    private final CvRepository cvRepo;
    private final QdrantMatchService qdrantMatchService;

    public MatchController(
        MatchService matchService,
        MatchResultRepository matchResultRepository,
        CvRepository cvRepo,
        QdrantMatchService qdrantMatchService
    ) {
        this.matchService = matchService;
        this.matchResultRepository = matchResultRepository;
        this.cvRepo = cvRepo;
        this.qdrantMatchService = qdrantMatchService;
    }

    // Run a new match against all job descriptions using a specific model
    @GetMapping("/{userId}")
    public List<MatchResult> matchNow(
            @PathVariable String userId,
            @RequestParam(defaultValue = "llama3.2") String modelName
    ) throws Exception {
        Cv cv = cvRepo.findById(userId).orElseThrow(() -> new RuntimeException("CV not found"));
        List<JobDescription> relevantJobs = qdrantMatchService.findTopRelevantJobs(cv);
        return matchService.scoreMatchesWithOllama(userId, modelName, cv, relevantJobs);
    }

    // Fetch last saved matches for a user
    @GetMapping("/last/{userId}")
    public List<MatchResult> getLastMatchResults(@PathVariable String userId) {
        return matchResultRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    // Pagination
    @GetMapping("/last/{userId}/paged")
    public Page<MatchResult> getPaginatedMatchResults(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return matchResultRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @PostMapping("/cv")
    public List<MatchResult> matchCv(@RequestBody Cv cv) throws Exception {
        Cv saved = cvRepo.save(cv);
        List<JobDescription> relevantJobs = qdrantMatchService.findTopRelevantJobs(saved);
        return matchService.scoreMatchesWithOllama(saved.getId(), "llama3.2", saved, relevantJobs);
    }

    @GetMapping("/recent")
    public List<MatchResult> getRecentMatches(
        @RequestParam String userId,
        @RequestParam int days
    ) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return matchResultRepository.findByUserIdAndTimestampAfter(userId, cutoff.toString());
    }
}
