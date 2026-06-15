# GitHub Release Guide

This project is positioned as a production-oriented flash-sale e-commerce system, not a static demo.

## Repository Positioning

Short description:

```text
Production-oriented Spring Cloud Alibaba e-commerce system with C-side mall, B-side seckill operations, Redis Lua pre-deduct, RocketMQ async order creation, Flyway migrations, CI, k6 pressure testing, and Prometheus/Grafana observability.
```

## Resume Highlights

- Built a Spring Cloud Alibaba microservice mall with Gateway, Auth, User, Product, Cart, Order, Inventory, Payment, Promotion, and Vue 3 front-end modules.
- Designed a high-concurrency flash-sale flow using short-lived seckill tokens, Redis Lua atomic pre-deduct, duplicate buyer guard, RocketMQ async order creation, and synchronous fallback.
- Hardened transaction consistency with order request idempotency, stock lock uniqueness, local message retry, MQ consumer idempotency, order timeout release, and seckill reservation rollback.
- Added B-side flash-sale operations for activity/session/SKU management, Redis stock preheat, operation audit logs, and order status timeline diagnostics.
- Implemented production-readiness assets: Flyway migrations, GitHub Actions CI, Testcontainers inventory integration test, k6 pressure test script, Sentinel gateway rules, Prometheus/Grafana dashboard, and risk register.

## Evidence Documents

- `docs/acceptance-report.md`
- `docs/resume-summary.md`
- `docs/production-readiness-audit.md`
- `docs/risk-register.md`
- `docs/performance-testing.md`

## Suggested README Screenshots

Add screenshots after local startup acceptance:

| Screenshot | Page |
| --- | --- |
| C-side mall home/product list | `http://localhost:5173` |
| Flash-sale channel | `http://localhost:5173/seckill` |
| Admin seckill console | `http://localhost:5173/admin/seckill` |
| Prometheus targets | `http://localhost:9090/targets` |
| Grafana seckill dashboard | `http://localhost:3000` |
| k6 summary output | terminal after `k6 run scripts/k6/seckill.js` |

Recommended image folder:

```text
docs/assets/
```

## Release Checklist

- `mvn -q test` passes.
- `mvn -q -DskipTests package` passes.
- `cd mall-web && npm.cmd run build` passes.
- Backend services start against VM middleware or containerized middleware.
- Sentinel Dashboard is reachable on `8858`, and gateway rules are documented in `docs/sentinel.md`.
- Admin can create/update activity/session/SKU and preheat stock.
- Member can browse catalog, issue seckill token, submit seckill, poll result, pay order.
- `wms_stock_flow`, `oms_order_status_log`, `promo_operation_log`, and `oms_mq_consume_log` contain records after acceptance.
- Prometheus scrapes backend services and Grafana dashboard shows seckill metrics.
- k6 smoke test meets the thresholds in `docs/performance-testing.md`.
- If k6 is unavailable locally, `scripts/load-seckill.ps1` fallback result is recorded in `docs/acceptance-report.md`.
- `.env` files, local logs, `target`, `node_modules`, and `dist` are not committed.

## Evidence Templates

- `docs/templates/acceptance-report-template.md`
- `docs/templates/performance-report-template.md`

## GitHub Topics

Suggested topics:

```text
spring-boot
spring-cloud-alibaba
vue3
ecommerce
flash-sale
redis
rocketmq
mysql
microservices
prometheus
k6
sentinel
```
