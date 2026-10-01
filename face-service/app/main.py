"""HTTP wrapper around the face engine. Called only by the Spring Boot backend."""
from __future__ import annotations

import hmac
import logging
import os
from contextlib import asynccontextmanager
from dataclasses import dataclass, field
from typing import Callable

from fastapi import Depends, FastAPI, File, Form, Header, Request, UploadFile
from fastapi.responses import JSONResponse

from .engine import FaceEngine, FaceError, InsightFaceEngine, Purpose, decode_image, extract_face

log = logging.getLogger("face-service")


@dataclass(frozen=True)
class Settings:
    api_key: str = field(default_factory=lambda: os.getenv("FACE_SERVICE_API_KEY", ""))
    model_pack: str = field(default_factory=lambda: os.getenv("FACE_MODEL_PACK", "buffalo_l"))
    det_size: int = field(default_factory=lambda: int(os.getenv("FACE_DET_SIZE", "640")))
    min_face_px: int = field(default_factory=lambda: int(os.getenv("FACE_MIN_FACE_PX", "80")))
    min_det_score: float = field(default_factory=lambda: float(os.getenv("FACE_MIN_DET_SCORE", "0.6")))
    max_upload_bytes: int = field(default_factory=lambda: int(os.getenv("FACE_MAX_UPLOAD_BYTES", str(5 * 1024 * 1024))))


def create_app(
    settings: Settings | None = None,
    engine_factory: Callable[[Settings], FaceEngine] | None = None,
) -> FastAPI:
    settings = settings or Settings()
    engine_factory = engine_factory or (lambda s: InsightFaceEngine(s.model_pack, s.det_size))

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        # Load once: model initialisation takes seconds, inference afterwards ~100-300 ms on CPU.
        app.state.engine = engine_factory(settings)
        log.info("Face engine ready: %s", app.state.engine.model_name)
        yield

    app = FastAPI(title="Exam attendance face service", version="1.0.0", lifespan=lifespan)

    @app.exception_handler(FaceError)
    async def face_error(_: Request, ex: FaceError):
        status = 400 if ex.code in ("INVALID_IMAGE", "IMAGE_TOO_LARGE") else 422
        return JSONResponse(status_code=status, content={"code": ex.code, "message": ex.message})

    def require_key(x_face_service_key: str | None = Header(default=None)):
        if settings.api_key and not hmac.compare_digest(x_face_service_key or "", settings.api_key):
            raise _Unauthorised()

    @app.exception_handler(_Unauthorised)
    async def unauthorised(_: Request, __: _Unauthorised):
        return JSONResponse(status_code=401, content={"code": "UNAUTHORISED", "message": "Invalid face service key."})

    @app.get("/health")
    def health(request: Request):
        return {"status": "ok", "model": request.app.state.engine.model_name}

    # Sync handler: FastAPI runs it in a worker thread, so CPU-bound inference does not block the loop.
    @app.post("/embed", dependencies=[Depends(require_key)])
    def embed(
        request: Request,
        image: UploadFile = File(...),
        purpose: Purpose = Form(Purpose.VERIFICATION),
    ):
        data = image.file.read(settings.max_upload_bytes + 1)
        if len(data) > settings.max_upload_bytes:
            raise FaceError("IMAGE_TOO_LARGE", f"Images must be at most {settings.max_upload_bytes // (1024 * 1024)} MB.")
        engine: FaceEngine = request.app.state.engine
        result = extract_face(engine, decode_image(data), purpose, settings.min_face_px, settings.min_det_score)
        return {
            "model": engine.model_name,
            "embedding": [round(float(v), 6) for v in result.embedding],
            "detScore": round(result.det_score, 4),
            "bbox": [round(v, 1) for v in result.bbox],
            "faceCount": result.face_count,
        }

    return app


class _Unauthorised(Exception):
    pass


# The model loads in the lifespan hook, so importing this module stays cheap.
app = create_app()
