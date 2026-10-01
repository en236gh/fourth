package com.backend.fourth.face;

import com.backend.fourth.face.client.FaceEmbedding;
import com.backend.fourth.face.client.FacePhotoRejectedException;
import com.backend.fourth.face.client.FacePurpose;
import com.backend.fourth.face.client.FaceServiceClient;
import com.backend.fourth.face.client.FaceServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FaceServiceClientTest {
    private MockRestServiceServer server;
    private FaceServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://face");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FaceServiceClient(builder.build());
    }

    @Test
    void parsesEmbeddingAndSendsPurposeAsMultipart() {
        server.expect(requestTo("http://face/embed"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("name=\"purpose\"")))
                .andExpect(content().string(containsString("ENROLMENT")))
                .andExpect(content().string(containsString("name=\"image\"")))
                .andRespond(withSuccess(embeddingJson(512), MediaType.APPLICATION_JSON));

        FaceEmbedding embedding = client.embed(new byte[]{1, 2, 3}, "image/jpeg", FacePurpose.ENROLMENT);

        assertEquals("buffalo_l", embedding.model());
        assertEquals(512, embedding.embedding().length);
        assertEquals(0.1f, embedding.embedding()[0], 1e-6);
        assertEquals(0.88, embedding.detScore(), 1e-9);
        assertEquals(1, embedding.faceCount());
        server.verify();
    }

    @Test
    void photoProblemsBecomeRetryableRejections() {
        server.expect(requestTo("http://face/embed"))
                .andRespond(withStatus(HttpStatusCode.valueOf(422)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NO_FACE\",\"message\":\"No face was detected.\"}"));

        FacePhotoRejectedException ex = assertThrows(FacePhotoRejectedException.class,
                () -> client.embed(new byte[]{1}, "image/jpeg", FacePurpose.VERIFICATION));
        assertEquals("NO_FACE", ex.getCode());
        assertEquals("No face was detected.", ex.getMessage());
    }

    @Test
    void serverErrorsMeanTheServiceIsUnavailable() {
        server.expect(requestTo("http://face/embed")).andRespond(withServerError());
        assertThrows(FaceServiceUnavailableException.class,
                () -> client.embed(new byte[]{1}, "image/jpeg", FacePurpose.VERIFICATION));
    }

    @Test
    void wrongApiKeyIsTreatedAsUnavailableNotAsABadPhoto() {
        server.expect(requestTo("http://face/embed")).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"code\":\"UNAUTHORISED\",\"message\":\"x\"}"));
        assertThrows(FaceServiceUnavailableException.class,
                () -> client.embed(new byte[]{1}, "image/jpeg", FacePurpose.VERIFICATION));
    }

    @Test
    void wrongSizedEmbeddingIsRejected() {
        server.expect(requestTo("http://face/embed"))
                .andRespond(withSuccess(embeddingJson(128), MediaType.APPLICATION_JSON));
        assertThrows(FaceServiceUnavailableException.class,
                () -> client.embed(new byte[]{1}, "image/jpeg", FacePurpose.VERIFICATION));
    }

    @Test
    void unreachableServiceIsUnavailable() {
        // Port 9 (discard) is closed on developer machines and CI containers.
        FaceServiceClient offline = new FaceServiceClient(RestClient.builder().baseUrl("http://127.0.0.1:9").build());
        assertThrows(FaceServiceUnavailableException.class,
                () -> offline.embed(new byte[]{1}, "image/jpeg", FacePurpose.VERIFICATION));
    }

    private static String embeddingJson(int size) {
        String values = String.join(",", Collections.nCopies(size, "0.1"));
        return "{\"model\":\"buffalo_l\",\"embedding\":[" + values + "],\"detScore\":0.88,"
                + "\"bbox\":[1.0,2.0,3.0,4.0],\"faceCount\":1}";
    }
}
