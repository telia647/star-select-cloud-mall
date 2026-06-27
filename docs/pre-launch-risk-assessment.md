# 上线前风险评估与应急预案

本文档用于项目发布到 GitHub 前的生产化评审，目标是提前梳理真实上线可能遇到的异常、影响范围、监控信号、降级策略和应急动作。当前项目暂不实际商用上线，但按可落地生产系统标准进行风险预案设计。

## 1. 总体结论

当前系统已经具备 C 端商城、B 端秒杀运营、Redis 预扣库存、RocketMQ 异步建单、订单幂等、库存流水、订单状态日志、Sentinel 网关限流、Prometheus 指标和运维文档。若作为真实线上服务，还需要重点关注以下高风险点：

| 优先级 | 风险 | 结论 |
| --- | --- | --- |
| P0 | 秒杀流量突刺 | 必须依赖网关 Sentinel、秒杀 token、Redis Lua、MQ 削峰和前端排队态共同保护 |
| P0 | 库存与订单一致性 | Redis 活动库存、订单、库存锁、DB 活动库存需要对账和补偿 |
| P0 | MQ 异步建单失败 | 已有同步兜底和消费幂等，但仍需要死信、告警和人工补偿流程 |
| P0 | 支付状态不一致 | 当前是模拟支付，真实支付上线前必须补回调验签、状态机和重复回调处理 |
| P1 | Redis/MySQL/RocketMQ/Nacos 故障 | 需要明确降级边界，核心交易链路不能无保护地穿透到 DB |
| P1 | 安全与权限 | 管理接口已有 ADMIN 限制，真实部署仍需内网隔离、密钥管理和服务间鉴权 |
| P1 | 观测与告警 | 已有指标和规则文件，但实际告警通知渠道需要在部署环境配置 |

## 2. 流量与限流风险

### 2.1 秒杀接口被瞬时打满

可能现象：

- `/api/orders/seckill/tokens` 或 `/api/orders/seckill` RT 飙升。
- Gateway 线程、order 服务线程、Redis CPU、RocketMQ 堆积同时升高。
- 用户端大量出现排队中、抢购失败、请求超时。

现有控制：

- `mall-gateway` 启动时加载 Sentinel 网关规则。
- 秒杀提交前必须先领取短期 token。
- Redis Lua 原子校验 token、库存和重复购买。
- 提交成功后异步投递 RocketMQ 建单，避免同步打 DB。
- `mall-order` 暴露秒杀指标：token、submit、rejected、order created/failed。

监控指标：

- `http_server_requests_seconds{uri="/orders/seckill"}` p95/p99。
- `mall_seckill_submit_total` 按 `result`、`reason` 聚合。
- Redis CPU、命令耗时、连接数。
- RocketMQ topic backlog。
- Gateway 4xx/5xx 比例。

降级预案：

1. Sentinel 临时降低 `/api/orders/seckill` QPS，保护后端。
2. 前端按钮进入“排队中/稍后重试”，禁止连续点击。
3. 热点场次只开放 token 接口，提交接口分批放量。
4. 若 Redis 压力过高，关闭非核心商品详情实时刷新，保留秒杀主链路。
5. 若 order 服务压力过高，降低 MQ 消费并发，延长排队结果轮询提示。

后续增强：

- Sentinel 规则从静态代码迁移到 Nacos 动态配置。
- 增加用户/IP/设备维度频控。
- 热点 SKU 单独限流，非热点商品不受影响。

## 3. 数据一致性风险

### 3.1 Redis 秒杀库存与 DB 活动库存不一致

可能现象：

- C 端看到仍有库存，但提交时返回售罄。
- Redis 库存已扣减，订单未创建。
- DB `promo_seckill_sku.available_stock` 与 Redis 活动库存存在短时差异。

现有控制：

- Redis Lua 预扣库存，防止超卖。
- `oms_seckill_reservation` 记录 Redis 预扣与订单关系。
- 订单取消或超时关闭时释放 Redis 秒杀库存和用户购买标记。
- 支付成功后标记预约成交。
- `mall-promotion` 定时对账 Redis 库存并回写活动库存。

监控指标：

- 秒杀成功请求数、订单创建数、支付数。
- `promo_seckill_sku.available_stock` 与 Redis stock key 差异。
- `oms_seckill_reservation` 中长时间 `RESERVED` 未支付记录。
- 库存负数或异常突变。

应急预案：

1. 暂停目标活动 SKU：B 端将活动 SKU 下架或关闭场次。
2. 停止继续预热该 SKU 库存，避免扩大影响。
3. 根据 `oms_seckill_reservation`、订单状态日志、库存流水做人工对账。
4. 对未建单但已预扣的请求执行库存释放。
5. 对已支付订单以订单为准，保证用户权益。

后续增强：

- 增加独立对账报表：Redis 库存、活动库存、订单数、支付数、释放数。
- 增加异常差异告警阈值。

### 3.2 普通订单重复提交

可能现象：

- 用户网络重试导致多笔相同订单。
- 库存被重复锁定。

现有控制：

- 普通下单支持 `requestId` 幂等键。
- 订单表对 `user_id + request_id` 建唯一约束。
- 购物车结算默认生成 `requestId`。

应急预案：

1. 以 `requestId` 和用户维度排查重复订单。
2. 对未支付重复订单执行取消并释放库存。
3. 对已支付异常重复订单走退款/人工客服流程。

后续增强：

- 前端结算页展示提交中锁定态。
- 增加订单相似度风控：同用户、同 SKU、短时间重复购买提醒。

## 4. MQ 与异步建单风险

### 4.1 RocketMQ 不可用或消息堆积

可能现象：

- 秒杀提交成功但长时间查询不到订单。
- `seckill-order-topic` 堆积上升。
- MQ send 失败日志增加。

现有控制：

- 秒杀提交投递 MQ 失败时有同步兜底建单。
- order 服务消费者使用 `oms_mq_consume_log` 做幂等。
- 本地消息和重试任务用于支付等事件补偿。

监控指标：

- RocketMQ topic backlog。
- 消费失败次数。
- `mall_seckill_order_total{result="failed"}`。
- 订单创建延迟：提交 accepted 到 created 的耗时。

降级预案：

1. 若 MQ send 失败但 order 服务可用，启用同步兜底建单。
2. 若 MQ 堆积过高，降低秒杀提交入口 QPS。
3. 临时延长前端轮询等待提示。
4. 对长时间 `ACCEPTED` 请求按 requestId 查 Redis 结果、reservation 和订单。
5. 必要时暂停活动场次，等消息消费追平后恢复。

后续增强：

- 配置死信队列与死信处理台账。
- 增加 MQ backlog 告警。
- 增加 requestId 维度异步建单延迟指标。

## 5. 支付风险

### 5.1 支付成功但订单未更新

可能现象：

- payment 服务显示已支付，order 仍是待支付。
- 库存锁未最终扣减。

现有控制：

- 支付调用订单标记已支付。
- 支付事件通过 RocketMQ 发布，订单支付处理幂等。
- 订单状态日志记录支付成功。

当前限制：

- 目前是模拟支付，不包含第三方支付回调验签、异步通知、退款。

真实上线前必须补：

- 支付单状态机：待支付、支付中、成功、失败、关闭、退款中、已退款。
- 第三方回调签名校验。
- 回调幂等表或唯一流水号。
- 支付成功但订单更新失败的补偿扫描。
- 退款和关单流程。

应急预案：

1. 以支付单号 `payNo` 查询支付单。
2. 若支付已成功但订单未支付，重放支付成功事件。
3. 若库存扣减失败，暂停发货并进入人工补偿。
4. 对真实支付场景，以支付机构结果为资金事实源，订单系统做补偿同步。

## 6. 中间件故障风险

| 组件 | 故障影响 | 降级策略 | 恢复动作 |
| --- | --- | --- | --- |
| Redis | 商品缓存失效、秒杀库存/token 不可用 | 秒杀提交直接降级为暂不可抢；商品读取限流后查 DB | 恢复 Redis 后重新预热热点商品和秒杀库存 |
| MySQL | 核心数据不可写，订单/用户/商品受影响 | C 端只读缓存页可保留，交易链路关闭 | 恢复后检查 Flyway、订单、库存、消息一致性 |
| RocketMQ | 秒杀异步建单和支付事件延迟 | 启用同步兜底，降低入口 QPS | 消费堆积追平，检查失败消息 |
| Nacos | 服务发现异常，新实例注册失败 | 已注册实例短期可继续；避免发布重启 | 恢复 Nacos 后检查所有服务注册状态 |
| Sentinel Dashboard | 控制台不可见，不影响静态规则运行 | 保持代码内默认规则 | 恢复后检查网关资源和规则展示 |
| Gateway | 全站 API 不可用 | 前端展示系统维护，禁止交易提交 | 恢复后检查鉴权、路由和限流规则 |

## 7. 安全与权限风险

可能风险：

- 管理接口被普通用户访问。
- 内部接口绕过网关访问。
- JWT secret、数据库密码泄露。
- Sentinel/RocketMQ/Nacos 控制台暴露公网。

现有控制：

- 网关阻断 `/api/**/internal`。
- Admin 接口使用 `RoleGuard` 和 `ADMIN` 角色。
- JWT secret、CORS、MySQL/Redis/RocketMQ 地址支持环境变量。
- README 和 runbook 明确本地账号密码仅用于验收。

上线预案：

1. 所有 Java 服务只暴露在内网，公网只暴露 gateway。
2. 控制台类服务仅内网或 VPN 可访问。
3. 使用独立 MySQL 低权限账号，不使用 root。
4. 替换 `admin / 123456`、`sentinel / sentinel` 等默认账号。
5. JWT secret 使用密钥管理系统注入。
6. CORS 按域名白名单配置。

后续增强：

- 服务间签名或 mTLS。
- 完整 RBAC 表模型和权限点。
- 管理操作二次确认和审计检索。

## 8. 可观测性与告警

上线前必须确认：

- 每个服务 `/actuator/health` 正常。
- Prometheus 能抓取 gateway、order、promotion、inventory 等核心服务。
- Grafana 秒杀面板可打开。
- Sentinel Dashboard 能看到 gateway 资源。
- RocketMQ Dashboard 能看到 topic 与 consumer。

建议告警：

| 告警 | 触发条件 |
| --- | --- |
| 秒杀拒绝率过高 | rejected / submit > 20%，持续 5 分钟 |
| 秒杀建单失败 | `mall_seckill_order_total{result="failed"}` 增加 |
| 秒杀接口慢 | `/orders/seckill` p95 > 1.5s，持续 5 分钟 |
| MQ 堆积 | `seckill-order-topic` backlog 超阈值 |
| 库存不一致 | Redis stock 与 DB available_stock 差异超阈值 |
| 订单超时释放失败 | expire close 任务异常或释放失败 |
| 支付补偿失败 | 支付成功事件重试超过阈值 |

## 9. 发布与回滚风险

发布前检查：

- `mvn test` 通过。
- `mvn -DskipTests package` 通过。
- `npm run build` 通过。
- Flyway migration 顺序正确。
- 目标数据库已备份。
- 中间件连通性脚本通过。
- 烟测脚本通过。

数据库迁移策略：

- 本项目使用 Flyway 正向迁移。
- 对新增表如 `ums_member_coupon`，回滚风险较低。
- 对修改核心表的迁移，真实生产中应提前准备 forward-fix 脚本，而不是直接回滚数据库。

应用回滚策略：

1. 先停止入口流量或降低 Sentinel QPS。
2. 回滚 Java 服务版本。
3. 保留已执行的兼容性迁移。
4. 检查订单、库存、消息堆积。
5. 只在确认无新版本写入数据依赖时，才考虑数据层修复。

## 10. 应急处理流程

### 10.1 秒杀活动异常总流程

1. 网关限流：降低秒杀提交 QPS。
2. B 端暂停活动 SKU 或场次。
3. 查看 Grafana/Sentinel/RocketMQ Dashboard。
4. 按 requestId 查询秒杀结果、订单、reservation。
5. 对已支付用户优先保障订单。
6. 对未支付异常 reservation 释放库存。
7. 形成事故记录：时间线、影响用户、根因、修复动作。

### 10.2 库存异常处理

1. 冻结活动 SKU。
2. 导出订单数、支付数、取消数、reservation 数、库存流水。
3. 以支付成功订单为最终履约依据。
4. 回补或释放 Redis 库存。
5. 手工修正 DB 活动库存。
6. 恢复活动前执行小流量验证。

### 10.3 MQ 堆积处理

1. 降低入口 QPS。
2. 检查消费者是否在线。
3. 扩容 order 消费实例或提高消费线程。
4. 对重复失败消息进入死信/人工处理。
5. 堆积追平后恢复入口 QPS。

## 11. Go / No-Go 清单

当前 GitHub 发布和本地验收前必须满足：

- [ ] 所有服务健康检查通过。
- [ ] Gateway、Sentinel、Redis、RocketMQ、MySQL、Nacos 可用。
- [ ] 管理接口必须 ADMIN 权限。
- [ ] 内部接口不能通过公网网关访问。
- [ ] 秒杀活动可预热、可抢购、可建单、可支付。
- [ ] 重复提交不会重复下单。
- [ ] 售罄不会超卖。
- [ ] 订单超时能释放库存。
- [ ] MQ 重复消费不会重复处理。
- [ ] Prometheus/Grafana/Sentinel/RocketMQ Dashboard 可观测。
- [ ] 烟测和压测脚本完成一次记录。
- [ ] 数据库迁移和回滚/forward-fix 方案已确认。

真实部署时再额外确认：

- [ ] 生产密钥、数据库账号、中间件账号已替换为部署环境值。
- [ ] Grafana 或 Alertmanager 已配置通知渠道。
- [ ] 已按目标机器压测结果调整 Sentinel QPS。
- [ ] 公网只暴露 Gateway/Nginx，业务服务和中间件仅内网访问。

## 12. 当前项目残余风险

这些风险不会影响 GitHub 发布和本地验收。按当前项目定位，支付保持模拟支付，中间件密码保持本地验收配置，不作为本轮待修问题。

已完成代码或脚本修复：

- Sentinel 网关限流规则已配置化，可通过环境变量调整 QPS 和 burst。
- 新增生产默认 JWT secret 启动校验开关。
- Gateway 已阻断 `/api/**/internal`。
- Smoke 脚本已覆盖会员权益、券包、订单列表、秒杀建单、支付、订单日志和库存流水。
- Prometheus 已按服务拆分 scrape job，并补充秒杀错误率、服务不可用等告警规则。
- 新增 pre-launch 检查脚本，覆盖核心 API、Dashboard 可达性和生产密钥检查。
- 订单幂等、库存锁、Redis 秒杀预扣、MQ 消费幂等、订单超时释放、库存流水和状态日志已落地。

真实部署环境中再执行，不要求现在完成：

- Grafana / Alertmanager 通知渠道配置。
- 目标机器真实压测和 QPS 调参。
- 公网只暴露 Gateway/Nginx，服务和中间件放内网。

仍可继续增强但不阻塞发布：

- B 端 RBAC 仍是角色级控制，还没有细粒度权限点。
- 前端商品图片仍是原型级素材，真实商用需要 CDN、图片压缩和懒加载策略。
