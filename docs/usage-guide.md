# 使用教程

本文按角色说明这个秒杀商城项目怎么使用，以及发布秒杀活动后为什么 C 端可能没有下单入口。

## 账号

本地验收默认账号：

- 管理员：`admin / 123456`
- 普通用户：`demo / 123456`

前端地址：

- 商城前台：`http://localhost:5173`
- 秒杀运营后台：`http://localhost:5173/admin/seckill`

后端网关：

- API 网关：`http://localhost:8080/api`

## 启动顺序

1. 启动 VM 里的中间件：MySQL、Redis、Nacos、RocketMQ、Sentinel。
2. 启动后端服务。
3. 启动前端。

后端一键启动示例：

```powershell
.\mvnw.cmd -q -DskipTests package
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend-vm.ps1 -VmHost <你的 VM IP>
```

前端启动：

```powershell
cd mall-web
npm install
npm run dev
```

## C 端用户使用流程

### 普通商城下单

1. 打开 `http://localhost:5173`。
2. 登录普通用户：`demo / 123456`。
3. 进入“商品”。
4. 选择商品，进入详情页。
5. 点击“加入购物车”或“立即购买”。
6. 进入“购物车”，确认商品和数量。
7. 点击“提交订单”。
8. 在订单详情页点击“模拟支付”。
9. 支付完成后可以回到订单详情查看状态。

### 秒杀下单

1. 打开 `http://localhost:5173/seckill`。
2. 登录普通用户。
3. 选择一个“正在抢购”的场次。
4. 找到有剩余库存的秒杀商品。
5. 点击“立即抢购”。
6. 系统会先领取秒杀令牌，再提交秒杀请求。
7. 如果返回“已创建订单”，点击“查看订单”。
8. 在订单详情页点击“模拟支付”。

## B 端运营使用流程

进入后台：

```text
http://localhost:5173/admin/seckill
```

用 `admin / 123456` 登录。

### 发布一个可下单的秒杀活动

只创建“活动”不够。完整链路必须配置三层数据：

1. 活动
2. 场次
3. 活动 SKU，也就是具体秒杀商品

推荐流程：

1. 在“营销活动”区域点击“新建”。
2. 填写活动名称、标题、描述。
3. 状态选择“启用”。
4. 点击“保存活动”。
5. 在“场次”区域点击“新建”。
6. 填写场次名称。
7. 开始时间要早于当前时间，结束时间要晚于当前时间，这样 C 端才会显示“正在抢购”。
8. 状态选择“启用”。
9. 点击“保存场次”。
10. 在“活动 SKU”区域点击“新建 SKU”。
11. 填写 `SKU ID`、商品 ID、商品名称、SKU 编码、副标题、原价、秒杀价、总库存、可售库存、单人限购、角标。
12. 状态选择“启用”。
13. 点击“保存 SKU”。
14. 在 SKU 表格里点击“预热”，把库存写入 Redis。
15. 回到 C 端秒杀页刷新。

## 为什么用户只能看到活动，没有下单功能

这个项目的 C 端秒杀页不是直接按“活动”下单，而是按“场次里的秒杀 SKU”下单。

如果你只发布了活动，用户最多只能看到活动或场次，不会出现真正可点的“立即抢购”按钮。

必须同时满足这些条件：

- 活动状态是“启用”。
- 场次状态是“启用”。
- 场次当前时间状态是“正在抢购”。
- 场次下面已经添加秒杀 SKU。
- 秒杀 SKU 状态是“启用”。
- 秒杀 SKU 的可售库存大于 0。
- 秒杀 SKU 已经点击“预热”，Redis 里有库存。
- 普通用户已登录。

按钮显示规则：

- 场次未开始：按钮显示“提醒我”，不能下单。
- 场次已结束：按钮显示“已结束”，不能下单。
- 可售库存为 0：按钮显示“已抢光”，不能下单。
- 场次正在进行且库存大于 0：按钮显示“立即抢购”。

## 运维和验收入口

检查 VM 中间件：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-vm-middleware.ps1
```

普通 API 冒烟：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1
```

真实秒杀下单冒烟：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-api.ps1 -RunSeckill
```

小并发秒杀验收：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-seckill.ps1 -Users 10 -Concurrency 3 -Stock 20
```

## 已完成内容

- C 端商城：商品列表、商品详情、购物车、下单、订单详情、模拟支付、个人中心。
- C 端秒杀：场次列表、秒杀商品列表、秒杀令牌、Redis 预扣库存、异步建单、结果轮询。
- B 端秒杀运营：活动管理、场次管理、活动 SKU 管理、库存预热、操作日志。
- 管理诊断：订单状态时间线、库存流水查询。
- 后端微服务：网关、认证、用户、商品、促销、购物车、订单、库存、支付。
- 中间件部署：MySQL、Redis、Nacos、RocketMQ、RocketMQ Dashboard、Sentinel Dashboard。
- 生产化能力：JWT、角色权限、网关拦截、Sentinel 限流、RocketMQ、Redis Lua、幂等 requestId、本地消息表、消费幂等、Flyway、Actuator、Prometheus 指标。
- 验收脚本：普通 API 冒烟、真实秒杀下单、小并发秒杀验收。

## 仍可继续增强

- 订单列表页：现在有订单详情，但没有“我的订单列表”。
- 商品后台：现在商品数据主要靠初始化数据，后台没有完整商品管理页面。
- 秒杀活动发布体验：目前要按活动、场次、SKU 分步配置，后续可以做成向导式发布。
- 真实支付：当前是模拟支付。
- 更大规模压测：已有脚本基础，但还可以补 k6 报告和压测截图。
- CI/CD：可以补 GitHub Actions 自动跑后端测试和前端构建。
- 截图和演示文档：GitHub 发布前建议补首页、秒杀页、运营后台、订单详情截图。
