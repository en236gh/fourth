package com.backend.fourth.incident.storage;

import java.time.Duration;

public interface EvidenceObjectStorage {
    void upload(String bucket, String objectPath, String contentType, byte[] content);

    void delete(String bucket, String objectPath);

    String createDownloadUrl(String bucket, String objectPath, Duration validity);
}