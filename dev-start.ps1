<#
.SYNOPSIS
    DaiGo local development bootstrap.

.DESCRIPTION
    Starts everything the mini programs need, in dependency order:
        MySQL (3306) -> Redis (6379) -> Spring Boot API (8080) -> WeChat DevTools project windows
    Optionally also starts the two web consoles (merchant-web 5174 / admin-web 5173).

    The script is idempotent: every port is probed first, so running it twice does nothing harmful.
    Background processes are created through WMI (Win32_Process.Create) instead of Start-Process,
    so they are detached from the caller's job object and keep running after this window / IDE terminal closes.

.PARAMETER WithWeb
    Also start the Vite dev servers for merchant-web and admin-web.

.PARAMETER Rebuild
    Force "mvn package" even when the jar is newer than the sources.

.PARAMETER SkipDevTools
    Do not touch WeChat DevTools (useful for CI or when it is already open).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\dev-start.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\dev-start.ps1 -WithWeb -Rebuild
#>
[CmdletBinding()]
param(
    [switch]$WithWeb,
    [switch]$Rebuild,
    [switch]$SkipDevTools
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
if (-not $Root) { $Root = (Get-Location).Path }
$Root = $Root.TrimEnd('\')

# WeChat DevTools uses the raw path string as the project key, so "D:\..." and "d:\..."
# are stored as TWO separate projects (duplicate cards, split caches).
# Never rewrite the letter case here; instead reuse whatever casing DevTools already
# recorded for this folder - see Get-RecordedProjectPath below.

# ----------------------------------------------------------------------------
# Configurable paths (edit here when the local toolchain moves)
# ----------------------------------------------------------------------------
$MySqlHome = 'D:\software\mysql-8.0.29-winx64'
$MySqlExe  = Join-Path $MySqlHome 'bin\mysqld.exe'
$MySqlIni  = Join-Path $MySqlHome 'my.ini'
$RedisHome = 'D:\software\Redis'
$RedisExe  = Join-Path $RedisHome 'redis-server.exe'
$JavaExe   = 'D:\software\jdk-17.0.20.1+1\bin\java.exe'
$MavenCmd  = 'D:\tools\apache-maven-3.9.6\bin\mvn.cmd'

$MySqlPort    = 3306
$RedisPort    = 6379
$ApiPort      = 8080
# WeChat DevTools CLI service port: the real value lives in User Data\<hash>\Default\.cli,
# so it is discovered at runtime instead of hard-coded here.
$DevToolsDefaultPort = 9420

$MiniPrograms = @('miniprogram-user', 'miniprogram-merchant')
$WebConsoles  = @(
    [pscustomobject]@{ Name = 'merchant-web'; Port = 5174 },
    [pscustomobject]@{ Name = 'admin-web';    Port = 5173 }
)

# ----------------------------------------------------------------------------
# Session log: one line per run, so you can tell afterwards whether the IDE
# auto-start task actually fired and what it did.
# ----------------------------------------------------------------------------
$LogDir = Join-Path $Root 'logs'
if (-not (Test-Path $LogDir)) { $null = New-Item -ItemType Directory -Path $LogDir }
$LogFile = Join-Path $LogDir 'dev-start.log'

function Write-Log {
    param([string]$Text)
    try {
        Add-Content -Path $LogFile -Value ('{0}  {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Text) -Encoding UTF8
    } catch { }
}

# ----------------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------------
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

function Wait-Port {
    param([int]$Port, [int]$TimeoutSec, [string]$Name)
    for ($i = 0; $i -lt $TimeoutSec; $i++) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Seconds 1
    }
    throw "$Name did not open port $Port within $TimeoutSec seconds"
}

# Some dev servers (Vite) bind the port first and then exit when another instance already owns it.
# Only report success when the port keeps accepting connections for a few seconds.
function Wait-PortStable {
    param([int]$Port, [int]$TimeoutSec, [int]$StableSec = 3)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) {
            $stable = $true
            for ($j = 0; $j -lt $StableSec; $j++) {
                Start-Sleep -Seconds 1
                if (-not (Test-Port $Port)) { $stable = $false; break }
            }
            if ($stable) { return $true }
        } else {
            Start-Sleep -Seconds 1
        }
    }
    return $false
}

function Start-Detached {
    param(
        [string]$FilePath,
        [string]$Arguments = '',
        [string]$WorkDir
    )
    if (-not (Test-Path $FilePath)) { throw "Executable not found: $FilePath" }
    $cmd = if ($Arguments) { '"{0}" {1}' -f $FilePath, $Arguments } else { '"{0}"' -f $FilePath }
    $callArgs = @{ CommandLine = $cmd }
    if ($WorkDir) { $callArgs['CurrentDirectory'] = $WorkDir }
    $res = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments $callArgs
    if ($res.ReturnValue -ne 0) {
        throw "Cannot start $FilePath (Win32 return code $($res.ReturnValue))"
    }
    return [int]$res.ProcessId
}

function Write-Step  { param([string]$Text) Write-Host "==> $Text" -ForegroundColor Cyan; Write-Log "==> $Text" }
function Write-Ok    { param([string]$Text) Write-Host "    OK   $Text" -ForegroundColor Green; Write-Log "    OK   $Text" }
function Write-Skip  { param([string]$Text) Write-Host "    SKIP $Text (already running)" -ForegroundColor DarkGray; Write-Log "    SKIP $Text (already running)" }
function Write-Warn2 { param([string]$Text) Write-Host "    WARN $Text" -ForegroundColor Yellow; Write-Log "    WARN $Text" }

# DevTools stores each opened project under its literal path string in projectList, e.g.
#   {"D:\\github\\DaiGo\\miniprogram-user":{"projectPath":"D:\\github\\DaiGo\\miniprogram-user",...}}
# Opening the same folder with another letter case creates a second entry (that is where the
# duplicate cards come from). Always hand DevTools back the casing it already knows.
function Get-RecordedProjectPath {
    param([string]$Path)
    $normalized = $Path.TrimEnd('\')
    $candidate = $null
    foreach ($base in (Get-ChildItem $env:LOCALAPPDATA -Directory -ErrorAction SilentlyContinue)) {
        $userData = Join-Path $base.FullName 'User Data'
        if (-not (Test-Path $userData)) { continue }
        foreach ($profile in (Get-ChildItem $userData -Directory -ErrorAction SilentlyContinue)) {
            $dataDir = Join-Path $profile.FullName 'WeappLocalData'
            if (-not (Test-Path $dataDir)) { continue }
            foreach ($file in (Get-ChildItem $dataDir -File -Filter 'localstorage_*.json' -ErrorAction SilentlyContinue)) {
                $raw = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
                if (-not $raw) { continue }
                if ($raw -notmatch 'projectPath') { continue }
                foreach ($m in [regex]::Matches($raw, '"projectPath"\s*:\s*"([^"]+)"')) {
                    $known = $m.Groups[1].Value.Replace('\\', '\')
                    if ($known -ieq $normalized) {
                        # Prefer the casing that has an appId / cover image attached
                        if ($null -eq $candidate -or $raw -match ('"' + [regex]::Escape($m.Groups[1].Value) + '"\s*:\s*\{[^}]*"appId"\s*:\s*"(?!touristappid)[^"]+"')) {
                            $candidate = $known
                        }
                    }
                }
            }
        }
    }
    if ($candidate) { return $candidate }
    return $normalized
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

# The DevTools service port is configurable and stored in "User Data\<hash>\Default\.cli".
# Read every candidate port, then return the first one that is actually listening (0 = service port disabled).
function Get-DevToolsServicePort {
    $candidates = New-Object System.Collections.Generic.List[int]
    foreach ($base in (Get-ChildItem $env:LOCALAPPDATA -Directory -ErrorAction SilentlyContinue)) {
        $userData = Join-Path $base.FullName 'User Data'
        if (-not (Test-Path $userData)) { continue }
        foreach ($profile in (Get-ChildItem $userData -Directory -ErrorAction SilentlyContinue)) {
            $cliFile = Join-Path $profile.FullName 'Default\.cli'
            if (-not (Test-Path $cliFile)) { continue }
            $raw = (Get-Content $cliFile -Raw -ErrorAction SilentlyContinue)
            if (-not $raw) { continue }
            $port = 0
            if ([int]::TryParse($raw.Trim(), [ref]$port) -and $port -gt 0 -and -not $candidates.Contains($port)) {
                $candidates.Add($port)
            }
        }
    }
    $candidates.Add($DevToolsDefaultPort)
    foreach ($port in $candidates) {
        if (Test-Port $port) { return $port }
    }
    return 0
}

function Get-ApiJar {
    $dir = Join-Path $Root 'target'
    if (-not (Test-Path $dir)) { return $null }
    return Get-ChildItem $dir -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*original*' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Test-SourceNewerThan {
    param([datetime]$Timestamp)
    $src = Join-Path $Root 'src'
    if (-not (Test-Path $src)) { return $false }
    $newest = Get-ChildItem $src -Recurse -File -Include *.java, *.xml, *.yml, *.yaml -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    return ($newest -and $newest.LastWriteTime -gt $Timestamp)
}

$summary = New-Object System.Collections.Generic.List[object]

# ----------------------------------------------------------------------------
# 1. MySQL
# ----------------------------------------------------------------------------
Write-Step "MySQL ($MySqlPort)"
if (Test-Port $MySqlPort) {
    Write-Skip 'MySQL'
} else {
    $pidMy = Start-Detached -FilePath $MySqlExe -Arguments "--defaults-file=$MySqlIni" -WorkDir $MySqlHome
    Write-Ok "mysqld started (pid $pidMy)"
    $null = Wait-Port -Port $MySqlPort -TimeoutSec 40 -Name 'MySQL'
    Write-Ok "MySQL is accepting connections"
}
$summary.Add([pscustomobject]@{ Service = 'MySQL'; Port = $MySqlPort; Status = 'UP' })

# ----------------------------------------------------------------------------
# 2. Redis
# ----------------------------------------------------------------------------
Write-Step "Redis ($RedisPort)"
if (Test-Port $RedisPort) {
    Write-Skip 'Redis'
} else {
    $pidRedis = Start-Detached -FilePath $RedisExe -WorkDir $RedisHome
    Write-Ok "redis-server started (pid $pidRedis)"
    $null = Wait-Port -Port $RedisPort -TimeoutSec 20 -Name 'Redis'
    Write-Ok "Redis is accepting connections"
}
$summary.Add([pscustomobject]@{ Service = 'Redis'; Port = $RedisPort; Status = 'UP' })

# ----------------------------------------------------------------------------
# 3. Spring Boot API
# ----------------------------------------------------------------------------
Write-Step "Spring Boot API ($ApiPort)"

if (Test-Port $ApiPort) {
    Write-Skip 'API'
} else {
    $jar = Get-ApiJar
    $needBuild = $Rebuild -or (-not $jar)
    if (-not $needBuild -and (Test-SourceNewerThan -Timestamp $jar.LastWriteTime)) {
        Write-Warn2 "sources are newer than $($jar.Name), rebuilding"
        $needBuild = $true
    }

    if ($needBuild) {
        if (-not (Test-Path $MavenCmd)) { throw "Maven not found: $MavenCmd" }
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $JavaExe)
        Write-Host "    building with Maven (offline first)..." -ForegroundColor DarkGray
        Push-Location $Root
        try {
            & $MavenCmd -q -o -DskipTests package
            if ($LASTEXITCODE -ne 0) {
                Write-Warn2 'offline build failed, retrying online'
                & $MavenCmd -q -DskipTests package
            }
            if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
        } finally {
            Pop-Location
        }
        $jar = Get-ApiJar
        Write-Ok "jar built: $($jar.Name)"
    }

    $pidApi = Start-Detached -FilePath $JavaExe -Arguments ('-jar "{0}"' -f $jar.FullName) -WorkDir $Root
    Write-Ok "java started (pid $pidApi)"

    $healthy = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        try {
            $health = Invoke-RestMethod "http://localhost:$ApiPort/api/actuator/health" -TimeoutSec 3
            if ($health.status -eq 'UP') { $healthy = $true; break }
        } catch { }
    }
    if (-not $healthy) {
        throw "API did not become healthy. Check logs\zhao_waimai.log (common causes: MySQL/Redis down, port 8080 taken)"
    }
    Write-Ok 'API health = UP'
}
$summary.Add([pscustomobject]@{ Service = 'Spring Boot API'; Port = $ApiPort; Status = 'UP' })

# ----------------------------------------------------------------------------
# 4. WeChat DevTools mini programs
# ----------------------------------------------------------------------------
if (-not $SkipDevTools) {
    Write-Step 'WeChat DevTools projects'
    $devtools = Find-DevTools
    $cli = if ($devtools) { Join-Path (Split-Path -Parent $devtools) 'cli.bat' } else { $null }
    $projects = $MiniPrograms | ForEach-Object { Get-RecordedProjectPath (Join-Path $Root $_) } | Where-Object { Test-Path $_ }

    if (-not $devtools) {
        Write-Warn2 'WeChat DevTools not found, open the mini programs manually'
    } elseif (($servicePort = Get-DevToolsServicePort) -gt 0) {
        foreach ($proj in $projects) {
            Write-Host "    opening $(Split-Path -Leaf $proj)..." -ForegroundColor DarkGray
            & $cli open --project $proj | Out-Null
        }
        Write-Ok "projects handed to DevTools CLI (service port $servicePort)"
    } else {
        $devtoolsRunning = @(Get-Process -Name wechatdevtools -ErrorAction SilentlyContinue).Count -gt 0
        if ($devtoolsRunning) {
            Write-Warn2 'DevTools is running but its CLI service port is OFF (nothing is listening).'
            Write-Warn2 'Enable it once: DevTools -> Settings -> Security -> Service port, then restart DevTools.'
            Write-Warn2 'After that this script opens both mini programs by itself. For now use Project -> View All Projects.'
        } else {
            foreach ($proj in $projects) {
                Start-Process -FilePath $devtools -ArgumentList $proj
                Start-Sleep -Seconds 15
            }
            Write-Ok 'DevTools launched (open the 2nd project manually until the service port is enabled)'
        }
    }
}

# ----------------------------------------------------------------------------
# 5. Optional web consoles
# ----------------------------------------------------------------------------
if ($WithWeb) {
    Write-Step 'Web consoles'
    $node = (Get-Command node.exe -ErrorAction SilentlyContinue).Source
    if (-not $node) {
        Write-Warn2 'node.exe not found, skip web consoles'
    } else {
        foreach ($web in $WebConsoles) {
            if (Test-Port $web.Port) {
                Write-Skip $web.Name
                continue
            }
            $webDir = Join-Path $Root $web.Name
            $vite = Join-Path $webDir 'node_modules\vite\bin\vite.js'
            if (-not (Test-Path $vite)) {
                Write-Warn2 "$($web.Name): node_modules not installed, run 'npm install' first"
                continue
            }
            try {
                $null = Start-Detached -FilePath $node -Arguments ('"{0}"' -f $vite) -WorkDir $webDir
                if (Wait-PortStable -Port $web.Port -TimeoutSec 60 -StableSec 3) {
                    Write-Ok "$($web.Name) on http://localhost:$($web.Port)"
                } else {
                    Write-Warn2 "$($web.Name) did not stay up; run 'npm run dev' in .\$($web.Name) to see the error"
                }
            } catch {
                Write-Warn2 "$($web.Name) failed to start: $($_.Exception.Message)"
            }
        }
    }
}

# ----------------------------------------------------------------------------
# 6. Smoke test + summary
# ----------------------------------------------------------------------------
Write-Step 'Smoke test'
try {
    $public = Invoke-RestMethod "http://localhost:$ApiPort/api/config/public" -TimeoutSec 5
    Write-Ok "GET /api/config/public -> code=$($public.code)"
} catch {
    Write-Warn2 "GET /api/config/public failed: $($_.Exception.Message)"
}

Write-Host ''
$summary | Format-Table -AutoSize | Out-String -Width 120 | Write-Host
Write-Host 'Mini program API base: http://localhost:8080/api' -ForegroundColor Gray
if ($WithWeb) {
    Write-Host 'merchant-web: http://localhost:5174   admin-web: http://localhost:5173' -ForegroundColor Gray
}
Write-Host 'Done.' -ForegroundColor Green
