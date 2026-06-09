# Acceptance Report

Date: 2026-06-09

Scope: local Windows Java services, Vue front-end build, and VM Docker middleware acceptance. Middleware IPs and credentials are environment-specific; the local VM address used during this run is not a project constant.

## Environment

| Item | Value |
| --- | --- |
| OS | Windows 11 |
| Java runtime | Java 25 local runtime; CI uses JDK 17 |
| Backend | Spring Boot 3.5.0, Spring Cloud Alibaba |
| Front-end | Vue 3, Vite |
| Middleware | VM Docker MySQL, Redis, Nacos, RocketMQ |
| Gateway | `http://localhost:8080/api` |

## Verification Summary

| Check | Command | Result |
| --- | --- | --- |
| VM middleware connectivity | `powershell -ExecutionPolicy Bypass -File .\scripts\check-vm-middleware.ps1` | PASS |
| Backend tests | `.\mvnw.cmd -q test` | PASS |
| Backend package | `.\mvnw.cmd -q -DskipTests package` | PASS |
| Front-end build | `cd mall-web; npm.cmd run build` | PASS |
| Backend liveness | `/actuator/health/liveness` on ports `8080-8087,8089` | PASS |
| API smoke | `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1` | PASS |
| Real seckill smoke | `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1 -RunSeckill` | PASS |
| Local concurrent seckill smoke | `powershell -ExecutionPolicy Bypass -File .\scripts\load-seckill.ps1 -Users 10 -Concurrency 3 -Stock 20` | PASS |

## Notable Output

Backend tests passed. The inventory Testcontainers test detected that Docker is unavailable on the Windows host and skipped the container-backed path as designed.

Real seckill smoke:

```text
[OK]   seckill stock preheated
[OK]   seckill token issued
[OK]   seckill submitted: CREATED
[OK]   seckill final result: CREATED
```

Local concurrent seckill smoke:

```text
Users=10 Concurrency=3 Stock=20
[OK]   success=10 created=10 failed=0
[OK]   min=702ms p95=1905ms max=1905ms
```

## Residual Validation Gap

k6 is not installed on the current Windows host, so the reproducible k6 run in `scripts/k6/seckill.js` was not executed locally. The k6 script and thresholds are kept in `docs/performance-testing.md` for a Docker/k6-enabled machine or later GitHub evidence.

## Release Decision

Go for GitHub portfolio release after adding screenshots and, optionally, one k6 run from a machine with k6 installed.
