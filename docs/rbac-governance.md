# RBAC And Access Governance

The current project implements a lightweight RBAC baseline that is enough for local acceptance and GitHub demonstration, while keeping the production evolution path explicit.

## Implemented Baseline

| Layer | Control |
| --- | --- |
| User data | `ums_user.role_code` stores `ADMIN` or `MEMBER` |
| Auth | JWT access token carries the `role` claim |
| Gateway | JWT is validated once and forwarded as `X-User-Id`, `X-Username`, `X-User-Role` |
| Internal surface | Gateway blocks `/api/**/internal` |
| Backend guard | Admin APIs call `RoleGuard.requireAdmin(roleCode)` |
| Front-end | Admin console route and navigation are hidden unless `roleCode === "ADMIN"` |

Seed account for local acceptance:

```text
admin / 123456
```

This account is for local development only. Disable or rotate it before any real deployment.

## Protected Admin Surfaces

| Surface | Required Role | Notes |
| --- | --- | --- |
| `POST /api/orders/seckill/stocks` | `ADMIN` | Redis seckill stock preheat |
| `GET /api/orders/admin/{orderNo}/status-logs` | `ADMIN` | Order status timeline for diagnostics |
| `GET /api/inventory/admin/stock-flows?orderNo={orderNo}` | `ADMIN` | Inventory lock/release/deduct flow diagnostics |
| `/api/promotions/admin/seckill/**` | `ADMIN` | Activity, session, SKU, operation log management |
| `/admin/seckill` | `ADMIN` | Front-end operation console |

## Production Evolution

For a real deployment, keep the current role claim as a fast authorization hint, but move authorization ownership to explicit RBAC tables.

Recommended tables:

```sql
CREATE TABLE sys_admin_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_admin_user_username (username)
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_sys_role_code (role_code)
);

CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL,
    resource_pattern VARCHAR(255) NOT NULL,
    action VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_sys_permission_code (permission_code)
);

CREATE TABLE sys_admin_user_role (
    admin_user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (admin_user_id, role_id)
);

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);
```

## Hardening Checklist

- Admin and member identities should be separated in production.
- Internal service ports should be private; do not rely only on gateway path blocking.
- Add service-to-service request signing or mTLS for internal APIs.
- Treat `X-User-*` headers as trusted only after gateway validation.
- Persist admin operation logs with `operatorId`, `traceId`, resource type, resource ID, and request summary.
- Add permission-level guards before splitting to a standalone admin service.
