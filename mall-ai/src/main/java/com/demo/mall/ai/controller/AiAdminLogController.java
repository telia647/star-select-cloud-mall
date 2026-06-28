package com.demo.mall.ai.controller;

import com.demo.mall.ai.dto.AiLogResponse;
import com.demo.mall.ai.service.AiAdminLogService;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.context.RoleGuard;
import com.demo.mall.common.security.header.SecurityHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/admin/logs")
public class AiAdminLogController {

    private final AiAdminLogService logService;

    public AiAdminLogController(AiAdminLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/model-calls")
    public Result<List<AiLogResponse>> modelCalls(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(logService.modelCalls());
    }

    @GetMapping("/tool-calls")
    public Result<List<AiLogResponse>> toolCalls(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(logService.toolCalls());
    }
}
