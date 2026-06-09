package com.demo.mall.auth.service;

import com.demo.mall.auth.client.UserClient;
import com.demo.mall.auth.client.dto.UserCredentialResponse;
import com.demo.mall.auth.dto.LoginRequest;
import com.demo.mall.auth.dto.LoginResponse;
import com.demo.mall.auth.dto.RefreshTokenRequest;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.common.security.jwt.JwtService;
import com.demo.mall.common.security.jwt.TokenPair;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int ENABLED_STATUS = 1;

    private final UserClient userClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserClient userClient, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userClient = userClient;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Result<UserCredentialResponse> result = userClient.findByUsername(request.username());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }

        UserCredentialResponse user = result.getData();
        if (!Integer.valueOf(ENABLED_STATUS).equals(user.status())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.password())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        return toLoginResponse(jwtService.issueTokens(user.id(), user.username(), user.roleCode()));
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        Claims claims = jwtService.parse(request.refreshToken());
        if (!jwtService.isRefreshToken(claims)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return toLoginResponse(jwtService.issueTokens(
                jwtService.getUserId(claims),
                jwtService.getUsername(claims),
                jwtService.getRoleCode(claims)
        ));
    }

    private LoginResponse toLoginResponse(TokenPair tokenPair) {
        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn(),
                "Bearer"
        );
    }
}
