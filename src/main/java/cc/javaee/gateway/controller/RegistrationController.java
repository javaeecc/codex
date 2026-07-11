package cc.javaee.gateway.controller;

import cc.javaee.gateway.client.RegistrationProxyClient;
import cc.javaee.gateway.config.SitePageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Handles registration requests and appends the server-side invite code.
 */
@RestController
public class RegistrationController {

    private final SitePageProperties sitePageProperties;
    private final RegistrationProxyClient registrationProxyClient;
    private final ObjectMapper objectMapper;

    public RegistrationController(SitePageProperties sitePageProperties,
                                  RegistrationProxyClient registrationProxyClient,
                                  ObjectMapper objectMapper) {
        this.sitePageProperties = sitePageProperties;
        this.registrationProxyClient = registrationProxyClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/auth/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<JsonNode>> register(@RequestBody ObjectNode requestBody) {
        String inviteCode = sitePageProperties.getInviteCode();
        if (!StringUtils.hasText(inviteCode)) {
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Gateway invite code is not configured"));
        }

        ObjectNode forwardedRequest = requestBody == null
                ? objectMapper.createObjectNode()
                : requestBody.deepCopy();
        forwardedRequest.put("inviteCode", inviteCode.trim());

        return registrationProxyClient.register(forwardedRequest)
                .onErrorResume(ex -> Mono.just(errorResponse(HttpStatus.BAD_GATEWAY.value(),
                        "Registration service is unavailable")));
    }

    private ResponseEntity<JsonNode> errorResponse(int code, String message) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", code);
        body.put("message", message);
        body.putNull("data");
        HttpStatus status = code == HttpStatus.BAD_GATEWAY.value()
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(body);
    }
}
