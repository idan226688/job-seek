package com.jobseek.service;

public interface EmbeddingService {
    float[] embed(String text) throws Exception;
}
