[CmdletBinding()]
param(
    [ValidateSet('doctor', 'test', 'verify', 'build', 'run', 'db-up', 'db-stop')]
    [string]$Action = 'doctor'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    # Select an installed JDK 21 without relying on the Oracle javapath launcher.
    $candidates = @(
        $env:JAVA_HOME,
        'C:\Program Files\Java\jdk-21'
    )
    foreach ($base in @('C:\Program Files\Eclipse Adoptium', (Join-Path $env:USERPROFILE '.jdks'))) {
        if (Test-Path -LiteralPath $base) {
            $candidates += Get-ChildItem -LiteralPath $base -Directory |
                Sort-Object Name -Descending | ForEach-Object FullName
        }
    }
    $selectedJdk = $null
    foreach ($candidate in $candidates) {
        if (!$candidate) { continue }
        $releaseFile = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath $releaseFile) -and
            (Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe')) -and
            (Select-String -LiteralPath $releaseFile -Pattern '^JAVA_VERSION="21[."]' -Quiet)) {
            $selectedJdk = $candidate
            break
        }
    }
    if (!$selectedJdk) { throw 'JDK 21 was not found. Install JDK 21 and set JAVA_HOME to its directory.' }
    $env:JAVA_HOME = $selectedJdk
    $env:PATH = "$selectedJdk\bin;$env:PATH"

    function Invoke-Native {
        param([string]$Executable, [string[]]$Arguments)
        & $Executable @Arguments
        if ($LASTEXITCODE -ne 0) { throw "$Executable failed (exit $LASTEXITCODE)." }
    }

    function Assert-Docker {
        & docker info --format '{{.ServerVersion}}'
        if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop with the Linux containers engine, then retry.' }
    }

    switch ($Action) {
        'doctor' {
            Invoke-Native java @('-version')
            Invoke-Native javac @('-version')
            Invoke-Native '.\mvnw.cmd' @('-version')
            Invoke-Native git @('--version')
            Invoke-Native docker @('compose', 'version')
            Assert-Docker
            if (!(Test-Path -LiteralPath '.env')) {
                Write-Warning 'Local .env is missing. Copy .env.example to .env and set a local password.'
            }
        }
        'test' { Invoke-Native '.\mvnw.cmd' @('-s', '.mvn/settings.xml', '-B', '-ntp', 'test') }
        'verify' {
            Assert-Docker
            Invoke-Native '.\mvnw.cmd' @('-s', '.mvn/settings.xml', '-B', '-ntp', 'clean', 'verify')
        }
        'build' { Invoke-Native '.\mvnw.cmd' @('-s', '.mvn/settings.xml', '-B', '-ntp', 'package', '-DskipTests') }
        'run' {
            Invoke-Native '.\mvnw.cmd' @('-s', '.mvn/settings.xml', '-B', '-ntp', 'package', '-DskipTests')
            Invoke-Native java @('-jar', 'fitora-bootstrap/target/fitora-bootstrap-0.0.1-SNAPSHOT.jar',
                '--spring.profiles.active=local')
        }
        'db-up' {
            Assert-Docker
            if (!(Test-Path -LiteralPath '.env')) { throw 'Copy .env.example to .env and set a local password first.' }
            Invoke-Native docker @('compose', 'up', '-d', '--wait')
        }
        'db-stop' { Invoke-Native docker @('compose', 'stop') }
    }
} finally {
    Pop-Location
}
