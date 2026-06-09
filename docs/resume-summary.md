# Resume Project Summary

## One-Line Description

Production-oriented Spring Cloud Alibaba flash-sale e-commerce system with C-side mall, B-side seckill operations, Redis Lua pre-deduct, RocketMQ async order creation, Flyway migrations, observability, and automated acceptance scripts.

## Resume Bullets

- Built a microservice e-commerce system with Gateway, Auth, User, Product, Cart, Order, Inventory, Payment, Promotion, and Vue 3 front-end modules.
- Designed a flash-sale order path using short-lived seckill tokens, Redis Lua atomic stock pre-deduct, duplicate buyer guard, server-side price validation, RocketMQ async order creation, and synchronous fallback.
- Hardened consistency with request idempotency, stock lock uniqueness, MQ consumer idempotency, local message retry, order timeout release, Redis reservation rollback, and order/inventory audit logs.
- Delivered B-side seckill management for activity/session/SKU configuration, stock preheat, operation audit, order status timeline, and inventory flow diagnostics.
- Added production-readiness assets: Flyway migrations, GitHub Actions CI, Testcontainers integration coverage, Prometheus metrics, Grafana dashboard, Sentinel gateway rules, k6 pressure-test script, runbook, risk register, and acceptance report.

## Interview Talking Points

1. Hot-path stock is owned by Redis during the sale; MySQL stock is protected from direct high-concurrency writes.
2. Lua combines token validation, duplicate buyer guard, and stock decrement into one atomic Redis operation.
3. `requestId` protects order creation from repeated client retries; consumer logs protect MQ re-consumption.
4. RocketMQ decouples request admission from order creation, while synchronous fallback keeps local acceptance runnable when MQ send fails.
5. Reservations connect Redis pre-deduct stock with order timeout/cancel release, reducing stuck-stock risk.
6. Admin diagnostics expose order status logs and inventory flows, which is close to how production operations troubleshoot incidents.
7. Environment-specific middleware addresses, credentials, JWT secrets, and CORS settings are externalized with `MALL_*` variables.

## Suggested GitHub Repository Description

```text
Production-oriented Spring Cloud Alibaba flash-sale mall with Vue 3, Redis Lua, RocketMQ, MySQL/Flyway, Prometheus/Grafana, Sentinel, CI, and acceptance scripts.
```
