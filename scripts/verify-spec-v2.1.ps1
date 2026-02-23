param(
    [string]$ComposeCommand = "docker-compose",
    [string]$DbContainer = "safety-postgres",
    [string]$BackendContainer = "safety-backend",
    [string]$KafkaUiUrl = "http://localhost:8085"
)

$ErrorActionPreference = "Stop"

function Invoke-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Get-DbScalar {
    param([string]$Sql)
    $result = docker exec $DbContainer psql -U safety_user -d safety_db -tAc $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "DB query failed: $Sql"
    }
    return ($result | Out-String).Trim()
}

function Wait-Healthy {
    param(
        [string]$Container,
        [int]$TimeoutSec = 180
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $status = docker inspect --format='{{.State.Health.Status}}' $Container 2>$null
        if ($LASTEXITCODE -eq 0 -and ($status | Out-String).Trim() -eq "healthy") {
            return "healthy"
        }
        Start-Sleep -Seconds 3
    }
    $final = docker inspect --format='{{.State.Health.Status}}' $Container 2>$null
    return ($final | Out-String).Trim()
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSec = 120
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $status = (Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5).StatusCode
            if ($status -eq 200) {
                return 200
            }
        } catch {
            Start-Sleep -Seconds 3
            continue
        }
        Start-Sleep -Seconds 2
    }
    return -1
}

Invoke-Step "Bring up full stack"
& $ComposeCommand up -d postgres redis zookeeper kafka kafka-ui minio prometheus grafana backend frontend
& $ComposeCommand ps

Invoke-Step "Apply DB scripts (partitioning + constraints + logs)"
docker exec $DbContainer psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/06-monthly-partitioning.sql | Out-Null
docker exec $DbContainer psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/03-immutability-constraints.sql | Out-Null
docker exec $DbContainer psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/04-event-archive-manifest.sql | Out-Null
docker exec $DbContainer psql -U safety_user -d safety_db -f /docker-entrypoint-initdb.d/05-kafka-recovery-sla-log.sql | Out-Null

Invoke-Step "Verify monthly partitioning"
$relkind = Get-DbScalar "SELECT relkind FROM pg_class WHERE relname='safety_event_log';"
if ($relkind -ne "p") {
    throw "safety_event_log is not partitioned. relkind=$relkind"
}
$partitionCount = [int](Get-DbScalar "SELECT COUNT(*) FROM pg_inherits WHERE inhparent='safety_event_log'::regclass;")
if ($partitionCount -lt 1) {
    throw "No child partitions found for safety_event_log."
}
Write-Host "partitioned table verified. partitions=$partitionCount"

Invoke-Step "Wait backend healthy"
$backendHealth = Wait-Healthy -Container $BackendContainer -TimeoutSec 180
if ($backendHealth -ne "healthy") {
    throw "Backend health is not healthy: $backendHealth"
}
Write-Host "backend health=$backendHealth"

Invoke-Step "Validate immutability + audit trail"
$eventId = Get-DbScalar "SELECT event_id FROM safety_event_log LIMIT 1;"
if ([string]::IsNullOrWhiteSpace($eventId)) {
    throw "No event rows available for immutability verification."
}
$beforeCount = [int](Get-DbScalar "SELECT COUNT(*) FROM event_audit_log;")
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
if ($updateExitCode -eq 0 -or $updateOutput -notmatch "IMMUTABILITY_VIOLATION") {
    throw "Immutability check failed. Expected IMMUTABILITY_VIOLATION."
}
$afterCount = [int](Get-DbScalar "SELECT COUNT(*) FROM event_audit_log;")
if ($afterCount -le $beforeCount) {
    throw "Audit log did not increase after blocked update. before=$beforeCount after=$afterCount"
}
Write-Host "immutability verified. audit_count=$beforeCount->$afterCount"

Invoke-Step "Validate APIs"
$health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
if ($health.status -ne "UP") { throw "Actuator health is not UP." }

$storageSummary = Invoke-RestMethod -Uri "http://localhost:8080/api/storage/tiers/summary" -Method Get
$archiveRun = Invoke-RestMethod -Uri "http://localhost:8080/api/storage/tiers/archive/run" -Method Post
$lag = Invoke-RestMethod -Uri "http://localhost:8080/api/kafka/lag?groupId=safety-group" -Method Get
$sla = Invoke-RestMethod -Uri "http://localhost:8080/api/streams/sla" -Method Get
$auditStats = Invoke-RestMethod -Uri "http://localhost:8080/api/audit/stats" -Method Get
$outboxStats = Invoke-RestMethod -Uri "http://localhost:8080/api/outbox/stats" -Method Get
$sensorLatest = Invoke-RestMethod -Uri "http://localhost:8080/api/sensors/latest" -Method Get
$eventRecent = Invoke-RestMethod -Uri "http://localhost:8080/api/events/recent" -Method Get

if ($null -eq $storageSummary.hotCount) { throw "Storage summary API payload invalid." }
if ($null -eq $lag.totalLag) { throw "Kafka lag API payload invalid." }
if ($null -eq $sla.totalChecks) { throw "Streams SLA API payload invalid." }
if ($null -eq $auditStats.totalAttempts) { throw "Audit stats API payload invalid." }
if ($null -eq $outboxStats.pending) { throw "Outbox stats API payload invalid." }
if (($sensorLatest | Measure-Object).Count -lt 1) { throw "Sensor latest API returned no data." }
if (($eventRecent | Measure-Object).Count -lt 1) { throw "Recent events API returned no data." }
Write-Host "API verification passed."

Invoke-Step "Validate metrics and Kafka UI"
$metricsText = Invoke-RestMethod -Uri "http://localhost:8080/actuator/prometheus" -Method Get
if ($metricsText -notmatch "safety_immutability_violation_total") { throw "Missing immutability metric." }
if ($metricsText -notmatch "kafka_streams_state_recovery_time_ms") { throw "Missing streams recovery metric." }
if ($metricsText -notmatch "safety_outbox_sent_total") { throw "Missing outbox metric." }

$kafkaUiStatus = Wait-HttpOk -Url $KafkaUiUrl -TimeoutSec 180
if ($kafkaUiStatus -ne 200) {
    throw "Kafka UI is not reachable. status=$kafkaUiStatus"
}
Write-Host "Kafka UI reachable. status=$kafkaUiStatus"

Invoke-Step "Result"
Write-Host "PROJECT_SPECIFICATION_v2.1 verification completed successfully." -ForegroundColor Green
Write-Host "storage.hot=$($storageSummary.hotCount), storage.warm=$($storageSummary.warmCount), storage.cold=$($storageSummary.coldCount)"
Write-Host "kafka.totalLag=$($lag.totalLag), streams.latestRecoveryMs=$($sla.latestRecoveryDurationMs), slaPassed=$($sla.passedChecks)/$($sla.totalChecks)"
Write-Host "auditAttempts=$($auditStats.totalAttempts), outboxPending=$($outboxStats.pending)"
