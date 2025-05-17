package com.jobseek.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jobseek.model.Cv;
import com.jobseek.model.JobDescription;
import com.jobseek.model.MatchResult;
import com.jobseek.qdrant.QdrantMatchService;
import com.jobseek.repository.CvRepository;
import com.jobseek.repository.MatchResultRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    private final CvRepository cvRepository;
    private final MatchResultRepository matchResultRepository;
    @Autowired(required = false)
    private final QdrantMatchService qdrantMatchService;
    private final MatchCacheService matchCacheService;
    @Value("${embedding.url}")
    private String embeddingBaseUrl;

    public MatchService(
        CvRepository cvRepository,
        MatchResultRepository matchResultRepository,
        QdrantMatchService qdrantMatchService,
        MatchCacheService matchCacheService
    ) {
        this.cvRepository = cvRepository;
        this.matchResultRepository = matchResultRepository;
        this.qdrantMatchService = qdrantMatchService;
        this.matchCacheService = matchCacheService;
    }

    public List<MatchResult> scoreMatchesWithOllama(String userId, String modelName, Cv userCv, List<JobDescription> jobs) throws Exception {
        List<MatchResult> results = new ArrayList<>();
        logger.info("Number of jobs to score: {}", jobs.size());

        for (JobDescription job : jobs) {
            Double cachedScore = matchCacheService.getScore(userId, job.getId().toString(), modelName);
            logger.info("cachedScore = {}", cachedScore);

            if (cachedScore != null) 
            {
                logger.info("Using cached score from Redis for CV '{}', Job '{}': score = {}", userId, job.getId(), cachedScore.intValue());
                MatchResult cached = new MatchResult(userId, job.getId(), cachedScore.intValue(), LocalDateTime.now().toString());
                results.add(cached);
                continue;
            }           
            logger.info("Running Ollama scoring for user: {}", userId);
            String prompt = """
                You are a job matching assistant.

                Given a CV and a Job Description, evaluate how well the candidate fits the job based on these factors:
                - The type of job (e.g., Software Engineer, Data Scientist).
                - The years of required experience.
                - The required technical and domain knowledge (e.g., Java, Spring Boot, Python, Machine Learning).
                - The required education (degree level, fields of study).
                - If the CV is missing mandatory requirements (such as degree, years of experience, critical skills), 
                you must score it lower than 50.

                Instructions:
                - Only consider information explicitly stated in the CV.
                - Be strict: missing critical experience, skills, or education must lower the score.
                - Focus mainly on matching the job's required skills and experience, not on soft skills.
                - Score the CV from 0 to 100 based on the match quality.
                - Output the result only in this format: score = <number>. Nothing else.

                Input:

                CV:
                %s

                Job Description:
                %s
                """.formatted(userCv.getContent(), job.getContent());

            String escapedPrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            String body = String.format("""
                {
                    "model": "%s",
                    "prompt": "%s"
                }
                """, modelName, escapedPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingBaseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            StringBuilder fullText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonObject jsonLine = JsonParser.parseString(line).getAsJsonObject();
                    if (jsonLine.has("response")) {
                        fullText.append(jsonLine.get("response").getAsString());
                    }
                } catch (JsonSyntaxException e) {
                    logger.warn("Invalid JSON line from Ollama: {}", line);
                }
            }

            int score = extractScore(fullText.toString());
            logger.info("Job ID: {}, Score: {}", job.getId(), score);
            logger.debug("Scoring with Ollama for job: {}", job.getId());
            logger.debug("Prompt sent to Ollama:\n{}", prompt);
            logger.debug("Ollama raw response: {}", fullText);
            logger.debug("Extracted score: {}", score);

            MatchResult result = new MatchResult(userId, job.getId(), score, LocalDateTime.now().toString());
            matchResultRepository.save(result);
            logger.info("Caching result in Redis: CV '{}', Job '{}', Model '{}', Score: {}", userId, job.getId(), modelName, score);
            matchCacheService.saveScore(userId, job.getId().toString(), modelName, score);
            results.add(result);
        }

        return results;
    }

    private int extractScore(String response) {
        String lower = response.toLowerCase();

        int index = lower.indexOf("score =");
        if (index != -1) {
            int start = index + 7;
            while (start < lower.length() && !Character.isDigit(lower.charAt(start))) {
                start++;
            }

            int end = start;
            while (end < lower.length() && Character.isDigit(lower.charAt(end))) {
                end++;
            }

            String number = lower.substring(start, end);
            try {
                return Integer.parseInt(number);
            } catch (NumberFormatException e) {
                logger.warn("Could not parse score from: {}", number);
            }
        }

        logger.warn("Score not found in Ollama response: {}", response);
        return 0;
    }

    public List<MatchResult> matchJobsForUser(String userId, String modelName) throws Exception {
        Cv cv = cvRepository.findById(userId).orElseThrow(() -> new RuntimeException("CV not found"));
        List<JobDescription> relevantJobs = qdrantMatchService.findTopRelevantJobs(cv);
        return scoreMatchesWithOllama(cv.getId(), modelName, cv, relevantJobs);
    }
}
