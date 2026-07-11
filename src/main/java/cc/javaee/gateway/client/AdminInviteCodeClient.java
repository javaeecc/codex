package cc.javaee.gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Loads the invite code belonging to an authenticated administrator.
 */
@Component
public class AdminInviteCodeClient {

    private final WebClient webClient;
    private final String upstreamBaseUrl;

    public AdminInviteCodeClient(WebClient.Builder webClientBuilder,
                                 @Value("${codex.gateway.upstream-base-url}") String upstreamBaseUrl) {
        this.webClient = webClientBuilder.build();
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    public Mono<String> load(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return Mono.error(new IllegalArgumentException("Administrator login is required"));
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(upstreamBaseUrl)
                .path("/api/admin/auth/invite-code")
                .build()
                .toUri();

        return webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                "Administrator authentication returned an empty response")))
                        .flatMap(payload -> {
                            if (!response.statusCode().is2xxSuccessful()
                                    || payload.path("code").asInt(-1) != 0) {
                                String message = payload.path("message").asText("Administrator authentication failed");
                                return Mono.error(new IllegalArgumentException(message));
                            }
                            String inviteCode = payload.path("data").path("inviteCode").asText(null);
                            return StringUtils.hasText(inviteCode)
                                    ? Mono.just(inviteCode.trim())
                                    : Mono.error(new IllegalArgumentException("Invite code is unavailable"));
                        }));
    }
}
