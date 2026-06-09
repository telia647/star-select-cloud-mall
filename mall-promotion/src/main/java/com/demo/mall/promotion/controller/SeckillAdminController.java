package com.demo.mall.promotion.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.context.RoleGuard;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.promotion.dto.AdminIdResponse;
import com.demo.mall.promotion.dto.PromotionActivityAdminRequest;
import com.demo.mall.promotion.dto.PromotionOperatorContext;
import com.demo.mall.promotion.dto.PromotionSeckillSkuAdminRequest;
import com.demo.mall.promotion.dto.PromotionSessionAdminRequest;
import com.demo.mall.promotion.entity.PromotionActivity;
import com.demo.mall.promotion.entity.PromotionOperationLog;
import com.demo.mall.promotion.entity.PromotionSeckillSku;
import com.demo.mall.promotion.entity.PromotionSession;
import com.demo.mall.promotion.service.PromotionOperationLogService;
import com.demo.mall.promotion.service.SeckillAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/promotions/admin/seckill")
public class SeckillAdminController {

    private final SeckillAdminService seckillAdminService;
    private final PromotionOperationLogService operationLogService;

    public SeckillAdminController(SeckillAdminService seckillAdminService,
                                  PromotionOperationLogService operationLogService) {
        this.seckillAdminService = seckillAdminService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/activities")
    public Result<List<PromotionActivity>> activities(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(seckillAdminService.listActivities());
    }

    @PostMapping("/activities")
    public Result<AdminIdResponse> saveActivity(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                @RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                @RequestHeader(SecurityHeaders.USERNAME) String username,
                                                @Valid @RequestBody PromotionActivityAdminRequest request) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(seckillAdminService.saveActivity(operator(userId, username, roleCode), request));
    }

    @GetMapping("/activities/{activityId}/sessions")
    public Result<List<PromotionSession>> sessions(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                   @PathVariable("activityId") Long activityId) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(seckillAdminService.listSessions(activityId));
    }

    @PostMapping("/sessions")
    public Result<AdminIdResponse> saveSession(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                               @RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                               @RequestHeader(SecurityHeaders.USERNAME) String username,
                                               @Valid @RequestBody PromotionSessionAdminRequest request) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(seckillAdminService.saveSession(operator(userId, username, roleCode), request));
    }

    @GetMapping("/sessions/{sessionId}/items")
    public Result<List<PromotionSeckillSku>> items(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                   @PathVariable("sessionId") Long sessionId) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(seckillAdminService.listItems(sessionId));
    }

    @PostMapping("/items")
    public Result<AdminIdResponse> saveItem(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                            @RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                            @RequestHeader(SecurityHeaders.USERNAME) String username,
                                            @Valid @RequestBody PromotionSeckillSkuAdminRequest request) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(seckillAdminService.saveItem(operator(userId, username, roleCode), request));
    }

    @GetMapping("/operation-logs")
    public Result<List<PromotionOperationLog>> operationLogs(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(operationLogService.recent(30));
    }

    private PromotionOperatorContext operator(Long userId, String username, String roleCode) {
        return new PromotionOperatorContext(userId, username, roleCode);
    }
}
