$ErrorActionPreference = 'SilentlyContinue'
$user = $env:USERNAME

Write-Host "========================================"
Write-Host "    C Drive Full Cleanup Analysis"
Write-Host "========================================"
Write-Host ""

function Get-SizeGB($path) {
    if (Test-Path $path) {
        $size = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        return [math]::Round($size / 1GB, 2)
    }
    return 0
}

function Get-SizeMB($path) {
    if (Test-Path $path) {
        $size = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        return [math]::Round($size / 1MB, 2)
    }
    return 0
}

function Check-Path($label, $path, $risk) {
    if (Test-Path $path) {
        $gb = Get-SizeGB $path
        $mb = Get-SizeMB $path
        if ($gb -ge 0.01 -or $mb -ge 10) {
            $sizeStr = if ($gb -ge 1) { "$gb GB" } else { "$mb MB" }
            $riskColor = switch ($risk) {
                "safe" { "Green" }
                "low" { "Yellow" }
                "medium" { "Red" }
                default { "Gray" }
            }
            $riskLabel = switch ($risk) {
                "safe" { "SAFE" }
                "low" { "LOW RISK" }
                "medium" { "MEDIUM RISK" }
                default { "UNKNOWN" }
            }
            Write-Host "[$riskLabel] $label" -ForegroundColor $riskColor
            Write-Host "  Path: $path"
            Write-Host "  Size: $sizeStr"
            Write-Host ""
        }
    }
}

Write-Host "====== 1. TEMP FILES ======"
Write-Host ""
Check-Path "User Temp" "C:\Users\$user\AppData\Local\Temp" "safe"
Check-Path "Windows Temp" "C:\Windows\Temp" "safe"
Write-Host ""

Write-Host "====== 2. WINDOWS UPDATE & DELIVERY OPTIMIZATION ======"
Write-Host ""
Check-Path "Windows Update Download" "C:\Windows\SoftwareDistribution\Download" "safe"
Check-Path "Delivery Optimization" "C:\Windows\SoftwareDistribution\DeliveryOptimization" "safe"
Check-Path "Windows Old" "C:\Windows.old" "low"
Check-Path "Windows Upgrade" "C:\Windows\~BT" "low"
Check-Path "Windows WSUS Backup" "C:\Windows\WinSxS\Backup" "low"
Write-Host ""

Write-Host "====== 3. WINDOWS ERROR REPORTS & LOGS ======"
Write-Host ""
Check-Path "Windows Error Reports" "C:\ProgramData\Microsoft\Windows\WER" "safe"
Check-Path "Windows Event Logs" "C:\Windows\System32\winevt\Logs" "low"
Check-Path "Windows CBS Logs" "C:\Windows\Logs\CBS" "safe"
Check-Path "Windows DISM Logs" "C:\Windows\Logs\DISM" "safe"
Check-Path "Windows Setup Logs" "C:\Windows\Panther" "low"
Check-Path "Memory Dumps" "C:\Windows\Minidump" "safe"
Check-Path "Full Memory Dump" "C:\Windows\MEMORY.DMP" "safe"
Write-Host ""

Write-Host "====== 4. BROWSER CACHES ======"
Write-Host ""
Check-Path "Chrome Cache" "C:\Users\$user\AppData\Local\Google\Chrome\User Data\Default\Cache" "safe"
Check-Path "Chrome Code Cache" "C:\Users\$user\AppData\Local\Google\Chrome\User Data\Default\Code Cache" "safe"
Check-Path "Chrome GPU Cache" "C:\Users\$user\AppData\Local\Google\Chrome\User Data\Default\GPUCache" "safe"
Check-Path "Edge Cache" "C:\Users\$user\AppData\Local\Microsoft\Edge\User Data\Default\Cache" "safe"
Check-Path "Edge Code Cache" "C:\Users\$user\AppData\Local\Microsoft\Edge\User Data\Default\Code Cache" "safe"
Check-Path "Firefox Cache" "C:\Users\$user\AppData\Local\Mozilla\Firefox\Profiles" "safe"
Write-Host ""

Write-Host "====== 5. WINDOWS THUMBNAIL & ICON CACHE ======"
Write-Host ""
Check-Path "Thumbnail Cache" "C:\Users\$user\AppData\Local\Microsoft\Windows\Explorer" "low"
Check-Path "Icon Cache" "C:\Users\$user\AppData\Local\IconCache.db" "safe"
Check-Path "Windows Font Cache" "C:\Users\$user\AppData\Local\FontCache" "safe"
Write-Host ""

Write-Host "====== 6. WINDOWS STORE & APP CACHE ======"
Write-Host ""
Check-Path "Windows Store Cache" "C:\Users\$user\AppData\Local\Packages\Microsoft.WindowsStore_8wekyb3d8bbwe\LocalCache" "safe"
Check-Path "Cortana Cache" "C:\Users\$user\AppData\Local\Packages\Microsoft.549981C3F5F10_8wekyb3d8bbwe\LocalCache" "safe"
Check-Path "Windows Ink Cache" "C:\Users\$user\AppData\Local\Packages\Microsoft.WindowsInkWorkspace_8wekyb3d8bbwe\LocalCache" "safe"
Write-Host ""

Write-Host "====== 7. DEVELOPER TOOLS CACHE ======"
Write-Host ""
Check-Path "npm cache" "C:\Users\$user\AppData\Local\npm-cache" "safe"
Check-Path "npm cache (roaming)" "C:\Users\$user\AppData\Roaming\npm-cache" "safe"
Check-Path "nuget cache" "C:\Users\$user\.nuget\packages" "safe"
Check-Path "pip cache" "C:\Users\$user\AppData\Local\pip\cache" "safe"
Check-Path "yarn cache" "C:\Users\$user\AppData\Local\Yarn\Cache" "safe"
Check-Path "pnpm store" "C:\Users\$user\AppData\Local\pnpm-store" "safe"
Check-Path "gradle cache" "C:\Users\$user\.gradle\caches" "safe"
Check-Path "maven cache" "C:\Users\$user\.m2\repository" "low"
Check-Path "Android SDK" "C:\Users\$user\AppData\Local\Android" "low"
Check-Path "VS Code Extensions" "C:\Users\$user\.vscode\extensions" "low"
Check-Path "JetBrains IDEs" "C:\Users\$user\AppData\Local\JetBrains" "low"
Check-Path "VS Build Tools Cache" "C:\Users\$user\AppData\Local\Microsoft\VisualStudio" "low"
Write-Host ""

Write-Host "====== 8. GAMING & MEDIA CACHE ======"
Write-Host ""
Check-Path "Steam Cache" "C:\Program Files (x86)\Steam\depotcache" "safe"
Check-Path "Epic Games Cache" "C:\ProgramData\Epic\UnrealEngineLauncher" "safe"
Check-Path "NVIDIA Shader Cache" "C:\Users\$user\AppData\Local\NVIDIA\DXCache" "safe"
Check-Path "NVIDIA GL Cache" "C:\Users\$user\AppData\Local\NVIDIA\GLCache" "safe"
Check-Path "NVIDIA Optimizing Cache" "C:\Users\$user\AppData\Local\NVIDIA\OptixCache" "safe"
Check-Path "NVIDIA Corporation" "C:\ProgramData\NVIDIA Corporation" "low"
Write-Host ""

Write-Host "====== 9. WINDOWS COMPONENT STORE ======"
Write-Host ""
Check-Path "WinSxS Temp" "C:\Windows\WinSxS\Temp" "low"
Check-Path "Installer Cache" "C:\Windows\Installer" "medium"
Write-Host ""

Write-Host "====== 10. RECYCLE BIN ======"
Write-Host ""
try {
    $shell = New-Object -ComObject Shell.Application
    $rb = $shell.NameSpace(0xa)
    $rbCount = $rb.Items().Count
    if ($rbCount -gt 0) {
        Write-Host "[SAFE] Recycle Bin" -ForegroundColor Green
        Write-Host "  Items: $rbCount"
        Write-Host ""
    }
} catch {}
Write-Host ""

Write-Host "====== 11. OTHER COMMON CACHE LOCATIONS ======"
Write-Host ""
Check-Path "Windows Prefetch" "C:\Windows\Prefetch" "safe"
Check-Path "Crash Dumps" "C:\Users\$user\AppData\Local\CrashDumps" "safe"
Check-Path "Windows Font Cache 2" "C:\Windows\ServiceProfiles\LocalService\AppData\Local\FontCache" "safe"
Check-Path "OneDrive Cache" "C:\Users\$user\AppData\Local\Microsoft\OneDrive\logs" "safe"
Check-Path "Teams Cache" "C:\Users\$user\AppData\Local\Microsoft\Teams" "low"
Check-Path "Skype Cache" "C:\Users\$user\AppData\Local\Microsoft\Skype for Desktop" "safe"
Check-Path "Discord Cache" "C:\Users\$user\AppData\Local\discord" "low"
Write-Host ""

Write-Host "====== 12. LARGE FOLDERS ON C DRIVE (Top 15) ======"
Write-Host ""
$folders = Get-ChildItem "C:\" -Directory -Force -ErrorAction SilentlyContinue | Where-Object { $_.Name -notin @('$Recycle.Bin', 'System Volume Information') }
$results = @()
foreach ($f in $folders) {
    $size = (Get-ChildItem $f.FullName -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($size -gt 100MB) {
        $results += [PSCustomObject]@{
            Name = $f.FullName
            SizeGB = [math]::Round($size / 1GB, 2)
        }
    }
}
$results | Sort-Object SizeGB -Descending | Select-Object -First 15 | ForEach-Object {
    Write-Host "  $($_.SizeGB) GB  =>  $($_.Name)"
}
Write-Host ""

Write-Host "====== 13. USER PROFILE LARGE FOLDERS (Top 15) ======"
Write-Host ""
$userFolders = Get-ChildItem "C:\Users\$user" -Directory -Force -ErrorAction SilentlyContinue
$results2 = @()
foreach ($f in $userFolders) {
    $size = (Get-ChildItem $f.FullName -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($size -gt 100MB) {
        $results2 += [PSCustomObject]@{
            Name = $f.FullName
            SizeGB = [math]::Round($size / 1GB, 2)
        }
    }
}
$results2 | Sort-Object SizeGB -Descending | Select-Object -First 15 | ForEach-Object {
    Write-Host "  $($_.SizeGB) GB  =>  $($_.Name)"
}
Write-Host ""

Write-Host "====== 14. APPDATA LOCAL LARGE FOLDERS (Top 15) ======"
Write-Host ""
$localFolders = Get-ChildItem "C:\Users\$user\AppData\Local" -Directory -Force -ErrorAction SilentlyContinue
$results3 = @()
foreach ($f in $localFolders) {
    $size = (Get-ChildItem $f.FullName -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($size -gt 100MB) {
        $results3 += [PSCustomObject]@{
            Name = $f.FullName
            SizeGB = [math]::Round($size / 1GB, 2)
        }
    }
}
$results3 | Sort-Object SizeGB -Descending | Select-Object -First 15 | ForEach-Object {
    Write-Host "  $($_.SizeGB) GB  =>  $($_.Name)"
}
Write-Host ""

Write-Host "====== 15. APPDATA ROAMING LARGE FOLDERS (Top 10) ======"
Write-Host ""
$roamingFolders = Get-ChildItem "C:\Users\$user\AppData\Roaming" -Directory -Force -ErrorAction SilentlyContinue
$results4 = @()
foreach ($f in $roamingFolders) {
    $size = (Get-ChildItem $f.FullName -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($size -gt 100MB) {
        $results4 += [PSCustomObject]@{
            Name = $f.FullName
            SizeGB = [math]::Round($size / 1GB, 2)
        }
    }
}
$results4 | Sort-Object SizeGB -Descending | Select-Object -First 10 | ForEach-Object {
    Write-Host "  $($_.SizeGB) GB  =>  $($_.Name)"
}
Write-Host ""

Write-Host "========================================"
Write-Host "    Analysis Complete!"
Write-Host "========================================"
