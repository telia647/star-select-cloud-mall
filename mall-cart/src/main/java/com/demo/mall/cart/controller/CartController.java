package com.demo.mall.cart.controller;

import com.demo.mall.cart.dto.CartCheckoutRequest;
import com.demo.mall.cart.dto.CartCheckoutResponse;
import com.demo.mall.cart.dto.CartItemAddRequest;
import com.demo.mall.cart.dto.CartItemResponse;
import com.demo.mall.cart.dto.CartItemUpdateRequest;
import com.demo.mall.cart.service.CartService;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public Result<CartItemResponse> add(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                        @Valid @RequestBody CartItemAddRequest request) {
        return Result.success(cartService.add(userId, request));
    }

    @GetMapping("/items")
    public Result<List<CartItemResponse>> list(@RequestHeader(SecurityHeaders.USER_ID) Long userId) {
        return Result.success(cartService.list(userId));
    }

    @PutMapping("/items/{skuId}")
    public Result<CartItemResponse> update(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                           @PathVariable("skuId") Long skuId,
                                           @Valid @RequestBody CartItemUpdateRequest request) {
        return Result.success(cartService.update(userId, skuId, request));
    }

    @DeleteMapping("/items/{skuId}")
    public Result<Void> remove(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                               @PathVariable("skuId") Long skuId) {
        cartService.remove(userId, skuId);
        return Result.success();
    }

    @DeleteMapping("/items")
    public Result<Void> clear(@RequestHeader(SecurityHeaders.USER_ID) Long userId) {
        cartService.clear(userId);
        return Result.success();
    }

    @PostMapping("/checkout")
    public Result<CartCheckoutResponse> checkout(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                 @Valid @RequestBody CartCheckoutRequest request) {
        return Result.success(cartService.checkout(userId, request));
    }
}
