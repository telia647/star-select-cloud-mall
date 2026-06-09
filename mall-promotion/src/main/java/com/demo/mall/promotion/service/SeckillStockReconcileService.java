package com.demo.mall.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.demo.mall.promotion.entity.PromotionSeckillSku;
import com.demo.mall.promotion.entity.PromotionSession;
import com.demo.mall.promotion.mapper.PromotionSeckillSkuMapper;
import com.demo.mall.promotion.mapper.PromotionSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillStockReconcileService {

    private static final Logger log = LoggerFactory.getLogger(SeckillStockReconcileService.class);

    private static final int ENABLED = 1;
    private static final String STOCK_KEY = "mall:seckill:stock:";

    private final PromotionSessionMapper sessionMapper;
    private final PromotionSeckillSkuMapper seckillSkuMapper;
    private final StringRedisTemplate redisTemplate;

    public SeckillStockReconcileService(PromotionSessionMapper sessionMapper,
                                        PromotionSeckillSkuMapper seckillSkuMapper,
                                        StringRedisTemplate redisTemplate) {
        this.sessionMapper = sessionMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelayString = "${mall.promotion.stock-reconcile-delay-ms:15000}")
    public void reconcileRunningSessionStock() {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<PromotionSession>()
                .eq(PromotionSession::getStatus, ENABLED)
                .le(PromotionSession::getStartTime, now)
                .ge(PromotionSession::getEndTime, now.minusHours(2))
                .last("LIMIT 100"));

        for (PromotionSession session : sessions) {
            try {
                reconcileSession(session);
            } catch (RuntimeException ex) {
                log.warn("Seckill stock reconcile failed, sessionId={}", session.getId(), ex);
            }
        }
    }

    private void reconcileSession(PromotionSession session) {
        List<PromotionSeckillSku> items = seckillSkuMapper.selectList(new LambdaQueryWrapper<PromotionSeckillSku>()
                .eq(PromotionSeckillSku::getSessionId, session.getId())
                .eq(PromotionSeckillSku::getStatus, ENABLED)
                .last("LIMIT 500"));

        for (PromotionSeckillSku item : items) {
            String value = redisTemplate.opsForValue().get(stockKey(item));
            if (value == null) {
                continue;
            }
            Integer redisStock = parseStock(value, item);
            if (redisStock == null || redisStock.equals(item.getAvailableStock())) {
                continue;
            }
            seckillSkuMapper.update(null, new UpdateWrapper<PromotionSeckillSku>()
                    .set("available_stock", redisStock)
                    .eq("id", item.getId()));
        }
    }

    private Integer parseStock(String value, PromotionSeckillSku item) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            log.warn("Invalid seckill stock in Redis, key={}, value={}", stockKey(item), value);
            return null;
        }
    }

    private String stockKey(PromotionSeckillSku item) {
        return STOCK_KEY + item.getActivityId() + ":" + item.getSessionId() + ":" + item.getSkuId();
    }
}
