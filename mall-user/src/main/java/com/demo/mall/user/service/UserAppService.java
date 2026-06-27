package com.demo.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.user.dto.MemberBenefitResponse;
import com.demo.mall.user.dto.MemberCouponResponse;
import com.demo.mall.user.dto.UserCredentialResponse;
import com.demo.mall.user.dto.UserRegisterRequest;
import com.demo.mall.user.dto.UserResponse;
import com.demo.mall.user.entity.MemberCoupon;
import com.demo.mall.user.entity.User;
import com.demo.mall.user.mapper.MemberCouponMapper;
import com.demo.mall.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAppService {

    private static final int ENABLED_STATUS = 1;
    private static final String MEMBER_ROLE = "MEMBER";

    private final UserMapper userMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAppService(UserMapper userMapper,
                          MemberCouponMapper memberCouponMapper,
                          PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.memberCouponMapper = memberCouponMapper;
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

    public List<MemberBenefitResponse> listBenefits(Long userId) {
        getById(userId);
        return List.of(
                new MemberBenefitResponse("COUPON", "会员专享券", "领取满减券并在下单时抵扣", 1),
                new MemberBenefitResponse("SECKILL_PRIORITY", "秒杀提醒", "关注秒杀场次，提前进入抢购链路", 2),
                new MemberBenefitResponse("SELF_OPERATED", "正品保障", "星选自营商品提供正品和售后保障", 3),
                new MemberBenefitResponse("FREE_SHIPPING", "基础包邮", "自营商品默认免基础运费", 4)
        );
    }

    @Transactional
    public List<MemberCouponResponse> listCoupons(Long userId) {
        getById(userId);
        ensureWelcomeCoupons(userId);
        LocalDateTime now = LocalDateTime.now();
        return memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCoupon>()
                        .eq(MemberCoupon::getUserId, userId)
                        .ge(MemberCoupon::getValidTo, now)
                        .orderByAsc(MemberCoupon::getStatus)
                        .orderByAsc(MemberCoupon::getValidTo))
                .stream()
                .map(this::toCouponResponse)
                .toList();
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

    private void ensureWelcomeCoupons(Long userId) {
        Long count = memberCouponMapper.selectCount(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getUserId, userId));
        if (count != null && count > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<MemberCoupon> coupons = List.of(
                buildCoupon(userId, "新人满 199 减 20", "FULL_REDUCTION", new BigDecimal("20.00"), new BigDecimal("199.00"), now),
                buildCoupon(userId, "会员满 399 减 50", "FULL_REDUCTION", new BigDecimal("50.00"), new BigDecimal("399.00"), now),
                buildCoupon(userId, "秒杀免运券", "SHIPPING", new BigDecimal("8.00"), BigDecimal.ZERO, now)
        );
        coupons.forEach(memberCouponMapper::insert);
    }

    private MemberCoupon buildCoupon(Long userId,
                                     String couponName,
                                     String couponType,
                                     BigDecimal discountAmount,
                                     BigDecimal thresholdAmount,
                                     LocalDateTime now) {
        MemberCoupon coupon = new MemberCoupon();
        coupon.setUserId(userId);
        coupon.setCouponName(couponName);
        coupon.setCouponType(couponType);
        coupon.setDiscountAmount(discountAmount);
        coupon.setThresholdAmount(thresholdAmount);
        coupon.setStatus(1);
        coupon.setValidFrom(now);
        coupon.setValidTo(now.plusDays(30));
        return coupon;
    }

    private MemberCouponResponse toCouponResponse(MemberCoupon coupon) {
        return new MemberCouponResponse(
                coupon.getId(),
                coupon.getCouponName(),
                coupon.getCouponType(),
                coupon.getDiscountAmount(),
                coupon.getThresholdAmount(),
                coupon.getStatus(),
                coupon.getValidFrom(),
                coupon.getValidTo()
        );
    }
}
