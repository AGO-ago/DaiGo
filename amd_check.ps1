$ErrorActionPreference = 'SilentlyContinue'

Write-Host "========================================"
Write-Host "       C Drive AMD Cache Check Report"
Write-Host "========================================"
Write-Host ""

function Get-FolderSizeGB($path) {
    if (Test-Path $path) {
        $size = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        return [math]::Round($size / 1GB, 2)
    }
    return -1
}

function Show-Dir($label, $path, $canDelete) {
    Write-Host "--- $label ---"
    Write-Host "Path: $path"
    if (Test-Path $path) {
        $gb = Get-FolderSizeGB $path
        Write-Host "Size: $gb GB"
        if ($canDelete) {
            Write-Host "Recommendation: SAFE TO DELETE" -ForegroundColor Green
        }
        else {
            Write-Host "Recommendation: CAUTION - may contain running programs" -ForegroundColor Yellow
        }
        $subs = Get-ChildItem $path -Directory -ErrorAction SilentlyContinue
        if ($subs) {
            Write-Host "Subdirectories:"
            foreach ($sub in $subs) {
                $subSize = (Get-ChildItem $sub.FullName -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
                $subGB = [math]::Round($subSize / 1GB, 2)
                Write-Host "  $($sub.Name) => $subGB GB"
            }
        }
    }
    else {
        Write-Host "Status: NOT FOUND" -ForegroundColor Gray
    }
    Write-Host ""
}

$user = $env:USERNAME

Show-Dir "AMD Driver Install Cache" "C:\AMD" $true
Show-Dir "AMD ProgramData (cache/logs)" "C:\ProgramData\AMD" $true
Show-Dir "AMD Program Files" "C:\Program Files\AMD" $false
Show-Dir "DirectX Shader Cache" "C:\Users\$user\AppData\Local\D3DSCache" $true
Show-Dir "User AMD Cache" "C:\Users\$user\AppData\Local\AMD" $true
Show-Dir "User ATI Cache" "C:\Users\$user\AppData\Local\ATI" $true

Write-Host "--- Temp Directory AMD Cache ---"
$tempPath = $env:TEMP
$amdTemp = Get-ChildItem $tempPath -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'AMD|ATI|Radeon' }
if ($amdTemp) {
    foreach ($f in $amdTemp) {
        $gb = Get-FolderSizeGB $f.FullName
        Write-Host "  $($f.FullName) => $gb GB [SAFE TO DELETE]" -ForegroundColor Green
    }
}
else {
    Write-Host "  No AMD temp files found" -ForegroundColor Gray
}
Write-Host ""

Write-Host "--- Windows Temp AMD Cache ---"
$winTemp = "C:\Windows\Temp"
$amdWinTemp = Get-ChildItem $winTemp -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'AMD|ATI|Radeon' }
if ($amdWinTemp) {
    foreach ($f in $amdWinTemp) {
        $gb = Get-FolderSizeGB $f.FullName
        Write-Host "  $($f.FullName) => $gb GB [SAFE TO DELETE]" -ForegroundColor Green
    }
}
else {
    Write-Host "  No AMD temp files found" -ForegroundColor Gray
}
Write-Host ""

Write-Host "========================================"
Write-Host "              SUMMARY"
Write-Host "========================================"
$totalPaths = @(
    "C:\AMD",
    "C:\ProgramData\AMD",
    "C:\Users\$user\AppData\Local\D3DSCache",
    "C:\Users\$user\AppData\Local\AMD",
    "C:\Users\$user\AppData\Local\ATI"
)
$totalGB = 0
foreach ($tp in $totalPaths) {
    if (Test-Path $tp) {
        $totalGB += Get-FolderSizeGB $tp
    }
}
$rounded = [math]::Round($totalGB, 2)
Write-Host "Total cleanable AMD cache: $rounded GB" -ForegroundColor Cyan
