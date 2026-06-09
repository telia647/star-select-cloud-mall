# Architecture

## Service Topology

```mermaid
flowchart LR
  Web[mall-web Vue 3] --> Gateway[mall-gateway]
  Gateway --> Auth[mall-auth]
  Gateway --> User[mall-user]
  Gateway --> Product[mall-product]
  Gateway --> Cart[mall-cart]
  Gateway --> Promotion[mall-promotion]
  Gateway --> Order[mall-order]
  Gateway --> Inventory[mall-inventory]
  Gateway --> Payment[mall-payment]

  Auth --> MySQLUser[(mall_user)]
  User --> MySQLUser
  Product --> MySQLProduct[(mall_product)]
  Product --> Redis[(Redis)]
  Cart --> MySQLCart[(mall_cart)]
  Promotion --> MySQLPromotion[(mall_promotion)]
  Promotion --> Redis
  Order --> MySQLOrder[(mall_order)]
  Order --> Redis
  Order --> Inventory
  Order --> Product
  Inventory --> MySQLInventory[(mall_inventory)]
  Payment --> MySQLPayment[(mall_payment)]
  Payment --> Order

  Order --> RocketMQ[(RocketMQ)]
  Payment --> RocketMQ
  RocketMQ --> Order
```

## Seckill Request Flow

```mermaid
sequenceDiagram
  participant C as C-side Web
  participant G as Gateway
  participant O as mall-order
  participant P as mall-promotion
  participant R as Redis
  participant MQ as RocketMQ
  participant I as mall-inventory

  C->>G: POST /orders/seckill/tokens
  G->>O: user headers + token request
  O->>P: validate activity/session/SKU
  P-->>O: seckill price, limit, available stock
  O->>R: SETNX stock if not preheated
  O->>R: SET token with TTL
  O-->>C: seckill token

  C->>G: POST /orders/seckill
  G->>O: user headers + token + requestId
  O->>P: validate activity/session/SKU
  O->>R: Lua token + duplicate + stock pre-deduct
  O->>R: save ACCEPTED result
  O->>MQ: SeckillOrderEvent
  O-->>C: ACCEPTED

  MQ->>O: consume SeckillOrderEvent
  O->>I: lock normal inventory
  O->>O: create seckill order with server-side seckill price
  O->>O: record oms_seckill_reservation
  O->>R: save CREATED result
  C->>G: GET /orders/seckill/{requestId}
  G->>O: result query
  O-->>C: CREATED + orderNo
```

## Payment And Timeout Flow

```mermaid
sequenceDiagram
  participant C as C-side Web
  participant Pay as mall-payment
  participant O as mall-order
  participant I as mall-inventory
  participant R as Redis
  participant MQ as RocketMQ

  C->>Pay: POST /payments/pay
  Pay->>O: mark paid
  O->>I: deduct locked stock
  O->>O: mark order PAID
  O->>O: mark seckill reservation PAID
  O->>MQ: InventoryDeductedEvent

  O->>O: scheduled scan expired orders
  O->>I: release locked stock
  O->>O: mark order CANCELED
  O->>O: mark seckill reservation RELEASED
  O->>R: increment activity stock and delete buyer key
```

## Admin Operation Flow

```mermaid
sequenceDiagram
  participant A as Admin Web
  participant G as Gateway
  participant P as mall-promotion
  participant O as mall-order
  participant DB as mall_promotion
  participant R as Redis

  A->>G: POST /promotions/admin/seckill/items
  G->>P: X-User-Id, X-Username, X-User-Role
  P->>P: RoleGuard.requireAdmin
  P->>DB: save activity/session/SKU
  P->>DB: insert promo_operation_log
  P-->>A: saved id

  A->>G: POST /orders/seckill/stocks
  G->>O: admin headers + stock request
  O->>R: preheat Redis activity stock
```

## Consistency Model

- Redis owns hot-path seckill pre-deduct stock during the sale.
- `mall-promotion` owns activity/session/SKU configuration and periodically reconciles Redis stock into `promo_seckill_sku.available_stock`.
- `mall-order` owns order status and seckill reservation state.
- `mall-inventory` owns normal SKU stock locks and final deduction.
- Payment success marks order paid and confirms the seckill reservation.
- Payment timeout or cancel releases normal inventory and Redis seckill stock.
