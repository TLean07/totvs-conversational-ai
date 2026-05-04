package com.tlean07.fiap.challenge.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    public void check(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Deque<Instant> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (bucket) {
            Instant threshold = now.minus(window);
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(threshold)) {
                bucket.removeFirst();
            }

            if (bucket.size() >= maxRequests) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas. Aguarde alguns instantes e tente novamente."
                );
            }

            bucket.addLast(now);
        }
    }
}
