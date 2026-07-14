package cc.javaee.gateway.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

/**
 * Extracts and forwards client IP related headers for proxied requests.
 */
public final class ClientIpForwardingSupport {

    private ClientIpForwardingSupport() {
    }

    public static void applyForwardHeaders(ServerHttpRequest sourceRequest, HttpHeaders targetHeaders) {
        if (sourceRequest == null || targetHeaders == null) {
            return;
        }
        String forwardedFor = resolveForwardedFor(sourceRequest);
        String realIp = resolveClientIp(sourceRequest);
        if (StringUtils.hasText(forwardedFor)) {
            targetHeaders.set("X-Forwarded-For", forwardedFor);
        }
        if (StringUtils.hasText(realIp)) {
            targetHeaders.set("X-Real-IP", realIp);
        }
    }

    public static String resolveForwardedFor(ServerHttpRequest request) {
        String forwardedFor = firstHeaderValue(request, "X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return trimToLength(forwardedFor, 256);
        }
        return resolveClientIp(request);
    }

    public static String resolveClientIp(ServerHttpRequest request) {
        String ip = firstHeaderValue(request, "X-Forwarded-For");
        if (!StringUtils.hasText(ip)) {
            ip = firstHeaderValue(request, "X-Real-IP");
        }
        if (!StringUtils.hasText(ip)) {
            ip = firstHeaderValue(request, "Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip)) {
            ip = firstHeaderValue(request, "WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip)
                && request != null
                && request.getRemoteAddress() != null
                && request.getRemoteAddress().getAddress() != null) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        return trimToLength(ip, 64);
    }

    public static String firstHeaderValue(ServerHttpRequest request, String headerName) {
        if (request == null) {
            return null;
        }
        return firstHeaderValue(request.getHeaders(), headerName);
    }

    public static String firstHeaderValue(HttpHeaders headers, String headerName) {
        if (headers == null || !StringUtils.hasText(headerName)) {
            return null;
        }
        String value = headers.getFirst(headerName);
        if (!StringUtils.hasText(value) || "unknown".equalsIgnoreCase(value)) {
            return null;
        }
        int commaIndex = value.indexOf(',');
        if (commaIndex >= 0) {
            value = value.substring(0, commaIndex);
        }
        value = value.trim();
        return value.length() == 0 ? null : value;
    }

    private static String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
