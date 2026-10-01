"""End-to-end check with the real buffalo_l model. Opt-in: downloads ~280 MB on first run.

    FACE_LIVE_TEST=1 pytest tests/test_live_model.py -s
"""
import os

import cv2
import numpy as np
import pytest

pytestmark = pytest.mark.skipif(os.getenv("FACE_LIVE_TEST") != "1", reason="set FACE_LIVE_TEST=1 to run the real model")


@pytest.fixture(scope="module")
def engine():
    from app.engine import InsightFaceEngine

    return InsightFaceEngine()


@pytest.fixture(scope="module")
def portraits(engine):
    """Crop each person out of InsightFace's bundled group photo to get single-person portraits."""
    from insightface.data import get_image

    group = get_image("t1")
    portraits = []
    for detected in engine.detect(group):
        x1, y1, x2, y2 = detected.bbox
        pad_w, pad_h = (x2 - x1) * 0.6, (y2 - y1) * 0.6
        crop = group[max(0, int(y1 - pad_h)):int(y2 + pad_h), max(0, int(x1 - pad_w)):int(x2 + pad_w)]
        portraits.append(cv2.resize(crop, None, fx=2.0, fy=2.0, interpolation=cv2.INTER_CUBIC))
    assert len(portraits) >= 4, "sample image should contain several people"
    return portraits


def embed(engine, image):
    from app.engine import Purpose, extract_face

    return extract_face(engine, image, Purpose.VERIFICATION, min_face_px=40, min_det_score=0.5).embedding


def test_same_person_scores_higher_than_different_people(engine, portraits):
    same, different = [], []
    for i, portrait in enumerate(portraits):
        reference = embed(engine, portrait)
        # A second "capture": mirrored, darker and slightly blurred, like a phone camera in a hall.
        probe = cv2.GaussianBlur(cv2.convertScaleAbs(cv2.flip(portrait, 1), alpha=0.8, beta=-10), (3, 3), 0)
        same.append(float(np.dot(reference, embed(engine, probe))))
        different += [float(np.dot(reference, embed(engine, other))) for j, other in enumerate(portraits) if j != i]

    print(f"\nsame-person cosine: min={min(same):.3f} mean={np.mean(same):.3f}")
    print(f"different-person cosine: max={max(different):.3f} mean={np.mean(different):.3f}")
    # Backend defaults: match >= 0.45, manual review between 0.30 and 0.45, reject below 0.30.
    assert min(same) >= 0.45
    assert max(different) < 0.30
