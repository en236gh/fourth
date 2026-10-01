package com.backend.fourth.face.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FaceEmbedding(String model, float[] embedding, double detScore, int faceCount) {
}
