package com.backend.fourth.face.client;

/** The photo itself is unusable (no face, several faces, too small, unreadable). The user can retake it. */
public class FacePhotoRejectedException extends IllegalArgumentException {
    private final String code;

    public FacePhotoRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
