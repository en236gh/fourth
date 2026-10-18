package com.backend.fourth.incident.dto;

import java.time.Instant;

public record EvidenceDownloadUrlResponse(String url, Instant expiresAt) {
}