# 设置和文件浏览器功能优化计划

## 需求概述

用户要求进行以下修改：
1. 给文件列表添加排序功能
2. 把设置中刮削设置功能删除
3. 把已存在刮削信息在strm生成时复制到生成strm目录的功能独立出来
4. 保留字幕文件功能也独立出来
5. 添加是否覆盖已存在nfo的选项
6. 去除AI文件名识别设置

## 任务分解

### [ ] 任务1: 修改后端SystemConfigService - 调整默认配置结构
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 修改 `getDefaultConfig()` 方法
  - 新增独立配置项：
    - `copyExistingScrapingInfo`: 是否复制已存在的刮削信息
    - `keepSubtitleFiles`: 是否保留字幕文件
    - `overwriteExistingNfo`: 是否覆盖已存在的NFO文件
  - 保持向后兼容性
- **Success Criteria**:
  - 新配置结构正确生成
  - 旧配置能正常迁移
- **Test Requirements**:
  - `programmatic` TR-1.1: 调用 getSystemConfig() 返回包含新配置项的完整配置
  - `programmatic` TR-1.2: 旧配置文件能正确合并新配置项

### [ ] 任务2: 修改后端SystemConfigService - 新增独立配置获取方法
- **Priority**: P0
- **Depends On**: Task 1
- **Description**:
  - 新增 `getCopyExistingScrapingInfoConfig()` 方法
  - 新增 `getKeepSubtitleFilesConfig()` 方法
  - 新增 `getOverwriteExistingNfoConfig()` 方法
- **Success Criteria**:
  - 新方法能正确从配置中读取对应项
- **Test Requirements**:
  - `programmatic` TR-2.1: 每个新增方法能返回正确的默认值和用户配置值

### [ ] 任务3: 修改后端MediaScrapingService - 使用新的独立配置
- **Priority**: P0
- **Depends On**: Task 2
- **Description**:
  - 修改 `scrapMedia()` 方法，使用新的独立配置项
  - 确保配置与刮削功能解耦
- **Success Criteria**:
  - 刮削服务能正确使用新的独立配置
- **Test Requirements**:
  - `programmatic` TR-3.1: 复制已存在刮削信息功能独立工作
  - `programmatic` TR-3.2: 保留字幕文件功能独立工作
  - `programmatic` TR-3.3: 覆盖NFO配置能正确生效

### [ ] 任务4: 修改前端settings.vue - 删除刮削设置卡片
- **Priority**: P0
- **Depends On**: Task 1-3
- **Description**:
  - 删除整个刮削设置卡片 (lines 157-258)
  - 保留TMDB配置、Emby配置、日志配置
- **Success Criteria**:
  - 设置页面不再显示刮削设置
- **Test Requirements**:
  - `human-judgement` TR-4.1: 刮削设置卡片完全从UI中移除

### [ ] 任务5: 修改前端settings.vue - 添加独立的文件复制设置卡片
- **Priority**: P0
- **Depends On**: Task 4
- **Description**:
  - 新增独立的"文件复制设置"卡片
  - 包含以下配置项：
    - 复制已存在的刮削信息
    - 保留字幕文件
    - 覆盖已存在的NFO文件
- **Success Criteria**:
  - 新卡片正确显示并保存配置
- **Test Requirements**:
  - `human-judgement` TR-5.1: 新卡片UI正确显示
  - `programmatic` TR-5.2: 配置项能正确保存到后端

### [ ] 任务6: 修改前端settings.vue - 删除AI识别设置卡片
- **Priority**: P0
- **Depends On**: Task 5
- **Description**:
  - 删除整个AI识别设置卡片 (lines 260-380)
  - 删除相关的响应式数据和方法
- **Success Criteria**:
  - AI识别设置完全移除
- **Test Requirements**:
  - `human-judgement` TR-6.1: AI识别设置卡片从UI中完全移除

### [ ] 任务7: 修改前端file-browser/[id].vue - 添加文件列表排序功能
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 添加排序选项：
    - 按名称排序（升序/降序）
    - 按大小排序（升序/降序）
    - 按修改时间排序（升序/降序）
    - 按类型排序（文件夹在前）
  - 在UI中添加排序控制按钮
  - 实现本地排序逻辑
- **Success Criteria**:
  - 文件列表支持多种排序方式
- **Test Requirements**:
  - `human-judgement` TR-7.1: 排序控制UI正确显示
  - `programmatic` TR-7.2: 按名称排序能正确工作
  - `programmatic` TR-7.3: 按大小排序能正确工作
  - `programmatic` TR-7.4: 按时间排序能正确工作
  - `programmatic` TR-7.5: 文件夹始终显示在前面

### [ ] 任务8: 修改后端SystemConfigController - 确保保存API支持新配置
- **Priority**: P0
- **Depends On**: Task 1-7
- **Description**:
  - 检查和修改保存配置的API
  - 确保新配置项能正确保存
- **Success Criteria**:
  - 所有新配置项能正确持久化
- **Test Requirements**:
  - `programmatic` TR-8.1: POST /system/config 能正确保存所有新配置项

## 技术实现细节

### 配置结构变更

**旧结构**:
```json
{
  "scraping": {
    "enabled": true,
    "generateNfo": true,
    "downloadPoster": true,
    "downloadBackdrop": false,
    "keepSubtitleFiles": false,
    "useExistingScrapingInfo": false,
    "overwriteExisting": false
  }
}
```

**新结构**:
```json
{
  "copyExistingScrapingInfo": false,
  "keepSubtitleFiles": false,
  "overwriteExistingNfo": false
}
```

### 前端排序功能实现要点

1. 新增排序状态:
   - `sortBy`: 'name' | 'size' | 'modified' | 'type'
   - `sortOrder`: 'asc' | 'desc'

2. 新增计算属性 `sortedFiles`，根据当前排序设置返回排序后的文件列表

3. 排序优先级:
   - 文件夹始终在最前面
   - 同类型内再按所选字段排序

4. UI添加排序下拉菜单或按钮组

### 向后兼容性

- 旧配置中的 `scraping.keepSubtitleFiles` → 映射到新配置的 `keepSubtitleFiles`
- 旧配置中的 `scraping.useExistingScrapingInfo` → 映射到新配置的 `copyExistingScrapingInfo`
- 旧配置中的 `scraping.overwriteExisting` → 映射到新配置的 `overwriteExistingNfo`

## 验收标准

1. ✅ 文件列表支持多种排序方式
2. ✅ 设置页面不再显示刮削设置
3. ✅ 复制已存在刮削信息功能独立出来
4. ✅ 保留字幕文件功能独立出来
5. ✅ 覆盖已存在NFO的选项独立出来
6. ✅ AI文件名识别设置完全删除
7. ✅ 所有功能正常工作，无编译错误
