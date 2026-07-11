package cc.javaee.gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public Mono<ResponseEntity<JsonNode>> register(JsonNode requestBody) {
        URI uri = UriComponentsBuilder.fromHttpUrl(upstreamBaseUrl)
                .path("/api/auth/register")
                .build()
                .toUri();

        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> response.toEntity(JsonNode.class));
    }
}
