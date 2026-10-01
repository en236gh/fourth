package com.backend.fourth.face.client;

public enum FacePurpose {
    /** Controlled photo: exactly one face must be visible. */
    ENROLMENT,
    /** Exam-room capture: the dominant face is used if others are clearly in the background. */
    VERIFICATION
}
