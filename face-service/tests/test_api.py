import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.engine import DetectedFace
from app.main import Settings, create_app


def face(width: float, score: float = 0.9, seed: int = 1) -> DetectedFace:
    vector = np.random.default_rng(seed).normal(size=512).astype(np.float32) * 3  # deliberately not normalised
    return DetectedFace(bbox=(10.0, 10.0, 10.0 + width, 10.0 + width * 1.2), det_score=score, embedding=vector)


class FakeEngine:
    model_name = "fake"

    def __init__(self):
        self.faces: list[DetectedFace] = []
        self.seen_shape = None

    def detect(self, image_bgr):
        self.seen_shape = image_bgr.shape
        return self.faces


def png(width: int = 200, height: int = 200) -> bytes:
    ok, encoded = cv2.imencode(".png", np.full((height, width, 3), 127, dtype=np.uint8))
    assert ok
    return encoded.tobytes()


@pytest.fixture
def engine():
    return FakeEngine()


def client_for(engine, **overrides):
    settings = Settings(**{"api_key": "", "min_face_px": 80, "min_det_score": 0.6, "max_upload_bytes": 1024 * 1024, **overrides})
    return TestClient(create_app(settings, lambda _: engine))


def post_embed(client, data=None, purpose=None, headers=None):
    form = {"purpose": purpose} if purpose else {}
    return client.post("/embed", files={"image": ("face.png", data if data is not None else png(), "image/png")},
                       data=form, headers=headers or {})


def test_health_reports_model(engine):
    with client_for(engine) as client:
        assert client.get("/health").json() == {"status": "ok", "model": "fake"}


def test_single_face_returns_unit_length_embedding(engine):
    engine.faces = [face(120)]
    with client_for(engine) as client:
        response = post_embed(client, purpose="ENROLMENT")
    assert response.status_code == 200
    body = response.json()
    assert body["model"] == "fake"
    assert body["faceCount"] == 1
    assert len(body["embedding"]) == 512
    assert np.linalg.norm(body["embedding"]) == pytest.approx(1.0, abs=1e-4)


def test_no_face_is_422_with_stable_code(engine):
    with client_for(engine) as client:
        response = post_embed(client)
    assert response.status_code == 422
    assert response.json()["code"] == "NO_FACE"


def test_low_confidence_detections_are_ignored(engine):
    engine.faces = [face(150, score=0.3)]
    with client_for(engine) as client:
        assert post_embed(client).json()["code"] == "NO_FACE"


def test_enrolment_rejects_any_second_face(engine):
    engine.faces = [face(150), face(40, seed=2)]
    with client_for(engine) as client:
        response = post_embed(client, purpose="ENROLMENT")
    assert response.status_code == 422
    assert response.json()["code"] == "MULTIPLE_FACES"


def test_verification_uses_dominant_face_when_background_face_is_small(engine):
    engine.faces = [face(40, seed=2), face(150, seed=1)]
    with client_for(engine) as client:
        body = post_embed(client, purpose="VERIFICATION").json()
    expected = face(150, seed=1).embedding
    assert body["faceCount"] == 2
    assert np.dot(body["embedding"], expected / np.linalg.norm(expected)) == pytest.approx(1.0, abs=1e-4)


def test_verification_rejects_two_similar_sized_faces(engine):
    engine.faces = [face(150), face(140, seed=2)]
    with client_for(engine) as client:
        assert post_embed(client, purpose="VERIFICATION").json()["code"] == "MULTIPLE_FACES"


def test_small_face_is_rejected(engine):
    engine.faces = [face(50)]
    with client_for(engine) as client:
        assert post_embed(client).json()["code"] == "FACE_TOO_SMALL"


def test_non_image_upload_is_400(engine):
    with client_for(engine) as client:
        response = post_embed(client, data=b"definitely not an image")
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_IMAGE"


def test_oversized_upload_is_400(engine):
    with client_for(engine, max_upload_bytes=100) as client:
        response = post_embed(client)
    assert response.status_code == 400
    assert response.json()["code"] == "IMAGE_TOO_LARGE"


def test_large_images_are_downscaled_before_detection(engine):
    engine.faces = [face(150)]
    with client_for(engine) as client:
        post_embed(client, data=png(4000, 3000))
    assert max(engine.seen_shape[:2]) == 1280


def test_unknown_purpose_is_rejected(engine):
    with client_for(engine) as client:
        assert post_embed(client, purpose="SURVEILLANCE").status_code == 422


def test_api_key_is_enforced_when_configured(engine):
    engine.faces = [face(120)]
    with client_for(engine, api_key="s3cret") as client:
        assert post_embed(client).status_code == 401
        assert post_embed(client, headers={"X-Face-Service-Key": "wrong"}).status_code == 401
        assert post_embed(client, headers={"X-Face-Service-Key": "s3cret"}).status_code == 200
        assert client.get("/health").status_code == 200
