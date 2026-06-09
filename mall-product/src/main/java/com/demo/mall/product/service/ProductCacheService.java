package com.demo.mall.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.product.dto.CategoryResponse;
import com.demo.mall.product.dto.ProductDetailResponse;
import com.demo.mall.product.dto.ProductSkuResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductCacheService {

    private static final String NULL_VALUE = "__NULL__";
    private static final String PRODUCT_DETAIL_KEY = "mall:product:detail:";
    private static final String PRODUCT_SKU_KEY = "mall:product:sku:";
    private static final String CATEGORY_LIST_KEY = "mall:product:categories";
    private static final String LOCK_KEY = "mall:lock:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public CacheValue<ProductDetailResponse> getProductDetail(Long id) {
        return read(PRODUCT_DETAIL_KEY + id, ProductDetailResponse.class);
    }

    public void putProductDetail(Long id, ProductDetailResponse detail) {
        write(PRODUCT_DETAIL_KEY + id, detail, ttlWithJitter(Duration.ofMinutes(30)));
    }

    public void putEmptyProductDetail(Long id) {
        writeEmpty(PRODUCT_DETAIL_KEY + id);
    }

    public CacheValue<ProductSkuResponse> getSku(Long skuId) {
        return read(PRODUCT_SKU_KEY + skuId, ProductSkuResponse.class);
    }

    public void putSku(Long skuId, ProductSkuResponse sku) {
        write(PRODUCT_SKU_KEY + skuId, sku, ttlWithJitter(Duration.ofMinutes(30)));
    }

    public void putEmptySku(Long skuId) {
        writeEmpty(PRODUCT_SKU_KEY + skuId);
    }

    public CacheValue<List<CategoryResponse>> getCategories() {
        return read(CATEGORY_LIST_KEY, new TypeReference<>() {
        });
    }

    public void putCategories(List<CategoryResponse> categories) {
        write(CATEGORY_LIST_KEY, categories, ttlWithJitter(Duration.ofHours(1)));
    }

    public boolean tryLock(String lockName, String token) {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY + lockName, token, Duration.ofSeconds(5));
        return Boolean.TRUE.equals(locked);
    }

    public void unlock(String lockName, String token) {
        String key = LOCK_KEY + lockName;
        String current = redisTemplate.opsForValue().get(key);
        if (Objects.equals(current, token)) {
            redisTemplate.delete(key);
        }
    }

    private <T> CacheValue<T> read(String key, Class<T> type) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return CacheValue.miss();
        }
        if (NULL_VALUE.equals(value)) {
            return CacheValue.emptyHit();
        }
        try {
            return CacheValue.hit(objectMapper.readValue(value, type));
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(key);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "product cache decode failed");
        }
    }

    private <T> CacheValue<T> read(String key, TypeReference<T> type) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return CacheValue.miss();
        }
        if (NULL_VALUE.equals(value)) {
            return CacheValue.emptyHit();
        }
        try {
            return CacheValue.hit(objectMapper.readValue(value, type));
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(key);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "product cache decode failed");
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "product cache encode failed");
        }
    }

    private void writeEmpty(String key) {
        redisTemplate.opsForValue().set(key, NULL_VALUE, Duration.ofMinutes(2));
    }

    private Duration ttlWithJitter(Duration baseTtl) {
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(30, 180));
    }

    public record CacheValue<T>(boolean hit, T value) {

        static <T> CacheValue<T> miss() {
            return new CacheValue<>(false, null);
        }

        static <T> CacheValue<T> emptyHit() {
            return new CacheValue<>(true, null);
        }

        static <T> CacheValue<T> hit(T value) {
            return new CacheValue<>(true, value);
        }
    }
}
