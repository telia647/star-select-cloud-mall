package com.demo.mall.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.gateway.flow")
public class GatewayFlowControlProperties {

    private Rule authLogin = new Rule(80, 20);
    private Rule productRead = new Rule(600, 100);
    private Rule seckillCatalog = new Rule(500, 100);
    private Rule seckillToken = new Rule(200, 50);
    private Rule seckillSubmit = new Rule(120, 30);
    private Rule orderRoute = new Rule(700, 100);
    private Rule promotionRoute = new Rule(700, 100);

    public Rule getAuthLogin() {
        return authLogin;
    }

    public void setAuthLogin(Rule authLogin) {
        this.authLogin = authLogin;
    }

    public Rule getProductRead() {
        return productRead;
    }

    public void setProductRead(Rule productRead) {
        this.productRead = productRead;
    }

    public Rule getSeckillCatalog() {
        return seckillCatalog;
    }

    public void setSeckillCatalog(Rule seckillCatalog) {
        this.seckillCatalog = seckillCatalog;
    }

    public Rule getSeckillToken() {
        return seckillToken;
    }

    public void setSeckillToken(Rule seckillToken) {
        this.seckillToken = seckillToken;
    }

    public Rule getSeckillSubmit() {
        return seckillSubmit;
    }

    public void setSeckillSubmit(Rule seckillSubmit) {
        this.seckillSubmit = seckillSubmit;
    }

    public Rule getOrderRoute() {
        return orderRoute;
    }

    public void setOrderRoute(Rule orderRoute) {
        this.orderRoute = orderRoute;
    }

    public Rule getPromotionRoute() {
        return promotionRoute;
    }

    public void setPromotionRoute(Rule promotionRoute) {
        this.promotionRoute = promotionRoute;
    }

    public record Rule(double qps, int burst) {
    }
}
