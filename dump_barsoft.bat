@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

echo ========================================
echo   Barsoft 360加固脱壳工具 v2.1
echo ========================================
echo.

:: ===== 配置 =====
set PACKAGE=com.bd.printer.btapp
set ACTIVITY=com.bd.printer.btapp.ui.activity.Splash
set OUTDIR=%~dp0barsoft_dump

:: ===== ADB路径（按优先级自动检测） =====
set ADB=
if exist "D:\Android\SDK\platform-tools\adb.exe" (
    set "ADB=D:\Android\SDK\platform-tools\adb.exe"
)
if not defined ADB if exist "%LOCALAPPDATA%\Temp\platform-tools-latest\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Temp\platform-tools-latest\platform-tools\adb.exe"
)
if not defined ADB if exist "%LOCALAPPDATA%\Temp\adb-platform-tools\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Temp\adb-platform-tools\platform-tools\adb.exe"
)
if not defined ADB (
    where adb >nul 2>&1
    if not errorlevel 1 set "ADB=adb"
)
if not defined ADB (
    echo [ERROR] 找不到adb.exe
    echo 请将adb所在目录添加到PATH，或修改本脚本ADB路径
    pause
    exit /b 1
)
echo ADB路径: %ADB%

:: ===== Step 1: 检查adb =====
echo [1/7] 检查adb...
"%ADB%" version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] adb无法运行，请检查路径
    pause
    exit /b 1
)
echo OK: adb可用
echo.

:: ===== Step 2: 检查设备连接 =====
echo [2/7] 检查设备连接...
set DEVICE=
for /f "tokens=1" %%d in ('"%ADB%" devices 2^>nul ^| findstr /v "List" ^| findstr /r "^[a-zA-Z0-9]"') do set DEVICE=%%d
if "%DEVICE%"=="" (
    echo [ERROR] 没有检测到设备！
    echo.
    echo 请选择连接方式：
    echo   1. USB线连接（开启USB调试后插线）
    echo   2. WiFi连接（需手机和电脑在同一网络）
    echo.
    set /p CONNECT_TYPE="输入选择 (1/2): "
    if "!CONNECT_TYPE!"=="2" (
        set /p WIFI_ADDR="输入手机IP:端口 (如 192.168.1.100:5555): "
        echo 正在连接 !WIFI_ADDR!...
        "%ADB%" connect !WIFI_ADDR!
        timeout /t 3 /nobreak >nul
        for /f "tokens=1" %%d in ('"%ADB%" devices 2^>nul ^| findstr /v "List" ^| findstr /r "^[a-zA-Z0-9]"') do set DEVICE=%%d
    )
)
if "%DEVICE%"=="" (
    echo [ERROR] 仍未检测到设备，退出
    pause
    exit /b 1
)
echo OK: 设备已连接 - %DEVICE%
echo.

:: ===== Step 3: 检查root权限 =====
echo [3/7] 检查root权限...
"%ADB%" shell "su -c 'id'" >nul 2>&1
if errorlevel 1 (
    echo [WARN] 未检测到root权限
    set HAS_ROOT=0
) else (
    echo OK: 已获取root权限
    set HAS_ROOT=1
)
echo.

:: ===== Step 4: 启动Barsoft =====
echo [4/7] 启动Barsoft APP...
"%ADB%" shell am start -n %PACKAGE%/.ui.activity.Splash >nul 2>&1
if errorlevel 1 (
    echo [WARN] 标准Activity启动失败，尝试备选路径...
    "%ADB%" shell am start -n %PACKAGE%/.ui.Splash >nul 2>&1
)
echo 等待APP加载（360加固需要时间解密dex）...
timeout /t 8 /nobreak >nul
echo OK: Barsoft已启动
echo.

:: ===== Step 5: 获取PID =====
echo [5/7] 获取Barsoft进程PID...
set PID=
for /f "tokens=2" %%p in ('"%ADB%" shell "pidof %PACKAGE%" 2^>nul') do set PID=%%p
if "%PID%"=="" (
    for /f "tokens=2" %%p in ('"%ADB%" shell "ps | grep %PACKAGE%" 2^>nul') do set PID=%%p
)
if "%PID%"=="" (
    echo [ERROR] 找不到Barsoft进程，请确认APP正在运行
    pause
    exit /b 1
)
echo OK: PID = %PID%
echo.

:: ===== Step 6: 创建输出目录 =====
if not exist "%OUTDIR%" mkdir "%OUTDIR%"
"%ADB%" shell "mkdir -p /sdcard/barsoft_dump" >nul 2>&1

:: ===== Step 7: 根据root状态选择脱壳方案 =====
echo [6/7] 执行脱壳...
echo.

if "%HAS_ROOT%"=="1" (
    echo === 方案A: Root内存dump ===
    echo.
    
    echo [A1] 导出内存映射表...
    "%ADB%" shell "su -c 'cat /proc/%PID%/maps'" > "%OUTDIR%\maps.txt"
    echo OK: maps.txt已保存
    
    echo [A2] 搜索DEX内存区域...
    findstr /i "dalvik-DEX dex" "%OUTDIR%\maps.txt" > "%OUTDIR%\dex_regions.txt"
    echo OK: dex_regions.txt已保存
    
    echo [A3] Dump DEX文件...
    set DEX_COUNT=0
    for /f "tokens=1,2,6" %%a in (%OUTDIR%\dex_regions.txt) do (
        set /a DEX_COUNT+=1
        echo   正在dump第!DEX_COUNT!个dex: %%a - %%b [%%c]
        for /f "tokens=1,2 delims=-" %%x in ("%%a") do (
            "%ADB%" shell "su -c 'dd if=/proc/%PID%/mem bs=1 skip=%%x count=%%y of=/sdcard/barsoft_dump/dex_!DEX_COUNT!.dex 2>/dev/null'"
        )
    )
    echo OK: 共dump !DEX_COUNT! 个dex文件
    
    echo [A4] 拉取到本地...
    "%ADB%" pull /sdcard/barsoft_dump/ "%OUTDIR%\"
    echo.
    
    echo === Root脱壳完成！===
    echo DEX文件在: %OUTDIR%
    echo 用jadx-gui打开即可查看业务代码
    echo.
    
) else (
    echo === 方案B: 无Root脱壳 ===
    echo.
    echo 直接内存dump需要root权限，当前设备无root。
    echo 尝试以下替代方案：
    echo.
    
    echo [B1] 尝试 am dumpheap...
    "%ADB%" shell "am dumpheap %PACKAGE% /sdcard/barsoft_dump/heap.hprof" 2>nul
    if not errorlevel 1 (
        echo   heap dump已生成
        "%ADB%" pull /sdcard/barsoft_dump/heap.hprof "%OUTDIR%\"
        echo   已保存到 %OUTDIR%\heap.hprof
        echo   注意: heap.hprof是堆转储，不是dex，需用MAT/Android Studio分析
    ) else (
        echo   am dumpheap失败（部分系统不允许）
    )
    echo.
    
    echo [B2] 尝试备份提取APK...
    "%ADB%" shell "pm path %PACKAGE%" 2>nul
    for /f "tokens=2 delims=:" %%a in ('"%ADB%" shell "pm path %PACKAGE%" 2^>nul') do (
        echo   提取APK: %%a
        "%ADB%" pull "%%a" "%OUTDIR%\barsoft.apk"
    )
    echo   注意: 原始APK含360加固，jadx只能看壳代码
    echo.
    
    echo [B3] 尝试 dalvik-cache 提取...
    "%ADB%" shell "ls /data/dalvik-cache/arm64/*.apk@classes.dex" 2>nul >nul
    if not errorlevel 1 (
        echo   发现dalvik-cache，尝试提取...
        "%ADB%" shell "cp /data/dalvik-cache/arm64/data@app@@%PACKAGE%*.apk@classes.dex /sdcard/barsoft_dump/" 2>nul
        if not errorlevel 1 (
            "%ADB%" pull /sdcard/barsoft_dump/ "%OUTDIR%\"
            echo   dalvik-cache dex已提取
        ) else (
            echo   dalvik-cache无权限访问
        )
    ) else (
        echo   dalvik-cache不可访问（需root）
    )
    echo.
    
    echo ========================================
    echo   无Root替代方案（推荐）
    echo ========================================
    echo.
    echo 方案1: BlackDex（最简单，免root免电脑）
    echo   - 手机浏览器下载: https://github.com/CodingGay/BlackDex/releases
    echo   - 下载 BlackDex-arm64.apk 安装
    echo   - 打开BlackDex - 选择Barsoft - 点击脱壳
    echo   - dex保存在 /sdcard/BlackDex/
    echo   - 然后用 adb pull /sdcard/BlackDex/ 拉到电脑
    echo.
    echo 方案2: Frida Gadget注入（免root，需重打包APK）
    echo   - apktool解包Barsoft.apk
    echo   - 注入frida-gadget.so到lib目录
    echo   - 修改smali加载gadget
    echo   - 重签名安装，启动时frida监听dump dex
    echo.
    echo 方案3: 不脱壳，直接基于SDK知识实现CPCL打印
    echo   - 已掌握CPCL指令链、XML模板格式、数据绑定体系
    echo   - 可自行实现XML到CPCL的转换逻辑
    echo.
)

echo.
echo [7/7] 清理手机临时文件...
"%ADB%" shell "rm -rf /sdcard/barsoft_dump" >nul 2>&1
echo.

echo ========================================
echo   操作完成
echo   输出目录: %OUTDIR%
echo ========================================
pause
