package com.demo.mall.user.service;

import com.demo.mall.user.dto.MemberCouponResponse;
import com.demo.mall.user.entity.MemberCoupon;
import com.demo.mall.user.entity.User;
import com.demo.mall.user.mapper.MemberCouponMapper;
import com.demo.mall.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAppServiceTest {

    private UserMapper userMapper;
    private MemberCouponMapper memberCouponMapper;
    private UserAppService userAppService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        memberCouponMapper = mock(MemberCouponMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        userAppService = new UserAppService(userMapper, memberCouponMapper, passwordEncoder);
    }

    @Test
    void listCouponsIssuesWelcomeCouponsForFirstVisit() {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setStatus(1);
        user.setRoleCode("MEMBER");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(memberCouponMapper.selectCount(any())).thenReturn(0L);
        when(memberCouponMapper.selectList(any())).thenReturn(List.of(
                coupon(1001L, "新人满 199 减 20", new BigDecimal("20.00")),
                coupon(1002L, "会员满 399 减 50", new BigDecimal("50.00"))
        ));

        List<MemberCouponResponse> coupons = userAppService.listCoupons(1L);

        ArgumentCaptor<MemberCoupon> couponCaptor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponMapper, times(3)).insert(couponCaptor.capture());
        assertThat(couponCaptor.getAllValues())
                .extracting(MemberCoupon::getCouponName)
                .containsExactly("新人满 199 减 20", "会员满 399 减 50", "秒杀免运券");
        assertThat(coupons).hasSize(2);
    }

    private MemberCoupon coupon(Long id, String name, BigDecimal discountAmount) {
        MemberCoupon coupon = new MemberCoupon();
        coupon.setId(id);
        coupon.setUserId(1L);
        coupon.setCouponName(name);
        coupon.setCouponType("FULL_REDUCTION");
        coupon.setDiscountAmount(discountAmount);
        coupon.setThresholdAmount(new BigDecimal("199.00"));
        coupon.setStatus(1);
        coupon.setValidFrom(LocalDateTime.now());
        coupon.setValidTo(LocalDateTime.now().plusDays(30));
        return coupon;
    }
}
