package com.demo.mall.user.dto;

public record UserCredentialResponse(
        Long id,
        String username,
        String password,
        Integer status,
        String roleCode
) {
}
