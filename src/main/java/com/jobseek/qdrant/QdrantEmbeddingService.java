package com.jobseek.qdrant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jobseek.model.JobDescription;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@ConditionalOnProperty(name = "qdrant.enabled", havingValue = "true", matchIfMissing = true)
public class QdrantEmbeddingService {

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${embedding.url}")
    private String embeddingUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    public void upsertJobEmbeddings(List<JobDescription> jobs) throws Exception {
        JsonArray points = new JsonArray();

        for (JobDescription job : jobs) {
            float[] embedding = embed(job.getContent());

            JsonObject point = new JsonObject();
            point.addProperty("id", job.getId().intValue());

            JsonArray vectorArray = new JsonArray();
            for (float value : embedding) {
                vectorArray.add(value);
            }
            point.add("vector", vectorArray);

            JsonObject payload = new JsonObject();
            payload.addProperty("content", job.getContent());
            point.add("payload", payload);

            points.add(point);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.add("points", points);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(qdrantUrl + "/collections/job_descriptions/points?wait=true"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to upsert points to Qdrant: " + response.body());
        }
    }

    public float[] embed(String text) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "all-minilm");
        requestBody.addProperty("prompt", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(embeddingUrl + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get embedding: " + response.body());
        }

        JsonObject json = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray embeddingArray = json.getAsJsonArray("embedding");

        float[] result = new float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            result[i] = embeddingArray.get(i).getAsFloat();
        }

        return result;
    }
}
