package com.jobseek.qdrant;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qdrant.enabled", havingValue = "true", matchIfMissing = true)
public class QdrantInitializer {

    private static final Logger logger = LoggerFactory.getLogger(QdrantInitializer.class);
    private final QdrantClient qdrantClient;

    public QdrantInitializer() {
        this.qdrantClient = new QdrantClient();
    }

    @PostConstruct
    public void init() {
        try {
            boolean created = qdrantClient.createCollection("job_descriptions", 384); // use your embedding size
            logger.info("Qdrant collection 'job_descriptions' created: {}", created);
        } catch (Exception e) {
            logger.error("[QdrantInitializer] Failed to create collection", e);
        }
    }
}