package com.demo.mall.common.security.context;

import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;

public final class RoleGuard {

    private RoleGuard() {
    }

    public static void requireAdmin(String roleCode) {
        if (!Roles.ADMIN.equals(roleCode)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
