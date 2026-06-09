package com.demo.mall.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.promotion.dto.AdminIdResponse;
import com.demo.mall.promotion.dto.PromotionActivityAdminRequest;
import com.demo.mall.promotion.dto.PromotionOperatorContext;
import com.demo.mall.promotion.dto.PromotionSeckillSkuAdminRequest;
import com.demo.mall.promotion.dto.PromotionSessionAdminRequest;
import com.demo.mall.promotion.entity.PromotionActivity;
import com.demo.mall.promotion.entity.PromotionSeckillSku;
import com.demo.mall.promotion.entity.PromotionSession;
import com.demo.mall.promotion.mapper.PromotionActivityMapper;
import com.demo.mall.promotion.mapper.PromotionSeckillSkuMapper;
import com.demo.mall.promotion.mapper.PromotionSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeckillAdminService {

    private final PromotionActivityMapper activityMapper;
    private final PromotionSessionMapper sessionMapper;
    private final PromotionSeckillSkuMapper seckillSkuMapper;
    private final PromotionOperationLogService operationLogService;

    public SeckillAdminService(PromotionActivityMapper activityMapper,
                               PromotionSessionMapper sessionMapper,
                               PromotionSeckillSkuMapper seckillSkuMapper,
                               PromotionOperationLogService operationLogService) {
        this.activityMapper = activityMapper;
        this.sessionMapper = sessionMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.operationLogService = operationLogService;
    }

    public List<PromotionActivity> listActivities() {
        return activityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .orderByDesc(PromotionActivity::getId));
    }

    public List<PromotionSession> listSessions(Long activityId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<PromotionSession>()
                .eq(PromotionSession::getActivityId, activityId)
                .orderByAsc(PromotionSession::getSort)
                .orderByAsc(PromotionSession::getStartTime));
    }

    public List<PromotionSeckillSku> listItems(Long sessionId) {
        return seckillSkuMapper.selectList(new LambdaQueryWrapper<PromotionSeckillSku>()
                .eq(PromotionSeckillSku::getSessionId, sessionId)
                .orderByAsc(PromotionSeckillSku::getSort)
                .orderByAsc(PromotionSeckillSku::getId));
    }

    @Transactional
    public AdminIdResponse saveActivity(PromotionOperatorContext operator, PromotionActivityAdminRequest request) {
        PromotionActivity activity = new PromotionActivity();
        activity.setId(request.id());
        activity.setName(request.name());
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setStatus(request.status());
        saveActivity(activity);
        operationLogService.record(
                operator.userId(),
                operator.username(),
                operator.roleCode(),
                request.id() == null ? "CREATE_ACTIVITY" : "UPDATE_ACTIVITY",
                "PROMO_ACTIVITY",
                activity.getId(),
                request
        );
        return new AdminIdResponse(activity.getId());
    }

    @Transactional
    public AdminIdResponse saveSession(PromotionOperatorContext operator, PromotionSessionAdminRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "session endTime must be after startTime");
        }
        if (activityMapper.selectById(request.activityId()) == null) {
            throw new BizException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }

        PromotionSession session = new PromotionSession();
        session.setId(request.id());
        session.setActivityId(request.activityId());
        session.setName(request.name());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setStatus(request.status());
        session.setSort(request.sort());
        saveSession(session);
        operationLogService.record(
                operator.userId(),
                operator.username(),
                operator.roleCode(),
                request.id() == null ? "CREATE_SESSION" : "UPDATE_SESSION",
                "PROMO_SESSION",
                session.getId(),
                request
        );
        return new AdminIdResponse(session.getId());
    }

    @Transactional
    public AdminIdResponse saveItem(PromotionOperatorContext operator, PromotionSeckillSkuAdminRequest request) {
        PromotionSession session = sessionMapper.selectById(request.sessionId());
        if (session == null || !request.activityId().equals(session.getActivityId())) {
            throw new BizException(ErrorCode.SECKILL_SESSION_NOT_FOUND);
        }
        if (request.availableStock() > request.totalStock()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "availableStock must not exceed totalStock");
        }
        if (request.seckillPrice().compareTo(request.originalPrice()) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "seckillPrice must not exceed originalPrice");
        }

        PromotionSeckillSku item = new PromotionSeckillSku();
        item.setId(request.id());
        item.setActivityId(request.activityId());
        item.setSessionId(request.sessionId());
        item.setSkuId(request.skuId());
        item.setProductId(request.productId());
        item.setProductName(request.productName());
        item.setSkuCode(request.skuCode());
        item.setSubtitle(request.subtitle());
        item.setOriginalPrice(request.originalPrice());
        item.setSeckillPrice(request.seckillPrice());
        item.setTotalStock(request.totalStock());
        item.setAvailableStock(request.availableStock());
        item.setLimitPerUser(request.limitPerUser());
        item.setBadge(request.badge());
        item.setSort(request.sort());
        item.setStatus(request.status());
        saveItem(item);
        operationLogService.record(
                operator.userId(),
                operator.username(),
                operator.roleCode(),
                request.id() == null ? "CREATE_SECKILL_SKU" : "UPDATE_SECKILL_SKU",
                "PROMO_SECKILL_SKU",
                item.getId(),
                request
        );
        return new AdminIdResponse(item.getId());
    }

    private void saveActivity(PromotionActivity activity) {
        if (activity.getId() == null || activityMapper.selectById(activity.getId()) == null) {
            activityMapper.insert(activity);
            return;
        }
        activityMapper.updateById(activity);
    }

    private void saveSession(PromotionSession session) {
        if (session.getId() == null || sessionMapper.selectById(session.getId()) == null) {
            sessionMapper.insert(session);
            return;
        }
        sessionMapper.updateById(session);
    }

    private void saveItem(PromotionSeckillSku item) {
        if (item.getId() == null || seckillSkuMapper.selectById(item.getId()) == null) {
            seckillSkuMapper.insert(item);
            return;
        }
        seckillSkuMapper.updateById(item);
    }
}
