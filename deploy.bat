@echo off
chcp 65001 >nul
echo ========================================
echo OSTRM Docker镜像部署脚本
echo ========================================
echo.

if "%~1"=="" (
    echo 用法: deploy.bat [镜像tar文件路径]
    echo 示例: deploy.bat ostrm-image-linux-amd64.tar
    pause
    exit /b 1
)

set TAR_FILE=%~1

if not exist "%TAR_FILE%" (
    echo 错误: 文件不存在 - %TAR_FILE%
    pause
    exit /b 1
)

echo [1/5] 正在加载Docker镜像...
docker load -i "%TAR_FILE%"
if %errorlevel% neq 0 (
    echo 错误: 镜像加载失败
    pause
    exit /b 1
)
echo ✓ 镜像加载成功
echo.

echo [2/5] 检查并停止正在运行的容器...
docker ps -a --filter "name=ostrm" --format "{{.Names}}" | findstr /r "^ostrm$" >nul 2>&1
if %errorlevel% equ 0 (
    echo 找到正在运行的ostrm容器，正在停止...
    docker stop ostrm >nul 2>&1
    docker rm ostrm >nul 2>&1
    echo ✓ 容器已停止并删除
) else (
    echo 没有找到正在运行的ostrm容器
)
echo.

echo [3/5] 清理旧镜像...
for /f "tokens=3" %%i in ('docker images --filter "reference=ostrm" --format "{{.ID}} {{.Repository}}:{{.Tag}}" ^| findstr /v "latest"') do (
    echo 删除旧镜像: %%i
    docker rmi -f %%i >nul 2>&1
)
echo ✓ 旧镜像清理完成
echo.

echo [4/5] 查看当前镜像...
docker images ostrm
echo.

echo [5/5] 部署完成！
echo.
echo ========================================
echo 下一步操作:
echo 1. 使用 docker-compose up -d 启动容器
echo 2. 或使用 docker run 命令手动启动
echo ========================================
echo.
pause
