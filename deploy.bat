@echo off
setlocal

:: 设置变量
set APP_NAME=java_site
set JAR_FILE=target\%APP_NAME%-0.0.1-SNAPSHOT.jar
set WORK_DIR=%~dp0

:: 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java not found
    exit /b 1
)

:: 创建目录
if not exist "%WORK_DIR%\logs" mkdir "%WORK_DIR%\logs"

:: 停止现有服务
for /f "tokens=1" %%i in ('jps -l ^| findstr %APP_NAME%') do (
    taskkill /F /PID %%i >nul 2>&1
)
timeout /t 3 >nul

:: 启动服务
echo Starting service...
start /b javaw -jar %JAR_FILE% --spring.profiles.active=prod > logs\app.log 2>&1

:: 等待服务启动
timeout /t 5 >nul

:: 创建快捷命令
echo @echo off > manage.bat
echo if "%%1"=="start" goto start >> manage.bat
echo if "%%1"=="stop" goto stop >> manage.bat
echo if "%%1"=="restart" goto restart >> manage.bat
echo if "%%1"=="log" goto log >> manage.bat
echo echo Usage: manage.bat {start^|stop^|restart^|log} >> manage.bat
echo goto :eof >> manage.bat
echo :start >> manage.bat
echo call deploy.bat >> manage.bat
echo goto :eof >> manage.bat
echo :stop >> manage.bat
echo for /f "tokens=1" %%%%i in ('jps -l ^| findstr %APP_NAME%') do taskkill /F /PID %%%%i >> manage.bat
echo goto :eof >> manage.bat
echo :restart >> manage.bat
echo call :stop >> manage.bat
echo timeout /t 2 ^>nul >> manage.bat
echo call :start >> manage.bat
echo goto :eof >> manage.bat
echo :log >> manage.bat
echo type logs\app.log >> manage.bat
echo goto :eof >> manage.bat

echo Deployment complete!
echo Use manage.bat {start^|stop^|restart^|log} to manage the service

endlocal