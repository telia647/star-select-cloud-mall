param(
    [string]$MysqlHost = $env:MALL_MYSQL_HOST,
    [string]$MysqlPort = $env:MALL_MYSQL_PORT,
    [string]$MysqlUser = $env:MALL_MYSQL_USERNAME,
    [string]$MysqlPassword = $env:MALL_MYSQL_PASSWORD,
    [string]$Charset = "utf8mb4",
    [string]$Collation = "utf8mb4_0900_ai_ci",
    [switch]$DryRun
)

$databases = @(
    "mall_user",
    "mall_product",
    "mall_promotion",
    "mall_cart",
    "mall_order",
    "mall_inventory",
    "mall_payment",
    "mall_system"
)

if ([string]::IsNullOrWhiteSpace($MysqlHost)) {
    # VM addresses differ per developer. Prefer MALL_VM_HOST; this fallback is only a local example.
    $MysqlHost = if ([string]::IsNullOrWhiteSpace($env:MALL_VM_HOST)) { "192.168.56.101" } else { $env:MALL_VM_HOST }
}
if ([string]::IsNullOrWhiteSpace($MysqlPort)) {
    $MysqlPort = "3306"
}
if ([string]::IsNullOrWhiteSpace($MysqlUser)) {
    # Local VM example only. Use a dedicated privileged migration account outside local acceptance.
    $MysqlUser = "root"
}
if ([string]::IsNullOrWhiteSpace($MysqlPassword)) {
    # Local VM example only. Inject real passwords through environment variables or secret management.
    $MysqlPassword = "root"
}

$statements = $databases | ForEach-Object {
    "CREATE DATABASE IF NOT EXISTS $_ DEFAULT CHARACTER SET $Charset COLLATE $Collation;"
}
$sql = $statements -join [Environment]::NewLine

if ($DryRun) {
    Write-Host "Dry run. SQL to execute on ${MysqlHost}:${MysqlPort}:"
    Write-Host $sql
    exit 0
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    Write-Host "[FAIL] mysql client not found. Install MySQL client or run the SQL below manually:"
    Write-Host $sql
    exit 1
}

$portOk = Test-NetConnection -ComputerName $MysqlHost -Port ([int]$MysqlPort) -InformationLevel Quiet
if (-not $portOk) {
    Write-Host "[FAIL] Cannot connect to MySQL ${MysqlHost}:${MysqlPort}"
    exit 1
}

$oldMysqlPwd = $env:MYSQL_PWD
$env:MYSQL_PWD = $MysqlPassword
try {
    $output = $sql | & mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] Database initialization failed"
        Write-Host $output
        exit $LASTEXITCODE
    }

    $databaseList = & mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser -N -e "SHOW DATABASES LIKE 'mall_%';" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] Database verification failed"
        Write-Host $databaseList
        exit $LASTEXITCODE
    }

    $existing = @($databaseList | Where-Object { $_ -and $_.Trim() -ne "" } | ForEach-Object { $_.Trim() })
    $missing = @($databases | Where-Object { $existing -notcontains $_ })
    if ($missing.Count -gt 0) {
        Write-Host "[FAIL] Missing databases: $($missing -join ', ')"
        exit 1
    }

    Write-Host "[OK] Required mall databases exist on ${MysqlHost}:${MysqlPort}"
    $existing | Sort-Object | ForEach-Object { Write-Host " - $_" }
} finally {
    $env:MYSQL_PWD = $oldMysqlPwd
}
