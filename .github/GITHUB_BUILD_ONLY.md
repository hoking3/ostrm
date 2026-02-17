# GitHub Actions 只构建不推送使用指南

## 📋 概述

新增了 `docker-build-only.yml` 工作流，用于在 GitHub Actions 中自动构建 Docker 镜像但**不推送到 Docker Hub**。

---

## 🎯 工作流特点

- ✅ **只构建不推送** - `push: false`
- ✅ **自动触发** - 推送到 main/develop 分支或 PR 时自动构建
- ✅ **手动触发** - 支持在 Actions 页面手动运行
- ✅ **构建产物** - 镜像作为 Artifact 上传，保留 7 天
- ✅ **缓存加速** - 使用 GHA 缓存加速后续构建
- ✅ **多架构** - 支持 amd64（可选 arm64）

---

## 🚀 使用方法

### 1. 自动触发

以下情况会自动触发构建：
- 推送代码到 `main` 分支
- 推送代码到 `develop` 分支
- 创建 PR 到 `main` 或 `develop` 分支

### 2. 手动触发

1. 进入 GitHub 仓库的 **Actions** 页面
2. 选择 **"Build Docker Image Only (No Push)"** 工作流
3. 点击 **"Run workflow"** 按钮
4. 选择分支，点击 **"Run workflow"**

---

## 📦 下载和使用构建的镜像

### 步骤 1: 下载 Artifact

1. 在 Actions 页面找到成功的构建
2. 点击进入构建详情
3. 在 **Artifacts** 区域下载镜像文件（如 `ostrm-image-linux-amd64.zip`）

### 步骤 2: 加载镜像到本地 Docker

```bash
# 解压下载的文件
unzip ostrm-image-linux-amd64.zip

# 加载镜像
docker load -i ostrm-image-linux-amd64.tar

# 查看镜像
docker images | findstr ostrm
```

### 步骤 3: 运行镜像

```bash
# 使用 docker-compose
docker-compose up -d

# 或直接运行
docker run -d -p 3111:80 --name ostrm-app ostrm:<tag>
```

---

## ⚙️ 配置说明

### 修改触发条件

编辑 `.github/workflows/docker-build-only.yml` 中的 `on` 部分：

```yaml
on:
  push:
    branches:
      - main
      - develop
      - your-custom-branch  # 添加你的分支
  pull_request:
    branches:
      - main
  workflow_dispatch:  # 保留手动触发
```

### 启用 arm64 构建

取消注释以下行：

```yaml
platform:
  - linux/amd64
  - linux/arm64  # 取消注释启用
```

### 修改 Artifact 保留时间

修改 `retention-days`：

```yaml
retention-days: 30  # 改为 30 天
```

---

## 📊 两个工作流对比

| 特性 | docker-build-push.yml | docker-build-only.yml |
|------|---------------------|---------------------|
| 触发条件 | 打 tag (v*, beta-v*) | push/PR 到 main/develop + 手动 |
| 推送到 Docker Hub | ✅ 是 | ❌ 否 |
| 多架构 | amd64 + arm64 | amd64 (可选 arm64) |
| Artifact 上传 | ❌ 否 | ✅ 是 (7天) |
| 创建 Release | ✅ 是 | ❌ 否 |

---

## 🔒 权限说明

`docker-build-only.yml` 只需要 `contents: read` 权限，不需要：
- Docker Hub 凭据
- packages write 权限
- 任何 Secrets

更安全，适合 PR 和开发分支构建！

---

## 💡 使用场景推荐

| 场景 | 使用工作流 |
|------|----------|
| 日常开发测试 | `docker-build-only.yml` |
| PR 验证 | `docker-build-only.yml` |
| 正式发布 | `docker-build-push.yml` |
| Beta 版本发布 | `docker-build-push.yml` (beta-v*) |
