## Context

### 当前状态

在任务执行流程中，`FileProcessorChain` 按顺序执行各个 Handler，但处理器匹配逻辑存在问题：

1. **处理器类型匹配问题**：`FileProcessorChain.supports()` 方法会根据当前文件类型匹配处理器
   - 处理视频文件时，只调用 `getHandledTypes()` 包含 `VIDEO` 的处理器
   - `SubtitleCopyHandler` (Order 42) 的 `getHandledTypes()` 返回 `SUBTITLE`
   - `ImageDownloadHandler` (Order 41) 的 `getHandledTypes()` 返回 `IMAGE`
   - 结果：这两个处理器在处理视频文件时被跳过

2. **图片下载逻辑限制**：`ImageDownloadHandler` 只查找特定命名的图片文件
   - 期望：`{baseFileName}-poster.jpg`、`{baseFileName}-fanart.jpg`、`{baseFileName}-thumb.jpg`
   - 实际：用户使用任意命名的图片文件（如 `FomalhautABC.jpeg`）

3. **配置依赖问题**：`SubtitleCopyHandler` 依赖 `scrapingConfig.keepSubtitleFiles` 配置
   - 默认值为 `false`
   - 配置可能未被正确保存或传递

### 约束条件

- 需要保持现有 Handler 链的执行顺序
- 不能破坏其他 Handler 的正常功能
- 修改应尽量小而集中
- 需要兼容现有的任务配置和文件结构

## Goals / Non-Goals

**Goals:**
- 修复字幕文件下载问题，在处理视频文件时自动下载同目录的字幕
- 修复图片文件下载问题，支持下载任意命名的本地图片
- 移除字幕下载的配置依赖，改为默认启用
- 添加配置验证日志，确保配置正确读取

**Non-Goals:**
- 不改变 Handler 链的整体架构
- 不修改 NFO 文件的下载逻辑
- 不引入新的外部依赖
- 不改变现有的 API 接口或数据库结构

## Decisions

### 决策 1：处理器匹配逻辑

**方案 A**：修改 `FileProcessorChain.supports()`，让所有处理器都能处理视频文件
- 优点：改动最小，所有处理器都能响应视频文件
- 缺点：可能触发不需要的处理器

**方案 B**：修改 `SubtitleCopyHandler` 和 `ImageDownloadHandler` 的 `getHandledTypes()`，添加 `VIDEO`
- 优点：只修改特定处理器，保持其他处理器逻辑不变
- 缺点：需要修改多个处理器

**最终选择**：方案 B
- 修改 `SubtitleCopyHandler.getHandledTypes()` 返回 `Set.of(FileType.SUBTITLE, FileType.VIDEO)`
- 修改 `ImageDownloadHandler.getHandledTypes()` 返回 `Set.of(FileType.IMAGE, FileType.VIDEO)`
- 这样这两个处理器既能处理对应类型文件，也能在处理视频文件时被调用

### 决策 2：图片下载逻辑

**方案 A**：修改 `ImageDownloadHandler`，查找同目录下所有图片文件
- 优点：简单直接，下载所有图片
- 缺点：可能下载不需要的图片

**方案 B**：保留现有逻辑，优先查找特定命名格式，其次查找任意图片
- 优点：向后兼容，优先使用规范命名的图片
- 缺点：逻辑稍复杂

**最终选择**：方案 B
- 优先查找 `{baseFileName}-poster.jpg` 等特定格式
- 如果不存在，降级下载同目录下任意图片文件
- 使用 `useExistingScrapingInfo` 配置控制是否启用

### 决策 3：字幕下载配置

**方案 A**：移除配置依赖，默认启用字幕下载
- 优点：简单，用户无需配置
- 缺点：可能不符合所有用户需求

**方案 B**：添加独立的任务配置项
- 优点：灵活可控
- 缺点：需要新增配置项

**最终选择**：方案 A + 配置验证
- 移除对 `scrapingConfig.keepSubtitleFiles` 的依赖
- 默认启用字幕下载
- 添加配置验证日志，确保配置正确读取

## Risks / Trade-offs

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 下载过多不需要的图片 | 中 | 低 | 优先查找特定命名格式，再降级到任意图片 |
| 字幕下载与预期不符 | 低 | 中 | 默认启用，添加配置验证日志 |
| 回归问题 | 低 | 中 | 审查修改，确保不影响其他处理器 |

## Migration Plan

### 部署步骤
1. 提交代码修改到 feature 分支
2. 构建 Docker 镜像并测试
3. 在测试环境验证修复效果
4. 合并到主分支并发布

### 回滚策略
- 如果发现问题，可以通过 Docker 镜像回滚到 previous 版本
- 数据库无需修改，回滚不影响数据

## Open Questions

1. 是否需要限制下载的图片文件数量（如只下载 1 张海报、1 张背景图）？
2. 是否需要支持配置排除特定扩展名的图片文件？
