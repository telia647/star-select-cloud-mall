package com.demo.mall.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.cart.client.OrderClient;
import com.demo.mall.cart.client.ProductClient;
import com.demo.mall.cart.client.dto.OrderCreateItemRequest;
import com.demo.mall.cart.client.dto.OrderCreateRequest;
import com.demo.mall.cart.client.dto.OrderCreateResponse;
import com.demo.mall.cart.client.dto.ProductSkuResponse;
import com.demo.mall.cart.dto.CartCheckoutRequest;
import com.demo.mall.cart.dto.CartCheckoutResponse;
import com.demo.mall.cart.dto.CartItemAddRequest;
import com.demo.mall.cart.dto.CartItemResponse;
import com.demo.mall.cart.dto.CartItemUpdateRequest;
import com.demo.mall.cart.entity.CartItem;
import com.demo.mall.cart.mapper.CartItemMapper;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartService {

    private static final Duration SKU_CACHE_TTL = Duration.ofMinutes(5);

    private final CartItemMapper cartItemMapper;
    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final ConcurrentHashMap<Long, CachedSku> skuCache = new ConcurrentHashMap<>();

    public CartService(CartItemMapper cartItemMapper, ProductClient productClient, OrderClient orderClient) {
        this.cartItemMapper = cartItemMapper;
        this.productClient = productClient;
        this.orderClient = orderClient;
    }

    @Transactional
    public CartItemResponse add(Long userId, CartItemAddRequest request) {
        ProductSkuResponse sku = getSku(request.skuId());
        CartItem item = findItem(userId, request.skuId());
        if (item == null) {
            item = new CartItem();
            item.setUserId(userId);
            item.setSkuId(sku.skuId());
            item.setQuantity(request.quantity());
        } else {
            item.setQuantity(item.getQuantity() + request.quantity());
        }
        refreshSkuSnapshot(item, sku);
        if (item.getId() == null) {
            cartItemMapper.insert(item);
        } else {
            cartItemMapper.updateById(item);
        }
        return toResponse(item);
    }

    public List<CartItemResponse> list(Long userId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CartItemResponse update(Long userId, Long skuId, CartItemUpdateRequest request) {
        CartItem item = findItem(userId, skuId);
        if (item == null) {
            throw new BizException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        item.setQuantity(request.quantity());
        refreshSkuSnapshot(item, getSku(skuId));
        cartItemMapper.updateById(item);
        return toResponse(item);
    }

    @Transactional
    public void remove(Long userId, Long skuId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId));
    }

    @Transactional
    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
    }

    @Transactional
    public CartCheckoutResponse checkout(Long userId, CartCheckoutRequest request) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByAsc(CartItem::getId));
        if (items.isEmpty()) {
            throw new BizException(ErrorCode.ORDER_ITEM_EMPTY);
        }
        Result<OrderCreateResponse> orderResult = orderClient.create(userId, new OrderCreateRequest(
                items.stream()
                        .map(item -> new OrderCreateItemRequest(item.getSkuId(), item.getQuantity()))
                        .toList(),
                request.remark(),
                request.requestId()
        ));
        assertSuccess(orderResult);
        clear(userId);
        OrderCreateResponse order = orderResult.getData();
        return new CartCheckoutResponse(order.orderNo(), order.totalAmount(), order.status());
    }

    private CartItem findItem(Long userId, Long skuId) {
        return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId));
    }

    private ProductSkuResponse getSku(Long skuId) {
        CachedSku cached = skuCache.get(skuId);
        if (cached != null && !cached.isExpired()) {
            return cached.sku();
        }

        Result<ProductSkuResponse> result = productClient.getSku(skuId);
        assertSuccess(result);
        ProductSkuResponse sku = result.getData();
        skuCache.put(skuId, new CachedSku(sku, Instant.now().plus(SKU_CACHE_TTL)));
        return sku;
    }

    private void refreshSkuSnapshot(CartItem item, ProductSkuResponse sku) {
        item.setProductId(sku.productId());
        item.setProductName(sku.productName());
        item.setSkuCode(sku.skuCode());
        item.setSpecJson(sku.specJson());
        item.setPrice(sku.price());
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getSkuId(),
                item.getProductId(),
                item.getProductName(),
                item.getSkuCode(),
                item.getSpecJson(),
                item.getQuantity(),
                item.getPrice(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }

    private void assertSuccess(Result<?> result) {
        if (result == null || !result.isSuccess()) {
            String message = result == null ? "remote service call failed" : result.getMessage();
            throw new BizException(ErrorCode.INTERNAL_ERROR, message);
        }
    }

    private record CachedSku(ProductSkuResponse sku, Instant expiresAt) {

        private boolean isExpired() {
            return !Instant.now().isBefore(expiresAt);
        }
    }
}
