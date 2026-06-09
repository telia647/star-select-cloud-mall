# Observability

The project exposes Spring Boot Actuator, Micrometer metrics, and Prometheus scrape endpoints for every backend service.

## Endpoints

- Health: `http://localhost:<servicePort>/actuator/health`
- Metrics catalog: `http://localhost:<servicePort>/actuator/metrics`
- Prometheus: `http://localhost:<servicePort>/actuator/prometheus`

Service ports:

| Service | Port |
| --- | --- |
| `mall-gateway` | 8080 |
| `mall-auth` | 8081 |
| `mall-user` | 8082 |
| `mall-product` | 8083 |
| `mall-order` | 8084 |
| `mall-inventory` | 8085 |
| `mall-cart` | 8086 |
| `mall-payment` | 8087 |
| `mall-promotion` | 8089 |

## Prometheus And Grafana

Start middleware plus observability services:

```powershell
cd deploy
docker compose --profile observability up -d
```

Open:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Grafana local credentials: `admin / admin`

The bundled Prometheus config scrapes `host.docker.internal:<servicePort>/actuator/prometheus`. If Java services run on another machine, update `deploy/prometheus/prometheus.yml` to use that host IP.

Grafana is provisioned automatically:

- Datasource: `Prometheus`
- Dashboard folder: `Mall`
- Dashboard: `Mall Seckill Overview`

Prometheus also loads alerting rules from `deploy/prometheus/rules/seckill-alerts.yml`.

## Seckill Metrics

`mall-order` publishes dedicated flash-sale counters:

- `mall_seckill_token_total{result="issued"}`
- `mall_seckill_submit_total{result="accepted",mode="async|sync"}`
- `mall_seckill_submit_total{result="rejected",reason="sold_out|invalid_token|stock_not_ready|redis_script_null"}`
- `mall_seckill_submit_total{result="duplicate"}`
- `mall_seckill_order_total{result="created|failed",mode="async|sync"}`
- `mall_seckill_stock_init_total`

Useful PromQL during pressure tests:

```promql
sum(rate(mall_seckill_submit_total[1m])) by (result)
sum(rate(mall_seckill_submit_total{result="rejected"}[1m])) by (reason)
sum(rate(mall_seckill_order_total[1m])) by (result, mode)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/orders/seckill"}[1m])) by (le))
```

## Bundled Alerts

- `MallSeckillRejectedRateHigh`: rejected submit ratio is above 20% for 5 minutes.
- `MallSeckillOrderCreateFailures`: async/sync seckill order creation failures are observed.
- `MallSeckillSubmitP95Slow`: `/orders/seckill` p95 latency stays above 1.5 seconds.
