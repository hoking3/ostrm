#!/bin/bash

set -e

echo "========================================"
echo "OSTRM Docker镜像部署脚本"
echo "========================================"
echo ""

if [ $# -eq 0 ]; then
    echo "用法: $0 [镜像tar文件路径]"
    echo "示例: $0 ostrm-image-linux-amd64.tar"
    exit 1
fi

TAR_FILE="$1"

if [ ! -f "$TAR_FILE" ]; then
    echo "错误: 文件不存在 - $TAR_FILE"
    exit 1
fi

echo "[1/5] 正在加载Docker镜像..."
docker load -i "$TAR_FILE"
echo "✓ 镜像加载成功"
echo ""

echo "[2/5] 检查并停止正在运行的容器..."
if docker ps -a --filter "name=ostrm" --format "{{.Names}}" | grep -q "^ostrm$"; then
    echo "找到正在运行的ostrm容器，正在停止..."
    docker stop ostrm > /dev/null 2>&1 || true
    docker rm ostrm > /dev/null 2>&1 || true
    echo "✓ 容器已停止并删除"
else
    echo "没有找到正在运行的ostrm容器"
fi
echo ""

echo "[3/5] 清理旧镜像..."
docker images --filter "reference=ostrm" --format "{{.ID}} {{.Repository}}:{{.Tag}}" | grep -v "latest" | while read -r image_id image_name; do
    echo "删除旧镜像: $image_id"
    docker rmi -f "$image_id" > /dev/null 2>&1 || true
done
echo "✓ 旧镜像清理完成"
echo ""

echo "[4/5] 查看当前镜像..."
docker images ostrm
echo ""

echo "[5/5] 部署完成！"
echo ""
echo "========================================"
echo "下一步操作:"
echo "1. 使用 docker-compose up -d 启动容器"
echo "2. 或使用 docker run 命令手动启动"
echo "========================================"
echo ""
