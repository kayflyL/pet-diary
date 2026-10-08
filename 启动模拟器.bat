@echo off
title ParrotEmulator
set "ANDROID_SDK_ROOT=C:\Users\16891\AppData\Local\Android\Sdk"
set "ANDROID_AVD_HOME=C:\Users\16891\.android\avd"
rem 若模拟器已在运行，提示后退出
tasklist /FI "IMAGENAME eq qemu-system-x86_64.exe" 2>nul | find /I "qemu-system" >nul
if not errorlevel 1 (
    echo 模拟器已经在运行了，直接使用即可。
    timeout /t 4 >nul
    exit /b 0
)
rem 清掉上次异常退出留下的锁
del /f /q "%ANDROID_AVD_HOME%\ParrotEmulator.avd\hardware-qemu.ini.lock" 2>nul
rmdir /s /q "%ANDROID_AVD_HOME%\ParrotEmulator.avd\hardware-qemu.ini.lock" 2>nul
"%ANDROID_SDK_ROOT%\emulator\emulator.exe" -avd ParrotEmulator -no-snapshot -no-boot-anim -no-audio
if errorlevel 1 (
    echo.
    echo 模拟器启动失败，请把上面的报错截图发给 Claude。
    pause
)
