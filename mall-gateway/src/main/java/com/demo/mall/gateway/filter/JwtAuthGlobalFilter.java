package com.demo.mall.gateway.filter;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.common.security.jwt.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthGlobalFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isInternalPath(path)) {
            return forbidden(exchange);
        }
        if (isPublicPath(path) || HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = jwtService.parse(authorization.substring(BEARER_PREFIX.length()));
            if (!jwtService.isAccessToken(claims)) {
                return unauthorized(exchange);
            }
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> builder
                            .header(SecurityHeaders.USER_ID, String.valueOf(jwtService.getUserId(claims)))
                            .header(SecurityHeaders.USERNAME, jwtService.getUsername(claims))
                            .header(SecurityHeaders.USER_ROLE, jwtService.getRoleCode(claims)))
                    .build();
            return chain.filter(mutatedExchange);
        } catch (RuntimeException ex) {
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isPublicPath(String path) {
        List<String> exactPaths = List.of(
                "/api/auth/login",
                "/api/auth/refresh-token",
                "/api/users/register",
                "/api/payments/callback/mock"
        );
        return exactPaths.contains(path)
                || path.startsWith("/api/products")
                || path.startsWith("/api/categories")
                || path.startsWith("/api/promotions/seckill")
                || path.startsWith("/actuator");
    }

    private boolean isInternalPath(String path) {
        return path.startsWith("/api/") && path.contains("/internal");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = toJson(Result.failure(ErrorCode.UNAUTHORIZED)).getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = toJson(Result.failure(ErrorCode.FORBIDDEN)).getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String toJson(Result<Void> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            return "{\"code\":401,\"message\":\"unauthorized\"}";
        }
    }
}
