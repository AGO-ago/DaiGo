@echo off
chcp 65001 >nul
echo ========================================
echo  微信开发者工具安装脚本
echo ========================================
echo.
echo 安装程序: D:\software\wechat_devtools_setup.exe
echo 安装目录: D:\software\微信开发者工具
echo.
echo 正在安装，请稍候...
echo.

"D:\software\wechat_devtools_setup.exe" /S /D=D:\software\微信开发者工具

echo.
echo ========================================
echo  安装完成！
echo ========================================
echo.
echo 安装目录: D:\software\微信开发者工具
echo.
echo 按任意键退出...
pause >nul
