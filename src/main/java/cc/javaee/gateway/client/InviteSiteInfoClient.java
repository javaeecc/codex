package cc.javaee.gateway.client;

import cc.javaee.gateway.bean.InviteSiteInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 邀请码网站信息接口客户端。
 */
@Component
public class InviteSiteInfoClient {

    private final WebClient webClient;
    private final String upstreamBaseUrl;

    public InviteSiteInfoClient(WebClient.Builder webClientBuilder,
                                @Value("${codex.gateway.upstream-base-url}") String upstreamBaseUrl) {
        this.webClient = webClientBuilder.build();
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    public Mono<InviteSiteInfo> load(String inviteCode) {
        if (!StringUtils.hasText(inviteCode)) {
            return Mono.just(InviteSiteInfo.empty());
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(upstreamBaseUrl)
                .path("/api/auth/invite-code")
                .queryParam("invite_code", inviteCode.trim())
                .build()
                .encode()
                .toUri();

        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parse)
                .onErrorReturn(InviteSiteInfo.empty());
    }

    private InviteSiteInfo parse(JsonNode payload) {
        if (payload == null || payload.path("code").asInt(-1) != 0) {
            return InviteSiteInfo.empty();
        }

        JsonNode data = payload.path("data");
        return new InviteSiteInfo(
                text(data, "websiteTitle"),
                text(data, "websiteKeywords"),
                text(data, "websiteDescription"),
                text(data, "websiteImageBase64"));
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull() || !StringUtils.hasText(value.asText())) {
            return null;
        }
        return value.asText().trim();
    }
}
