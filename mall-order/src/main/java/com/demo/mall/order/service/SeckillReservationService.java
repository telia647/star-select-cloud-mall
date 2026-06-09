package com.demo.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.demo.mall.order.entity.SeckillReservation;
import com.demo.mall.order.mapper.SeckillReservationMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SeckillReservationService {

    private static final int RESERVED = 1;
    private static final int RELEASED = 2;
    private static final int PAID = 3;

    private final SeckillReservationMapper reservationMapper;
    private final StringRedisTemplate redisTemplate;

    public SeckillReservationService(SeckillReservationMapper reservationMapper,
                                     StringRedisTemplate redisTemplate) {
        this.reservationMapper = reservationMapper;
        this.redisTemplate = redisTemplate;
    }

    public void recordReserved(String requestId,
                               String orderNo,
                               Long userId,
                               Long activityId,
                               Long sessionId,
                               Long seckillSkuId,
                               Long skuId,
                               Integer quantity) {
        SeckillReservation reservation = new SeckillReservation();
        reservation.setRequestId(requestId);
        reservation.setOrderNo(orderNo);
        reservation.setUserId(userId);
        reservation.setActivityId(activityId);
        reservation.setSessionId(sessionId);
        reservation.setSeckillSkuId(seckillSkuId);
        reservation.setSkuId(skuId);
        reservation.setQuantity(quantity);
        reservation.setStatus(RESERVED);
        try {
            reservationMapper.insert(reservation);
        } catch (DuplicateKeyException ignored) {
            // Idempotent order retries may attempt to record the same reservation again.
        }
    }

    public void markPaidByOrder(String orderNo, LocalDateTime paidTime) {
        reservationMapper.update(null, new UpdateWrapper<SeckillReservation>()
                .set("status", PAID)
                .set("paid_time", paidTime)
                .eq("order_no", orderNo)
                .eq("status", RESERVED));
    }

    public void releaseByOrder(String orderNo) {
        SeckillReservation reservation = reservationMapper.selectOne(new LambdaQueryWrapper<SeckillReservation>()
                .eq(SeckillReservation::getOrderNo, orderNo));
        if (reservation == null || !Integer.valueOf(RESERVED).equals(reservation.getStatus())) {
            return;
        }

        LocalDateTime releaseTime = LocalDateTime.now();
        int updated = reservationMapper.update(null, new UpdateWrapper<SeckillReservation>()
                .set("status", RELEASED)
                .set("release_time", releaseTime)
                .eq("id", reservation.getId())
                .eq("status", RESERVED));
        if (updated != 1) {
            return;
        }

        redisTemplate.opsForValue().increment(
                SeckillRedisKeys.stockKey(
                        reservation.getActivityId(),
                        reservation.getSessionId(),
                        reservation.getSkuId()
                ),
                reservation.getQuantity()
        );
        redisTemplate.delete(SeckillRedisKeys.buyerKey(
                reservation.getUserId(),
                reservation.getActivityId(),
                reservation.getSessionId(),
                reservation.getSkuId()
        ));
    }
}
