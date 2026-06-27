param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Password = "123456",
    [long]$ActivityId = 7002,
    [long]$SessionId = 7112,
    [long]$SeckillSkuId = 3010
)

$ErrorActionPreference = "Stop"

$users = @(
    @{ username = "demo"; phone = "13800000000"; skuIds = @(3010, 3012); pay = $true },
    @{ username = "alice"; phone = "13800000001"; skuIds = @(3014, 3020); pay = $false },
    @{ username = "bob"; phone = "13800000002"; skuIds = @(3016, 3022); pay = $true },
    @{ username = "carol"; phone = "13800000003"; skuIds = @(3024, 3028); pay = $false }
)

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
        TimeoutSec = 20
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

function Register-User {
    param([hashtable]$User)
    try {
        Invoke-Api -Method "POST" -Path "/users/register" -Body @{
            username = $User.username
            password = $Password
            phone = $User.phone
        } | Out-Null
        Write-Host "[OK]   registered user $($User.username)"
    } catch {
        Write-Host "[SKIP] user $($User.username) already exists or cannot register: $($_.Exception.Message)"
    }
}

function Login {
    param([string]$Username)
    $data = Invoke-Api -Method "POST" -Path "/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    return $data.accessToken
}

function Create-CartOrder {
    param(
        [hashtable]$User,
        [string]$Token
    )
    Invoke-Api -Method "DELETE" -Path "/cart/items" -Token $Token | Out-Null
    foreach ($skuId in $User.skuIds) {
        Invoke-Api -Method "POST" -Path "/cart/items" -Token $Token -Body @{
            skuId = $skuId
            quantity = 1
        } | Out-Null
    }
    $order = Invoke-Api -Method "POST" -Path "/cart/checkout" -Token $Token -Body @{
        remark = "演示订单-$($User.username)"
        requestId = "seed-cart-$($User.username)-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
    }
    Write-Host "[OK]   created cart order $($order.orderNo) for $($User.username)"
    if ($User.pay) {
        $payment = Invoke-Api -Method "POST" -Path "/payments/pay" -Token $Token -Body @{
            orderNo = $order.orderNo
            payChannel = "MOCK"
        }
        Write-Host "[OK]   paid order $($order.orderNo), payNo=$($payment.payNo)"
    } else {
        Invoke-Api -Method "POST" -Path "/orders/$($order.orderNo)/cancel" -Token $Token | Out-Null
        Write-Host "[OK]   canceled order $($order.orderNo)"
    }
}

function Create-SeckillOrder {
    param(
        [string]$Token
    )
    try {
        $adminToken = Login -Username "admin"
        Invoke-Api -Method "POST" -Path "/orders/seckill/stocks" -Token $adminToken -Body @{
            activityId = $ActivityId
            sessionId = $SessionId
            skuId = $SeckillSkuId
            quantity = 20
        } | Out-Null
        Write-Host "[OK]   seckill stock preheated"
    } catch {
        Write-Host "[WARN] seckill stock preheat skipped: $($_.Exception.Message)"
    }

    $tokenResponse = Invoke-Api -Method "POST" -Path "/orders/seckill/tokens" -Token $Token -Body @{
        activityId = $ActivityId
        sessionId = $SessionId
        skuId = $SeckillSkuId
        quantity = 1
    }
    $requestId = "seed-seckill-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
    $submit = Invoke-Api -Method "POST" -Path "/orders/seckill" -Token $Token -Body @{
        activityId = $ActivityId
        sessionId = $SessionId
        skuId = $SeckillSkuId
        quantity = 1
        token = $tokenResponse.token
        requestId = $requestId
    }
    for ($i = 0; $i -lt 10 -and $submit.status -eq "ACCEPTED"; $i++) {
        Start-Sleep -Seconds 1
        $submit = Invoke-Api -Method "GET" -Path "/orders/seckill/$requestId" -Token $Token
    }
    if ($submit.orderNo) {
        Write-Host "[OK]   created seckill order $($submit.orderNo)"
        Invoke-Api -Method "POST" -Path "/payments/pay" -Token $Token -Body @{
            orderNo = $submit.orderNo
            payChannel = "MOCK"
        } | Out-Null
        Write-Host "[OK]   paid seckill order $($submit.orderNo)"
    } else {
        Write-Host "[WARN] seckill order not created: $($submit.status) $($submit.message)"
    }
}

Write-Host "Seeding demo transaction data against $BaseUrl"

foreach ($user in $users) {
    Register-User -User $user
    $token = Login -Username $user.username
    Invoke-Api -Method "GET" -Path "/users/me/benefits" -Token $token | Out-Null
    Invoke-Api -Method "GET" -Path "/users/me/coupons" -Token $token | Out-Null
    Create-CartOrder -User $user -Token $token
}

$demoToken = Login -Username "demo"
Create-SeckillOrder -Token $demoToken

Write-Host "[OK]   demo data seed completed"
