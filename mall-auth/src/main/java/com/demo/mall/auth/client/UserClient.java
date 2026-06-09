package com.demo.mall.auth.client;

import com.demo.mall.auth.client.dto.UserCredentialResponse;
import com.demo.mall.common.api.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mall-user", path = "/users/internal")
public interface UserClient {

    @GetMapping("/by-username")
    Result<UserCredentialResponse> findByUsername(@RequestParam("username") String username);
}
