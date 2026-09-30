#Requires -Version 7.0
param([switch] $RunPostmanCollection, [switch] $RunWebTests, [int] $Port = 5081)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$databaseUrl = $env:CACAOSELVA_TEST_DB_URL
if (-not $databaseUrl -or $databaseUrl -notmatch '^jdbc:postgresql://[^/]+/cacaoselva_test_[a-z0-9]+$') {
    throw 'Ejecuta scripts/verify.ps1. Este script solo admite una base temporal cacaoselva_test_*.'
}
$logDirectory = Join-Path $projectRoot ('.tools/validation/' + ($databaseUrl -split '/')[-1])
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$javaCommand = (Get-Command java -ErrorAction Stop).Source
$runtimeLine = & $javaCommand -XshowSettings:properties -version 2>&1 |
    Where-Object { "$_" -match '^\s*java.home\s*=' } | Select-Object -First 1
$runtimeDirectory = ("$runtimeLine" -split '=', 2)[1].Trim()
$javaCommand = Join-Path $runtimeDirectory $(if ($IsWindows) { 'bin/java.exe' } else { 'bin/java' })
$baseUrl = "http://127.0.0.1:$Port"
$apiProcess = $null
$monitorProcess = $null
$oldUrl = $env:CACAOSELVA_DB_URL
$oldUser = $env:CACAOSELVA_DB_USER
$oldPassword = $env:CACAOSELVA_DB_PASSWORD
$oldApiToken = $env:CACAOSELVA_API_TOKEN
$probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $probe.Start() } finally { $probe.Stop() }

function Start-Jar([string] $module, [string] $name) {
    $jar = Join-Path $projectRoot "$module/target/$module-1.0.0-SNAPSHOT.jar"
    $arguments = @('-jar', ('"{0}"' -f $jar))
    if ($module -eq 'monitor') {
        $arguments = @("-Dcacaoselva.api.baseUrl=$baseUrl", '-Dcacaoselva.api.timeoutSeconds=2',
            '-Dcacaoselva.monitor.intervalSeconds=1') + $arguments
    } else { $arguments += "--server.port=$Port" }
    $options = @{
        FilePath=$javaCommand; ArgumentList=$arguments; WorkingDirectory=$projectRoot; PassThru=$true
        RedirectStandardOutput=(Join-Path $logDirectory "$name.out.log")
        RedirectStandardError=(Join-Path $logDirectory "$name.err.log")
    }
    if ($IsWindows) { $options.WindowStyle = 'Hidden' }
    Start-Process @options
}

function Wait-Condition([scriptblock] $condition, [string] $description) {
    $deadline = [DateTime]::UtcNow.AddSeconds(45)
    do {
        if (& $condition) { return }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Tiempo agotado: $description"
}

function Test-ApiReady {
    try { return (Invoke-WebRequest "$baseUrl/actuator/health" -TimeoutSec 2 -SkipHttpErrorCheck).StatusCode -eq 200 }
    catch [System.Net.Http.HttpRequestException] { return $false }
    catch [System.Threading.Tasks.TaskCanceledException] { return $false }
}

function Read-MonitorLog {
    $path = Join-Path $logDirectory 'monitor.out.log'
    if (Test-Path -LiteralPath $path) { return [string](Get-Content -LiteralPath $path -Raw) }
    return ''
}

try {
    $env:CACAOSELVA_DB_URL = $databaseUrl
    $env:CACAOSELVA_DB_USER = $env:CACAOSELVA_TEST_DB_USER
    $env:CACAOSELVA_DB_PASSWORD = $env:CACAOSELVA_TEST_DB_PASSWORD
    $apiProcess = Start-Jar 'api' 'api'
    Wait-Condition { Test-ApiReady } 'inicio de API'
    $login = Invoke-RestMethod "$baseUrl/auth/login" -Method Post -ContentType 'application/json' `
        -Body (@{ usuario='admin'; contrasena=$env:CACAOSELVA_ADMIN_PASSWORD } | ConvertTo-Json)
    $authHeaders = @{ Authorization = "Bearer $($login.token)" }
    $env:CACAOSELVA_API_TOKEN = $login.token
    if ((Invoke-WebRequest "$baseUrl/lotes" -SkipHttpErrorCheck -TimeoutSec 5).StatusCode -ne 401) { throw 'La API permite consultar lotes sin token.' }
    $cases = @{ '/lotes'=200; '/lotes/1'=200; '/lotes/999'=404; '/lotes/abc'=400; '/lotes/0'=400; '/lotes/pendientes/conteo'=200 }
    foreach ($path in $cases.Keys) {
        $response = Invoke-WebRequest ($baseUrl + $path) -Headers $authHeaders -SkipHttpErrorCheck -TimeoutSec 5
        if ($response.StatusCode -ne $cases[$path]) { throw "Código incorrecto: $path" }
        Write-Output "PASS GET $path -> $($response.StatusCode)"
    }
    $lotes = Invoke-RestMethod "$baseUrl/lotes" -Headers $authHeaders
    if ($lotes.Count -ne 30) { throw 'Se esperaban los 30 lotes iniciales.' }
    if ($RunWebTests) {
        $env:CACAOSELVA_WEB_TEST_URL = $baseUrl
        Push-Location (Join-Path $projectRoot 'web-tests')
        try { & npm test; if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas web.' } }
        finally { Pop-Location; Remove-Item Env:CACAOSELVA_WEB_TEST_URL -ErrorAction SilentlyContinue }
    }
    if ($RunPostmanCollection) {
        $collection = Join-Path $projectRoot 'docs/postman/CacaoSelva.postman_collection.json'
        $report = Join-Path $logDirectory 'postman-results.json'
        $newman = Get-Command newman -ErrorAction SilentlyContinue
        $newmanArguments = @('run', $collection, '--env-var', "baseUrl=$baseUrl", '--env-var', 'adminUsername=admin',
            '--env-var', "adminPassword=$env:CACAOSELVA_ADMIN_PASSWORD", '--reporters', 'cli,json',
            '--reporter-json-export', $report, '--timeout-request', '5000')
        if ($newman) {
            & $newman.Source @newmanArguments
        } else {
            $toolDirectory = Join-Path $projectRoot '.tools/newman'
            $localNewman = Join-Path $toolDirectory $(if ($IsWindows) { 'node_modules/.bin/newman.cmd' } else { 'node_modules/.bin/newman' })
            if (-not (Test-Path -LiteralPath $localNewman)) {
                & npm install --prefix $toolDirectory newman@6.2.1 --no-audit --no-fund
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar Newman en .tools/newman.' }
            }
            & $localNewman @newmanArguments
        }
        if ($LASTEXITCODE -ne 0) { throw 'Falló la colección Postman.' }
    }
    $body = @{ socioId=1; pesoKg=44.125; estado='PENDIENTE' } | ConvertTo-Json
    $lote = Invoke-RestMethod "$baseUrl/lotes" -Method Post -Headers $authHeaders -ContentType 'application/json' -Body $body
    $monitorProcess = Start-Jar 'monitor' 'monitor'
    Wait-Condition { (Read-MonitorLog) -match 'Pendientes: 21' } 'monitor conectado'
    Stop-Process -Id $apiProcess.Id
    $apiProcess.WaitForExit()
    $apiProcess = $null
    Wait-Condition { (Read-MonitorLog) -match 'Se reintentar' } 'detección de caída'
    if ($monitorProcess.HasExited) { throw 'El monitor se cerró tras el fallo.' }
    $apiProcess = Start-Jar 'api' 'api-restarted'
    Wait-Condition { Test-ApiReady } 'reinicio de API'
    $persisted = Invoke-RestMethod "$baseUrl/lotes/$($lote.id)" -Headers $authHeaders
    if ($persisted.socioId -ne 1 -or $persisted.pesoKg -ne 44.125) { throw 'No se conservó el lote.' }
    $afterRestart = Invoke-RestMethod "$baseUrl/lotes" -Headers $authHeaders
    if ($afterRestart.Count -ne 31) { throw "Se esperaban 31 registros tras reiniciar; llegaron $($afterRestart.Count)." }
    Write-Output 'PASS Persistencia tras reinicio; migraciones sin duplicados'
    Wait-Condition { (Read-MonitorLog) -match 'recuperada' } 'recuperación del monitor'
    Invoke-RestMethod "$baseUrl/lotes/$($lote.id)" -Method Delete -Headers $authHeaders | Out-Null
    Wait-Condition { (Read-MonitorLog) -match 'Pendientes: 20' } 'detección del cambio de pendientes'
    Write-Output 'PASS Monitor detecta caída, se recupera y observa el cambio 21 -> 20'
    Write-Output "Logs: $logDirectory"
} finally {
    foreach ($process in @($monitorProcess, $apiProcess)) {
        if ($null -ne $process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
            $process.WaitForExit()
        }
    }
    $env:CACAOSELVA_DB_URL = $oldUrl
    $env:CACAOSELVA_DB_USER = $oldUser
    $env:CACAOSELVA_DB_PASSWORD = $oldPassword
    $env:CACAOSELVA_API_TOKEN = $oldApiToken
}
