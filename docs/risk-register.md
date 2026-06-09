# Production Risk Register

This register tracks the current production-readiness risks after the flash-sale hardening work.

| Area | Current Control | Residual Risk | Next Mitigation |
| --- | --- | --- | --- |
| Traffic spike | Gateway Sentinel rules for login, product read, seckill catalog, token, submit, and order/promotion route caps | Rules are static and not yet managed through Nacos/Sentinel Dashboard | Move rules to dynamic config and add per-IP/user/device risk limits |
| Oversell | Redis Lua pre-deduct, duplicate buyer key, token key, server-side offer validation, reservation release | Redis stock and DB catalog stock are eventually consistent | Keep reconcile task enabled, add alert on negative or divergent stock |
| Duplicate order | `requestId` order idempotency and seckill buyer key | Client may retry with different request IDs | Add user/activity/SKU DB unique purchase record after order creation |
| MQ reliability | Local message retry and consumer idempotency logs | RocketMQ outage can increase order creation latency and fallback pressure | Add dead-letter handling docs and retry dashboard |
| Inventory audit | `wms_stock_lock` unique key and `wms_stock_flow` state transition snapshots | Failed lock attempts are not persisted after transaction rollback | Add rejected stock attempt log if operations need forensic failure analysis |
| Payment consistency | Payment event publishing and idempotent paid handling | Payment is still a mock provider without callback signature verification | Add provider callback state machine and signature verification sample |
| Security | JWT role claim, admin route guard, gateway blocks `/api/**/internal`, env-driven secrets/CORS, RBAC evolution design | Internal services still trust forwarded headers if ports are directly exposed | Deploy services on private network and add service-to-service signing or mTLS |
| Order audit | `oms_order_status_log` records create, seckill create, user cancel, pay success, and expire close transitions; admin can query order status logs and inventory flows | No single merged timeline endpoint yet | Add admin order detail endpoint that merges status logs and inventory flows |
| Observability | Actuator, Prometheus endpoints, seckill counters, trace ID propagation, provisioned Grafana dashboard, Prometheus alert rules | Alert delivery channel is not configured | Add Alertmanager or Grafana contact points for the target environment |
| Testing | Unit tests, CI, Testcontainers inventory integration test skipped without Docker, k6 script, local PowerShell concurrent seckill smoke | End-to-end seckill acceptance is not yet automated in CI | Add docker-enabled integration workflow or nightly environment test |
| Release | Flyway migrations, runbook, env examples, GitHub Actions | No rollback playbook per schema migration | Add migration rollback/forward-fix notes for each risky migration |

## Go/No-Go Checklist

- Backend tests and front-end build pass.
- VM middleware connectivity is verified with explicit `MALL_MYSQL_HOST`, `MALL_REDIS_HOST`, `MALL_NACOS_ADDR`, and `MALL_ROCKETMQ_NAME_SERVER`.
- Gateway, order, promotion, inventory, payment, product, auth, user, and cart health endpoints are green.
- Admin can configure activity/session/SKU and preheat stock.
- Member can issue a token, submit seckill, poll result, and pay the created order.
- Prometheus can scrape `mall-order`, `mall-gateway`, `mall-promotion`, and `mall-inventory`.
- k6 smoke test meets the thresholds in `docs/performance-testing.md`, or the local fallback result from `scripts/load-seckill.ps1` is recorded when k6 is unavailable.
- Inventory `wms_stock_flow` and promotion operation logs contain records for the acceptance flow.
