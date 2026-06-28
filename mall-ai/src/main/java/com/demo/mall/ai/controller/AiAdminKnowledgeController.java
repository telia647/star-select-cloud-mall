package com.demo.mall.ai.controller;

import com.demo.mall.ai.dto.KnowledgeDocRequest;
import com.demo.mall.ai.dto.KnowledgeDocResponse;
import com.demo.mall.ai.service.KnowledgeService;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.context.RoleGuard;
import com.demo.mall.common.security.header.SecurityHeaders;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/admin/knowledge/docs")
public class AiAdminKnowledgeController {

    private final KnowledgeService knowledgeService;

    public AiAdminKnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public Result<List<KnowledgeDocResponse>> list(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) Integer status) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(knowledgeService.list(keyword, category, status));
    }

    @PostMapping
    public Result<KnowledgeDocResponse> create(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                               @RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                               @Valid @RequestBody KnowledgeDocRequest request) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(knowledgeService.create(userId, request));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDocResponse> detail(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                               @PathVariable("id") Long id) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(knowledgeService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeDocResponse> update(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                               @RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                               @PathVariable("id") Long id,
                                               @Valid @RequestBody KnowledgeDocRequest request) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(knowledgeService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                               @PathVariable("id") Long id) {
        RoleGuard.requireAdmin(roleCode);
        knowledgeService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/embedding")
    public Result<KnowledgeDocResponse> embedding(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                  @PathVariable("id") Long id) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(knowledgeService.index(id));
    }

    @PostMapping("/sync-products")
    public Result<List<KnowledgeDocResponse>> syncProducts(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                           @RequestHeader(SecurityHeaders.USER_ROLE) String roleCode) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(knowledgeService.syncProducts(userId));
    }
}
