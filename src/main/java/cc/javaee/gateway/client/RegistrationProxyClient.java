package cc.javaee.gateway.client;

import cc.javaee.gateway.support.ClientIpForwardingSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Forwards registration requests to the upstream Codex API.
 */
@Component
public class RegistrationProxyClient {

    private final WebClient webClient;
    private final String upstreamBaseUrl;

    public RegistrationProxyClient(WebClient.Builder webClientBuilder,
                                   @Value("${codex.gateway.upstream-base-url}") String upstreamBaseUrl) {
        this.webClient = webClientBuilder.build();
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    public Mono<ResponseEntity<JsonNode>> register(JsonNode requestBody, ServerHttpRequest sourceRequest) {
        URI uri = UriComponentsBuilder.fromHttpUrl(upstreamBaseUrl)
                .path("/api/auth/register")
                .build()
                .toUri();

        HttpHeaders forwardedHeaders = new HttpHeaders();
        ClientIpForwardingSupport.applyForwardHeaders(sourceRequest, forwardedHeaders);

        WebClient.RequestBodySpec request = webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        forwardedHeaders.forEach((headerName, values) -> {
            if (values != null && !values.isEmpty()) {
                request.header(headerName, values.toArray(new String[0]));
            }
        });

        return request.bodyValue(requestBody)
                .exchangeToMono(response -> response.toEntity(JsonNode.class));
    }

    public Mono<ResponseEntity<JsonNode>> register(JsonNode requestBody, String forwardedFor, String realIp) {
        HttpHeaders forwardedHeaders = new HttpHeaders();
        if (hasText(forwardedFor)) {
            forwardedHeaders.set("X-Forwarded-For", forwardedFor);
        }
        if (hasText(realIp)) {
            forwardedHeaders.set("X-Real-IP", realIp);
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(upstreamBaseUrl)
                .path("/api/auth/register")
                .build()
                .toUri();

        WebClient.RequestBodySpec request = webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        forwardedHeaders.forEach((headerName, values) -> {
            if (values != null && !values.isEmpty()) {
                request.header(headerName, values.toArray(new String[0]));
            }
        });

        return request.bodyValue(requestBody)
                .exchangeToMono(response -> response.toEntity(JsonNode.class));
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
