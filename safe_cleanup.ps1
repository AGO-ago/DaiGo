$ErrorActionPreference = 'SilentlyContinue'
$user = $env:USERNAME

Write-Host "========================================"
Write-Host "   Safe Cleanup Starting..."
Write-Host "========================================"
Write-Host ""

$totalFreed = 0

function Clean-FolderContents($path, $label) {
    if (Test-Path $path) {
        $before = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        $beforeMB = [math]::Round($before / 1MB, 2)
        
        Get-ChildItem $path -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        
        $after = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        $afterMB = [math]::Round($after / 1MB, 2)
        $freedMB = [math]::Round(($before - $after) / 1MB, 2)
        
        if ($freedMB -gt 0) {
            Write-Host "[OK] $label => Freed $freedMB MB" -ForegroundColor Green
        } else {
            Write-Host "[OK] $label => Already clean" -ForegroundColor Gray
        }
        return ($before - $after)
    } else {
        Write-Host "[SKIP] $label => Not found" -ForegroundColor Gray
        return 0
    }
}

function Clean-SingleFile($path, $label) {
    if (Test-Path $path) {
        $size = (Get-Item $path -Force -ErrorAction SilentlyContinue).Length
        $sizeMB = [math]::Round($size / 1MB, 2)
        Remove-Item $path -Force -ErrorAction SilentlyContinue
        if (-not (Test-Path $path)) {
            Write-Host "[OK] $label => Freed $sizeMB MB" -ForegroundColor Green
            return $size
        } else {
            Write-Host "[LOCKED] $label => File in use" -ForegroundColor Yellow
            return 0
        }
    } else {
        Write-Host "[SKIP] $label => Not found" -ForegroundColor Gray
        return 0
    }
}

# 1. User Temp
Write-Host "--- 1. User Temp ---"
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Temp" "User Temp"
$totalFreed += $f
Write-Host ""

# 2. Windows Temp
Write-Host "--- 2. Windows Temp ---"
$f = Clean-FolderContents "C:\Windows\Temp" "Windows Temp"
$totalFreed += $f
Write-Host ""

# 3. Windows Update Download
Write-Host "--- 3. Windows Update Download ---"
$f = Clean-FolderContents "C:\Windows\SoftwareDistribution\Download" "Windows Update Download"
$totalFreed += $f
Write-Host ""

# 4. CBS Logs
Write-Host "--- 4. CBS Logs ---"
$f = Clean-FolderContents "C:\Windows\Logs\CBS" "CBS Logs"
$totalFreed += $f
Write-Host ""

# 5. DISM Logs
Write-Host "--- 5. DISM Logs ---"
$f = Clean-FolderContents "C:\Windows\Logs\DISM" "DISM Logs"
$totalFreed += $f
Write-Host ""

# 6. Minidump
Write-Host "--- 6. Minidump ---"
$f = Clean-FolderContents "C:\Windows\Minidump" "Minidump"
$totalFreed += $f
Write-Host ""

# 7. Memory.dmp
Write-Host "--- 7. Memory Dump ---"
$f = Clean-SingleFile "C:\Windows\MEMORY.DMP" "MEMORY.DMP"
$totalFreed += $f
Write-Host ""

# 8. Crash Dumps
Write-Host "--- 8. Crash Dumps ---"
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\CrashDumps" "User CrashDumps"
$totalFreed += $f
Write-Host ""

# 9. Chrome Cache
Write-Host "--- 9. Chrome Cache ---"
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Google\Chrome\User Data\Default\Cache" "Chrome Cache"
$totalFreed += $f
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Google\Chrome\User Data\Default\Code Cache" "Chrome Code Cache"
$totalFreed += $f
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Google\Chrome\User Data\Default\GPUCache" "Chrome GPU Cache"
$totalFreed += $f
Write-Host ""

# 10. Edge Cache
Write-Host "--- 10. Edge Cache ---"
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Microsoft\Edge\User Data\Default\Cache" "Edge Cache"
$totalFreed += $f
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Microsoft\Edge\User Data\Default\Code Cache" "Edge Code Cache"
$totalFreed += $f
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Microsoft\Edge\User Data\Default\GPUCache" "Edge GPU Cache"
$totalFreed += $f
Write-Host ""

# 11. OneDrive Logs
Write-Host "--- 11. OneDrive Logs ---"
$f = Clean-FolderContents "C:\Users\$user\AppData\Local\Microsoft\OneDrive\logs" "OneDrive Logs"
$totalFreed += $f
Write-Host ""

# 12. Windows Error Reports
Write-Host "--- 12. Windows Error Reports ---"
$f = Clean-FolderContents "C:\ProgramData\Microsoft\Windows\WER\ReportArchive" "WER ReportArchive"
$totalFreed += $f
$f = Clean-FolderContents "C:\ProgramData\Microsoft\Windows\WER\ReportQueue" "WER ReportQueue"
$totalFreed += $f
$f = Clean-FolderContents "C:\ProgramData\Microsoft\Windows\WER\Temp" "WER Temp"
$totalFreed += $f
Write-Host ""

# 13. Windows Prefetch
Write-Host "--- 13. Windows Prefetch ---"
$f = Clean-FolderContents "C:\Windows\Prefetch" "Prefetch"
$totalFreed += $f
Write-Host ""

# 14. Recycle Bin
Write-Host "--- 14. Recycle Bin ---"
try {
    Clear-RecycleBin -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] Recycle Bin => Emptied" -ForegroundColor Green
} catch {
    Write-Host "[SKIP] Recycle Bin => Could not clear" -ForegroundColor Yellow
}
Write-Host ""

# Summary
Write-Host "========================================"
$totalFreedMB = [math]::Round($totalFreed / 1MB, 2)
$totalFreedGB = [math]::Round($totalFreed / 1GB, 2)
Write-Host "   Cleanup Complete!"
Write-Host "   Total Freed: $totalFreedMB MB ($totalFreedGB GB)" -ForegroundColor Cyan
Write-Host "========================================"
