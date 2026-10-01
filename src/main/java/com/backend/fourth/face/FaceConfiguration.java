package com.backend.fourth.face;

import com.backend.fourth.face.client.FaceServiceClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(FaceProperties.class)
public class FaceConfiguration {
    @Bean
    FaceServiceClient faceServiceClient(FaceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getServiceUrl())
                .requestFactory(requestFactory);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.defaultHeader(FaceServiceClient.API_KEY_HEADER, properties.getApiKey());
        }
        return new FaceServiceClient(builder.build());
    }
}
