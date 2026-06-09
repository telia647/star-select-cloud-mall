package com.demo.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.user.dto.UserCredentialResponse;
import com.demo.mall.user.dto.UserRegisterRequest;
import com.demo.mall.user.dto.UserResponse;
import com.demo.mall.user.entity.User;
import com.demo.mall.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAppService {

    private static final int ENABLED_STATUS = 1;
    private static final String MEMBER_ROLE = "MEMBER";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAppService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(UserRegisterRequest request) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (existing != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setStatus(ENABLED_STATUS);
        user.setRoleCode(MEMBER_ROLE);
        userMapper.insert(user);
        return toResponse(user);
    }

    public UserResponse getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toResponse(user);
    }

    public UserCredentialResponse findCredentialByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return new UserCredentialResponse(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus(),
                user.getRoleCode()
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getPhone(), user.getStatus(), user.getRoleCode());
    }
}
