# Local Acceptance Report

## Environment

| Field | Value |
| --- | --- |
| Date |  |
| Commit |  |
| Middleware mode | VM Docker middleware |
| VM IP | `192.168.150.103` |
| Backend mode | local IDEA / local jar |
| Front-end mode | Vite dev server |

## Middleware Checks

Command:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-vm-middleware.ps1
```

| Middleware | Result |
| --- | --- |
| MySQL `3306` |  |
| Redis `6379` |  |
| Nacos `8848` |  |
| RocketMQ NameServer `9876` |  |
| RocketMQ Broker `10909/10911` |  |

## Backend Health

| Service | Port | Health |
| --- | --- | --- |
| `mall-gateway` | 8080 |  |
| `mall-auth` | 8081 |  |
| `mall-user` | 8082 |  |
| `mall-product` | 8083 |  |
| `mall-order` | 8084 |  |
| `mall-inventory` | 8085 |  |
| `mall-cart` | 8086 |  |
| `mall-payment` | 8087 |  |
| `mall-promotion` | 8089 |  |

## Functional Acceptance

| Flow | Result | Evidence |
| --- | --- | --- |
| Admin login |  |  |
| Admin activity/session/SKU config |  |  |
| Redis seckill stock preheat |  |  |
| Member login |  |  |
| Product browsing |  |  |
| Cart checkout |  |  |
| Payment |  |  |
| Seckill token issue |  |  |
| Seckill submit |  |  |
| Seckill result polling |  |  |
| Order status timeline query |  |  |
| Inventory stock flow query |  |  |

## Data Verification

| Table | Expected Evidence |
| --- | --- |
| `promo_operation_log` | Admin operation records |
| `oms_order_status_log` | Create/pay/cancel/expire records |
| `wms_stock_flow` | LOCK/DEDUCT/RELEASE records |
| `oms_mq_consume_log` | Consumer idempotency records |
| `oms_seckill_reservation` | Seckill reservation state |

## Open Issues

- 

## Final Decision

Result:

```text
PASS / FAIL
```
