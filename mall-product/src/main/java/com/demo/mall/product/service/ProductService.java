package com.demo.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.mall.common.api.PageResult;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.product.dto.ProductDetailResponse;
import com.demo.mall.product.dto.ProductListItemResponse;
import com.demo.mall.product.dto.ProductQueryRequest;
import com.demo.mall.product.dto.ProductSkuResponse;
import com.demo.mall.product.dto.SkuResponse;
import com.demo.mall.product.entity.Product;
import com.demo.mall.product.entity.Shop;
import com.demo.mall.product.entity.Sku;
import com.demo.mall.product.mapper.ProductMapper;
import com.demo.mall.product.mapper.ShopMapper;
import com.demo.mall.product.mapper.SkuMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class ProductService {

    private static final int ENABLED_STATUS = 1;

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final ShopMapper shopMapper;
    private final ProductCacheService productCacheService;

    public ProductService(ProductMapper productMapper,
                          SkuMapper skuMapper,
                          ShopMapper shopMapper,
                          ProductCacheService productCacheService) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.shopMapper = shopMapper;
        this.productCacheService = productCacheService;
    }

    public PageResult<ProductListItemResponse> page(ProductQueryRequest request) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ENABLED_STATUS)
                .eq(request.categoryId() != null, Product::getCategoryId, request.categoryId())
                .like(request.keyword() != null && !request.keyword().isBlank(), Product::getName, request.keyword())
                .orderByDesc(Product::getId);

        IPage<Product> page = productMapper.selectPage(
                new Page<>(request.normalizedPageNo(), request.normalizedPageSize()),
                wrapper
        );

        Map<Long, Shop> shopsById = findShops(page.getRecords());
        List<ProductListItemResponse> records = page.getRecords()
                .stream()
                .map(product -> toListItem(product, shopsById))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public ProductDetailResponse detail(Long id) {
        ProductCacheService.CacheValue<ProductDetailResponse> cached = productCacheService.getProductDetail(id);
        if (cached.hit()) {
            if (cached.value() == null) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            return cached.value();
        }

        String token = UUID.randomUUID().toString();
        if (!productCacheService.tryLock("product:detail:" + id, token)) {
            sleepBriefly();
            cached = productCacheService.getProductDetail(id);
            if (cached.hit()) {
                if (cached.value() == null) {
                    throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                return cached.value();
            }
            return loadDetailFromDb(id);
        }

        try {
            cached = productCacheService.getProductDetail(id);
            if (cached.hit()) {
                if (cached.value() == null) {
                    throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
                }
                return cached.value();
            }
            ProductDetailResponse detail = loadDetailFromDb(id);
            productCacheService.putProductDetail(id, detail);
            return detail;
        } catch (BizException ex) {
            if (ex.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND) {
                productCacheService.putEmptyProductDetail(id);
            }
            throw ex;
        } finally {
            productCacheService.unlock("product:detail:" + id, token);
        }
    }

    public ProductSkuResponse getSku(Long skuId) {
        ProductCacheService.CacheValue<ProductSkuResponse> cached = productCacheService.getSku(skuId);
        if (cached.hit()) {
            if (cached.value() == null) {
                throw new BizException(ErrorCode.SKU_NOT_FOUND);
            }
            return cached.value();
        }

        String token = UUID.randomUUID().toString();
        if (!productCacheService.tryLock("product:sku:" + skuId, token)) {
            sleepBriefly();
            cached = productCacheService.getSku(skuId);
            if (cached.hit()) {
                if (cached.value() == null) {
                    throw new BizException(ErrorCode.SKU_NOT_FOUND);
                }
                return cached.value();
            }
            return loadSkuFromDb(skuId);
        }

        try {
            cached = productCacheService.getSku(skuId);
            if (cached.hit()) {
                if (cached.value() == null) {
                    throw new BizException(ErrorCode.SKU_NOT_FOUND);
                }
                return cached.value();
            }
            ProductSkuResponse sku = loadSkuFromDb(skuId);
            productCacheService.putSku(skuId, sku);
            return sku;
        } catch (BizException ex) {
            if (ex.getErrorCode() == ErrorCode.SKU_NOT_FOUND) {
                productCacheService.putEmptySku(skuId);
            }
            throw ex;
        } finally {
            productCacheService.unlock("product:sku:" + skuId, token);
        }
    }

    private ProductDetailResponse loadDetailFromDb(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Integer.valueOf(ENABLED_STATUS).equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<SkuResponse> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                        .eq(Sku::getProductId, id)
                        .eq(Sku::getStatus, ENABLED_STATUS)
                        .orderByAsc(Sku::getId))
                .stream()
                .map(this::toSkuResponse)
                .toList();

        return new ProductDetailResponse(
                product.getId(),
                product.getCategoryId(),
                product.getShopId(),
                shopName(product.getShopId()),
                product.getName(),
                product.getSubtitle(),
                product.getMainImage(),
                product.getGalleryImages(),
                product.getStatus(),
                skus
        );
    }

    private ProductSkuResponse loadSkuFromDb(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null || !Integer.valueOf(ENABLED_STATUS).equals(sku.getStatus())) {
            throw new BizException(ErrorCode.SKU_NOT_FOUND);
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || !Integer.valueOf(ENABLED_STATUS).equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return new ProductSkuResponse(
                sku.getId(),
                sku.getProductId(),
                product.getName(),
                sku.getSkuCode(),
                sku.getSpecJson(),
                sku.getPrice(),
                sku.getStatus()
        );
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private ProductListItemResponse toListItem(Product product, Map<Long, Shop> shopsById) {
        Shop shop = product.getShopId() == null ? null : shopsById.get(product.getShopId());
        return new ProductListItemResponse(
                product.getId(),
                product.getCategoryId(),
                product.getShopId(),
                shop == null ? null : shop.getName(),
                product.getName(),
                product.getSubtitle(),
                product.getMainImage(),
                product.getStatus()
        );
    }

    private Map<Long, Shop> findShops(List<Product> products) {
        List<Long> shopIds = products.stream()
                .map(Product::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (shopIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return shopMapper.selectBatchIds(shopIds)
                .stream()
                .collect(Collectors.toMap(Shop::getId, Function.identity(), (left, right) -> left));
    }

    private String shopName(Long shopId) {
        if (shopId == null) {
            return null;
        }
        Shop shop = shopMapper.selectById(shopId);
        return shop == null ? null : shop.getName();
    }

    private SkuResponse toSkuResponse(Sku sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getProductId(),
                sku.getSkuCode(),
                sku.getSpecJson(),
                sku.getPrice(),
                sku.getStatus()
        );
    }
}
