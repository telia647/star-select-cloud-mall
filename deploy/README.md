# VM Middleware Deploy

This directory is the VM-side middleware package. Copy `deploy/` to the Linux VM and run Docker Compose there. Java services can still run from Windows/IDEA and connect to this VM.

## Services

- MySQL `3306`
- Redis `6379`
- Nacos service ports `8848`, `9848`, `9849`; Nacos 3.x console `8849`
- RocketMQ NameServer `9876`
- RocketMQ Broker `10909`, `10911`
- RocketMQ topic initializer for `order-paid-topic`, `seckill-order-topic`, and `inventory-deducted-topic`
- RocketMQ Dashboard `8088` on the VM, mapped to the dashboard container's embedded Tomcat port `8082`
- Sentinel Dashboard `8858`
- Milvus standalone `19530`, health/metrics `9091`
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
docker compose up -d --build mysql redis nacos rocketmq-namesrv rocketmq-broker rocketmq-init rocketmq-dashboard sentinel-dashboard milvus
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

If RocketMQ Dashboard is up but `http://<vm-ip>:8088` resets the connection, check the port mapping:

```bash
grep -n "8088" docker-compose.yml
docker port mall-rocketmq-dashboard
```

The current dashboard image starts Tomcat on container port `8082`, so the mapping should be:

```yaml
ports:
  - "8088:8082"
```

After changing the VM copy of `docker-compose.yml`, recreate only the dashboard:

```bash
docker compose up -d --force-recreate rocketmq-dashboard
curl -v http://127.0.0.1:8088/
```

For Nacos 3.x, the service endpoint remains `8848`, but the web console is exposed separately on host port `8849`:

```bash
grep -n "8849" docker-compose.yml
docker compose up -d --force-recreate nacos
curl -v http://127.0.0.1:8849/
```

Open the console at:

```text
http://<vm-ip>:8849
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
MALL_MILVUS_HOST=<your-vm-ip>
MALL_MILVUS_PORT=19530
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
