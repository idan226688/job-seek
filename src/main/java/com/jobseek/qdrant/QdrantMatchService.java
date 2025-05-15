package com.jobseek.qdrant;

import com.jobseek.model.Cv;
import com.jobseek.model.JobDescription;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@ConditionalOnProperty(name = "qdrant.enabled", havingValue = "true", matchIfMissing = true)
public class QdrantMatchService {

    private final QdrantClient qdrantClient;
    private final QdrantEmbeddingService embeddingService;
    private static final Logger logger = LoggerFactory.getLogger(QdrantMatchService.class);

    public QdrantMatchService(QdrantClient qdrantClient, QdrantEmbeddingService embeddingService) {
        this.qdrantClient = qdrantClient;
        this.embeddingService = embeddingService;
    }

public List<JobDescription> findTopRelevantJobs(Cv cv) throws Exception {
    float[] cvEmbedding = embeddingService.embed(cv.getContent());

    Map<Integer, Float> idScoreMap = qdrantClient.searchWithScores(cvEmbedding);
    logger.info("Qdrant returned: {}", idScoreMap);

    List<Integer> filteredIds = idScoreMap.entrySet().stream()
        .filter(e -> e.getValue() > 0.3f)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
        logger.info("Filtered job IDs: {}", filteredIds);
    return qdrantClient.fetchJobDescriptionsByIds(filteredIds);
}

    public void indexJobDescription(JobDescription job) throws Exception {
        float[] embedding = embeddingService.embed(job.getContent());
        qdrantClient.upsertPoint("job_descriptions", job.getId(), embedding);    
    }
}
