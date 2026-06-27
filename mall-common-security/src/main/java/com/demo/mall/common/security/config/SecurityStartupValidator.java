package com.demo.mall.common.security.config;

import com.demo.mall.common.security.jwt.JwtProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator implements InitializingBean {

    private final JwtProperties jwtProperties;
    private final SecurityHardeningProperties hardeningProperties;

    public SecurityStartupValidator(JwtProperties jwtProperties, SecurityHardeningProperties hardeningProperties) {
        this.jwtProperties = jwtProperties;
        this.hardeningProperties = hardeningProperties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!hardeningProperties.isFailOnDefaultSecret()) {
            return;
        }
        String secret = jwtProperties.getSecret();
        if (secret == null
                || secret.isBlank()
                || JwtProperties.DEFAULT_DEVELOPMENT_SECRET.equals(secret)
                || "mall-demo-jwt-secret-key-for-hs256-please-change".equals(secret)
                || "replace-with-at-least-32-byte-secret".equals(secret)) {
            throw new IllegalStateException("MALL_JWT_SECRET must be replaced before production startup");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("MALL_JWT_SECRET must contain at least 32 characters");
        }
    }
}
