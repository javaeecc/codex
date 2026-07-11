package cc.javaee.gateway.filter;

import cc.javaee.gateway.config.SitePageProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Keeps the public gateway unavailable until its local invite code is initialized.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayInitializationFilter implements WebFilter {

    private final SitePageProperties sitePageProperties;

    public GatewayInitializationFilter(SitePageProperties sitePageProperties) {
        this.sitePageProperties = sitePageProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (sitePageProperties.isInitialized() || isInitializationRequest(exchange)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
        exchange.getResponse().getHeaders().setLocation(URI.create("/initialize"));
        return exchange.getResponse().setComplete();
    }

    private boolean isInitializationRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return "/initialize".equals(path)
                || path.startsWith("/initialize/")
                || "/public-pages.css".equals(path)
                || "/favicon.ico".equals(path)
                || path.startsWith("/api/admin/auth/");
    }
}
