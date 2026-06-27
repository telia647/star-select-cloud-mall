package com.demo.mall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@EnableConfigurationProperties(GatewayFlowControlProperties.class)
public class GatewaySentinelConfiguration {

    private static final String API_AUTH_LOGIN = "api_auth_login";
    private static final String API_PRODUCT_READ = "api_product_read";
    private static final String API_SECKILL_CATALOG = "api_seckill_catalog";
    private static final String API_SECKILL_TOKEN = "api_seckill_token";
    private static final String API_SECKILL_SUBMIT = "api_seckill_submit";

    private final GatewayFlowControlProperties flowProperties;

    public GatewaySentinelConfiguration(GatewayFlowControlProperties flowProperties) {
        this.flowProperties = flowProperties;
    }

    @PostConstruct
    public void initGatewayRules() {
        GatewayApiDefinitionManager.loadApiDefinitions(Set.of(
                exactApi(API_AUTH_LOGIN, "/api/auth/login"),
                prefixApi(API_PRODUCT_READ, "/api/products"),
                prefixApi(API_SECKILL_CATALOG, "/api/promotions/seckill"),
                exactApi(API_SECKILL_TOKEN, "/api/orders/seckill/tokens"),
                exactApi(API_SECKILL_SUBMIT, "/api/orders/seckill")
        ));

        GatewayRuleManager.loadRules(Set.of(
                customApiRule(API_AUTH_LOGIN, flowProperties.getAuthLogin()),
                customApiRule(API_PRODUCT_READ, flowProperties.getProductRead()),
                customApiRule(API_SECKILL_CATALOG, flowProperties.getSeckillCatalog()),
                customApiRule(API_SECKILL_TOKEN, flowProperties.getSeckillToken()),
                customApiRule(API_SECKILL_SUBMIT, flowProperties.getSeckillSubmit()),
                routeRule("mall-order", flowProperties.getOrderRoute()),
                routeRule("mall-promotion", flowProperties.getPromotionRoute())
        ));
    }

    private ApiDefinition exactApi(String apiName, String path) {
        return api(apiName, path, SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT);
    }

    private ApiDefinition prefixApi(String apiName, String pathPrefix) {
        return api(apiName, pathPrefix, SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX);
    }

    private ApiDefinition api(String apiName, String pattern, int matchStrategy) {
        return new ApiDefinition(apiName)
                .setPredicateItems(Set.of(new ApiPathPredicateItem()
                        .setPattern(pattern)
                        .setMatchStrategy(matchStrategy)));
    }

    private GatewayFlowRule customApiRule(String apiName, double qps, int burst) {
        return baseRule(apiName, qps, burst)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME);
    }

    private GatewayFlowRule customApiRule(String apiName, GatewayFlowControlProperties.Rule rule) {
        return customApiRule(apiName, rule.qps(), rule.burst());
    }

    private GatewayFlowRule routeRule(String routeId, double qps, int burst) {
        return baseRule(routeId, qps, burst)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
    }

    private GatewayFlowRule routeRule(String routeId, GatewayFlowControlProperties.Rule rule) {
        return routeRule(routeId, rule.qps(), rule.burst());
    }

    private GatewayFlowRule baseRule(String resource, double qps, int burst) {
        return new GatewayFlowRule(resource)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(qps)
                .setBurst(burst)
                .setIntervalSec(1);
    }
}
