package com.demo.mall.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.security")
public class SecurityHardeningProperties {

    private boolean failOnDefaultSecret = false;

    public boolean isFailOnDefaultSecret() {
        return failOnDefaultSecret;
    }

    public void setFailOnDefaultSecret(boolean failOnDefaultSecret) {
        this.failOnDefaultSecret = failOnDefaultSecret;
    }
}
