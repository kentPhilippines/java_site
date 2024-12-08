@echo off
setlocal

set APP_NAME=java_site
set JAR_FILE=target\%APP_NAME%-0.0.1-SNAPSHOT.jar

echo 开始部署 %APP_NAME%

:: 停止现有服务
echo 停止现有服务...
for /f "tokens=1" %%i in ('jps -l ^| findstr %APP_NAME%') do (
    taskkill /F /PID %%i
)
timeout /t 3 > nul

:: 启动服务
echo 启动服务...
start /b javaw -jar %JAR_FILE% > app.log 2>&1

:: 等待服务启动
timeout /t 3 > nul

:: 创建快捷命令说明
echo ===== 常用命令 ===== > commands.txt
echo 查看日志：type app.log >> commands.txt
echo 重启服务：deploy.bat >> commands.txt
echo 停止服务：taskkill /F /FI "WINDOWTITLE eq %APP_NAME%" >> commands.txt
echo ================== >> commands.txt

echo 部署完成！
echo 常用命令已保存到 commands.txt
type commands.txt

pause 