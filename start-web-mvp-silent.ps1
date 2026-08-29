param(
    [switch]$Hidden
)

$ErrorActionPreference = 'Stop'

# A directly launched .ps1 inherits a visible PowerShell window. Relaunch once
# with a hidden host so direct execution and desktop-launcher execution behave alike.
if (-not $Hidden) {
    $powerShellExecutable = (Get-Process -Id $PID).Path
    Start-Process -FilePath $powerShellExecutable `
        -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-WindowStyle', 'Hidden', '-File', "`"$PSCommandPath`"", '-Hidden' `
        -WindowStyle Hidden
    exit 0
}

$projectCandidates = @(
    $PSScriptRoot
    'D:\code\gk-job-helper'
    'D:\studyCode\gk-job-helper'
) | Select-Object -Unique

$projectDir = $projectCandidates |
    Where-Object {
        (Test-Path (Join-Path $_ 'pom.xml')) -and
        (Test-Path (Join-Path $_ 'gk-job-helper-web\package.json'))
    } |
    Select-Object -First 1

if (-not $projectDir) {
    throw "Project directory was not found. Checked: $($projectCandidates -join ', ')"
}

$webDir = Join-Path $projectDir 'gk-job-helper-web'
$logDir = Join-Path $projectDir 'runtime-logs'

# Keep desktop/Explorer launches working before the updated user PATH is refreshed.
$nodeDir = 'D:\Apps\nodejs'
if ((Test-Path (Join-Path $nodeDir 'node.exe')) -and (Test-Path (Join-Path $nodeDir 'npm.cmd'))) {
    $env:Path = "$nodeDir;$env:Path"
}

function Test-LocalPort([int]$Port) {
    return (Test-NetConnection -ComputerName '127.0.0.1' -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue)
}

function Test-HttpEndpoint([string]$Uri) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 2
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

if (-not (Test-Path (Join-Path $webDir 'package.json'))) {
    throw "Frontend directory was not found: $webDir"
}

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

if (-not (Test-LocalPort 8080)) {
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvn spring-boot:run' -WorkingDirectory $projectDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'backend.log') -RedirectStandardError (Join-Path $logDir 'backend-error.log')
}

if (-not (Test-LocalPort 5173)) {
    $frontendCommand = if (Test-Path (Join-Path $webDir 'node_modules')) {
        'npm run dev -- --host 127.0.0.1'
    } else {
        'npm install && npm run dev -- --host 127.0.0.1'
    }
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $frontendCommand -WorkingDirectory $webDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'frontend.log') -RedirectStandardError (Join-Path $logDir 'frontend-error.log')
}

for ($attempt = 0; $attempt -lt 90; $attempt++) {
    $backendReady = Test-HttpEndpoint 'http://127.0.0.1:8080/api/profile'
    $frontendReady = Test-HttpEndpoint 'http://127.0.0.1:5173'
    if ($backendReady -and $frontendReady) {
        Start-Process 'http://127.0.0.1:5173'
        exit 0
    }
    Start-Sleep -Seconds 1
}

if (-not $backendReady) {
    throw 'Backend did not become ready within 90 seconds. Check runtime-logs\\backend.log and backend-error.log.'
}

throw 'Frontend did not become ready within 90 seconds. Check runtime-logs\\frontend.log and frontend-error.log.'
