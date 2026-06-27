param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "mall-user",
        "mall-auth",
        "mall-product",
        "mall-promotion",
        "mall-inventory",
        "mall-order",
        "mall-payment",
        "mall-cart",
        "mall-gateway"
    )]
    [string]$Service,

    [string]$VmHost = $env:MALL_VM_HOST,
    # Local VM example credentials. Override for any shared or deployed environment.
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root",
    [switch]$DryRun
)

# VM addresses are local environment values. Set MALL_VM_HOST or pass -VmHost.
# The fallback below is only this repository's local example, not a project constant.
$exampleVmHost = "192.168.56.101"
if ([string]::IsNullOrWhiteSpace($VmHost)) {
    $VmHost = $exampleVmHost
    Write-Warning "MALL_VM_HOST was not set. Using example VM host $VmHost. Replace it with your own VM IP when running locally."
}

$env:MALL_VM_HOST = $VmHost
# These addresses assume all middleware containers run on the same VM.
# Override individual MALL_* variables in deployment when services are split across hosts.
$env:MALL_NACOS_ADDR = "$VmHost`:8848"
$env:MALL_MYSQL_HOST = $VmHost
$env:MALL_MYSQL_PORT = "3306"
$env:MALL_MYSQL_USERNAME = $MysqlUser
$env:MALL_MYSQL_PASSWORD = $MysqlPassword
$env:MALL_REDIS_HOST = $VmHost
$env:MALL_REDIS_PORT = "6379"
$env:MALL_ROCKETMQ_NAME_SERVER = "$VmHost`:9876"
$env:MALL_SENTINEL_DASHBOARD = "$VmHost`:8858"
$env:MALL_CORS_ALLOWED_ORIGIN_PATTERNS = "http://localhost:5173"

if ([string]::IsNullOrWhiteSpace($env:MALL_JWT_SECRET)) {
    # Local acceptance placeholder only. Set MALL_JWT_SECRET explicitly for any shared or deployed environment.
    $env:MALL_JWT_SECRET = "replace-with-at-least-32-byte-secret"
}

Write-Host "Starting $Service with VM middleware at $VmHost"
Write-Host "MALL_MYSQL_HOST=$env:MALL_MYSQL_HOST"
Write-Host "MALL_NACOS_ADDR=$env:MALL_NACOS_ADDR"
Write-Host "MALL_REDIS_HOST=$env:MALL_REDIS_HOST"
Write-Host "MALL_ROCKETMQ_NAME_SERVER=$env:MALL_ROCKETMQ_NAME_SERVER"

if ($DryRun) {
    Write-Host "Dry run only. Command:"
    Write-Host ".\mvnw.cmd -pl $Service spring-boot:run"
    exit 0
}

& .\mvnw.cmd -pl $Service spring-boot:run
