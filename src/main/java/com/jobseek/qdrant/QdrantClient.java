package com.jobseek.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobseek.model.JobDescription;
import com.jobseek.repository.JobRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qdrant.enabled", havingValue = "true", matchIfMissing = true)
public class QdrantClient {
    private static final Logger logger = LoggerFactory.getLogger(QdrantClient.class);

    @Value("${qdrant.url}")
    private final String qdrantUrl = "http://qdrant:6333";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean upsertPoint(String collectionName, Integer id, float[] vector) throws Exception {
        StringBuilder vectorJson = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            vectorJson.append(vector[i]);
            if (i < vector.length - 1) vectorJson.append(",");
        }
        vectorJson.append("]");
    
        String json = String.format("""
        {
          "points": [
            {
              "id": %d,
              "vector": %s
            }
          ]
        }
        """, id, vectorJson);
    
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(qdrantUrl + "/collections/" + collectionName + "/points?wait=true"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
    
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Upsert response code: {}", response.statusCode());
        logger.info("Upsert response body: {}", response.body());
    
        return response.statusCode() == 200;
    }

    public String searchSimilarVectors(String collectionName, float[] vector, int topK) throws Exception {
        StringBuilder vectorJson = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            vectorJson.append(vector[i]);
            if (i < vector.length - 1) vectorJson.append(",");
        }
        vectorJson.append("]");

        String json = String.format("""
        {
            "vector": %s,
            "top": %d
        }
        """, vectorJson, topK);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(qdrantUrl + "/collections/" + collectionName + "/points/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Search response: {}", response.body());
        return response.body();
    }

    public Map<Integer, Float> searchWithScores(float[] vector) throws Exception {
        String responseJson = searchSimilarVectors("job_descriptions", vector, 100);
        Map<Integer, Float> scores = new HashMap<>();
    
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode resultArray = root.path("result");
        for (JsonNode node : resultArray) {
            float score = node.path("score").floatValue();
            int id = node.path("id").asInt();
            scores.put(id, score);
        }
    
        return scores;
    }

@Autowired
private JobRepository jobRepository;

public List<JobDescription> fetchJobDescriptionsByIds(List<Integer> ids) {
    return jobRepository.findAllById(ids);
}
    private HttpRequest buildCollectionRequest(String collectionName, int vectorSize) {
        String json = String.format("""
        {
          "vectors": {
            "size": %d,
            "distance": "Cosine"
          }
        }
        """, vectorSize);
    
        return HttpRequest.newBuilder()
                .uri(URI.create(qdrantUrl + "/collections/" + collectionName))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
    
    public boolean createCollection(String collectionName, int vectorSize) throws Exception {
        HttpRequest request = buildCollectionRequest(collectionName, vectorSize);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Create collection response: {}", response.body());
        return response.statusCode() == 200;
    }
    
    public void createCollectionIfNotExists(String collectionName, int vectorSize) throws Exception {
        HttpRequest request = buildCollectionRequest(collectionName, vectorSize);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Collection create/update response: {}", response.body());
    }

    public void clearCollection(String collectionName) throws Exception {
        String json = "{\"filter\": {\"must\": []}}";
    
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(qdrantUrl + "/collections/" + collectionName + "/points/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Clear collection response: {}", response.body());
    }
}