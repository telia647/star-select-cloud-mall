param(
    [string]$VmHost = $env:MALL_VM_HOST,
    # Local VM example credentials. Override for any shared or deployed environment.
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root",
    [switch]$DryRun
)

$services = @(
    @{ Name = "mall-user"; Port = 8082 },
    @{ Name = "mall-auth"; Port = 8081 },
    @{ Name = "mall-product"; Port = 8083 },
    @{ Name = "mall-promotion"; Port = 8089 },
    @{ Name = "mall-inventory"; Port = 8085 },
    @{ Name = "mall-order"; Port = 8084 },
    @{ Name = "mall-payment"; Port = 8087 },
    @{ Name = "mall-cart"; Port = 8086 },
    @{ Name = "mall-gateway"; Port = 8080 }
)

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$logDir = Join-Path $root "logs"
$pidDir = Join-Path $root ".pids"

# VM addresses are local environment values. Set MALL_VM_HOST or pass -VmHost.
# The fallback below is only this repository's local example, not a project constant.
$exampleVmHost = "192.168.56.101"
if ([string]::IsNullOrWhiteSpace($VmHost)) {
    $VmHost = $exampleVmHost
    Write-Warning "MALL_VM_HOST was not set. Using example VM host $VmHost. Replace it with your own VM IP when running locally."
}

function Resolve-JavaExe {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeExe = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $javaHomeExe) {
            return (Resolve-Path $javaHomeExe).Path
        }
    }

    $javaCommand = Get-Command java -ErrorAction Stop
    $javaExe = $javaCommand.Source

    if ($javaExe -like "*\Common Files\Oracle\Java\javapath\java.exe") {
        $settings = & $javaExe -XshowSettings:properties -version 2>&1
        $javaHomeLine = $settings | Where-Object { $_ -match "^\s*java\.home\s*=\s*(.+)\s*$" } | Select-Object -First 1
        if ($javaHomeLine -and $javaHomeLine -match "^\s*java\.home\s*=\s*(.+)\s*$") {
            $resolved = Join-Path $Matches[1].Trim() "bin\java.exe"
            if (Test-Path $resolved) {
                return (Resolve-Path $resolved).Path
            }
        }
    }

    return $javaExe
}

function Get-PortOwner {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $connection) {
        return $null
    }
    $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
    return [pscustomobject]@{
        ProcessId = $connection.OwningProcess
        ProcessName = if ($process) { $process.ProcessName } else { "unknown" }
    }
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

Write-Host "Backend services will use VM middleware at $VmHost"
Write-Host "Logs: $logDir"

$javaExe = Resolve-JavaExe
Write-Host "Java: $javaExe"

foreach ($service in $services) {
    $name = $service.Name
    $jar = Join-Path $root "$name\target\$name-0.1.0-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "[FAIL] Missing jar for ${name}: $jar"
        Write-Host "Run .\mvnw.cmd -q -DskipTests package first."
        exit 1
    }
}

if (-not $DryRun) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    New-Item -ItemType Directory -Force -Path $pidDir | Out-Null
}

foreach ($service in $services) {
    $name = $service.Name
    $jar = Join-Path $root "$name\target\$name-0.1.0-SNAPSHOT.jar"
    $outLog = Join-Path $logDir "$name.out.log"
    $errLog = Join-Path $logDir "$name.err.log"
    $pidFile = Join-Path $pidDir "$name.pid"
    $port = [int]$service.Port

    $portOwner = Get-PortOwner -Port $port
    if ($portOwner) {
        Write-Host "[FAIL] $name cannot start: port $port is already used by PID=$($portOwner.ProcessId) PROCESS=$($portOwner.ProcessName)"
        Write-Host "Stop the existing process or run scripts\stop-backend.ps1 -IncludeIdeaProcesses when it is an old project service."
        exit 1
    }

    if (Test-Path $pidFile) {
        $existingPid = Get-Content $pidFile -ErrorAction SilentlyContinue
        if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
            Write-Host "[SKIP] $name already running with PID $existingPid"
            continue
        }
    }

    $javaArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_OPTS)) {
        $javaArgs += ($env:JAVA_OPTS -split "\s+" | Where-Object { $_ -and $_.Trim() -ne "" })
    }
    $javaArgs += @("-jar", $jar)
    $command = "$javaExe $($javaArgs -join ' ')"
    if ($DryRun) {
        Write-Host "[DRY] $name -> $command"
        continue
    }

    $process = Start-Process -FilePath $javaExe `
        -ArgumentList $javaArgs `
        -WorkingDirectory $root `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden `
        -PassThru
    $process.Id | Set-Content $pidFile
    Write-Host "[OK]   started $name PID=$($process.Id) port=$($service.Port)"
    Start-Sleep -Seconds 2
}

Write-Host "Use scripts\stop-backend.ps1 to stop these services."
