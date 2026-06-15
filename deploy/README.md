# VM Middleware Deploy

This directory is the VM-side middleware package. Copy `deploy/` to the Linux VM and run Docker Compose there. Java services can still run from Windows/IDEA and connect to this VM.

## Services

- MySQL `3306`
- Redis `6379`
- Nacos `8848`, `9848`, `9849`
- RocketMQ NameServer `9876`
- RocketMQ Broker `10909`, `10911`
- RocketMQ topic initializer for `order-paid-topic`, `seckill-order-topic`, and `inventory-deducted-topic`
- RocketMQ Dashboard `8088`
- Sentinel Dashboard `8858`
- Optional Prometheus `9090` and Grafana `3000`

## Start On VM

```bash
cd deploy
cp .env.example .env
vi .env
docker compose up -d --build
docker compose ps
```

Set `MALL_VM_HOST` in `.env` to the current VM IP. The VM IP can change between network modes or restarts, so it is intentionally not hard-coded in the project.

If only the required middleware is needed:

```bash
docker compose up -d --build mysql redis nacos rocketmq-namesrv rocketmq-broker rocketmq-init rocketmq-dashboard sentinel-dashboard
```

`rocketmq-init` is a one-shot container. It should exit after the three business topics are created. Run `docker compose logs rocketmq-init` if RocketMQ consumers report missing topic routes.

If Sentinel Dashboard fails while downloading from GitHub during build, change `SENTINEL_DASHBOARD_JAR_URL` in `.env` to another reachable mirror. The official jar URL is:

```text
https://github.com/alibaba/Sentinel/releases/download/1.8.9/sentinel-dashboard-1.8.9.jar
```

The default `.env.example` uses a GitHub proxy mirror so VM builds do not have to reach GitHub directly.

Optional observability:

```bash
docker compose --profile observability up -d
```

## Windows/IDEA Environment

Use the same VM IP in each backend service run configuration:

```text
MALL_VM_HOST=<your-vm-ip>
MALL_NACOS_ADDR=<your-vm-ip>:8848
MALL_MYSQL_HOST=<your-vm-ip>
MALL_MYSQL_PORT=3306
MALL_MYSQL_USERNAME=root
MALL_MYSQL_PASSWORD=root
MALL_REDIS_HOST=<your-vm-ip>
MALL_REDIS_PORT=6379
MALL_ROCKETMQ_NAME_SERVER=<your-vm-ip>:9876
MALL_SENTINEL_DASHBOARD=<your-vm-ip>:8858
MALL_JWT_SECRET=replace-with-at-least-32-byte-secret
```

`root/root`, empty Redis password, and placeholder JWT secrets are for local acceptance only. For any shared or deployed environment, use dedicated accounts, enable middleware authentication where appropriate, and inject secrets via environment variables or a secret manager.

## Verify From Windows

From the repository root on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-vm-middleware.ps1
```

If RocketMQ producers cannot connect, check `MALL_VM_HOST` or set `MALL_ROCKETMQ_BROKER_IP` in `.env` to the VM address reachable from Windows, then recreate the broker:

```bash
docker compose up -d --force-recreate rocketmq-broker
docker compose up rocketmq-init
```
