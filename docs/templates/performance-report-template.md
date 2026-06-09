# Seckill Performance Report

## Test Metadata

| Field | Value |
| --- | --- |
| Date |  |
| Commit |  |
| Operator |  |
| Environment | VM middleware + local Java services |
| Gateway URL | `http://localhost:8080/api` |
| Activity / Session / SKU |  |

## Machine Specs

| Component | Spec |
| --- | --- |
| Windows host |  |
| VM host |  |
| MySQL container |  |
| Redis container |  |
| RocketMQ container |  |
| JVM version |  |

## Test Plan

| Parameter | Value |
| --- | --- |
| Tool | k6 |
| Script | `scripts/k6/seckill.js` |
| VUs |  |
| Duration |  |
| Preheated stock |  |
| User account |  |

## Command

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

## Results

| Metric | Value |
| --- | --- |
| Total requests |  |
| Failed request rate |  |
| Submit success rate |  |
| p50 latency |  |
| p95 latency |  |
| p99 latency |  |
| Max latency |  |
| Created orders |  |
| Rejected requests |  |

## Prometheus Snapshots

Capture these queries:

```promql
sum(rate(mall_seckill_submit_total[1m])) by (result)
sum(rate(mall_seckill_submit_total{result="rejected"}[1m])) by (reason)
sum(rate(mall_seckill_order_total[1m])) by (result, mode)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/orders/seckill"}[1m])) by (le))
```

| Screenshot | Path |
| --- | --- |
| Grafana seckill overview |  |
| Prometheus targets |  |
| k6 terminal summary |  |

## Findings

- Capacity conclusion:
- Bottleneck:
- Error causes:
- Follow-up:
