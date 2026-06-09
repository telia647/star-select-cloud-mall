package com.demo.mall.user.dto;

public record UserResponse(
        Long id,
        String username,
        String phone,
        Integer status,
        String roleCode
) {
}
