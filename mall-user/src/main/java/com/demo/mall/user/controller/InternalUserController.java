package com.demo.mall.user.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.user.dto.UserCredentialResponse;
import com.demo.mall.user.service.UserAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/internal")
public class InternalUserController {

    private final UserAppService userAppService;

    public InternalUserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    @GetMapping("/by-username")
    public Result<UserCredentialResponse> findByUsername(@RequestParam("username") String username) {
        return Result.success(userAppService.findCredentialByUsername(username));
    }
}
