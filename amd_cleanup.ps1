$ErrorActionPreference = 'SilentlyContinue'

Write-Host "========================================"
Write-Host "       AMD Cache Cleanup Starting..."
Write-Host "========================================"
Write-Host ""

function Remove-FolderContents($path, $label) {
    if (Test-Path $path) {
        $before = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        $beforeGB = [math]::Round($before / 1GB, 2)
        
        Get-ChildItem $path -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        
        $after = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        $afterGB = [math]::Round($after / 1GB, 2)
        $freed = [math]::Round(($before - $after) / 1GB, 2)
        
        Write-Host "[OK] $label" -ForegroundColor Green
        Write-Host "     Freed: $freed GB (before: $beforeGB GB, after: $afterGB GB)"
    }
    else {
        Write-Host "[SKIP] $label - Not found" -ForegroundColor Gray
    }
    Write-Host ""
}

function Remove-EntireFolder($path, $label) {
    if (Test-Path $path) {
        $size = (Get-ChildItem $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        $sizeGB = [math]::Round($size / 1GB, 2)
        
        Remove-Item $path -Recurse -Force -ErrorAction SilentlyContinue
        
        if (-not (Test-Path $path)) {
            Write-Host "[OK] $label - Deleted $sizeGB GB" -ForegroundColor Green
        }
        else {
            Write-Host "[PARTIAL] $label - Some files may be locked" -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "[SKIP] $label - Not found" -ForegroundColor Gray
    }
    Write-Host ""
}

$totalFreed = 0

# 1. Clean DxcCache (biggest - 13+ GB)
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\DxcCache" "AMD DxcCache (DX12 Shader Cache)"

# 2. Clean DxCache
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\DxCache" "AMD DxCache (DX11 Cache)"

# 3. Clean VkCache
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\VkCache" "AMD VkCache (Vulkan Cache)"

# 4. Clean CN
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\CN" "AMD CN Cache"

# 5. Clean other AMD user cache subdirs
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\DX9Cache" "AMD DX9Cache"
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\OglCache" "AMD OpenGL Cache"
Remove-FolderContents "C:\Users\26613\AppData\Local\AMD\cl.cache" "AMD OpenCL Cache"

# 6. Clean D3DSCache
Remove-FolderContents "C:\Users\26613\AppData\Local\D3DSCache" "DirectX Shader Cache (D3DSCache)"

# 7. Clean C:\AMD (driver install cache)
Remove-EntireFolder "C:\AMD" "C:\AMD (Driver Install Cache)"

# 8. Clean ATI cache
Remove-FolderContents "C:\Users\26613\AppData\Local\ATI" "ATI Cache"

# Summary
Write-Host "========================================"
Write-Host "       Cleanup Complete!"
Write-Host "========================================"

# Check remaining
$remaining = 0
$paths = @("C:\AMD", "C:\Users\26613\AppData\Local\AMD", "C:\Users\26613\AppData\Local\D3DSCache", "C:\Users\26613\AppData\Local\ATI")
foreach ($p in $paths) {
    if (Test-Path $p) {
        $s = (Get-ChildItem $p -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
        $remaining += $s
    }
}
$remainingGB = [math]::Round($remaining / 1GB, 2)
Write-Host "Remaining AMD cache on C drive: $remainingGB GB" -ForegroundColor Cyan
