# Sentinel Gateway Flow Control

This project uses Sentinel in two layers:

- Runtime dependency: Spring Cloud Alibaba Sentinel is included in gateway and hot-path backend services.
- Control plane: Sentinel Dashboard is included in `deploy/docker-compose.yml` and exposed on `8858`.

Sentinel is not a data store like MySQL, Redis, or RocketMQ. It is a traffic protection component. The project can enforce static gateway rules without the Dashboard, but the Dashboard is included so a VM middleware deployment has the expected Sentinel console.

## Where The Rules Are

Gateway flow-control rules are loaded at startup from:

```text
mall-gateway/src/main/java/com/demo/mall/gateway/config/GatewaySentinelConfiguration.java
```

Protected resources:

| Resource | Match | Default QPS | Burst |
| --- | --- | ---: | ---: |
| `api_auth_login` | `/api/auth/login` | 80 | 20 |
| `api_product_read` | `/api/products/**` | 600 | 100 |
| `api_seckill_catalog` | `/api/promotions/seckill/**` | 500 | 100 |
| `api_seckill_token` | `/api/orders/seckill/tokens` | 200 | 50 |
| `api_seckill_submit` | `/api/orders/seckill` | 120 | 30 |
| `mall-order` route | Gateway route id | 700 | 100 |
| `mall-promotion` route | Gateway route id | 700 | 100 |

These defaults are deliberately conservative for local acceptance. They can be tuned without code changes through `mall.gateway.flow.*` properties or the matching environment variables:

| Variable | Default |
| --- | ---: |
| `MALL_FLOW_AUTH_LOGIN_QPS` / `MALL_FLOW_AUTH_LOGIN_BURST` | `80` / `20` |
| `MALL_FLOW_PRODUCT_READ_QPS` / `MALL_FLOW_PRODUCT_READ_BURST` | `600` / `100` |
| `MALL_FLOW_SECKILL_CATALOG_QPS` / `MALL_FLOW_SECKILL_CATALOG_BURST` | `500` / `100` |
| `MALL_FLOW_SECKILL_TOKEN_QPS` / `MALL_FLOW_SECKILL_TOKEN_BURST` | `200` / `50` |
| `MALL_FLOW_SECKILL_SUBMIT_QPS` / `MALL_FLOW_SECKILL_SUBMIT_BURST` | `120` / `30` |
| `MALL_FLOW_ORDER_ROUTE_QPS` / `MALL_FLOW_ORDER_ROUTE_BURST` | `700` / `100` |
| `MALL_FLOW_PROMOTION_ROUTE_QPS` / `MALL_FLOW_PROMOTION_ROUTE_BURST` | `700` / `100` |

Production deployments should still move rules to a dynamic rule source such as Nacos when runtime tuning without restart is required.

## Start Dashboard

In the VM or local Docker environment:

```bash
cd deploy
export MALL_VM_HOST=<your-vm-ip>
docker compose up -d --build sentinel-dashboard
```

Open:

```text
http://<your-vm-ip>:8858
```

Default Sentinel Dashboard credentials are `sentinel / sentinel` for local acceptance.

## Java Service Connection

Set:

```powershell
$env:MALL_SENTINEL_DASHBOARD="<your-vm-ip>:8858"
```

or let the provided VM scripts derive it from `MALL_VM_HOST`.

Every service that has Sentinel enabled uses:

```yaml
spring.cloud.sentinel.transport.dashboard: ${MALL_SENTINEL_DASHBOARD:${MALL_VM_HOST:localhost}:8858}
```

## Acceptance Check

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-vm-middleware.ps1
```

Expected Sentinel line:

```text
[OK]   Sentinel Dashboard <your-vm-ip>:8858
```

After starting the gateway and sending traffic to `/api/orders/seckill`, the Dashboard should show `mall-gateway` and gateway resources.

## Production Notes

- Keep gateway-side rules for coarse route protection.
- Add user/IP/device risk controls for login and seckill submission.
- Store dynamic Sentinel rules in Nacos or another rule source instead of only code.
- Protect Sentinel Dashboard with network ACLs or private network access; do not expose it publicly.
