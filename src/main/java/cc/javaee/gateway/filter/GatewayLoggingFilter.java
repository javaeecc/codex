package cc.javaee.gateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Gateway 请求和响应日志过滤器。
 */
@Component
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);
    private static final int MAX_LOG_BODY_LENGTH = 4096;
    private static final List<MediaType> LOGGABLE_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.TEXT_XML,
            MediaType.APPLICATION_XML
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!shouldLog(request)) {
            return chain.filter(exchange);
        }

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        long startTime = System.currentTimeMillis();
        String queryParams = formatQueryParams(request.getQueryParams());
        String accessUrl = request.getURI().toString();

        if (!shouldLogRequestBody(request)) {
            log.info("[gateway][{}] request start method={} url={} query={}",
                    traceId, request.getMethodValue(), accessUrl, queryParams);
            return chain.filter(exchange.mutate()
                            .response(decorateResponse(exchange, traceId, startTime, null, queryParams, accessUrl))
                            .build())
                    .doOnError(ex -> log.error("[gateway][{}] request error method={} url={} message={}",
                            traceId, request.getMethodValue(), accessUrl, ex.getMessage(), ex));
        }

        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bodyBytes);
                    DataBufferUtils.release(dataBuffer);

                    String requestBody = bodyBytes.length == 0 ? "" : toDisplayBody(bodyBytes, request.getHeaders());
                    log.info("[gateway][{}] request start method={} url={} query={} body={}",
                            traceId, request.getMethodValue(), accessUrl, queryParams, requestBody);

                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                            return Flux.just(bufferFactory.wrap(bodyBytes));
                        }
                    };

                    ServerWebExchange decoratedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .response(decorateResponse(exchange, traceId, startTime, requestBody, queryParams, accessUrl))
                            .build();

                    return chain.filter(decoratedExchange)
                            .doOnError(ex -> log.error("[gateway][{}] request error method={} url={} message={}",
                                    traceId, request.getMethodValue(), accessUrl, ex.getMessage(), ex));
                });
    }

    private ServerHttpResponseDecorator decorateResponse(ServerWebExchange exchange,
                                                         String traceId,
                                                         long startTime,
                                                         String requestBody,
                                                         String queryParams,
                                                         String accessUrl) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                URI targetUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
                String targetUrl = targetUri == null ? "N/A" : targetUri.toString();

                if (!shouldLogResponseBody(getDelegate())) {
                    logResponse(traceId, exchange.getRequest().getMethodValue(), accessUrl, targetUrl,
                            queryParams, requestBody, statusCodeValue(getDelegate()), null, startTime);
                    return super.writeWith(body);
                }

                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                    byte[] responseBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(responseBytes);
                    DataBufferUtils.release(dataBuffer);

                    String responseBody = toDisplayBody(responseBytes, getDelegate().getHeaders());
                    logResponse(traceId, exchange.getRequest().getMethodValue(), accessUrl, targetUrl,
                            queryParams, requestBody, statusCodeValue(getDelegate()), responseBody, startTime);

                    return super.writeWith(Mono.just(bufferFactory.wrap(responseBytes)));
                });
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(publisher -> publisher));
            }
        };
    }

    private void logResponse(String traceId,
                             String method,
                             String accessUrl,
                             String targetUrl,
                             String queryParams,
                             String requestBody,
                             Integer statusCode,
                             String responseBody,
                             long startTime) {
        long costMs = System.currentTimeMillis() - startTime;
        log.info("[gateway][{}] request end method={} accessUrl={} targetUrl={} status={} costMs={} query={} requestBody={} responseBody={}",
                traceId,
                method,
                accessUrl,
                targetUrl,
                statusCode == null ? 0 : statusCode,
                costMs,
                queryParams,
                emptyToDash(requestBody),
                emptyToDash(responseBody));
    }

    private boolean shouldLog(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/api/")
                || path.startsWith("/v1/")
                || "/auth/register".equals(path);
    }

    private boolean shouldLogRequestBody(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        if (method == null || method == HttpMethod.GET || method == HttpMethod.DELETE) {
            return false;
        }
        return isLoggableMediaType(request.getHeaders().getContentType());
    }

    private boolean shouldLogResponseBody(ServerHttpResponse response) {
        return isLoggableMediaType(response.getHeaders().getContentType());
    }

    private boolean isLoggableMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return true;
        }
        if (MediaType.TEXT_EVENT_STREAM.includes(mediaType)
                || MediaType.APPLICATION_OCTET_STREAM.includes(mediaType)
                || MediaType.MULTIPART_FORM_DATA.includes(mediaType)) {
            return false;
        }
        for (MediaType candidate : LOGGABLE_MEDIA_TYPES) {
            if (candidate.includes(mediaType) || mediaType.includes(candidate)) {
                return true;
            }
        }
        String type = mediaType.toString().toLowerCase();
        return type.contains("json") || type.startsWith("text/");
    }

    private String toDisplayBody(byte[] bodyBytes, HttpHeaders headers) {
        Charset charset = headers.getContentType() != null && headers.getContentType().getCharset() != null
                ? headers.getContentType().getCharset()
                : StandardCharsets.UTF_8;
        String body = redactSensitiveFields(new String(bodyBytes, charset).replaceAll("\\s+", " ").trim());
        if (body.length() <= MAX_LOG_BODY_LENGTH) {
            return emptyToDash(body);
        }
        return body.substring(0, MAX_LOG_BODY_LENGTH) + "...(truncated)";
    }

    private String formatQueryParams(MultiValueMap<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "-";
        }
        return queryParams.toSingleValueMap().toString()
                .replaceAll("(?i)(invite_code=)[^,}\\s]+", "$1***");
    }

    private int statusCodeValue(ServerHttpResponse response) {
        return response.getStatusCode() == null ? 200 : response.getStatusCode().value();
    }

    private String emptyToDash(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value;
    }

    private String redactSensitiveFields(String value) {
        return value.replaceAll(
                "(?i)(\\\"(?:password|token|accessToken|captchaCode|inviteCode)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")",
                "$1***$2");
    }
}
