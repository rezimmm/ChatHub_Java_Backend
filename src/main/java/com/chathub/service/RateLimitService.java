package com.chathub.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> messageBuckets = new ConcurrentHashMap<>();

    public Bucket resolveGeneralBucket(String ip) {
        return generalBuckets.computeIfAbsent(ip, this::newGeneralBucket);
    }

    public Bucket resolveAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, this::newAuthBucket);
    }

    public Bucket resolveUploadBucket(String ip) {
        return uploadBuckets.computeIfAbsent(ip, this::newUploadBucket);
    }

    public Bucket resolveMessageBucket(String userId) {
        return messageBuckets.computeIfAbsent(userId, this::newMessageBucket);
    }

    private Bucket newGeneralBucket(String ip) {
        // 100 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket newAuthBucket(String ip) {
        // 5 requests per minute for login/register
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket newUploadBucket(String ip) {
        // 10 uploads per 5 minutes
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(5))))
                .build();
    }

    private Bucket newMessageBucket(String userId) {
        // 60 messages per minute (1 per second average)
        return Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                .build();
    }
}
