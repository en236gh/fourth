"""Face detection + embedding, independent of the HTTP layer.

The service never decides whether two faces match. It only turns one photo into one
L2-normalised embedding; the Spring Boot backend owns templates, thresholds and the
attendance decision.
"""
from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import Protocol, Sequence

import cv2
import numpy as np

EMBEDDING_SIZE = 512
MAX_IMAGE_SIDE = 1280


class Purpose(str, Enum):
    # Enrolment photos are taken in a controlled setting: exactly one face is allowed.
    ENROLMENT = "ENROLMENT"
    # Exam-room captures may catch people in the background: the dominant face is used.
    VERIFICATION = "VERIFICATION"


class FaceError(Exception):
    """A photo that cannot produce a usable embedding. `code` is stable for clients."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


@dataclass(frozen=True)
class DetectedFace:
    bbox: tuple[float, float, float, float]  # x1, y1, x2, y2 in pixels
    det_score: float
    embedding: np.ndarray

    @property
    def width(self) -> float:
        return self.bbox[2] - self.bbox[0]

    @property
    def area(self) -> float:
        return max(0.0, self.bbox[2] - self.bbox[0]) * max(0.0, self.bbox[3] - self.bbox[1])


@dataclass(frozen=True)
class FaceResult:
    embedding: np.ndarray
    det_score: float
    bbox: tuple[float, float, float, float]
    face_count: int


class FaceEngine(Protocol):
    model_name: str

    def detect(self, image_bgr: np.ndarray) -> Sequence[DetectedFace]: ...


class InsightFaceEngine:
    """InsightFace model pack (default buffalo_l: SCRFD-10G detector + ArcFace R50).

    Weights download to ~/.insightface/models on first start (~280 MB) and are
    licensed for non-commercial research use.
    """

    def __init__(self, model_pack: str = "buffalo_l", det_size: int = 640):
        from insightface.app import FaceAnalysis

        self.model_name = model_pack
        self._app = FaceAnalysis(
            name=model_pack,
            allowed_modules=["detection", "recognition"],
            providers=["CPUExecutionProvider"],
        )
        self._app.prepare(ctx_id=-1, det_size=(det_size, det_size))

    def detect(self, image_bgr: np.ndarray) -> Sequence[DetectedFace]:
        return [
            DetectedFace(
                bbox=tuple(float(v) for v in face.bbox),
                det_score=float(face.det_score),
                embedding=np.asarray(face.normed_embedding, dtype=np.float32),
            )
            for face in self._app.get(image_bgr)
        ]


def decode_image(data: bytes) -> np.ndarray:
    if not data:
        raise FaceError("INVALID_IMAGE", "The uploaded image is empty.")
    image = cv2.imdecode(np.frombuffer(data, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        raise FaceError("INVALID_IMAGE", "The upload is not a readable JPEG or PNG image.")
    height, width = image.shape[:2]
    scale = MAX_IMAGE_SIDE / max(height, width)
    if scale < 1:
        image = cv2.resize(image, (round(width * scale), round(height * scale)), interpolation=cv2.INTER_AREA)
    return image


def extract_face(
    engine: FaceEngine,
    image_bgr: np.ndarray,
    purpose: Purpose,
    min_face_px: int,
    min_det_score: float,
) -> FaceResult:
    faces = [f for f in engine.detect(image_bgr) if f.det_score >= min_det_score]
    if not faces:
        raise FaceError("NO_FACE", "No face was detected. Face the camera in good light and try again.")

    faces.sort(key=lambda f: f.area, reverse=True)
    primary = faces[0]
    if len(faces) > 1:
        if purpose is Purpose.ENROLMENT:
            raise FaceError("MULTIPLE_FACES", "More than one face is visible. Enrolment photos must show one person.")
        # A second face nearly as large as the first means we cannot tell who is being verified.
        if faces[1].area >= 0.5 * primary.area:
            raise FaceError("MULTIPLE_FACES", "Several faces are close to the camera. Capture only the student.")

    if primary.width < min_face_px:
        raise FaceError("FACE_TOO_SMALL", "The face is too far from the camera. Move closer and try again.")

    embedding = np.asarray(primary.embedding, dtype=np.float32).reshape(-1)
    if embedding.shape[0] != EMBEDDING_SIZE:
        raise RuntimeError(f"Model returned a {embedding.shape[0]}-d embedding, expected {EMBEDDING_SIZE}")
    norm = float(np.linalg.norm(embedding))
    if norm == 0 or not np.isfinite(norm):
        raise FaceError("NO_FACE", "The face could not be encoded. Try again with a clearer photo.")

    return FaceResult(
        embedding=embedding / norm,
        det_score=primary.det_score,
        bbox=primary.bbox,
        face_count=len(faces),
    )
