package com.backend.fourth.face.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the Python face service (face-service/), which turns one photo into one 512-d embedding. */
public class FaceServiceClient {
    public static final String API_KEY_HEADER = "X-Face-Service-Key";
    public static final int EMBEDDING_SIZE = 512;

    private final RestClient restClient;

    public FaceServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public FaceEmbedding embed(byte[] image, String contentType, FacePurpose purpose) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(contentType == null || contentType.isBlank()
                ? MediaType.IMAGE_JPEG
                : MediaType.parseMediaType(contentType));
        ByteArrayResource resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return "capture";
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", new HttpEntity<>(resource, partHeaders));
        form.add("purpose", purpose.name());

        try {
            return restClient.post()
                    .uri("/embed")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return requireValid(response.bodyTo(FaceEmbedding.class));
                        }
                        if (status == 400 || status == 422) {
                            ServiceError error = readError(response);
                            if (error != null && error.code() != null) {
                                throw new FacePhotoRejectedException(error.code(), error.message());
                            }
                        }
                        throw new FaceServiceUnavailableException("Face service returned HTTP " + status, null);
                    });
        } catch (RestClientException ex) {
            throw new FaceServiceUnavailableException("Face service is unreachable", ex);
        }
    }

    private static FaceEmbedding requireValid(FaceEmbedding embedding) {
        if (embedding == null || embedding.embedding() == null || embedding.embedding().length != EMBEDDING_SIZE
                || embedding.model() == null || embedding.model().isBlank()) {
            throw new FaceServiceUnavailableException("Face service returned an invalid embedding", null);
        }
        return embedding;
    }

    private static ServiceError readError(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        try {
            return response.bodyTo(ServiceError.class);
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ServiceError(String code, String message) {
    }
}
