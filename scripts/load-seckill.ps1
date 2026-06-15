param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456",
    [string]$UserPrefix = "load",
    [string]$Password = "123456",
    [long]$ActivityId = 7001,
    [long]$SessionId = 7101,
    [long]$SkuId = 3001,
    [int]$Users = 20,
    [int]$Concurrency = 5,
    [int]$Stock = 50
)

$ErrorActionPreference = "Stop"

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
    Invoke-Api -Method "POST" -Path "/promotions/admin/seckill/sessions" -Token $AdminToken -Body @{
        id = $TargetSessionId
        activityId = $TargetActivityId
        name = "Load Smoke Session"
        startTime = $now.AddMinutes(-10).ToString("yyyy-MM-ddTHH:mm:ss")
        endTime = $now.AddMinutes(50).ToString("yyyy-MM-ddTHH:mm:ss")
        status = 1
        sort = 1
    } | Out-Null
    Write-Host "[OK]   load smoke session refreshed: $TargetSessionId"
}

if ($Users -le 0) {
    throw "Users must be greater than 0"
}
if ($Concurrency -le 0) {
    throw "Concurrency must be greater than 0"
}

Write-Host "Running seckill load smoke against $BaseUrl"
Write-Host "Users=$Users Concurrency=$Concurrency Stock=$Stock"

$adminToken = Login -LoginUsername $AdminUsername -LoginPassword $AdminPassword
Ensure-RunningSession -TargetActivityId $ActivityId -TargetSessionId $SessionId -AdminToken $adminToken
Invoke-Api -Method "POST" -Path "/orders/seckill/stocks" -Token $adminToken -Body @{
    activityId = $ActivityId
    sessionId = $SessionId
    skuId = $SkuId
    quantity = $Stock
} | Out-Null
Write-Host "[OK]   seckill stock preheated"

$started = Get-Date
$jobs = New-Object System.Collections.Generic.List[object]
$results = New-Object System.Collections.Generic.List[object]

$worker = {
    param(
        [string]$BaseUrl,
        [string]$Username,
        [string]$Password,
        [long]$ActivityId,
        [long]$SessionId,
        [long]$SkuId
    )

    function Invoke-WorkerApi {
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

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        try {
            Invoke-WorkerApi -Method "POST" -Path "/users/register" -Body @{
                username = $Username
                password = $Password
                phone = "139$($Username.GetHashCode().ToString().Replace('-', '').PadLeft(8, '0').Substring(0, 8))"
            } | Out-Null
        } catch {
            if ($_.Exception.Message -notlike "*username already exists*") {
                throw
            }
        }

        $login = Invoke-WorkerApi -Method "POST" -Path "/auth/login" -Body @{
            username = $Username
            password = $Password
        }
        $token = $login.accessToken

        $seckillToken = Invoke-WorkerApi -Method "POST" -Path "/orders/seckill/tokens" -Token $token -Body @{
            activityId = $ActivityId
            sessionId = $SessionId
            skuId = $SkuId
            quantity = 1
        }

        $requestId = "$Username-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
        $submit = Invoke-WorkerApi -Method "POST" -Path "/orders/seckill" -Token $token -Body @{
            activityId = $ActivityId
            sessionId = $SessionId
            skuId = $SkuId
            quantity = 1
            token = $seckillToken.token
            requestId = $requestId
        }

        $status = $submit.status
        $message = $submit.message
        if ($status -eq "ACCEPTED") {
            for ($attempt = 1; $attempt -le 20; $attempt++) {
                Start-Sleep -Milliseconds 500
                $result = Invoke-WorkerApi -Method "GET" -Path "/orders/seckill/$requestId" -Token $token
                $status = $result.status
                $message = $result.message
                if ($status -eq "CREATED" -or $status -eq "FAILED") {
                    break
                }
            }
        }

        [pscustomobject]@{
            Username = $Username
            Success = $true
            Status = $status
            Message = $message
            LatencyMs = $sw.ElapsedMilliseconds
        }
    } catch {
        [pscustomobject]@{
            Username = $Username
            Success = $false
            Status = "FAILED"
            Message = $_.Exception.Message
            LatencyMs = $sw.ElapsedMilliseconds
        }
    } finally {
        $sw.Stop()
    }
}

for ($i = 1; $i -le $Users; $i++) {
    while (($jobs | Where-Object { $_.State -eq "Running" }).Count -ge $Concurrency) {
        $done = Wait-Job -Job $jobs -Any -Timeout 5
        if ($done) {
            foreach ($job in @($jobs | Where-Object { $_.State -ne "Running" })) {
                $results.Add((Receive-Job $job))
                Remove-Job $job
                $jobs.Remove($job) | Out-Null
            }
        }
    }

    $username = "$UserPrefix$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())$i"
    $jobs.Add((Start-Job -ScriptBlock $worker -ArgumentList $BaseUrl, $username, $Password, $ActivityId, $SessionId, $SkuId))
}

while ($jobs.Count -gt 0) {
    Wait-Job -Job $jobs -Any | Out-Null
    foreach ($job in @($jobs | Where-Object { $_.State -ne "Running" })) {
        $results.Add((Receive-Job $job))
        Remove-Job $job
        $jobs.Remove($job) | Out-Null
    }
}

$duration = (Get-Date) - $started
$successes = @($results | Where-Object { $_.Success })
$failures = @($results | Where-Object { -not $_.Success })
$created = @($successes | Where-Object { $_.Status -eq "CREATED" })
$pending = @($successes | Where-Object { $_.Status -eq "ACCEPTED" })

Write-Host "[OK]   completed in $([Math]::Round($duration.TotalSeconds, 2))s"
Write-Host "[OK]   success=$($successes.Count) created=$($created.Count) failed=$($failures.Count)"

if ($failures.Count -gt 0) {
    Write-Host "[FAIL] failures:"
    $failures | Select-Object -First 10 Username,Message,LatencyMs | Format-Table -AutoSize
    exit 1
}

if ($pending.Count -gt 0) {
    Write-Host "[FAIL] pending requests:"
    $pending | Select-Object -First 10 Username,Status,Message,LatencyMs | Format-Table -AutoSize
    exit 1
}

if ($Stock -ge $Users -and $created.Count -ne $Users) {
    Write-Host "[FAIL] expected all users to create orders when stock is sufficient"
    $results | Select-Object Username,Status,Message,LatencyMs | Format-Table -AutoSize
    exit 1
}

$latencies = @($successes | ForEach-Object { $_.LatencyMs } | Sort-Object)
if ($latencies.Count -gt 0) {
    $p95Index = [Math]::Min($latencies.Count - 1, [Math]::Ceiling($latencies.Count * 0.95) - 1)
    Write-Host "[OK]   min=$($latencies[0])ms p95=$($latencies[$p95Index])ms max=$($latencies[$latencies.Count - 1])ms"
}
