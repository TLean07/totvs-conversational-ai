package com.tlean07.fiap.challenge.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OriginValidationService {

    private final Set<String> allowedOrigins;

    public OriginValidationService(@Value("${cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.allowedOrigins = Stream.of(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toSet());
    }

    public void validate(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            origin = extractOrigin(request.getHeader("Referer"));
        }
        if (origin == null || origin.isBlank()) {
            return;
        }
        if (!allowedOrigins.contains(origin)) {
            throw new AccessDeniedException("Origem nao permitida");
        }
    }

    private String extractOrigin(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
