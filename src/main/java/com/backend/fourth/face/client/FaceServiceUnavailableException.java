package com.backend.fourth.face.client;

/** The face service could not be reached or answered unexpectedly. Callers should fall back to QR check-in. */
public class FaceServiceUnavailableException extends RuntimeException {
    public FaceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
