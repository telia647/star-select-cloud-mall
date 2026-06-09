package com.demo.mall.auth.client.dto;

public record UserCredentialResponse(
        Long id,
        String username,
        String password,
        Integer status,
        String roleCode
) {
}
