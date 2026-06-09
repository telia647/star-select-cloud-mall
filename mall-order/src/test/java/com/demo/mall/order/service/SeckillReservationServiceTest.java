package com.demo.mall.order.service;

import com.demo.mall.order.entity.SeckillReservation;
import com.demo.mall.order.mapper.SeckillReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillReservationServiceTest {

    private SeckillReservationMapper reservationMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SeckillReservationService service;

    @BeforeEach
    void setUp() {
        reservationMapper = mock(SeckillReservationMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new SeckillReservationService(reservationMapper, redisTemplate);
    }

    @Test
    void releaseByOrderRestoresRedisStockOnce() {
        SeckillReservation reservation = new SeckillReservation();
        reservation.setId(10L);
        reservation.setOrderNo("O1001");
        reservation.setUserId(1L);
        reservation.setActivityId(7001L);
        reservation.setSessionId(7101L);
        reservation.setSkuId(3001L);
        reservation.setQuantity(1);
        reservation.setStatus(1);
        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(reservationMapper.update(any(), any())).thenReturn(1);

        service.releaseByOrder("O1001");

        verify(valueOperations).increment("mall:seckill:stock:7001:7101:3001", 1);
        verify(redisTemplate).delete("mall:seckill:buyer:7001:7101:3001:1");
    }

    @Test
    void releaseByOrderSkipsNonReservedReservation() {
        SeckillReservation reservation = new SeckillReservation();
        reservation.setStatus(3);
        when(reservationMapper.selectOne(any())).thenReturn(reservation);

        service.releaseByOrder("O1001");

        verify(reservationMapper, never()).update(any(), any());
        verify(valueOperations, never()).increment(any(), anyLong());
    }
}
