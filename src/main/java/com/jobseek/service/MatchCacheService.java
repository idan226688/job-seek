package com.jobseek.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MatchCacheService {

    private final StringRedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(MatchCacheService.class);
    @Autowired
    public MatchCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info("MatchCacheService initialized");
    }

    // Generates a unique key based on cvId, jobId, and model
    private String generateKey(String cvId, String jobId, String model) {
        return "match:" + cvId + ":" + jobId + ":" + model;
    }

    public void saveScore(String cvId, String jobId, String model, double score) {
        String key = generateKey(cvId, jobId, model);
        redisTemplate.opsForValue().set(key, String.valueOf(score), Duration.ofHours(12)); // Cache for 12 hours
    }

    public Double getScore(String cvId, String jobId, String model) {
        String key = generateKey(cvId, jobId, model);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Double.parseDouble(value) : null;
    }
}