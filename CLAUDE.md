# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 提供代码仓库的操作指南。

## 项目概述

Ostrm 是一个现代化的全栈应用程序，用于将文件列表转换为 STRM 流媒体文件（原 OpenList to Stream 项目）。项目采用容器化架构，支持多平台部署，具备完整的用户认证、任务调度和媒体刮削功能。

- **前端**: Nuxt.js 3.13.0 + Vue 3.4.0 + Tailwind CSS 3.4.15
- **后端**: Spring Boot 3.3.9 + Gradle 8.12.1 + Java 21
- **数据库**: SQLite 3.47.1.0 + Flyway 11.4.0 数据库迁移
- **DevOps**: Docker + Docker Compose + GitHub Actions CI/CD
- **架构**: 多阶段容器化部署，支持热重载

## 项目架构

### 总体结构
```
├── frontend/           # Nuxt.js 3.13.0 前端应用
│   ├── pages/         # 自动路由页面（7个主要页面）
│   ├── components/    # 可复用 Vue 组件
│   ├── middleware/    # 路由中间件（auth.js, guest.js）
│   ├── stores/        # Pinia 状态管理
│   ├── plugins/       # Nuxt 插件和工具
│   └── assets/        # Tailwind CSS 样式和静态资源
├── backend/           # Spring Boot 3.3.9 后端应用
│   └── src/main/java/com/hienao/openlist2strm/
│       ├── controller/  # REST API 控制器（认证、配置、任务）
│       ├── service/     # 业务逻辑层
│       ├── handler/     # 文件处理器链（责任链模式）
│       │   ├── FileProcessorHandler.java     # 处理器接口
│       │   ├── FileProcessorChain.java       # 链执行器
│       │   ├── FileDiscoveryHandler.java     # Order: 10 - 文件发现
│       │   ├── FileFilterHandler.java        # Order: 20 - 文件过滤
│       │   ├── StrmGenerationHandler.java    # Order: 30 - STRM生成
│       │   ├── NfoDownloadHandler.java       # Order: 40 - NFO下载（优先级：本地 > OpenList > 刮削）
│       │   ├── ImageDownloadHandler.java     # Order: 41 - 图片下载
│       │   ├── SubtitleCopyHandler.java      # Order: 42 - 字幕复制
│       │   ├── MediaScrapingHandler.java     # Order: 50 - 媒体刮削
│       │   ├── OrphanCleanupHandler.java     # Order: 60 - 孤立文件清理
│       │   ├── context/                      # 共享上下文
│       │   └── FileProcessingContext.java    # 处理上下文
│       ├── mapper/      # MyBatis 数据访问层
│       ├── entity/      # 数据库实体
│       ├── job/         # Quartz 定时任务
│       ├── config/      # Spring 配置类
│       └── security/    # JWT 认证配置
├── .github/           # GitHub Actions CI/CD 工作流
├── docker-compose.yml # 容器编排
└── dev-docker.sh      # 开发环境脚本
```

### 核心组件

**前端 (Nuxt.js 3.13.0)**:
- 基于 JWT 令牌的认证，存储在 Cookie 中并支持自动刷新
- 中间件保护的路由（`auth.js`, `guest.js`）配合智能路由
- Tailwind CSS 3.4.15 毛玻璃设计系统
- Composition API + `<script setup>` 语法 + TypeScript 支持
- Pinia 状态管理，支持 localStorage/sessionStorage 持久化
- 响应式设计，渐变色主题和动画效果

**后端 (Spring Boot 3.3.9)**:
- RESTful API + JWT 认证 + Spring Security 集成
- Quartz 定时任务（由于 SQLite 兼容性使用 RAM 存储）
- MyBatis 3.0.4 ORM + SQLite 3.47.1.0 数据库
- Flyway 11.4.0 数据库迁移管理
- 多级缓存 + 异步处理
- WebSocket 实时更新支持

**核心功能**:
- OpenList 配置管理（CRUD 操作）
- STRM 文件生成任务（批量处理 + 进度跟踪）
- 定时任务执行（CRON 表达式 + 错误处理）
- AI 媒体刮削（可选，支持可配置 Provider）
- URL 编码控制和 Base URL 替换
- 多平台容器部署 + 健康检查

### 数据库

- **SQLite 3.47.1.0**: 主数据库，文件存储 + WAL 模式
- **数据表**: `openlist_config`, `task_config`, `user_info`, 迁移跟踪
- **迁移文件**: `backend/src/main/resources/db/migration/` 目录
- **路径**: `/maindata/db/openlist2strm.db`（容器标准化路径）
- **连接池**: HikariCP + 优化的 SQLite 配置
- **事务管理**: Spring Boot 事务管理 + 回滚支持

### API 结构

**主要端点**:
- `/api/auth/*` - 认证（登录、注册、登出、令牌刷新）
- `/api/openlist-config` - OpenList 服务器配置（CRUD）
- `/api/task-config` - STRM 任务管理（调度、执行）
- `/api/settings` - 应用设置和首选项管理
- `/api/scraping` - AI 媒体刮削配置和执行
- `/ws/*` - WebSocket 实时更新端点

**API 特性**:
- OpenAPI 3.0 文档 + Swagger UI
- 全局异常处理 + 结构化错误响应
- Bean Validation 请求验证
- 速率限制 + CORS 安全配置
- 结构化 JSON 日志输出

## 开发环境搭建

### 快速开始
```bash
# 克隆仓库
git clone https://github.com/hienao/openlist-strm.git
cd openlist-strm

# 初始化开发环境
./dev-docker.sh install

# 启动开发模式（热重载）
./dev-docker.sh start-dev

# 健康检查
./dev-docker.sh health
```

### 开发模式
```bash
# 生产模式（标准构建）
./dev-docker.sh start

# 开发模式（热重载）
./dev-docker.sh start-dev

# 实时日志
./dev-docker.sh logs-f

# 进入容器 Shell 调试
./dev-docker.sh exec
```

### 构建和部署
```bash
# 构建生产镜像
./dev-docker.sh build

# 强制重建（无缓存）
./dev-docker.sh rebuild --no-cache

# 清理所有容器、镜像、卷
./dev-docker.sh clean-all

# 从头完全重建
./dev-docker-rebuild.sh  # Linux/macOS
dev-docker-rebuild.bat    # Windows
```

### 开发环境 URL
- **前端开发服务器**: http://localhost:3000
- **后端 API 服务器**: http://localhost:8080
- **主应用**: http://localhost:3111
- **API 文档**: http://localhost:3111/swagger-ui.html
- **健康检查**: http://localhost:3111/actuator/health

## 开发规范

### 前端开发
- 使用 Composition API + `<script setup>` + TypeScript
- 保护页面使用 `auth` 中间件，配合智能路由
- 使用 `$fetch` 进行 API 调用，自动注入 Bearer 令牌和错误处理
- 遵循 Tailwind CSS 实用优先 + 毛玻璃设计系统
- 实现响应式设计，渐变色主题和流畅动画
- 使用 Pinia 状态管理，配合正确的持久化策略

### 后端开发
- 遵循 Spring Boot 3 规范的清晰分层架构
- 使用 `@RestController` + OpenAPI 3.0 注解
- 业务逻辑在 `@Service` 类中实现，配合事务管理
- 创建 MyBatis Mapper 时注意 SQL 映射和结果处理
- 使用 `@Valid` 进行 Bean Validation 请求/响应验证
- 使用 `@ControllerAdvice` 实现统一的异常处理
- 遵循 Spring Security + JWT 安全最佳实践

### 数据库开发
1. 使用 Flyway 规范创建迁移文件 `V{version}__{description}.sql`
2. 放置到 `backend/src/main/resources/db/migration/` 目录
3. 重启前使用 `./gradlew flywayMigrate` 测试迁移
4. 使用适当的索引和外键约束
5. 遵循 SQLite 性能和并发最佳实践

### 测试策略

**重要提示**：除非用户明确要求，否则开发完成后不要运行自动化测试。需要测试时，仅使用 Docker 容器构建脚本进行验证。

**需要测试时的操作**:
```bash
# 使用 Docker 构建脚本进行容器测试
./dev-docker.sh build

# 验证容器启动成功
./dev-docker.sh start

# 健康检查验证部署
./dev-docker.sh health

# 测试后清理
./dev-docker.sh clean-all
```

**可用的测试工具**（仅在明确要求时使用）:
- **后端**: JUnit 5 + Spring Boot Test + TestContainers
- **前端**: Vitest + Vue Test Utils 组件测试
- **代码质量**: PMD + Spotless（后端），ESLint + Prettier（前端）
- **覆盖率**: `./gradlew jacocoTestReport`（后端），`npm run test:coverage`（前端）
- **安全**: OWASP 依赖检查和安全扫描
- **性能**: 负载测试和性能分析

**测试理念**:
- 优先使用基于容器的集成测试，而非单元测试
- 使用 Docker 构建过程作为主要验证方法
- 关注功能验证，而非代码覆盖率指标
- 通过浏览器交互进行手动测试

### Git 工作流
- 使用特性分支工作流，命名需具有描述性
- 使用语义化提交信息（`feat:`, `fix:`, `docs:` 等）
- 所有代码变更创建 Pull Request，附带适当描述
- 确保所有质量检查通过后再合并
- 使用语义化版本（`v*.*.*` 格式）发布

### 环境配置
- 使用 `.env.docker.example` 作为本地开发模板
- 不要提交敏感配置到仓库
- 为开发（`dev`）、测试（`test`）、生产（`prod`）使用不同配置
- 为每个环境配置适当的日志级别
- 所有外部服务连接使用环境变量

## CI/CD 和容器部署

### 自动化构建和发布

**GitHub Actions 工作流**:
- **触发条件**: 标签推送（`v*.*.*` 发布版，`beta-v*.*.*` 测试版）
- **多平台**: 支持 linux/amd64 和 linux/arm64 架构
- **安全**: 启用 Docker 溯源和 SBOM 生成
- **仓库**: 自动推送到 Docker Hub 仓库

**发布流程**:
1. 创建版本标签: `git tag v1.2.0`
2. 推送标签: `git push origin v1.2.0`
3. 自动触发 GitHub Actions 工作流
4. 构建并推送多平台 Docker 镜像
5. 自动创建 GitHub Release 和资源
6. 更新最新更改的文档

```bash
# 使用最新镜像快速启动
docker-compose up -d

# 开发模式（热重载）
./dev-docker.sh start-dev

# 清理重建（移除所有容器、镜像、卷）
./dev-docker-rebuild.sh        # Linux/macOS
dev-docker-rebuild.bat         # Windows

# 手动重建命令
docker-compose down --rmi all --volumes
docker-compose build
docker-compose up -d

# 访问应用
http://localhost:3111
```

### 多平台架构支持

**支持的架构**:
- **linux/amd64**: 标准 x86_64 服务器和桌面
- **linux/arm64**: ARM64 服务器（AWS Graviton, Raspberry Pi 4+）

**Docker Buildx 集成**:
- 使用 QEMU 模拟进行原生多平台构建
- 单命令同时构建所有架构
- 优化层缓存以加快构建速度
- 自动创建多架构清单

**拉取镜像**:
```bash
# 拉取特定架构镜像
docker pull --platform linux/amd64 hienao6/ostrm:latest
docker pull --platform linux/arm64 hienao6/ostrm:latest

# 拉取多架构镜像（Docker 自动选择合适平台）
docker pull hienao6/ostrm:latest
```

自定义路径配置使用环境变量：

1. 复制环境配置模板:
```bash
cp .env.docker.example .env
```

2. 编辑 `.env` 文件自定义路径:
```bash
# Docker 卷的主机路径
LOG_PATH_HOST=./logs           # 日志文件主机路径
CONFIG_PATH_HOST=./data/config # 配置文件主机路径
DB_PATH_HOST=./data/db         # 数据库文件主机路径
STRM_PATH_HOST=./strm          # STRM 文件主机路径
```

**卷映射**:
- `${LOG_PATH_HOST}:/maindata/log` - 应用日志
- `${CONFIG_PATH_HOST}:/maindata/config` - 配置文件
- `${DB_PATH_HOST}:/maindata/db` - 数据库文件
- `${STRM_PATH_HOST}:/app/backend/strm` - 生成的 STRM 文件输出

**标准化路径结构**:
```
容器内部路径                    主机路径（默认）
/maindata/log/                 → ./logs/
/maindata/config/              → ./data/config/
/maindata/db/                  → ./data/db/
/app/backend/strm/             → ./strm/
```

### Docker 重建脚本 (`dev-docker-rebuild.sh`)
- 完全移除现有容器、网络、镜像和卷
- 配置 npm 仓库为中国镜像以提高连接性
- 使用 Docker Buildx 从头重建所有镜像
- 在分离模式下启动容器
- 自动应用标准化路径配置

### Docker 调试脚本
用于排查容器问题:
```bash
# 综合容器调试和设置（Linux/macOS/Git Bash）
./docker-debug.sh

# 功能特性:
# - 检查 Docker 守护进程状态和 Docker Buildx 配置
# - 从 .env.docker.example 创建/验证 .env 文件
# - 使用标准化结构创建必要的数据目录
# - 验证 Flyway 迁移文件和数据库模式
# - 提供数据库清理和重置选项
# - 使用 --no-cache 构建镜像以进行干净构建
# - 启动容器时使用正确的卷挂载和健康检查
# - 自动应用标准化路径配置
```

**跨平台 Docker 脚本**: 所有 Docker 脚本在 Windows 下有对应的 `.bat` 文件，但开发环境推荐使用 Git Bash 运行 `dev-docker.sh`。

### 直接 Docker 命令
```bash
# 构建多平台镜像
docker buildx build -t ostrm:latest --platform linux/amd64,linux/arm64 .

# 运行容器（单平台）
docker run -d \
  --name ostrm \
  -p 3111:80 \
  -v ./data/config:/maindata/config \
  -v ./data/db:/maindata/db \
  -v ./logs:/maindata/log \
  -v ./strm:/app/backend/strm \
  ostrm:latest
```

### 路径标准化优势

1. **一致性**: 所有组件使用标准化内部路径
2. **向后兼容**: 现有部署无需更改即可继续工作
3. **灵活性**: 环境变量允许自定义主机路径配置
4. **可维护性**: 集中式路径管理减少配置错误
5. **跨平台**: 在不同主机操作系统上一致工作
6. **容器编排**: 优化 Docker Compose 和 Kubernetes 部署

## 最近的架构更新

### CI/CD 优化（最近 5 次提交）
- **简化 Runner 配置**: 从复杂的矩阵策略改为标准 ubuntu-latest Runner
- **原生 Docker Buildx**: 切换到 Docker Buildx 原生多平台构建以获得更好性能
- **Gradle 版本管理**: 降级到 Gradle 8.12.1 以提高稳定性并修复 PMD 配置问题
- **构建性能**: 通过优化缓存策略提高构建时间
- **基础镜像简化**: 移除 noble 变体，使用标准 Ubuntu 22.04 保持一致性

### 开发体验改进
- **增强开发脚本**: 改进了 `dev-docker.sh`，提供更好的健康检查和错误处理
- **热重载稳定性**: 修复了容器文件同步问题，确保可靠开发
- **代码质量工具**: 集成了 PMD、Spotless 和全面的测试框架
- **多平台开发**: 完全支持 ARM64 和 AMD64 开发环境

## 重要说明

- **测试策略**: 除非用户明确要求，优先使用 Docker 容器构建而非自动化单元测试
- **Quartz 配置**: 由于 SQLite 兼容性，使用 RAM 存储（RAMJobStore）而非数据库持久化
- **认证**: JWT 令牌配合自动刷新机制和可配置过期时间
- **CORS**: 为开发和生产环境配置了适当安全头的 CORS
- **文件生成**: STRM 文件生成在 `/app/backend/strm` 目录，支持批量处理
- **AI 集成**: 可选的 AI 刮削功能，支持可配置的媒体元数据 Provider
- **路径管理**: 标准化路径确保跨部署环境的一致行为和向后兼容
- **多平台支持**: 原生支持 x86_64 和 ARM64 架构，自动选择镜像
- **性能优化**: 多级缓存、异步处理和数据库连接池优化性能
- **安全**: 全面的安全措施，包括依赖扫描、输入验证和安全配置管理

## 文件处理器链

系统采用责任链模式处理视频文件相关资源：

| Order | 处理器 | 功能 |
|-------|--------|------|
| 10 | FileDiscoveryHandler | 文件发现 |
| 20 | FileFilterHandler | 文件过滤 |
| 30 | StrmGenerationHandler | STRM 文件生成 |
| 40 | NfoDownloadHandler | NFO 文件下载 |
| 41 | ImageDownloadHandler | 图片文件下载（海报、背景图、缩略图） |
| 42 | SubtitleCopyHandler | 字幕文件复制 |
| 50 | MediaScrapingHandler | 媒体刮削 |
| 60 | OrphanCleanupHandler | 孤立文件清理 |

### 配置传递机制

处理器通过 `FileProcessingContext` 的属性传递配置：

```java
// 读取配置
private boolean isKeepSubtitleEnabled(FileProcessingContext context) {
    Object value = context.getAttribute("keepSubtitleFiles");
    return Boolean.TRUE.equals(value);
}

private boolean isImageScrapingEnabled(FileProcessingContext context) {
    Object value = context.getAttribute("useExistingScrapingInfo");
    return Boolean.TRUE.equals(value);
}
```

### URL 编码处理

系统使用智能 URL 编码处理中文路径和特殊字符：

```java
// UrlEncoder.java - 智能 URL 编码
public static String encodeUrlSmart(String url) {
    // 仅对路径部分编码，保留协议和主机
    // 处理中文、空格等特殊字符
}
```
