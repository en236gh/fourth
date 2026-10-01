FROM python:3.11-slim

# insightface compiles a small C extension; libgl/glib are needed by OpenCV.
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential libglib2.0-0 libgl1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /service
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# Model weights are downloaded on first start into this directory; mount a volume to keep them.
RUN mkdir -p /models && ln -s /models /root/.insightface
VOLUME /models

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
