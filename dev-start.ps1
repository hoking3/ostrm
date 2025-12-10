# OpenList to Stream 开发环境启动脚本 (PowerShell版本)
param(
    [switch]$Rebuild,
    [switch]$Cleanup,
    [switch]$Help
)

# 显示帮助信息
if ($Help) {
    Write-Host "OpenList to Stream 开发环境启动脚本" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "用法: .\dev-start.ps1 [选项]"
    Write-Host ""
    Write-Host "选项:"
    Write-Host "  -Rebuild        强制重新构建镜像"
    Write-Host "  -Cleanup        停止并清理容器"
    Write-Host "  -Help           显示此帮助信息"
    Write-Host ""
    Write-Host "示例:"
    Write-Host "  .\dev-start.ps1                # 首次启动"
    Write-Host "  .\dev-start.ps1 -Rebuild       # 重新构建并启动"
    Write-Host "  .\dev-start.ps1 -Cleanup       # 清理环境"
    Read-Host "按任意键退出..."
    exit 0
}

# 清理环境
if ($Cleanup) {
    Write-Host "[INFO] Cleaning up environment..." -ForegroundColor Yellow
    docker-compose down

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[INFO] Containers stopped" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Error stopping containers" -ForegroundColor Red
    }

    Read-Host "按任意键退出..."
    exit 0
}

# 设置标题
$Host.UI.RawUI.WindowTitle = "OpenList to Stream Development Environment"

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "OpenList to Stream Development Environment" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# 检查Docker
Write-Host "[STEP] Checking Docker environment..." -ForegroundColor Yellow
try {
    docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not running"
    }
    Write-Host "[INFO] Docker environment is OK" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Docker is not running, please start Docker Desktop" -ForegroundColor Red
    Read-Host "按任意键退出..."
    exit 1
}

# 检查docker-compose
Write-Host "[STEP] Checking docker-compose..." -ForegroundColor Yellow
$dockerComposeCmd = $null

try {
    docker-compose --version | Out-Null
    $dockerComposeCmd = "docker-compose"
} catch {
    try {
        docker compose version | Out-Null
        $dockerComposeCmd = "docker compose"
    } catch {
        Write-Host "[ERROR] docker-compose is not installed" -ForegroundColor Red
        Read-Host "按任意键退出..."
        exit 1
    }
}
Write-Host "[INFO] docker-compose is available: $dockerComposeCmd" -ForegroundColor Green

# 创建必要的目录
Write-Host "[STEP] Creating necessary directories..." -ForegroundColor Yellow
$directories = @("data\config", "data\db", "logs", "strm")
foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
}
Write-Host "[INFO] Directories created" -ForegroundColor Green

# 设置环境变量
Write-Host "[STEP] Setting up environment..." -ForegroundColor Yellow
if (!(Test-Path ".env")) {
    Write-Host "[WARNING] .env file does not exist, copying from .env.docker.example" -ForegroundColor Yellow
    Copy-Item ".env.docker.example" ".env" -ErrorAction SilentlyContinue
    Write-Host "[INFO] Created .env file, please modify as needed" -ForegroundColor Green
} else {
    Write-Host "[INFO] .env file already exists" -ForegroundColor Green
}

# 构建镜像
if ($Rebuild) {
    Write-Host "[STEP] Force rebuilding image..." -ForegroundColor Yellow
    Invoke-Expression "$dockerComposeCmd build --no-cache"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Image build failed" -ForegroundColor Red
        Write-Host "[DEBUG] Showing detailed error..." -ForegroundColor Yellow
        Invoke-Expression "$dockerComposeCmd build --no-cache"
        Read-Host "按任意键退出..."
        exit 1
    }
} else {
    Write-Host "[STEP] Building image (if not exists)..." -ForegroundColor Yellow
    Invoke-Expression "$dockerComposeCmd build"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Image build failed" -ForegroundColor Red
        Write-Host "[DEBUG] Showing detailed error..." -ForegroundColor Yellow
        Invoke-Expression "$dockerComposeCmd build"
        Read-Host "按任意键退出..."
        exit 1
    }
}
Write-Host "[INFO] Image build completed" -ForegroundColor Green

# 启动容器
Write-Host "[STEP] Starting containers..." -ForegroundColor Yellow
Invoke-Expression "$dockerComposeCmd up -d"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Container startup failed" -ForegroundColor Red
    Write-Host "[DEBUG] Showing container logs..." -ForegroundColor Yellow
    Invoke-Expression "$dockerComposeCmd logs"
    Read-Host "按任意键退出..."
    exit 1
}
Write-Host "[INFO] Containers started" -ForegroundColor Green

# 检查容器状态
Write-Host "[STEP] Checking container status..." -ForegroundColor Yellow
Invoke-Expression "$dockerComposeCmd ps"
Write-Host ""

# 等待应用启动
Write-Host "[STEP] Waiting for application to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 检查应用是否健康
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3111" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "[INFO] Application started successfully!" -ForegroundColor Green
    Write-Host "[INFO] Access URL: http://localhost:3111" -ForegroundColor Cyan
} catch {
    Write-Host "[WARNING] Application may still be starting" -ForegroundColor Yellow
    Write-Host "[INFO] Access URL: http://localhost:3111" -ForegroundColor Cyan
    Write-Host "[INFO] Use '$dockerComposeCmd logs -f' to view startup logs" -ForegroundColor Gray
}

# 显示有用的命令
Write-Host ""
Write-Host "[INFO] === Common Development Commands ===" -ForegroundColor Cyan
Write-Host "View logs:          $dockerComposeCmd logs -f" -ForegroundColor Gray
Write-Host "Stop services:      $dockerComposeCmd down" -ForegroundColor Gray
Write-Host "Restart services:  $dockerComposeCmd restart" -ForegroundColor Gray
Write-Host "Rebuild and start:  .\dev-start.ps1 -Rebuild" -ForegroundColor Gray
Write-Host "Enter container:    docker exec -it app bash" -ForegroundColor Gray
Write-Host "Check status:       $dockerComposeCmd ps" -ForegroundColor Gray
Write-Host ""
Write-Host "[INFO] === Development Tips ===" -ForegroundColor Cyan
Write-Host "Frontend dev (hot reload):" -ForegroundColor Gray
Write-Host "  cd frontend; npm run dev" -ForegroundColor White
Write-Host ""
Write-Host "Backend dev (hot reload):" -ForegroundColor Gray
Write-Host "  cd backend; gradlew.bat bootRun" -ForegroundColor White
Write-Host ""
Write-Host "[INFO] === Data Directories ===" -ForegroundColor Cyan
Write-Host "Config:   ./data/config" -ForegroundColor Gray
Write-Host "Database: ./data/db" -ForegroundColor Gray
Write-Host "Logs:     ./logs" -ForegroundColor Gray
Write-Host "STRM:     ./strm" -ForegroundColor Gray

Write-Host ""
Write-Host "[INFO] Development environment ready!" -ForegroundColor Green
Write-Host ""

Read-Host "按任意键退出..."