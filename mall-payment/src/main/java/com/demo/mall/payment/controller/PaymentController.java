package com.demo.mall.payment.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.payment.dto.PaymentCallbackRequest;
import com.demo.mall.payment.dto.PaymentRequest;
import com.demo.mall.payment.dto.PaymentResponse;
import com.demo.mall.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay")
    public Result<PaymentResponse> pay(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                       @Valid @RequestBody PaymentRequest request) {
        return Result.success(paymentService.pay(userId, request));
    }

    @PostMapping("/callback/mock")
    public Result<PaymentResponse> mockCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        return Result.success(paymentService.callback(request));
    }

    @GetMapping("/{payNo}")
    public Result<PaymentResponse> detail(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                          @PathVariable("payNo") String payNo) {
        return Result.success(paymentService.detail(userId, payNo));
    }
}
