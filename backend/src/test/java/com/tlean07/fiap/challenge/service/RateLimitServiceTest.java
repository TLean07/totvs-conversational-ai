package com.tlean07.fiap.challenge.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void blocksRequestsAboveLimitInsideWindow() {
        RateLimitService service = new RateLimitService();

        service.check("login:test", 2, Duration.ofMinutes(1));
        service.check("login:test", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> service.check("login:test", 2, Duration.ofMinutes(1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
    }
}
