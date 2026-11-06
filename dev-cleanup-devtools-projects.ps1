<#
.SYNOPSIS
    Remove duplicate mini program project cards from WeChat DevTools.

.DESCRIPTION
    DevTools keys every project by its literal path string, so the same folder shows up as
    two projects when the letter case of the drive differs:

        project2_D:\github\DaiGo\miniprogram-user    <- kept
        project2_d:\github\DaiGo\miniprogram-user    <- duplicate, removed

    This script closes DevTools (it rewrites these files on exit), backs up the storage,
    drops the duplicate entries from reduxPersist:projectList / last_all_projects, deletes the
    orphaned per-project caches, then restarts DevTools.

    Nothing inside your project folders is touched.

.PARAMETER Force
    Do not ask for confirmation before closing DevTools.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\dev-cleanup-devtools-projects.ps1
#>
[CmdletBinding()]
param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

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

function Save-TextNoBom {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}

function Get-DataDir {
    foreach ($base in (Get-ChildItem $env:LOCALAPPDATA -Directory -ErrorAction SilentlyContinue)) {
        $userData = Join-Path $base.FullName 'User Data'
        if (-not (Test-Path $userData)) { continue }
        foreach ($profile in (Get-ChildItem $userData -Directory -ErrorAction SilentlyContinue)) {
            $dir = Join-Path $profile.FullName 'WeappLocalData'
            if (-not (Test-Path (Join-Path $dir 'hash_key_map_2.json'))) { continue }
            return $dir
        }
    }
    return $null
}

Write-Host '==> WeChat DevTools project list' -ForegroundColor Cyan

$dataDir = Get-DataDir
if (-not $dataDir) {
    Write-Host '    Could not find the DevTools storage directory.' -ForegroundColor Red
    exit 1
}
Write-Host "    storage: $dataDir" -ForegroundColor DarkGray

$hashMapFile = Join-Path $dataDir 'hash_key_map_2.json'
$hashMap = Get-Content $hashMapFile -Raw -Encoding UTF8 | ConvertFrom-Json
$projectListKey = ($hashMap.PSObject.Properties | Where-Object { $_.Value -eq 'reduxPersist:projectList' } | Select-Object -First 1).Name
$lastAllKey     = ($hashMap.PSObject.Properties | Where-Object { $_.Value -eq 'last_all_projects' } | Select-Object -First 1).Name
if (-not $projectListKey) {
    Write-Host '    project list entry not found in hash_key_map_2.json.' -ForegroundColor Red
    exit 1
}

$projectListFile = Join-Path $dataDir ("localstorage_$projectListKey.json")
$lastAllFile     = if ($lastAllKey) { Join-Path $dataDir ("localstorage_$lastAllKey.json") } else { $null }

# Parse by text instead of ConvertFrom-Json: PowerShell compares JSON keys case-insensitively
# and would reject this file outright, because "D:\..." and "d:\..." collide.
$projectListText = Get-Content $projectListFile -Raw -Encoding UTF8
$entries = @()
foreach ($m in [regex]::Matches($projectListText, '"([A-Za-z]:[\\/]{1,2}[^"]*)":\{([^{}]*)\}')) {
    $literal = $m.Groups[1].Value
    $body = $m.Groups[2].Value
    $entries += [pscustomobject]@{
        Literal  = $literal
        Path     = $literal.Replace('\\', '\')
        HasCover = ($body -match '"appImageUrl"\s*:\s*"http')
        RealAppId = ($body -match '"appId"\s*:\s*"(?!touristappid)[^"]+"')
    }
}

Write-Host "    current entries: $($entries.Count)" -ForegroundColor DarkGray
foreach ($e in $entries) { Write-Host "      - $($e.Path)" -ForegroundColor DarkGray }

# Group by path, ignoring letter case; keep the richest record of each folder.
$duplicates = @()
$duplicateLiterals = @()
$keepMap = @{}
$buckets = @{}
foreach ($entry in $entries) {
    $bucketKey = $entry.Path.ToLower()
    if ($buckets.ContainsKey($bucketKey)) {
        $buckets[$bucketKey] = @($buckets[$bucketKey]) + $entry
    } else {
        $buckets[$bucketKey] = @($entry)
    }
}
foreach ($bucketKey in @($buckets.Keys)) {
    $bucket = @($buckets[$bucketKey])
    $keep = $bucket |
        Sort-Object @{ Expression = { if ($_.HasCover) { 0 } else { 1 } } }, @{ Expression = { if ($_.RealAppId) { 0 } else { 1 } } } |
        Select-Object -First 1
    $keepMap[$bucketKey] = $keep.Path
    if ($bucket.Count -gt 1) {
        Write-Host "      keep: $($keep.Path)" -ForegroundColor DarkGray
    }
    # -cne is required: PowerShell's -ne ignores letter case and would treat both as equal
    foreach ($extra in ($bucket | Where-Object { $_.Literal -cne $keep.Literal })) {
        $duplicates += $extra.Path
        $duplicateLiterals += $extra.Literal
    }
}

if ($duplicates.Count -eq 0) {
    Write-Host '    No duplicates found. Nothing to do.' -ForegroundColor Green
    exit 0
}

Write-Host '    will remove:' -ForegroundColor Yellow
foreach ($dup in $duplicates) { Write-Host "      - $dup" -ForegroundColor Yellow }

if (-not $Force) {
    Write-Host '    DevTools must be closed and will be restarted. Continue? [y/N] ' -NoNewline -ForegroundColor Yellow
    if ((Read-Host) -notmatch '^(y|yes)$') {
        Write-Host '    Cancelled.' -ForegroundColor DarkGray
        exit 0
    }
}

$devtools = Find-DevTools
$running = @(Get-Process -Name wechatdevtools -ErrorAction SilentlyContinue)
if ($running.Count -gt 0) {
    Write-Host '    closing DevTools...' -ForegroundColor DarkGray
    $running | Stop-Process -Force
    Start-Sleep -Seconds 5
}

# Backup once (never overwrite an existing backup)
foreach ($f in @($projectListFile, $lastAllFile, $hashMapFile)) {
    if ($f -and (Test-Path $f) -and -not (Test-Path "$f.bak")) {
        Copy-Item $f "$f.bak"
    }
}

# 1) rebuild the list from the surviving entries.
# Never do regex surgery on the commas: -replace is case-insensitive and would eat
# the sibling entry that only differs by drive letter case.
$keptItems = @()
foreach ($m in [regex]::Matches($projectListText, '"([A-Za-z]:[\\/]{1,2}[^"]*)":\{[^{}]*\}')) {
    if ($duplicateLiterals -cnotcontains $m.Groups[1].Value) { $keptItems += $m.Value }
}
$expected = $entries.Count - $duplicateLiterals.Count
if ($keptItems.Count -ne $expected) {
    Write-Host "    ABORT: kept $($keptItems.Count) entries, expected $expected. Files untouched." -ForegroundColor Red
    if ($devtools) { Start-Process -FilePath $devtools }
    exit 1
}
Save-TextNoBom -Path $projectListFile -Content ('{' + ($keptItems -join ',') + '}')
Write-Host "    projectList: $($entries.Count) -> $($keptItems.Count)" -ForegroundColor Green

# 2) normalise last_all_projects
if ($lastAllFile -and (Test-Path $lastAllFile)) {
    $parsed = Get-Content $lastAllFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $lastAll = if ($parsed -is [System.Array]) { @($parsed) } else { @(, $parsed) }
    $fixed = @()
    $seen = @{}
    foreach ($item in $lastAll) {
        $key = $item.projectid.ToLower()
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        if ($keepMap.ContainsKey($key)) { $item.projectid = $keepMap[$key] }
        $fixed += $item
    }
    Save-TextNoBom -Path $lastAllFile -Content ($fixed | ConvertTo-Json -Depth 6 -Compress)
    Write-Host "    last_all_projects: $($lastAll.Count) -> $($fixed.Count)" -ForegroundColor Green
}

# 3) remove orphaned per-project caches (project2_<path>, toolbar_<path>)
$orphanKeys = @()
foreach ($prop in @($hashMap.PSObject.Properties)) {
    foreach ($dup in $duplicates) {
        # -ceq: -eq ignores letter case and would also delete the surviving project's cache
        if ($prop.Value -ceq "project2_$dup" -or $prop.Value -ceq "toolbar_$dup") { $orphanKeys += $prop.Name }
    }
}
foreach ($key in $orphanKeys) {
    $orphanFile = Join-Path $dataDir ("localstorage_$key.json")
    if (Test-Path $orphanFile) {
        if (-not (Test-Path "$orphanFile.bak")) { Copy-Item $orphanFile "$orphanFile.bak" }
        Remove-Item $orphanFile -Force
    }
    $hashMap.PSObject.Properties.Remove($key)
}
if ($orphanKeys.Count -gt 0) {
    Save-TextNoBom -Path $hashMapFile -Content ($hashMap | ConvertTo-Json -Depth 6 -Compress)
}
Write-Host "    removed $($orphanKeys.Count) orphaned cache file(s)" -ForegroundColor Green

if ($devtools) {
    Write-Host '    starting DevTools...' -ForegroundColor DarkGray
    Start-Process -FilePath $devtools
}

Write-Host '    Done. Open the two projects from the DevTools start page.' -ForegroundColor Green
Write-Host '    Backups were written next to the originals as *.bak.' -ForegroundColor DarkGray
exit 0
