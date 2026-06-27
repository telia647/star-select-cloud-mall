package com.demo.mall.user.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.user.dto.MemberBenefitResponse;
import com.demo.mall.user.dto.MemberCouponResponse;
import com.demo.mall.user.dto.UserRegisterRequest;
import com.demo.mall.user.dto.UserResponse;
import com.demo.mall.user.service.UserAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(userAppService.register(request));
    }

    @GetMapping("/me")
    public Result<UserResponse> me(@RequestHeader(SecurityHeaders.USER_ID) Long userId) {
        return Result.success(userAppService.getById(userId));
    }

    @GetMapping("/me/benefits")
    public Result<List<MemberBenefitResponse>> benefits(@RequestHeader(SecurityHeaders.USER_ID) Long userId) {
        return Result.success(userAppService.listBenefits(userId));
    }

    @GetMapping("/me/coupons")
    public Result<List<MemberCouponResponse>> coupons(@RequestHeader(SecurityHeaders.USER_ID) Long userId) {
        return Result.success(userAppService.listCoupons(userId));
    }
}
