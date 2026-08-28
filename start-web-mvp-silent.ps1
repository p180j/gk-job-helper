$ErrorActionPreference = 'Stop'

$projectDir = 'D:\studyCode\gk-job-helper'
$webDir = Join-Path $projectDir 'gk-job-helper-web'
$logDir = Join-Path $projectDir 'runtime-logs'

function Test-LocalPort([int]$Port) {
    return (Test-NetConnection -ComputerName '127.0.0.1' -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue)
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

for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if (Test-LocalPort 5173) {
        Start-Process 'http://127.0.0.1:5173'
        exit 0
    }
    Start-Sleep -Seconds 1
}

throw 'Frontend did not start within 30 seconds. Check runtime-logs\\frontend-error.log.'
