\@echo off
title ParrotEmulator
rem 若模拟器已在运行，提示后退出（避免双开时窗口一闪就没）
tasklist /FI "IMAGENAME eq qemu-system-x86_64.exe" 2>nul | find /I "qemu-system" >nul
if not errorlevel 1 (
    echo 模拟器已经在运行了，直接使用即可。
    timeout /t 4 >nul
    exit /b 0
)
rem 清掉上次异常退出留下的锁（正常退出时这里本来就没有）
del /f /q "%USERPROFILE%\.androidvd\ParrotEmulator.avd\hardware-qemu.ini.lock" 2>nul
rmdir /s /q "%USERPROFILE%\.androidvd\ParrotEmulator.avd\hardware-qemu.ini.lock" 2>nul
"%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd ParrotEmulator -no-snapshot -no-boot-anim -no-audio
if errorlevel 1 (
    echo.
    echo 模拟器启动失败，请把上面的报错截图发给 Claude。
    pause
)
