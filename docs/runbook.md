# Runbook

## Startup Order

1. Confirm VM middleware is running: MySQL, Redis, Nacos, RocketMQ NameServer, RocketMQ Broker, and that `rocketmq-init` has completed once.
2. Start infrastructure-facing services:
   - `mall-user`
   - `mall-auth`
   - `mall-product`
   - `mall-promotion`
   - `mall-inventory`
3. Start transaction services:
   - `mall-order`
   - `mall-payment`
   - `mall-cart`
4. Start gateway:
   - `mall-gateway`
5. Start frontend:
   - `mall-web`

## Optional All-In-One Docker Validation

If you want a fully containerized validation environment on a machine with Docker, the middleware compose file can be combined with `docker-compose.apps.yml` to build and run all Java services:

```powershell
cd deploy
docker compose -f docker-compose.yml -f docker-compose.apps.yml up -d --build
```

This is not the default workflow for this workspace. For active local development and acceptance here, use IDEA plus VM middleware.

## Required Environment Variables

For the current VM middleware environment, replace `<your-vm-ip>` with your own VM address. Do not treat the sample IP used in local testing as a project constant:

```powershell
$env:MALL_VM_HOST="<your-vm-ip>"
$env:MALL_NACOS_ADDR="${env:MALL_VM_HOST}:8848"
$env:MALL_MYSQL_HOST="${env:MALL_VM_HOST}"
$env:MALL_MYSQL_PORT="3306"
$env:MALL_MYSQL_USERNAME="root"
$env:MALL_MYSQL_PASSWORD="root"
$env:MALL_REDIS_HOST="${env:MALL_VM_HOST}"
$env:MALL_REDIS_PORT="6379"
$env:MALL_ROCKETMQ_NAME_SERVER="${env:MALL_VM_HOST}:9876"
$env:MALL_SENTINEL_DASHBOARD="${env:MALL_VM_HOST}:8858"
$env:MALL_JWT_SECRET="replace-with-at-least-32-byte-secret"
```

`root / root`, empty Redis password, and the JWT placeholder are local acceptance values only. In any shared or deployed environment, replace them with environment-specific credentials and keep secrets out of Git.

In IDEA, prefer the environment-variable table editor instead of a single pasted line. This avoids hidden spaces in names such as `MALL_ROCKETMQ_NAME_SERVER`.

`MALL_MYSQL_HOST` is mandatory in this setup. If it is missing, services can fall back to `localhost` and fail during Flyway startup.

Before starting Java services, verify VM middleware connectivity:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-vm-middleware.ps1
```

If RocketMQ was started from an older deploy package, initialize business topics once on the VM:

```bash
cd deploy
docker compose up rocketmq-init
docker compose logs rocketmq-init
```

If databases are missing in the existing VM MySQL instance, create them idempotently:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-vm-databases.ps1
```

To start a single service without manually copying environment variables into IDEA:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-service-vm.ps1 -Service mall-user
```

To start all packaged backend services against VM middleware:

```powershell
.\mvnw.cmd -q -DskipTests package
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend-vm.ps1
```

## Sentinel Dashboard

Sentinel gateway rules are defined in `mall-gateway/src/main/java/com/demo/mall/gateway/config/GatewaySentinelConfiguration.java`.

The VM middleware stack should expose Sentinel Dashboard on port `8858`:

```bash
cd deploy
export MALL_VM_HOST=<your-vm-ip>
docker compose up -d --build sentinel-dashboard
```

Open `http://<your-vm-ip>:8858` with local credentials `sentinel / sentinel`.

The readiness script expects:

```text
[OK]   Sentinel Dashboard <your-vm-ip>:8858
```

Stop them with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-backend.ps1
```

## Health Checks

```powershell
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health
curl http://localhost:8089/actuator/health
curl http://localhost:8080/actuator/health
```

Prometheus scrape endpoints:

```powershell
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8084/actuator/prometheus
curl http://localhost:8085/actuator/prometheus
curl http://localhost:8087/actuator/prometheus
curl http://localhost:8089/actuator/prometheus
```

Optional observability stack:

```powershell
cd deploy
docker compose --profile observability up -d
```

Open Prometheus at `http://localhost:9090` and Grafana at `http://localhost:3000` (`admin / admin` locally).

## Common Failures

### MySQL Connects To localhost

Symptom:

```text
Access denied for user 'root'@'localhost'
```

Cause:

- `MALL_MYSQL_HOST` is missing or set to `localhost`.
- The IDEA run configuration does not include the same variables as other services.

Fix:

```text
MALL_MYSQL_HOST=<your-vm-ip>
MALL_MYSQL_PORT=3306
```

### RocketMQ Variable Typo

Correct variable:

```text
MALL_ROCKETMQ_NAME_SERVER=<your-vm-ip>:9876
```

Wrong examples:

```text
MALL_ROCKETMQ_NAME_SERVE R
MALL_ROCKETMQ_NAMESERVER
```

### RocketMQ Topic Or Subscription Warnings

Symptoms:

```text
No topic route info in name server for the topic
the consumer's subscription not exist
```

Fix:

- Run `docker compose up rocketmq-init` in the VM `deploy` directory to create business topics.
- Keep each logical consumer subscription on its own RocketMQ group. For this project, order payment and seckill order creation use separate groups: `mall-order-paid` and `mall-order-seckill`.

### Flyway Fails On New Service

Every service has its own database. Confirm the database exists:

```sql
CREATE DATABASE IF NOT EXISTS mall_promotion DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### Prometheus Cannot Scrape Services

The bundled config uses `host.docker.internal:<servicePort>`. If Prometheus runs inside a VM and Java services run on Windows, replace those targets in `deploy/prometheus/prometheus.yml` with the Windows host IP that the VM can reach.

## Seckill Acceptance Checklist

Scripted API smoke checks:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1 -RunSeckill
```

1. Login with `admin / 123456`.
2. Open `/admin/seckill`.
3. Create or update activity/session/SKU.
4. Preheat Redis stock for the target SKU.
5. Confirm Recent operations has audit entries.
6. Login with `demo / 123456`.
7. Open `/seckill`.
8. Click buy on a running session.
9. Confirm the result eventually returns `CREATED + orderNo`.
10. Pay the order and confirm order status becomes paid.
11. Query `/api/orders/admin/{orderNo}/status-logs` as admin and confirm create/pay events exist.
12. Query `/api/inventory/admin/stock-flows?orderNo={orderNo}` as admin and confirm lock/deduct or lock/release flows exist.

## Production Readiness Checklist

- Secrets are injected by environment or secret manager.
- CORS allowed origins are environment-specific.
- Internal endpoints are not exposed through the gateway.
- Admin APIs require `ADMIN`.
- Normal order creation uses `requestId` idempotency.
- MQ consumers are idempotent.
- Outbox retry jobs are enabled.
- Order timeout release is enabled.
- Seckill token, duplicate-buy guard, Redis pre-deduct, and async order creation are enabled.
- Operation audit logs are retained and queryable.
- Stock lock/release/deduct flows are retained in `wms_stock_flow`.
- Order create/pay/cancel/expire transitions are retained in `oms_order_status_log`.
- Sentinel gateway rules are loaded for login, product read, seckill catalog, token, and submit APIs.
- Prometheus can scrape every running backend service.
- k6 smoke test passes for the target environment.
- CI passes backend tests and frontend build.
