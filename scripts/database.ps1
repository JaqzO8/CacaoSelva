#Requires -Version 7.0
param(
    [ValidateSet('init', 'start', 'stop', 'status')][string] $Action = 'start',
    [int] $Port = 55432
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$dataRoot = Join-Path $projectRoot 'data'
$cluster = Join-Path $dataRoot 'postgres'
$settingsPath = Join-Path $dataRoot 'database.json'

function Find-PgBin {
    $command = Get-Command pg_ctl -ErrorAction SilentlyContinue
    if ($command) { return Split-Path -Parent $command.Source }
    if ($IsWindows) {
        $candidate = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue |
            Sort-Object { [int]$_.Name } -Descending | Select-Object -First 1
        if ($candidate) { return Join-Path $candidate.FullName 'bin' }
    } else {
        $pgConfig = Get-Command pg_config -ErrorAction SilentlyContinue
        if ($pgConfig) { return (& $pgConfig.Source --bindir).Trim() }
    }
    throw 'Instala PostgreSQL 17 o superior y añade su carpeta bin a PATH.'
}

function Start-Cluster($settings) {
    & (Join-Path $settings.bin 'pg_ctl') -D $cluster status *> $null
    if ($LASTEXITCODE -eq 0) { return }
    $arguments = @('-D', ('"{0}"' -f $cluster), '-l', ('"{0}"' -f (Join-Path $dataRoot 'postgres.log')),
        '-o', ('"-p {0} -h 127.0.0.1"' -f $settings.port), '-w', 'start')
    $options = @{ FilePath=(Join-Path $settings.bin 'pg_ctl'); ArgumentList=$arguments; PassThru=$true }
    if ($IsWindows) { $options.WindowStyle = 'Hidden' }
    $process = Start-Process @options
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        Get-Content -LiteralPath (Join-Path $dataRoot 'postgres.log') -Tail 20 -ErrorAction SilentlyContinue |
            Write-Output
        throw 'No se pudo iniciar PostgreSQL. Revisa data/postgres.log.'
    }
}

if (Test-Path -LiteralPath $settingsPath) {
    $settings = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
} elseif ($Action -in @('init', 'start')) {
    if (Test-Path -LiteralPath (Join-Path $projectRoot '.env.local')) {
        throw 'Ya existe .env.local. No se sobrescribirá; revisa la configuración antes de crear otra instancia.'
    }
    if (Test-Path -LiteralPath $cluster) { throw 'Ya existe el directorio PostgreSQL sin configuración. Revisa data/.' }
    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
    try { $probe.Start() } finally { $probe.Stop() }
    New-Item -ItemType Directory -Path $dataRoot -Force | Out-Null
    $settings = [pscustomobject]@{
        bin = Find-PgBin
        port = $Port
        database = 'cacaoselva'
        user = 'cacaoselva'
        password = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)).ToLowerInvariant()
        adminUser = 'cacaoselva_admin'
        adminPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)).ToLowerInvariant()
    }
    $passwordFile = Join-Path $dataRoot 'init-password.tmp'
    try {
        Set-Content -LiteralPath $passwordFile -Value $settings.adminPassword -Encoding utf8NoBOM
        & (Join-Path $settings.bin 'initdb') -D $cluster --username=$($settings.adminUser) `
            --pwfile=$passwordFile --auth=scram-sha-256 --encoding=UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'Falló initdb.' }
        # Todos los clientes usan TCP local. Evita requerir permisos sobre /var/run/postgresql en Linux.
        Add-Content -LiteralPath (Join-Path $cluster 'postgresql.conf') `
            -Value "unix_socket_directories = ''" -Encoding utf8NoBOM
    } finally {
        if (Test-Path -LiteralPath $passwordFile) { Remove-Item -LiteralPath $passwordFile }
    }
    $settings | ConvertTo-Json | Set-Content -LiteralPath $settingsPath -Encoding utf8NoBOM
    Start-Cluster $settings
} else {
    throw 'No existe una instancia del proyecto. Ejecuta database.ps1 init.'
}

if ($Action -in @('init', 'start') -and -not (Test-Path -LiteralPath (Join-Path $projectRoot '.env.local'))) {
    Start-Cluster $settings
    $previousPassword = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $settings.adminPassword
        $psql = Join-Path $settings.bin 'psql'
        $connection = @('-h', '127.0.0.1', '-p', $settings.port, '-U', $settings.adminUser, '-d', 'postgres', '-v', 'ON_ERROR_STOP=1')
        $roleExists = & $psql @connection -tAc "SELECT 1 FROM pg_roles WHERE rolname='cacaoselva'"
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar a PostgreSQL.' }
        if (-not $roleExists) {
            "CREATE ROLE cacaoselva LOGIN PASSWORD '$($settings.password)';" | & $psql @connection
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el usuario.' }
        }
        $databaseExists = & $psql @connection -tAc "SELECT 1 FROM pg_database WHERE datname='cacaoselva'"
        if (-not $databaseExists) {
            'CREATE DATABASE cacaoselva OWNER cacaoselva;' | & $psql @connection
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base.' }
        }
    } finally { $env:PGPASSWORD = $previousPassword }
    @(
        "CACAOSELVA_DB_URL=jdbc:postgresql://127.0.0.1:$($settings.port)/cacaoselva",
        'CACAOSELVA_DB_USER=cacaoselva',
        "CACAOSELVA_DB_PASSWORD=$($settings.password)"
    ) | Set-Content -LiteralPath (Join-Path $projectRoot '.env.local') -Encoding utf8NoBOM
}

switch ($Action) {
    { $_ -in @('init', 'start') } {
        Start-Cluster $settings
        Write-Output "PostgreSQL activo en 127.0.0.1:$($settings.port); base: cacaoselva; usuario: cacaoselva."
        Write-Output 'Credenciales: .env.local. Las migraciones y los 30 lotes se cargan al iniciar la API.'
    }
    'stop' {
        & (Join-Path $settings.bin 'pg_ctl') -D $cluster -m fast -w stop
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener la instancia.' }
    }
    'status' {
        & (Join-Path $settings.bin 'pg_ctl') -D $cluster status
        if ($LASTEXITCODE -ne 0) { throw 'La instancia del proyecto está detenida.' }
    }
}
