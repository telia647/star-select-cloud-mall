package com.demo.mall.common.security.context;

public record CurrentUser(Long userId, String username, String roleCode) {
}
