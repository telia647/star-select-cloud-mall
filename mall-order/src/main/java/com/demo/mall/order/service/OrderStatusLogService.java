package com.demo.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.order.dto.OrderStatusLogResponse;
import com.demo.mall.order.entity.OrderStatusLog;
import com.demo.mall.order.mapper.OrderStatusLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderStatusLogService {

    public static final String EVENT_CREATE = "CREATE";
    public static final String EVENT_SECKILL_CREATE = "SECKILL_CREATE";
    public static final String EVENT_USER_CANCEL = "USER_CANCEL";
    public static final String EVENT_PAY_SUCCESS = "PAY_SUCCESS";
    public static final String EVENT_EXPIRE_CLOSE = "EXPIRE_CLOSE";

    private final OrderStatusLogMapper orderStatusLogMapper;

    public OrderStatusLogService(OrderStatusLogMapper orderStatusLogMapper) {
        this.orderStatusLogMapper = orderStatusLogMapper;
    }

    public void record(String orderNo,
                       Long userId,
                       Integer fromStatus,
                       Integer toStatus,
                       String eventType,
                       String bizNo,
                       String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderNo(orderNo);
        log.setUserId(userId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setEventType(eventType);
        log.setBizNo(bizNo);
        log.setRemark(trim(remark));
        orderStatusLogMapper.insert(log);
    }

    public List<OrderStatusLogResponse> listByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "orderNo is required");
        }
        return orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderNo, orderNo.trim())
                        .orderByAsc(OrderStatusLog::getCreatedAt)
                        .orderByAsc(OrderStatusLog::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String trim(String remark) {
        if (remark == null || remark.isBlank()) {
            return null;
        }
        return remark.length() > 255 ? remark.substring(0, 255) : remark;
    }

    private OrderStatusLogResponse toResponse(OrderStatusLog log) {
        return new OrderStatusLogResponse(
                log.getOrderNo(),
                log.getUserId(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getEventType(),
                log.getBizNo(),
                log.getRemark(),
                log.getCreatedAt()
        );
    }
}
