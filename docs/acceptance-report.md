# Acceptance Report

Date: 2026-06-27

Scope: local Windows Java services, Vue front-end build, and VM Docker middleware acceptance. Middleware IPs and credentials are environment-specific; the local VM address used during this run is not a project constant.

## Environment

| Item | Value |
| --- | --- |
| OS | Windows 11 |
| Java runtime | Java 25 local runtime; CI uses JDK 17 |
| Backend | Spring Boot 3.5.0, Spring Cloud Alibaba |
| Front-end | Vue 3, Vite |
| Middleware | VM Docker MySQL, Redis, Nacos, RocketMQ, Sentinel Dashboard |
| Gateway | `http://localhost:8080/api` |

## Verification Summary

| Check | Command | Result |
| --- | --- | --- |
| VM middleware connectivity | Java services connected through VM middleware; direct TCP probe from the tool session could not reach VM ports | PASS via services |
| Backend tests | `.\mvnw.cmd -q test` | PASS |
| Backend package | `.\mvnw.cmd -q -DskipTests package` | PASS |
| Front-end build | `cd mall-web; npm.cmd run build` | PASS |
| API smoke | `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1` | PASS |
| Real seckill smoke | `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1 -RunSeckill` | PASS |
| Demo transaction seed | `powershell -ExecutionPolicy Bypass -File .\scripts\seed-demo-data.ps1` | PASS |
| Product image API | `GET /api/products`, `GET /api/products/2010` | PASS |
| Pre-launch API checks | `powershell -ExecutionPolicy Bypass -File .\scripts\prelaunch-check.ps1 -SkipDashboards` | PASS |
| Local concurrent seckill smoke | `powershell -ExecutionPolicy Bypass -File .\scripts\load-seckill.ps1 -Users 10 -Concurrency 3 -Stock 20` | PASS |

## Notable Output

Backend tests passed. The inventory Testcontainers test detected that Docker is unavailable on the Windows host and skipped the container-backed path as designed.

Real seckill smoke:

```text
[OK]   seckill stock preheated
[OK]   seckill token issued
[OK]   seckill submitted: CREATED
[OK]   seckill final result: CREATED
[OK]   seckill order detail
[OK]   seckill order paid
[OK]   order status logs
[OK]   inventory stock flows
```

The final acceptance run also verified that the RocketMQ seckill consumer uses an isolated group (`mall-order-seckill`). A previous shared consumer group configuration caused `the consumer's subscription not exist` warnings and could leave seckill requests in `ACCEPTED`; this was corrected before the final `-RunSeckill` pass.

Demo transaction seed:

```text
[OK]   created cart order for demo
[OK]   paid order for demo
[OK]   created cart order for alice
[OK]   canceled order for alice
[OK]   created cart order for bob
[OK]   paid order for bob
[OK]   created cart order for carol
[OK]   canceled order for carol
[OK]   created seckill order
[OK]   paid seckill order
```

Product image API validation:

```text
GET /api/products returned mainImage paths such as /demo-products/headset.svg
GET /api/products/2010 returned galleryImages with local demo product images
```

Local concurrent seckill smoke:

```text
Users=10 Concurrency=3 Stock=20
[OK]   success=10 created=10 failed=0
[OK]   min=3833ms p95=6664ms max=6664ms
```

## Residual Validation Gap

k6 is not installed on the current Windows host, so the reproducible k6 run in `scripts/k6/seckill.js` was not executed locally. The k6 script and thresholds are kept in `docs/performance-testing.md` for a Docker/k6-enabled machine or later GitHub evidence.

Dashboard checks for Prometheus, Grafana, RocketMQ Dashboard, and Sentinel Dashboard were skipped in the final automated pre-launch run because this tool session could not directly reach VM dashboard ports. Business services successfully used the VM middleware through the running backend services. The VM deploy configuration now documents Nacos 3.x console on host port `8849` and RocketMQ Dashboard on host port `8088` mapped to container port `8082`. Before final screenshots, open the dashboards from the browser and confirm the VM URLs manually.

Sentinel gateway rules are implemented in code and the Dashboard is included in Docker Compose on port `8858`. If an older VM middleware stack is already running, update the VM compose stack and run `docker compose up rocketmq-init` once so RocketMQ business topics are present before recording final evidence.

## Release Decision

Go for GitHub release after adding screenshots and, optionally, one k6 run from a machine with k6 installed.
