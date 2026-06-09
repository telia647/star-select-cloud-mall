package com.demo.mall.gateway.filter;

import com.demo.mall.common.constants.TraceConstants;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String traceId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header(TraceConstants.TRACE_ID_HEADER, traceId))
                .response(exchange.getResponse())
                .build();
        mutatedExchange.getResponse().getHeaders().set(TraceConstants.TRACE_ID_HEADER, traceId);
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
