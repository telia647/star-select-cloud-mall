param(
    [switch]$IncludeIdeaProcesses,
    [switch]$DryRun
)

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$pidDir = Join-Path $root ".pids"

function Stop-ProcessById {
    param(
        [int]$ProcessId,
        [string]$Label
    )
    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if (-not $process) {
        Write-Host "[SKIP] $Label is not running"
        return $false
    }
    if ($DryRun) {
        Write-Host "[DRY] stop $Label PID=$ProcessId"
        return $true
    }
    Stop-Process -Id $ProcessId -Force
    Write-Host "[OK]   stopped $Label PID=$ProcessId"
    return $true
}

if (Test-Path $pidDir) {
    Get-ChildItem $pidDir -Filter "*.pid" | ForEach-Object {
        $service = $_.BaseName
        $pidValue = Get-Content $_.FullName -ErrorAction SilentlyContinue
        if (-not $pidValue) {
            Remove-Item $_.FullName -Force
            return
        }
        Stop-ProcessById -ProcessId ([int]$pidValue) -Label $service | Out-Null
        if (-not $DryRun) {
            Remove-Item $_.FullName -Force
        }
    }
} else {
    Write-Host "No .pids directory found."
}

$rootPattern = [regex]::Escape($root.Path)
$jarPattern = "$rootPattern\\mall-[^\\]+\\target\\mall-[^\\]+-0\.1\.0-SNAPSHOT\.jar"
$ideaPattern = "$rootPattern\\mall-[^\\]+\\target\\classes"

Get-CimInstance Win32_Process -Filter "name = 'java.exe'" -ErrorAction SilentlyContinue |
    Where-Object {
        $_.CommandLine -match $jarPattern -or ($IncludeIdeaProcesses -and $_.CommandLine -match $ideaPattern)
    } |
    ForEach-Object {
        $label = if ($_.CommandLine -match "mall-[^\\]+-0\.1\.0-SNAPSHOT\.jar") {
            $Matches[0]
        } elseif ($_.CommandLine -match "mall-[^\\]+\\target\\classes") {
            $Matches[0]
        } else {
            "project java"
        }
        Stop-ProcessById -ProcessId ([int]$_.ProcessId) -Label $label | Out-Null
    }
