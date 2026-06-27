param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$GrafanaUrl = "http://localhost:3000",
    [string]$RocketMqDashboardUrl = "http://localhost:8088",
    [string]$SentinelDashboardUrl = "http://localhost:8858",
    [switch]$RequireProductionSecrets,
    [switch]$SkipDashboards
)

$ErrorActionPreference = "Stop"
$failed = 0

function Pass {
    param([string]$Message)
    Write-Host "[OK]   $Message" -ForegroundColor Green
}

function Fail {
    param([string]$Message)
    $script:failed += 1
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Test-Http {
    param(
        [string]$Name,
        [string]$Url,
        [int[]]$AllowedStatus = @(200, 401, 403)
    )
    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec 5 -UseBasicParsing
        if ($AllowedStatus -contains [int]$response.StatusCode) {
            Pass "$Name reachable ($($response.StatusCode))"
            return
        }
        Fail "$Name unexpected status $($response.StatusCode)"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -and ($AllowedStatus -contains [int]$status)) {
            Pass "$Name reachable ($status)"
            return
        }
        Fail "$Name unreachable: $($_.Exception.Message)"
    }
}

function Test-EnvSecret {
    param(
        [string]$Name,
        [string]$Value,
        [string[]]$BlockedValues
    )
    if ([string]::IsNullOrWhiteSpace($Value)) {
        Fail "$Name is empty"
        return
    }
    if ($BlockedValues -contains $Value) {
        Fail "$Name uses local/demo value"
        return
    }
    Pass "$Name is set"
}

Write-Host "Running pre-launch checks against $BaseUrl"

Test-Http -Name "Gateway health" -Url "$GatewayUrl/actuator/health" -AllowedStatus @(200)
Test-Http -Name "Product API" -Url "$BaseUrl/products" -AllowedStatus @(200)
Test-Http -Name "Seckill catalog" -Url "$BaseUrl/promotions/seckill/sessions" -AllowedStatus @(200)
Test-Http -Name "Internal API blocked by gateway" -Url "$BaseUrl/orders/internal/probe" -AllowedStatus @(401, 403, 404)

if (-not $SkipDashboards) {
    Test-Http -Name "Prometheus" -Url $PrometheusUrl -AllowedStatus @(200, 302)
    Test-Http -Name "Grafana" -Url $GrafanaUrl -AllowedStatus @(200, 302)
    Test-Http -Name "RocketMQ Dashboard" -Url $RocketMqDashboardUrl -AllowedStatus @(200, 302)
    Test-Http -Name "Sentinel Dashboard" -Url $SentinelDashboardUrl -AllowedStatus @(200, 302)
}

if ($RequireProductionSecrets) {
    Test-EnvSecret -Name "MALL_JWT_SECRET" -Value $env:MALL_JWT_SECRET -BlockedValues @(
        "mall-demo-jwt-secret-key-for-local-development",
        "mall-demo-jwt-secret-key-for-hs256-please-change",
        "replace-with-at-least-32-byte-secret"
    )
    Test-EnvSecret -Name "MALL_MYSQL_USERNAME" -Value $env:MALL_MYSQL_USERNAME -BlockedValues @("root")
    Test-EnvSecret -Name "MALL_MYSQL_PASSWORD" -Value $env:MALL_MYSQL_PASSWORD -BlockedValues @("root", "password", "123456")
    Test-EnvSecret -Name "MALL_PAYMENT_CALLBACK_SECRET" -Value $env:MALL_PAYMENT_CALLBACK_SECRET -BlockedValues @("local-mock-payment-callback-secret")
    if ($env:MALL_SECURITY_FAIL_ON_DEFAULT_SECRET -ne "true") {
        Fail "MALL_SECURITY_FAIL_ON_DEFAULT_SECRET must be true for production-like startup"
    } else {
        Pass "MALL_SECURITY_FAIL_ON_DEFAULT_SECRET=true"
    }
} else {
    Warn "Production secret checks skipped. Add -RequireProductionSecrets for deployment readiness."
}

if ($failed -gt 0) {
    throw "$failed pre-launch checks failed"
}

Pass "pre-launch checks passed"
