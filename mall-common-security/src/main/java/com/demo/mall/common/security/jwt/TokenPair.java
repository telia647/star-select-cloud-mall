package com.demo.mall.common.security.jwt;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
