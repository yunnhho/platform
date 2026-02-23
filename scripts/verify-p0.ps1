param(
    [string]$ComposeCommand = "docker-compose",
    [string]$DbContainer = "safety-postgres",
    [string]$BackendContainer = "safety-backend"
)

$ErrorActionPreference = "Stop"

function Invoke-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Wait-ForHealthy {
    param(
        [string]$Container,
        [int]$TimeoutSec = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $status = docker inspect --format='{{.State.Health.Status}}' $Container 2>$null
        if ($LASTEXITCODE -eq 0 -and ($status | Out-String).Trim() -eq "healthy") {
            return "healthy"
        }
        Start-Sleep -Seconds 3
    }

    $finalStatus = docker inspect --format='{{.State.Health.Status}}' $Container 2>$null
    return ($finalStatus | Out-String).Trim()
}

function Get-ScalarQuery {
    param([string]$Sql)
    $result = docker exec $DbContainer psql -U safety_user -d safety_db -tAc $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "Query failed: $Sql"
    }
    return ($result | Out-String).Trim()
}

Invoke-Step "Service status"
& $ComposeCommand ps

Invoke-Step "Pick one event"
$eventId = Get-ScalarQuery "SELECT event_id FROM safety_event_log LIMIT 1;"
if ([string]::IsNullOrWhiteSpace($eventId)) {
    throw "No rows in safety_event_log. Insert at least one event first."
}
Write-Host "event_id=$eventId"

Invoke-Step "Audit count before modification attempt"
$beforeCount = [int](Get-ScalarQuery "SELECT COUNT(*) FROM event_audit_log;")
Write-Host "before=$beforeCount"

Invoke-Step "Run blocked UPDATE attempt"
$updateSql = "UPDATE safety_event_log SET payload='{}'::jsonb WHERE event_id='$eventId'::uuid;"
$updateCommand = "docker exec $DbContainer psql -U safety_user -d safety_db -v ON_ERROR_STOP=1 -c `"$updateSql`""
$oldErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    $updateOutput = (cmd.exe /c $updateCommand 2>&1 | Out-String)
    $updateExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $oldErrorActionPreference
}
if ($updateExitCode -eq 0) {
    throw "Expected IMMUTABILITY_VIOLATION, but UPDATE succeeded."
}
if (($updateOutput | Out-String) -notmatch "IMMUTABILITY_VIOLATION") {
    throw "Expected IMMUTABILITY_VIOLATION in error output."
}
Write-Host "blocked as expected"

Invoke-Step "Audit count after modification attempt"
$afterCount = [int](Get-ScalarQuery "SELECT COUNT(*) FROM event_audit_log;")
Write-Host "after=$afterCount"
if ($afterCount -le $beforeCount) {
    throw "Audit log did not increase. before=$beforeCount, after=$afterCount"
}
Write-Host "audit log increased by $($afterCount - $beforeCount)"

Invoke-Step "Backend health status"
$healthStatus = Wait-ForHealthy -Container $BackendContainer -TimeoutSec 120
Write-Host "container health=$healthStatus"
if ($healthStatus -ne "healthy") {
    throw "Backend container is not healthy."
}

Invoke-Step "API checks"
$auditStats = Invoke-RestMethod -Uri "http://localhost:8080/api/audit/stats" -Method Get
$actuator = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
if ($actuator.status -ne "UP") {
    throw "Actuator health is not UP."
}
Write-Host "audit totalAttempts=$($auditStats.totalAttempts)"
Write-Host "actuator status=$($actuator.status)"

Invoke-Step "Result"
Write-Host "P0 verification completed successfully." -ForegroundColor Green
