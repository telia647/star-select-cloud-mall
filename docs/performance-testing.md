# Performance Testing

This project uses k6 for reproducible flash-sale pressure tests.

## Prerequisites

- Gateway and all backend services are running.
- Middleware is running: MySQL, Redis, Nacos, RocketMQ.
- A user exists, for example `demo / 123456`.
- The target seckill session is running.
- Redis activity stock is preheated or lazy preheat is acceptable for a smoke run.

## Smoke Run

```powershell
k6 run scripts/k6/seckill.js
```

Default target:

- `BASE_URL=http://localhost:8080/api`
- `USERNAME=demo`
- `PASSWORD=123456`
- `ACTIVITY_ID=7001`
- `SESSION_ID=7101`
- `SKU_ID=3001`
- `VUS=20`

## Custom Run

```powershell
$env:BASE_URL="http://localhost:8080/api"
$env:USERNAME="demo"
$env:PASSWORD="123456"
$env:ACTIVITY_ID="7001"
$env:SESSION_ID="7101"
$env:SKU_ID="3001"
$env:VUS="100"
$env:HOLD="2m"
k6 run scripts/k6/seckill.js
```

## What It Covers

The script follows the production seckill path:

1. Login and get JWT.
2. Issue short-lived seckill token.
3. Submit seckill request with `activityId`, `sessionId`, `skuId`, `token`, and `requestId`.
4. Poll seckill result while async order creation is in progress.

## Default Thresholds

- HTTP failed request rate below 5%.
- HTTP p95 latency below 1500 ms.
- Seckill submit success rate above 80%.

These are smoke-test thresholds, not capacity targets. Real capacity should be measured with multiple SKU sizes, traffic ramps, Redis/MQ/DB dashboards, and repeated runs.

## Local Windows Fallback

If k6 is not installed on the local Windows host, run a small multi-user acceptance check with PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-seckill.ps1 -Users 10 -Concurrency 3 -Stock 20
```

This fallback is not a replacement for k6 capacity testing. It verifies that multiple users can register or reuse accounts, log in, issue seckill tokens, and submit orders concurrently through the real gateway path.

Latest local fallback result:

```text
Users=10 Concurrency=3 Stock=20
success=10 created=10 failed=0
min=702ms p95=1905ms max=1905ms
```

## Acceptance Notes

For a resume/GitHub demonstration, keep at least one report with:

- Test date and commit.
- Middleware machine specs.
- JVM startup parameters.
- VU profile.
- p50/p95/p99 latency.
- Error rate.
- Redis CPU/memory and command stats.
- RocketMQ send/consume lag.
- MySQL CPU, connection count, and slow SQL.

## Metrics To Capture

During a seckill run, capture these Prometheus queries:

```promql
sum(rate(mall_seckill_submit_total[1m])) by (result)
sum(rate(mall_seckill_submit_total{result="rejected"}[1m])) by (reason)
sum(rate(mall_seckill_order_total[1m])) by (result, mode)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/orders/seckill"}[1m])) by (le))
```

The dedicated seckill counters are emitted by `mall-order`; see `docs/observability.md` for setup.
