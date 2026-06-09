package com.demo.mall.common.security.jwt;

import com.demo.mall.common.security.context.Roles;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtService {

    private static final String USERNAME_CLAIM = "username";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair issueTokens(Long userId, String username, String roleCode) {
        String accessToken = createToken(userId, username, roleCode, ACCESS_TOKEN_TYPE, properties.getAccessTokenTtlSeconds());
        String refreshToken = createToken(userId, username, roleCode, REFRESH_TOKEN_TYPE, properties.getRefreshTokenTtlSeconds());
        return new TokenPair(accessToken, refreshToken, properties.getAccessTokenTtlSeconds());
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get(USERNAME_CLAIM, String.class);
    }

    public String getRoleCode(Claims claims) {
        String roleCode = claims.get(ROLE_CLAIM, String.class);
        return roleCode == null || roleCode.isBlank() ? Roles.MEMBER : roleCode;
    }

    private String createToken(Long userId, String username, String roleCode, String tokenType, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of(
                        USERNAME_CLAIM, username,
                        ROLE_CLAIM, roleCode == null || roleCode.isBlank() ? Roles.MEMBER : roleCode,
                        TOKEN_TYPE_CLAIM, tokenType
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }
}
