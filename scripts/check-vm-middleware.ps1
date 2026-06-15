param(
    [string]$VmHost = $env:MALL_VM_HOST,
    [string]$NacosAddr = $env:MALL_NACOS_ADDR,
    [string]$MysqlHost = $env:MALL_MYSQL_HOST,
    [string]$MysqlPort = $env:MALL_MYSQL_PORT,
    [string]$MysqlUser = $env:MALL_MYSQL_USERNAME,
    [string]$MysqlPassword = $env:MALL_MYSQL_PASSWORD,
    [string]$RedisHost = $env:MALL_REDIS_HOST,
    [string]$RedisPort = $env:MALL_REDIS_PORT,
    [string]$RocketMqNameServer = $env:MALL_ROCKETMQ_NAME_SERVER,
    [string]$SentinelDashboard = $env:MALL_SENTINEL_DASHBOARD
)

$ErrorActionPreference = "Continue"

# VM addresses are local environment values. Set MALL_VM_HOST or pass -VmHost.
# The fallback below is only this repository's local example, not a project constant.
$exampleVmHost = "192.168.150.105"

if ([string]::IsNullOrWhiteSpace($VmHost)) {
    $VmHost = $exampleVmHost
    Write-Warning "MALL_VM_HOST was not set. Using example VM host $VmHost. Replace it with your own VM IP when running locally."
}
if ([string]::IsNullOrWhiteSpace($NacosAddr)) {
    $NacosAddr = "$VmHost`:8848"
}
if ([string]::IsNullOrWhiteSpace($MysqlHost)) {
    $MysqlHost = $VmHost
}
if ([string]::IsNullOrWhiteSpace($MysqlPort)) {
    $MysqlPort = "3306"
}
if ([string]::IsNullOrWhiteSpace($MysqlUser)) {
    # Local VM example only. Override MALL_MYSQL_USERNAME for shared or deployed environments.
    $MysqlUser = "root"
}
if ([string]::IsNullOrWhiteSpace($MysqlPassword)) {
    # Local VM example only. Override MALL_MYSQL_PASSWORD or pass -MysqlPassword outside local acceptance.
    $MysqlPassword = "root"
}
if ([string]::IsNullOrWhiteSpace($RedisHost)) {
    $RedisHost = $VmHost
}
if ([string]::IsNullOrWhiteSpace($RedisPort)) {
    $RedisPort = "6379"
}
if ([string]::IsNullOrWhiteSpace($RocketMqNameServer)) {
    $RocketMqNameServer = "$VmHost`:9876"
}
if ([string]::IsNullOrWhiteSpace($SentinelDashboard)) {
    $SentinelDashboard = "$VmHost`:8858"
}

function Split-Endpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Endpoint,
        [Parameter(Mandatory = $true)][int]$DefaultPort
    )
    $parts = $Endpoint.Split(":", 2)
    if ($parts.Length -eq 1) {
        return @{ Host = $parts[0]; Port = $DefaultPort }
    }
    return @{ Host = $parts[0]; Port = [int]$parts[1] }
}

function Test-Port {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$TargetHost,
        [Parameter(Mandatory = $true)][int]$Port
    )
    $result = Test-NetConnection -ComputerName $TargetHost -Port $Port -InformationLevel Quiet
    if ($result) {
        Write-Host "[OK]   $Name $TargetHost`:$Port"
        return $true
    }
    Write-Host "[FAIL] $Name $TargetHost`:$Port"
    return $false
}

Write-Host "Checking VM middleware endpoints..."
Write-Host "VM host: $VmHost"

$nacos = Split-Endpoint -Endpoint $NacosAddr -DefaultPort 8848
$rocketmq = Split-Endpoint -Endpoint $RocketMqNameServer -DefaultPort 9876
$sentinel = Split-Endpoint -Endpoint $SentinelDashboard -DefaultPort 8858
$allOk = $true

$mysqlPortOk = Test-Port -Name "MySQL" -TargetHost $MysqlHost -Port ([int]$MysqlPort)
$allOk = $mysqlPortOk -and $allOk
$allOk = (Test-Port -Name "Redis" -TargetHost $RedisHost -Port ([int]$RedisPort)) -and $allOk
$allOk = (Test-Port -Name "Nacos" -TargetHost $nacos.Host -Port $nacos.Port) -and $allOk
$allOk = (Test-Port -Name "RocketMQ NameServer" -TargetHost $rocketmq.Host -Port $rocketmq.Port) -and $allOk
$allOk = (Test-Port -Name "RocketMQ Broker 10909" -TargetHost $VmHost -Port 10909) -and $allOk
$allOk = (Test-Port -Name "RocketMQ Broker 10911" -TargetHost $VmHost -Port 10911) -and $allOk
$allOk = (Test-Port -Name "Sentinel Dashboard" -TargetHost $sentinel.Host -Port $sentinel.Port) -and $allOk

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysql -and $mysqlPortOk) {
    Write-Host "Checking MySQL login and required databases..."
    $requiredDatabases = @(
        "mall_user",
        "mall_product",
        "mall_promotion",
        "mall_cart",
        "mall_order",
        "mall_inventory",
        "mall_payment",
        "mall_system"
    )
    $databaseSql = "SHOW DATABASES LIKE 'mall_%';"
    $oldMysqlPwd = $env:MYSQL_PWD
    $env:MYSQL_PWD = $MysqlPassword
    $mysqlOutput = & mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser -N -e $databaseSql 2>&1
    $env:MYSQL_PWD = $oldMysqlPwd
    if ($LASTEXITCODE -eq 0) {
        $databases = @($mysqlOutput | Where-Object { $_ -and $_.Trim() -ne "" } | ForEach-Object { $_.Trim() })
        $missingDatabases = @($requiredDatabases | Where-Object { $databases -notcontains $_ })
        if ($missingDatabases.Count -eq 0) {
            Write-Host "[OK]   MySQL login succeeded; required mall databases exist"
        } else {
            Write-Host "[FAIL] MySQL login succeeded, but missing databases: $($missingDatabases -join ', ')"
            $allOk = $false
        }
    } else {
        Write-Host "[FAIL] MySQL login failed"
        Write-Host $mysqlOutput
        $allOk = $false
    }
} elseif (-not $mysql) {
    Write-Host "[SKIP] mysql client not found; TCP check only"
} else {
    Write-Host "[SKIP] MySQL login skipped because TCP check failed"
}

Write-Host ""
Write-Host "Environment values to copy into IDEA:"
Write-Host "MALL_VM_HOST=$VmHost"
Write-Host "MALL_NACOS_ADDR=$NacosAddr"
Write-Host "MALL_MYSQL_HOST=$MysqlHost"
Write-Host "MALL_MYSQL_PORT=$MysqlPort"
Write-Host "MALL_MYSQL_USERNAME=$MysqlUser"
Write-Host "MALL_MYSQL_PASSWORD=$MysqlPassword"
Write-Host "MALL_REDIS_HOST=$RedisHost"
Write-Host "MALL_REDIS_PORT=$RedisPort"
Write-Host "MALL_ROCKETMQ_NAME_SERVER=$RocketMqNameServer"
Write-Host "MALL_SENTINEL_DASHBOARD=$SentinelDashboard"

if (-not $allOk) {
    exit 1
}
