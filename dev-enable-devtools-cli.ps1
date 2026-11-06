<#
.SYNOPSIS
    One-off helper: enable the WeChat DevTools CLI service port.

.DESCRIPTION
    DevTools keeps this switch inside
        %LOCALAPPDATA%\<devtools>\User Data\<hash>\WeappLocalData\localstorage_*.json
    as  "security":{"enableServicePort":false,"port":null,"allowGetTicket":false}

    With the switch off, dev-start.ps1 can only start the databases / API / web consoles,
    it cannot open the mini program windows. This script flips the switch once:

        1. closes DevTools (it would overwrite the file on exit otherwise)
        2. backs up the local storage file
        3. sets enableServicePort = true and port = 9420
        4. restarts DevTools and waits until the port is listening

    Run it only when you are ready to lose the current DevTools window state.

.PARAMETER Force
    Skip the confirmation prompt.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\dev-enable-devtools-cli.ps1
#>
[CmdletBinding()]
param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$ServicePort = 9420

function Test-Port {
    param([int]$Port)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $null = $client.Connect('127.0.0.1', $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Find-DevTools {
    $parents = @('D:\software', 'C:\Program Files (x86)\Tencent', 'C:\Program Files\Tencent', "$env:LOCALAPPDATA\Programs")
    foreach ($dir in $parents) {
        if (-not (Test-Path $dir)) { continue }
        $hit = Get-ChildItem $dir -Directory -ErrorAction SilentlyContinue |
            ForEach-Object { Join-Path $_.FullName 'wechatdevtools.exe' } |
            Where-Object { Test-Path $_ } |
            Select-Object -First 1
        if ($hit) { return $hit }
    }
    return $null
}

function Get-LocalStorageFiles {
    $files = @()
    foreach ($base in (Get-ChildItem $env:LOCALAPPDATA -Directory -ErrorAction SilentlyContinue)) {
        $userData = Join-Path $base.FullName 'User Data'
        if (-not (Test-Path $userData)) { continue }
        foreach ($profile in (Get-ChildItem $userData -Directory -ErrorAction SilentlyContinue)) {
            $dataDir = Join-Path $profile.FullName 'WeappLocalData'
            if (-not (Test-Path $dataDir)) { continue }
            $files += Get-ChildItem $dataDir -File -Filter '*.json' -ErrorAction SilentlyContinue |
                Where-Object { (Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue) -match 'enableServicePort' }
        }
    }
    return $files
}

Write-Host '==> WeChat DevTools CLI service port' -ForegroundColor Cyan

if (Test-Port $ServicePort) {
    Write-Host "    Already enabled: port $ServicePort is listening. Nothing to do." -ForegroundColor Green
    exit 0
}

$devtools = Find-DevTools
if (-not $devtools) {
    Write-Host '    DevTools executable not found, cannot continue.' -ForegroundColor Red
    exit 1
}

$storageFiles = @(Get-LocalStorageFiles)
if ($storageFiles.Count -eq 0) {
    Write-Host '    Could not locate the DevTools local storage file; enable the port manually:' -ForegroundColor Yellow
    Write-Host '    DevTools -> 设置 -> 安全设置 -> 服务端口' -ForegroundColor Yellow
    exit 1
}

Write-Host "    target file: $($storageFiles[0].FullName)" -ForegroundColor DarkGray
if (-not $Force) {
    Write-Host '    DevTools will be closed and restarted. Continue? [y/N] ' -NoNewline -ForegroundColor Yellow
    $answer = Read-Host
    if ($answer -notmatch '^(y|yes)$') {
        Write-Host '    Cancelled.' -ForegroundColor DarkGray
        exit 0
    }
}

$running = @(Get-Process -Name wechatdevtools -ErrorAction SilentlyContinue)
if ($running.Count -gt 0) {
    Write-Host '    closing DevTools...' -ForegroundColor DarkGray
    $running | Stop-Process -Force
    Start-Sleep -Seconds 5
}

foreach ($file in $storageFiles) {
    $backup = "$($file.FullName).bak"
    if (-not (Test-Path $backup)) { Copy-Item $file.FullName $backup }
    $content = Get-Content $file.FullName -Raw
    $updated = $content -replace '"enableServicePort"\s*:\s*false', '"enableServicePort":true'
    $updated = $updated -replace '("enableServicePort"\s*:\s*true\s*,\s*"port"\s*:\s*)null', "`${1}$ServicePort"
    if ($updated -ne $content) {
        Set-Content -Path $file.FullName -Value $updated -Encoding UTF8 -NoNewline
        Write-Host "    updated $($file.Name)" -ForegroundColor Green
    } else {
        Write-Host "    no change needed in $($file.Name)" -ForegroundColor DarkGray
    }
}

Write-Host '    starting DevTools...' -ForegroundColor DarkGray
Start-Process -FilePath $devtools

for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Port $ServicePort) {
        Write-Host "    OK: service port $ServicePort is now listening." -ForegroundColor Green
        Write-Host '    dev-start.ps1 can open both mini programs from now on.' -ForegroundColor Green
        exit 0
    }
}

Write-Host '    Port did not open. Open DevTools -> 设置 -> 安全设置 -> 服务端口 and enable it manually.' -ForegroundColor Yellow
exit 1
