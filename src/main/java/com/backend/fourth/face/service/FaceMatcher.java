package com.backend.fourth.face.service;

import com.backend.fourth.face.FaceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FaceMatcher {
    public enum Decision { MATCH, REVIEW, REJECT }

    private final FaceProperties properties;

    /** Cosine similarity in [-1, 1]; ArcFace embeddings of the same person usually score well above 0.45. */
    public static double similarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException("Face embeddings must have the same non-zero length");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public Decision decide(double similarity) {
        if (similarity >= properties.getMatchThreshold()) return Decision.MATCH;
        if (similarity >= properties.getReviewThreshold()) return Decision.REVIEW;
        return Decision.REJECT;
    }

    public double matchThreshold() {
        return properties.getMatchThreshold();
    }

    public double reviewThreshold() {
        return properties.getReviewThreshold();
    }
}
