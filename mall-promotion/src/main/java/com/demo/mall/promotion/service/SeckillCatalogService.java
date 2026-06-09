package com.demo.mall.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.promotion.dto.SeckillItemResponse;
import com.demo.mall.promotion.dto.SeckillSessionResponse;
import com.demo.mall.promotion.dto.SeckillValidateRequest;
import com.demo.mall.promotion.dto.SeckillValidateResponse;
import com.demo.mall.promotion.entity.PromotionActivity;
import com.demo.mall.promotion.entity.PromotionSeckillSku;
import com.demo.mall.promotion.entity.PromotionSession;
import com.demo.mall.promotion.mapper.PromotionActivityMapper;
import com.demo.mall.promotion.mapper.PromotionSeckillSkuMapper;
import com.demo.mall.promotion.mapper.PromotionSessionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillCatalogService {

    private static final int ENABLED = 1;
    private static final String UPCOMING = "UPCOMING";
    private static final String RUNNING = "RUNNING";
    private static final String ENDED = "ENDED";

    private final PromotionActivityMapper activityMapper;
    private final PromotionSessionMapper sessionMapper;
    private final PromotionSeckillSkuMapper seckillSkuMapper;

    public SeckillCatalogService(PromotionActivityMapper activityMapper,
                                 PromotionSessionMapper sessionMapper,
                                 PromotionSeckillSkuMapper seckillSkuMapper) {
        this.activityMapper = activityMapper;
        this.sessionMapper = sessionMapper;
        this.seckillSkuMapper = seckillSkuMapper;
    }

    public List<SeckillSessionResponse> listSessions() {
        LocalDateTime now = LocalDateTime.now();
        return sessionMapper.selectList(new LambdaQueryWrapper<PromotionSession>()
                        .eq(PromotionSession::getStatus, ENABLED)
                        .orderByAsc(PromotionSession::getSort)
                        .orderByAsc(PromotionSession::getStartTime))
                .stream()
                .map(session -> toSessionResponse(session, now))
                .toList();
    }

    public List<SeckillItemResponse> listItems(Long sessionId) {
        PromotionSession session = sessionMapper.selectById(sessionId);
        String state = session == null ? ENDED : stateOf(session, LocalDateTime.now());
        return seckillSkuMapper.selectList(new LambdaQueryWrapper<PromotionSeckillSku>()
                        .eq(PromotionSeckillSku::getSessionId, sessionId)
                        .eq(PromotionSeckillSku::getStatus, ENABLED)
                        .orderByAsc(PromotionSeckillSku::getSort)
                        .orderByAsc(PromotionSeckillSku::getId))
                .stream()
                .map(item -> toItemResponse(item, state))
                .toList();
    }

    public SeckillValidateResponse validateForSubmit(SeckillValidateRequest request) {
        PromotionActivity activity = activityMapper.selectById(request.activityId());
        if (activity == null || !Integer.valueOf(ENABLED).equals(activity.getStatus())) {
            throw new BizException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }

        PromotionSession session = sessionMapper.selectById(request.sessionId());
        if (session == null
                || !request.activityId().equals(session.getActivityId())
                || !Integer.valueOf(ENABLED).equals(session.getStatus())) {
            throw new BizException(ErrorCode.SECKILL_SESSION_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            throw new BizException(ErrorCode.SECKILL_ACTIVITY_NOT_STARTED);
        }
        if (now.isAfter(session.getEndTime())) {
            throw new BizException(ErrorCode.SECKILL_ACTIVITY_ENDED);
        }

        PromotionSeckillSku item = seckillSkuMapper.selectOne(new LambdaQueryWrapper<PromotionSeckillSku>()
                .eq(PromotionSeckillSku::getActivityId, request.activityId())
                .eq(PromotionSeckillSku::getSessionId, request.sessionId())
                .eq(PromotionSeckillSku::getSkuId, request.skuId())
                .eq(PromotionSeckillSku::getStatus, ENABLED)
                .last("LIMIT 1"));
        if (item == null) {
            throw new BizException(ErrorCode.SECKILL_ITEM_NOT_FOUND);
        }
        if (request.quantity() > item.getLimitPerUser()) {
            throw new BizException(ErrorCode.SECKILL_LIMIT_EXCEEDED);
        }
        if (item.getAvailableStock() < request.quantity()) {
            throw new BizException(ErrorCode.SECKILL_SOLD_OUT);
        }

        return new SeckillValidateResponse(
                item.getId(),
                item.getActivityId(),
                item.getSessionId(),
                item.getSkuId(),
                item.getSeckillPrice(),
                item.getAvailableStock(),
                item.getLimitPerUser(),
                session.getEndTime()
        );
    }

    private SeckillSessionResponse toSessionResponse(PromotionSession session, LocalDateTime now) {
        return new SeckillSessionResponse(
                session.getId(),
                session.getActivityId(),
                session.getName(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                stateOf(session, now)
        );
    }

    private SeckillItemResponse toItemResponse(PromotionSeckillSku item, String sessionState) {
        String itemState = item.getAvailableStock() <= 0 ? ENDED : sessionState;
        return new SeckillItemResponse(
                item.getId(),
                item.getActivityId(),
                item.getSessionId(),
                item.getSkuId(),
                item.getProductId(),
                item.getProductName(),
                item.getSkuCode(),
                item.getSubtitle(),
                item.getOriginalPrice(),
                item.getSeckillPrice(),
                item.getTotalStock(),
                item.getAvailableStock(),
                soldPercent(item),
                item.getLimitPerUser(),
                item.getBadge(),
                item.getStatus(),
                itemState
        );
    }

    private String stateOf(PromotionSession session, LocalDateTime now) {
        if (now.isBefore(session.getStartTime())) {
            return UPCOMING;
        }
        if (now.isAfter(session.getEndTime())) {
            return ENDED;
        }
        return RUNNING;
    }

    private int soldPercent(PromotionSeckillSku item) {
        if (item.getTotalStock() == null || item.getTotalStock() <= 0) {
            return 100;
        }
        int sold = Math.max(0, item.getTotalStock() - Math.max(0, item.getAvailableStock()));
        return BigDecimal.valueOf(sold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(item.getTotalStock()), 0, RoundingMode.DOWN)
                .intValue();
    }
}
