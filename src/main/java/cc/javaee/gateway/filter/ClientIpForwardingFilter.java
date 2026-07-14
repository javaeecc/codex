package cc.javaee.gateway.filter;

import cc.javaee.gateway.support.ClientIpForwardingSupport;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensures every gateway-routed upstream request carries client IP headers.
 */
@Component
public class ClientIpForwardingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> ClientIpForwardingSupport.applyForwardHeaders(exchange.getRequest(), headers))
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
