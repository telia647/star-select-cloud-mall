package com.demo.mall.order.service;

final class SeckillRedisKeys {

    private static final String STOCK_KEY = "mall:seckill:stock:";
    private static final String BUYER_KEY = "mall:seckill:buyer:";
    private static final String TOKEN_KEY = "mall:seckill:token:";
    private static final String RESULT_KEY = "mall:seckill:result:";

    private SeckillRedisKeys() {
    }

    static String stockKey(Long activityId, Long sessionId, Long skuId) {
        return STOCK_KEY + activityId + ":" + sessionId + ":" + skuId;
    }

    static String buyerKey(Long userId, Long activityId, Long sessionId, Long skuId) {
        return BUYER_KEY + activityId + ":" + sessionId + ":" + skuId + ":" + userId;
    }

    static String tokenKey(Long userId, Long activityId, Long sessionId, Long skuId) {
        return TOKEN_KEY + activityId + ":" + sessionId + ":" + skuId + ":" + userId;
    }

    static String resultKey(String requestId) {
        return RESULT_KEY + requestId;
    }
}
