package com.demo.mall.order.service;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.common.event.SeckillOrderEvent;
import com.demo.mall.order.client.PromotionClient;
import com.demo.mall.order.client.dto.SeckillValidateRequest;
import com.demo.mall.order.client.dto.SeckillValidateResponse;
import com.demo.mall.order.dto.OrderCreateResponse;
import com.demo.mall.order.dto.SeckillCreateRequest;
import com.demo.mall.order.dto.SeckillCreateResponse;
import com.demo.mall.order.dto.SeckillOrderState;
import com.demo.mall.order.dto.SeckillStockRequest;
import com.demo.mall.order.dto.SeckillTokenRequest;
import com.demo.mall.order.dto.SeckillTokenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SeckillService {

    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_FAILED = "FAILED";
    private static final Duration REQUEST_TTL = Duration.ofHours(2);
    private static final DateTimeFormatter REQUEST_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final RedisScript<Long> PRE_DEDUCT_SCRIPT = RedisScript.of("""
            if redis.call('exists', KEYS[2]) == 1 then
                return 2
            end
            local token = redis.call('get', KEYS[3])
            if not token or token ~= ARGV[4] then
                return 3
            end
            local stock = redis.call('get', KEYS[1])
            if not stock then
                return -1
            end
            local quantity = tonumber(ARGV[1])
            if tonumber(stock) < quantity then
                return 0
            end
            redis.call('decrby', KEYS[1], quantity)
            redis.call('set', KEYS[2], ARGV[2], 'EX', ARGV[3])
            redis.call('del', KEYS[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;
    private final PromotionClient promotionClient;
    private final SeckillMetrics seckillMetrics;

    public SeckillService(StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper,
                          OrderService orderService,
                          OrderEventPublisher orderEventPublisher,
                          PromotionClient promotionClient,
                          SeckillMetrics seckillMetrics) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.orderEventPublisher = orderEventPublisher;
        this.promotionClient = promotionClient;
        this.seckillMetrics = seckillMetrics;
    }

    public void initStock(SeckillStockRequest request) {
        redisTemplate.opsForValue().set(
                SeckillRedisKeys.stockKey(request.activityId(), request.sessionId(), request.skuId()),
                String.valueOf(request.quantity())
        );
        seckillMetrics.stockInitialized();
    }

    public SeckillTokenResponse issueToken(Long userId, SeckillTokenRequest request) {
        SeckillValidateResponse offer = validateOffer(
                request.activityId(),
                request.sessionId(),
                request.skuId(),
                request.quantity()
        );
        ensureStockReady(offer);

        Duration ttl = tokenTtl(offer);
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                SeckillRedisKeys.tokenKey(userId, offer.activityId(), offer.sessionId(), offer.skuId()),
                token,
                ttl
        );
        seckillMetrics.tokenIssued();
        return new SeckillTokenResponse(token, ttl.toSeconds());
    }

    public SeckillCreateResponse submit(Long userId, SeckillCreateRequest request) {
        SeckillValidateResponse offer = validateOffer(request);
        ensureStockReady(offer);

        String requestId = normalizeRequestId(request.requestId());
        if (requestId == null) {
            requestId = nextRequestId();
        }
        String stockKey = SeckillRedisKeys.stockKey(offer.activityId(), offer.sessionId(), offer.skuId());
        String buyerKey = SeckillRedisKeys.buyerKey(userId, offer.activityId(), offer.sessionId(), offer.skuId());
        String tokenKey = SeckillRedisKeys.tokenKey(userId, offer.activityId(), offer.sessionId(), offer.skuId());
        Long result = redisTemplate.execute(
                PRE_DEDUCT_SCRIPT,
                List.of(stockKey, buyerKey, tokenKey),
                String.valueOf(request.quantity()),
                requestId,
                String.valueOf(REQUEST_TTL.toSeconds()),
                request.token()
        );

        if (result == null) {
            seckillMetrics.submitRejected("redis_script_null");
            throw new BizException(ErrorCode.INTERNAL_ERROR, "seckill stock pre-deduct failed");
        }
        if (Long.valueOf(-1).equals(result)) {
            seckillMetrics.submitRejected("stock_not_ready");
            throw new BizException(ErrorCode.SECKILL_STOCK_NOT_READY);
        }
        if (Long.valueOf(0).equals(result)) {
            seckillMetrics.submitRejected("sold_out");
            throw new BizException(ErrorCode.SECKILL_SOLD_OUT);
        }
        if (Long.valueOf(2).equals(result)) {
            seckillMetrics.submitDuplicate();
            return existingResult(userId, offer.activityId(), offer.sessionId(), offer.skuId());
        }
        if (Long.valueOf(3).equals(result)) {
            seckillMetrics.submitRejected("invalid_token");
            throw new BizException(ErrorCode.SECKILL_TOKEN_INVALID);
        }

        SeckillOrderState accepted = new SeckillOrderState(
                userId,
                offer.activityId(),
                offer.sessionId(),
                offer.seckillSkuId(),
                offer.skuId(),
                request.quantity(),
                requestId,
                null,
                STATUS_ACCEPTED,
                "seckill request accepted"
        );
        saveState(accepted);

        SeckillOrderEvent event = new SeckillOrderEvent(
                requestId,
                userId,
                offer.activityId(),
                offer.sessionId(),
                offer.seckillSkuId(),
                offer.seckillPrice(),
                offer.skuId(),
                request.quantity(),
                "seckill"
        );
        if (orderEventPublisher.publishSeckillOrder(event)) {
            seckillMetrics.submitAccepted("async");
            return toResponse(accepted);
        }

        return createSynchronously(event);
    }

    public void consume(SeckillOrderEvent event) {
        SeckillOrderState current = readState(event.requestId());
        if (current != null && STATUS_CREATED.equals(current.status())) {
            return;
        }
        try {
            OrderCreateResponse order = createOrder(event);
            saveState(new SeckillOrderState(
                    event.userId(),
                    event.activityId(),
                    event.sessionId(),
                    event.seckillSkuId(),
                    event.skuId(),
                    event.quantity(),
                    event.requestId(),
                    order.orderNo(),
                    STATUS_CREATED,
                    "seckill order created"
            ));
            seckillMetrics.orderCreated("async");
        } catch (RuntimeException ex) {
            rollbackReservation(event);
            saveState(new SeckillOrderState(
                    event.userId(),
                    event.activityId(),
                    event.sessionId(),
                    event.seckillSkuId(),
                    event.skuId(),
                    event.quantity(),
                    event.requestId(),
                    null,
                    STATUS_FAILED,
                    trimError(ex.getMessage())
            ));
            seckillMetrics.orderFailed("async");
        }
    }

    public SeckillCreateResponse result(Long userId, String requestId) {
        SeckillOrderState state = readState(requestId);
        if (state == null) {
            throw new BizException(ErrorCode.SECKILL_REQUEST_NOT_FOUND);
        }
        if (!state.userId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toResponse(state);
    }

    private SeckillCreateResponse createSynchronously(SeckillOrderEvent event) {
        try {
            OrderCreateResponse order = createOrder(event);
            SeckillOrderState created = new SeckillOrderState(
                    event.userId(),
                    event.activityId(),
                    event.sessionId(),
                    event.seckillSkuId(),
                    event.skuId(),
                    event.quantity(),
                    event.requestId(),
                    order.orderNo(),
                    STATUS_CREATED,
                    "seckill order created"
            );
            saveState(created);
            seckillMetrics.submitAccepted("sync");
            seckillMetrics.orderCreated("sync");
            return toResponse(created);
        } catch (RuntimeException ex) {
            rollbackReservation(event);
            saveState(new SeckillOrderState(
                    event.userId(),
                    event.activityId(),
                    event.sessionId(),
                    event.seckillSkuId(),
                    event.skuId(),
                    event.quantity(),
                    event.requestId(),
                    null,
                    STATUS_FAILED,
                    trimError(ex.getMessage())
            ));
            seckillMetrics.orderFailed("sync");
            throw ex;
        }
    }

    private OrderCreateResponse createOrder(SeckillOrderEvent event) {
        return orderService.createSeckill(
                event.userId(),
                event.activityId(),
                event.sessionId(),
                event.seckillSkuId(),
                event.skuId(),
                event.quantity(),
                event.seckillPrice(),
                event.requestId(),
                event.remark()
        );
    }

    private SeckillCreateResponse existingResult(Long userId, Long activityId, Long sessionId, Long skuId) {
        String requestId = redisTemplate.opsForValue()
                .get(SeckillRedisKeys.buyerKey(userId, activityId, sessionId, skuId));
        if (requestId == null) {
            throw new BizException(ErrorCode.SECKILL_REQUEST_NOT_FOUND);
        }
        SeckillOrderState state = readState(requestId);
        if (state == null) {
            return new SeckillCreateResponse(requestId, null, STATUS_ACCEPTED, "seckill request accepted");
        }
        return toResponse(state);
    }

    private void rollbackReservation(SeckillOrderEvent event) {
        redisTemplate.opsForValue().increment(
                SeckillRedisKeys.stockKey(event.activityId(), event.sessionId(), event.skuId()),
                event.quantity()
        );
        redisTemplate.delete(SeckillRedisKeys.buyerKey(
                event.userId(),
                event.activityId(),
                event.sessionId(),
                event.skuId()
        ));
    }

    private void saveState(SeckillOrderState state) {
        try {
            redisTemplate.opsForValue()
                    .set(SeckillRedisKeys.resultKey(state.requestId()), objectMapper.writeValueAsString(state), REQUEST_TTL);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "seckill state encode failed");
        }
    }

    private SeckillOrderState readState(String requestId) {
        String value = redisTemplate.opsForValue().get(SeckillRedisKeys.resultKey(requestId));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, SeckillOrderState.class);
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(SeckillRedisKeys.resultKey(requestId));
            throw new BizException(ErrorCode.INTERNAL_ERROR, "seckill state decode failed");
        }
    }

    private SeckillCreateResponse toResponse(SeckillOrderState state) {
        return new SeckillCreateResponse(state.requestId(), state.orderNo(), state.status(), state.message());
    }

    private SeckillValidateResponse validateOffer(SeckillCreateRequest request) {
        return validateOffer(request.activityId(), request.sessionId(), request.skuId(), request.quantity());
    }

    private SeckillValidateResponse validateOffer(Long activityId, Long sessionId, Long skuId, Integer quantity) {
        Result<SeckillValidateResponse> result = promotionClient.validate(new SeckillValidateRequest(
                activityId,
                sessionId,
                skuId,
                quantity
        ));
        if (result == null || !result.isSuccess()) {
            ErrorCode errorCode = result == null ? ErrorCode.INTERNAL_ERROR : ErrorCode.fromCode(result.getCode());
            String message = result == null ? "promotion validation failed" : result.getMessage();
            throw new BizException(errorCode, message);
        }
        if (result.getData() == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "promotion validation response is empty");
        }
        return result.getData();
    }

    private void ensureStockReady(SeckillValidateResponse offer) {
        String stockKey = SeckillRedisKeys.stockKey(offer.activityId(), offer.sessionId(), offer.skuId());
        Duration ttl = Duration.between(LocalDateTime.now(), offer.sessionEndTime().plus(REQUEST_TTL));
        if (ttl.isZero() || ttl.isNegative()) {
            ttl = REQUEST_TTL;
        }
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(offer.availableStock()), ttl);
    }

    private Duration tokenTtl(SeckillValidateResponse offer) {
        Duration ttl = Duration.between(LocalDateTime.now(), offer.sessionEndTime());
        if (ttl.isZero() || ttl.isNegative()) {
            throw new BizException(ErrorCode.SECKILL_ACTIVITY_ENDED);
        }
        return ttl;
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId.trim();
    }

    private String nextRequestId() {
        return "SK" + LocalDateTime.now().format(REQUEST_ID_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return "seckill order create failed";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
