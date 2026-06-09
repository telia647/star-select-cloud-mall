package com.demo.mall.common.error;

public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    CONFLICT(409, "conflict"),
    INTERNAL_ERROR(500, "internal server error"),

    INVALID_CREDENTIALS(10001, "invalid username or password"),
    USER_DISABLED(10002, "user is disabled"),
    USERNAME_EXISTS(10003, "username already exists"),
    USER_NOT_FOUND(10004, "user not found"),

    PRODUCT_NOT_FOUND(20001, "product not found"),
    SKU_NOT_FOUND(20002, "sku not found"),

    CART_ITEM_NOT_FOUND(30001, "cart item not found"),

    ORDER_NOT_FOUND(40001, "order not found"),
    ORDER_STATUS_INVALID(40002, "order status invalid"),
    ORDER_ITEM_EMPTY(40003, "order item is empty"),
    SECKILL_STOCK_NOT_READY(40004, "seckill stock is not ready"),
    SECKILL_SOLD_OUT(40005, "seckill stock is sold out"),
    SECKILL_REQUEST_NOT_FOUND(40006, "seckill request not found"),
    SECKILL_ACTIVITY_NOT_FOUND(40007, "seckill activity not found"),
    SECKILL_SESSION_NOT_FOUND(40008, "seckill session not found"),
    SECKILL_ITEM_NOT_FOUND(40009, "seckill item not found"),
    SECKILL_ACTIVITY_NOT_STARTED(40010, "seckill activity has not started"),
    SECKILL_ACTIVITY_ENDED(40011, "seckill activity has ended"),
    SECKILL_LIMIT_EXCEEDED(40012, "seckill purchase limit exceeded"),
    SECKILL_TOKEN_INVALID(40013, "seckill token is invalid"),

    STOCK_NOT_FOUND(50001, "stock not found"),
    STOCK_NOT_ENOUGH(50002, "stock is not enough"),
    STOCK_LOCK_NOT_FOUND(50003, "stock lock not found"),

    PAYMENT_NOT_FOUND(60001, "payment not found"),
    PAYMENT_ORDER_INVALID(60002, "payment order invalid");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
