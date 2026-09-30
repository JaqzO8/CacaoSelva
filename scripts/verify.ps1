#Requires -Version 7.0
param([switch] $Gui, [switch] $Postman, [switch] $Web)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
$testName = 'cacaoselva_test_' + [Guid]::NewGuid().ToString('N')
$created = $false
$previousPassword = $env:PGPASSWORD
$previousTestUrl = $env:CACAOSELVA_TEST_DB_URL
$previousTestUser = $env:CACAOSELVA_TEST_DB_USER
$previousTestPassword = $env:CACAOSELVA_TEST_DB_PASSWORD
$previousJwtSecret = $env:CACAOSELVA_JWT_SECRET
$previousAdminPassword = $env:CACAOSELVA_ADMIN_PASSWORD
try {
    & (Join-Path $PSScriptRoot 'database.ps1') start
    $settings = Get-Content -LiteralPath 'data/database.json' -Raw | ConvertFrom-Json
    $psql = Join-Path $settings.bin 'psql'
    $connection = @('-h', '127.0.0.1', '-p', $settings.port, '-U', $settings.adminUser, '-d', 'postgres', '-v', 'ON_ERROR_STOP=1')
    $env:PGPASSWORD = $settings.adminPassword
    "CREATE DATABASE $testName OWNER cacaoselva;" | & $psql @connection
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base temporal de pruebas.' }
    $created = $true
    $env:CACAOSELVA_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$($settings.port)/$testName"
    $env:CACAOSELVA_TEST_DB_USER = $settings.user
    $env:CACAOSELVA_TEST_DB_PASSWORD = $settings.password
    $randomBytes = [byte[]]::new(48)
    [Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
    $env:CACAOSELVA_JWT_SECRET = [Convert]::ToHexString($randomBytes).ToLowerInvariant()
    $adminBytes = [byte[]]::new(24)
    [Security.Cryptography.RandomNumberGenerator]::Fill($adminBytes)
    $env:CACAOSELVA_ADMIN_PASSWORD = [Convert]::ToHexString($adminBytes).ToLowerInvariant()
    $arguments = @('clean', 'install', '-Pintegration', '-B', '-ntp')
    if ($Gui) { $arguments += '-Dcacaoselva.test.javafx=true' }
    if ($IsWindows) { & '.\mvnw.cmd' @arguments } else { & sh './mvnw' @arguments }
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o las pruebas Maven.' }
    & (Join-Path $PSScriptRoot 'smoke-test.ps1') -RunPostmanCollection:$Postman -RunWebTests:$Web
    Write-Output 'VALIDACIÓN COMPLETA: unitarias, PostgreSQL, CRUD, persistencia y recuperación correctos.'
} finally {
    if ($created -and $testName -match '^cacaoselva_test_[a-f0-9]{32}$') {
        $env:PGPASSWORD = $settings.adminPassword
        "DROP DATABASE $testName WITH (FORCE);" | & $psql @connection
        if ($LASTEXITCODE -ne 0) { Write-Warning "No se pudo limpiar la base de pruebas $testName." }
    }
    $env:PGPASSWORD = $previousPassword
    $env:CACAOSELVA_TEST_DB_URL = $previousTestUrl
    $env:CACAOSELVA_TEST_DB_USER = $previousTestUser
    $env:CACAOSELVA_TEST_DB_PASSWORD = $previousTestPassword
    $env:CACAOSELVA_JWT_SECRET = $previousJwtSecret
    $env:CACAOSELVA_ADMIN_PASSWORD = $previousAdminPassword
    Pop-Location
}
