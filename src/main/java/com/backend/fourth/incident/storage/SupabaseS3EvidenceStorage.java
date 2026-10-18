package com.backend.fourth.incident.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URI;
import java.time.Duration;

@Component
public class SupabaseS3EvidenceStorage implements EvidenceObjectStorage {
    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public SupabaseS3EvidenceStorage(
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.region:eu-west-2}") String region,
            @Value("${app.storage.s3.access-key:}") String accessKey,
            @Value("${app.storage.s3.secret-key:}") String secretKey) {
        this.endpoint = endpoint;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public void upload(String bucket, String objectPath, String contentType, byte[] content) {
        try (S3Client client = createClient()) {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectPath)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Evidence could not be uploaded to private storage", exception);
        }
    }

    @Override
    public void delete(String bucket, String objectPath) {
        try (S3Client client = createClient()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectPath).build());
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Unlinked evidence object could not be removed", exception);
        }
    }

    @Override
    public String createDownloadUrl(String bucket, String objectPath, Duration validity) {
        try (S3Presigner presigner = createPresigner()) {
            PresignedGetObjectRequest request = presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(validity)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectPath).build())
                    .build());
            return request.url().toString();
        } catch (RuntimeException exception) {
            throw new StorageUnavailableException("Evidence download URL could not be generated", exception);
        }
    }

    private S3Client createClient() {
        requireConfigured();
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private S3Presigner createPresigner() {
        requireConfigured();
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private void requireConfigured() {
        if (endpoint.isBlank() || accessKey.isBlank() || secretKey.isBlank()) {
            throw new StorageUnavailableException("Supabase S3 storage is not configured");
        }
    }
}