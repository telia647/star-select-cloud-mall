param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456",
    [string]$Username = "demo",
    [string]$Password = "123456",
    [long]$ActivityId = 7001,
    [long]$SessionId = 7101,
    [long]$SkuId = 3001,
    [switch]$RunSeckill
)

$ErrorActionPreference = "Stop"

function Ensure-RunningSession {
    param(
        [Parameter(Mandatory = $true)][long]$TargetActivityId,
        [Parameter(Mandatory = $true)][long]$TargetSessionId,
        [Parameter(Mandatory = $true)][string]$AdminToken
    )
    $sessions = Invoke-Api -Method "GET" -Path "/promotions/seckill/sessions"
    $session = $sessions | Where-Object { $_.id -eq $TargetSessionId } | Select-Object -First 1
    if ($session -and $session.state -eq "RUNNING") {
        return
    }

    $now = Get-Date
    $startTime = $now.AddMinutes(-10).ToString("yyyy-MM-ddTHH:mm:ss")
    $endTime = $now.AddMinutes(50).ToString("yyyy-MM-ddTHH:mm:ss")
    Invoke-Api -Method "POST" -Path "/promotions/admin/seckill/sessions" -Token $AdminToken -Body @{
        id = $TargetSessionId
        activityId = $TargetActivityId
        name = "Smoke Session"
        startTime = $startTime
        endTime = $endTime
        status = 1
        sort = 1
    } | Out-Null
    Write-Host "[OK]   smoke session refreshed: $TargetSessionId"
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null,
        [string]$Token = $null
    )
    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }
    $response = Invoke-RestMethod @params
    if ($null -eq $response -or -not $response.success) {
        $message = if ($response) { $response.message } else { "empty response" }
        throw "$Method $Path failed: $message"
    }
    return $response.data
}

function Login {
    param(
        [string]$LoginUsername,
        [string]$LoginPassword
    )
    $data = Invoke-Api -Method "POST" -Path "/auth/login" -Body @{
        username = $LoginUsername
        password = $LoginPassword
    }
    return $data.accessToken
}

function Verify-SeckillOrder {
    param(
        [Parameter(Mandatory = $true)][string]$OrderNo,
        [Parameter(Mandatory = $true)][string]$MemberToken,
        [Parameter(Mandatory = $true)][string]$AdminToken
    )

    Invoke-Api -Method "GET" -Path "/orders/$OrderNo" -Token $MemberToken | Out-Null
    Write-Host "[OK]   seckill order detail"

    $payment = Invoke-Api -Method "POST" -Path "/payments/pay" -Token $MemberToken -Body @{
        orderNo = $OrderNo
        payChannel = "MOCK"
    }
    Write-Host "[OK]   seckill order paid: $($payment.payNo)"

    Invoke-Api -Method "GET" -Path "/payments/$($payment.payNo)" -Token $MemberToken | Out-Null
    Write-Host "[OK]   payment detail"

    Invoke-Api -Method "GET" -Path "/orders/admin/$OrderNo/status-logs" -Token $AdminToken | Out-Null
    Write-Host "[OK]   order status logs"

    Invoke-Api -Method "GET" -Path "/inventory/admin/stock-flows?orderNo=$OrderNo" -Token $AdminToken | Out-Null
    Write-Host "[OK]   inventory stock flows"
}

Write-Host "Running smoke checks against $BaseUrl"

try {
    Invoke-Api -Method "POST" -Path "/users/register" -Body @{
        username = $Username
        password = $Password
        phone = "13800000000"
    } | Out-Null
    Write-Host "[OK]   demo user registered"
} catch {
    Write-Host "[SKIP] demo user register skipped: $($_.Exception.Message)"
}

$adminToken = Login -LoginUsername $AdminUsername -LoginPassword $AdminPassword
Write-Host "[OK]   admin login"

$memberToken = Login -LoginUsername $Username -LoginPassword $Password
Write-Host "[OK]   member login"

Invoke-Api -Method "GET" -Path "/products" | Out-Null
Write-Host "[OK]   product list"

Invoke-Api -Method "GET" -Path "/promotions/seckill/sessions" | Out-Null
Write-Host "[OK]   seckill sessions"

Invoke-Api -Method "GET" -Path "/promotions/seckill/sessions/$SessionId/items" | Out-Null
Write-Host "[OK]   seckill session items"

Invoke-Api -Method "GET" -Path "/promotions/admin/seckill/activities" -Token $adminToken | Out-Null
Write-Host "[OK]   admin activities"

Invoke-Api -Method "GET" -Path "/users/me" -Token $memberToken | Out-Null
Write-Host "[OK]   current user"

Invoke-Api -Method "GET" -Path "/users/me/benefits" -Token $memberToken | Out-Null
Write-Host "[OK]   member benefits"

Invoke-Api -Method "GET" -Path "/users/me/coupons" -Token $memberToken | Out-Null
Write-Host "[OK]   member coupons"

Invoke-Api -Method "GET" -Path "/orders/me" -Token $memberToken | Out-Null
Write-Host "[OK]   my orders"

if (-not $RunSeckill) {
    Write-Host "Smoke checks passed. Add -RunSeckill to create a real seckill order."
    exit 0
}

Ensure-RunningSession -TargetActivityId $ActivityId -TargetSessionId $SessionId -AdminToken $adminToken

Invoke-Api -Method "POST" -Path "/orders/seckill/stocks" -Token $adminToken -Body @{
    activityId = $ActivityId
    sessionId = $SessionId
    skuId = $SkuId
    quantity = 10
} | Out-Null
Write-Host "[OK]   seckill stock preheated"

$token = Invoke-Api -Method "POST" -Path "/orders/seckill/tokens" -Token $memberToken -Body @{
    activityId = $ActivityId
    sessionId = $SessionId
    skuId = $SkuId
    quantity = 1
}
Write-Host "[OK]   seckill token issued"

$requestId = "smoke-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
$submit = Invoke-Api -Method "POST" -Path "/orders/seckill" -Token $memberToken -Body @{
    activityId = $ActivityId
    sessionId = $SessionId
    skuId = $SkuId
    quantity = 1
    token = $token.token
    requestId = $requestId
}
Write-Host "[OK]   seckill submitted: $($submit.status)"

if ($submit.status -eq "CREATED" -or $submit.status -eq "FAILED") {
    Write-Host "[OK]   seckill final result: $($submit.status)"
    if ($submit.orderNo) {
        Verify-SeckillOrder -OrderNo $submit.orderNo -MemberToken $memberToken -AdminToken $adminToken
    }
    exit 0
}

for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    $result = Invoke-Api -Method "GET" -Path "/orders/seckill/$requestId" -Token $memberToken
    Write-Host "[OK]   seckill result poll $($i + 1): $($result.status)"
    if ($result.status -eq "CREATED" -or $result.status -eq "FAILED") {
        Write-Host "[OK]   seckill final result: $($result.status)"
        if ($result.orderNo) {
            Verify-SeckillOrder -OrderNo $result.orderNo -MemberToken $memberToken -AdminToken $adminToken
            Invoke-Api -Method "GET" -Path "/orders/me" -Token $memberToken | Out-Null
            Write-Host "[OK]   my orders after seckill"
        }
        exit 0
        break
    }
}

throw "seckill request did not reach a final state after polling"
