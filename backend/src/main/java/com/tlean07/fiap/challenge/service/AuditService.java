package com.tlean07.fiap.challenge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger("security-audit");

    public void record(String event, String actorId, String targetId, String outcome) {
        log.info("event={} actorId={} targetId={} outcome={}",
                safe(event), safe(actorId), safe(targetId), safe(outcome));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
