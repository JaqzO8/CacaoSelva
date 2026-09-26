#Requires -Version 7.0
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $envFile = Join-Path $projectRoot '.env.local'
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw 'No existe .env.local. Ejecuta primero: pwsh -NoProfile -File scripts/database.ps1 start'
    }
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*#' -or $line -notmatch '^([^=]+)=(.*)$') { continue }
        $key = $matches[1].Trim()
        $value = $matches[2]
        if ($key -like 'CACAOSELVA_*') { Set-Item -Path ("Env:" + $key) -Value $value }
    }
    if ([string]::IsNullOrWhiteSpace($env:CACAOSELVA_API_TOKEN)) {
        if ([string]::IsNullOrWhiteSpace($env:CACAOSELVA_API_USERNAME)) { $env:CACAOSELVA_API_USERNAME = 'admin' }
        if ([string]::IsNullOrWhiteSpace($env:CACAOSELVA_API_PASSWORD)) {
            $env:CACAOSELVA_API_PASSWORD = $env:CACAOSELVA_ADMIN_PASSWORD
        }
    }
    if ([string]::IsNullOrWhiteSpace($env:CACAOSELVA_API_TOKEN) -and
        [string]::IsNullOrWhiteSpace($env:CACAOSELVA_API_PASSWORD)) {
        throw 'Configura una credencial API de lectura o CACAOSELVA_ADMIN_PASSWORD en .env.local.'
    }
    $java = (Get-Command java -ErrorAction Stop).Source
    $jar = Join-Path $projectRoot 'monitor/target/monitor-1.0.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'No existe el JAR. Compila primero con .\mvnw.cmd clean package.' }
    & $java -jar $jar
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
