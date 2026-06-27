package com.demo.mall.user.dto;

public record MemberBenefitResponse(
        String code,
        String title,
        String description,
        Integer sort
) {
}
