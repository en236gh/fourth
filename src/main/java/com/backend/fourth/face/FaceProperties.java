package com.backend.fourth.face;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.face")
@Getter
@Setter
public class FaceProperties {
    private String serviceUrl = "http://localhost:8000";
    /** Shared secret sent as X-Face-Service-Key; leave blank when the face service has none. */
    private String apiKey = "";
    /** Cosine similarity at or above which the student is verified automatically. */
    private double matchThreshold = 0.45;
    /** Between this and matchThreshold the invigilator must confirm; below it the attempt is rejected. */
    private double reviewThreshold = 0.30;
    private int connectTimeoutMs = 2_000;
    private int readTimeoutMs = 15_000;
}
