package com.backend.fourth.face;

import com.backend.fourth.face.service.FaceMatcher;
import com.backend.fourth.face.service.FaceMatcher.Decision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaceMatcherTest {
    private final FaceMatcher matcher = new FaceMatcher(new FaceProperties());

    @Test
    void cosineSimilarityIgnoresVectorLength() {
        assertEquals(1.0, FaceMatcher.similarity(new float[]{1, 2, 3}, new float[]{2, 4, 6}), 1e-9);
        assertEquals(0.0, FaceMatcher.similarity(new float[]{1, 0}, new float[]{0, 5}), 1e-9);
        assertEquals(-1.0, FaceMatcher.similarity(new float[]{1, 1}, new float[]{-1, -1}), 1e-9);
        assertEquals(0.0, FaceMatcher.similarity(new float[]{0, 0}, new float[]{1, 1}), 1e-9);
    }

    @Test
    void mismatchedEmbeddingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> FaceMatcher.similarity(new float[]{1}, new float[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> FaceMatcher.similarity(new float[0], new float[0]));
    }

    @Test
    void defaultThresholdsSplitIntoThreeBands() {
        assertEquals(Decision.MATCH, matcher.decide(0.80));
        assertEquals(Decision.MATCH, matcher.decide(0.45));
        assertEquals(Decision.REVIEW, matcher.decide(0.449));
        assertEquals(Decision.REVIEW, matcher.decide(0.30));
        assertEquals(Decision.REJECT, matcher.decide(0.299));
        assertEquals(Decision.REJECT, matcher.decide(-0.2));
    }
}
