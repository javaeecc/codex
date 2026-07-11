package cc.javaee.gateway.controller;

import cc.javaee.gateway.client.AdminInviteCodeClient;
import cc.javaee.gateway.config.SitePageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Initializes the gateway from an authenticated administrator session.
 */
@RestController
@RequestMapping("/initialize")
public class InitializationController {

    private final SitePageProperties sitePageProperties;
    private final AdminInviteCodeClient adminInviteCodeClient;
    private final ObjectMapper objectMapper;

    public InitializationController(SitePageProperties sitePageProperties,
                                     AdminInviteCodeClient adminInviteCodeClient,
                                     ObjectMapper objectMapper) {
        this.sitePageProperties = sitePageProperties;
        this.adminInviteCodeClient = adminInviteCodeClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/invite-code", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<JsonNode>> initialize(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return Mono.just(errorResponse(HttpStatus.UNAUTHORIZED, "Administrator login is required"));
        }
        if (sitePageProperties.isInitialized()) {
            return Mono.just(errorResponse(HttpStatus.CONFLICT, "Gateway is already initialized"));
        }

        return adminInviteCodeClient.load(authorizationHeader)
                .flatMap(inviteCode -> Mono.fromCallable(() -> sitePageProperties.initializeInviteCode(inviteCode))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(initialized -> initialized
                                ? Mono.just(successResponse(inviteCode))
                                : Mono.just(errorResponse(HttpStatus.CONFLICT, "Gateway is already initialized"))))
                .onErrorResume(ex -> Mono.just(errorResponse(HttpStatus.BAD_GATEWAY,
                        ex.getMessage() == null ? "Gateway initialization failed" : ex.getMessage())));
    }

    private ResponseEntity<JsonNode> successResponse(String inviteCode) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("inviteCode", inviteCode);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", 0);
        body.put("message", "success");
        body.set("data", data);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<JsonNode> errorResponse(HttpStatus status, String message) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", status.value());
        body.put("message", message);
        body.putNull("data");
        return ResponseEntity.status(status).body(body);
    }
}
