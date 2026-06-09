package com.demo.mall.order.dto;

public record SeckillTokenResponse(
        String token,
        Long expiresIn
) {
}
