# 文件重命名功能 - 实施计划

## 概述
在目录界面中实现两项核心功能：批量重命名功能和TMDB API匹配重命名功能。

## 现有架构分析
- 项目使用 Spring Boot 3.3.9 + Vue 3 + Nuxt.js + Tailwind CSS
- 已有完整的TMDB API集成（`TmdbApiService.java`）
- 已有媒体文件解析器（`MediaFileParser.java`）
- 已有媒体刮削服务（`MediaScrapingService.java`）
- 已有文件浏览器界面（`frontend/pages/file-browser/[id].vue`）
- 已有OpenList API集成（`OpenlistApiService.java`）

---

## 任务分解

### [ ] 任务 1：后端 - 添加文件重命名API端点
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 在 `OpenlistConfigController.java` 中新增重命名API
  - 在 `OpenlistApiService.java` 中添加重命名方法
  - 支持单个和批量文件重命名
  - 确保幂等性和错误处理
- **Success Criteria**:
  - API可以成功重命名文件
  - 支持批量操作
  - 错误处理完善
- **Test Requirements**:
  - `programmatic` TR-1.1: API返回200状态码，重命名成功
  - `programmatic` TR-1.2: 批量重命名所有文件都成功
  - `human-judgement` TR-1.3: 错误信息清晰明了

---

### [ ] 任务 2：后端 - 添加TMDB匹配服务
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 创建新的服务类 `FileRenamingService.java`
  - 实现TMDB搜索和匹配逻辑
  - 实现Emby命名规范生成（电影和电视剧）
  - 集成现有的 `MediaFileParser` 和 `TmdbApiService`
- **Success Criteria**:
  - 可以根据文件名搜索TMDB
  - 可以生成符合Emby规范的文件名
  - 支持电影和电视剧两种类型
- **Test Requirements**:
  - `programmatic` TR-2.1: TMDB搜索返回有效结果
  - `programmatic` TR-2.2: 生成的文件名符合Emby规范
  - `human-judgement` TR-2.3: 匹配结果合理准确

---

### [ ] 任务 3：前端 - 添加文件选择功能
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 修改 `file-browser/[id].vue`，添加复选框选择功能
  - 实现全选/取消全选
  - 支持点击选择
  - 显示已选择文件数量
- **Success Criteria**:
  - 可以选择多个文件
  - 选择状态正确更新
  - 显示选择统计
- **Test Requirements**:
  - `human-judgement` TR-3.1: 复选框界面清晰易用
  - `human-judgement` TR-3.2: 选择状态反馈明显

---

### [ ] 任务 4：前端 - 批量重命名UI组件
- **Priority**: P0
- **Depends On**: 任务 3
- **Description**:
  - 创建批量重命名对话框组件
  - 提供前缀、后缀、序号格式等配置选项
  - 实现重命名预览功能（原始文件名 vs 新文件名）
  - 实现撤销操作支持（本地存储原始名称）
- **Success Criteria**:
  - 可以配置重命名规则
  - 可以预览重命名结果
  - 可以应用重命名
  - 可以撤销操作
- **Test Requirements**:
  - `human-judgement` TR-4.1: UI直观易用
  - `programmatic` TR-4.2: 预览结果正确
  - `human-judgement` TR-4.3: 操作反馈清晰

---

### [ ] 任务 5：前端 - TMDB匹配重命名UI组件
- **Priority**: P0
- **Depends On**: 任务 3
- **Description**:
  - 创建TMDB匹配重命名对话框组件
  - 实现文件分析和TMDB搜索
  - 显示匹配结果（带海报）
  - 支持手动确认和调整匹配
  - 支持批量应用
- **Success Criteria**:
  - 可以搜索TMDB
  - 可以显示匹配结果
  - 可以手动调整
  - 可以批量应用
- **Test Requirements**:
  - `human-judgement` TR-5.1: TMDB搜索结果显示清晰
  - `programmatic` TR-5.2: 匹配结果准确
  - `human-judgement` TR-5.3: 手动调整流程顺畅

---

### [ ] 任务 6：后端 - 新增相关API端点
- **Priority**: P0
- **Depends On**: 任务 2
- **Description**:
  - 在 `OpenlistConfigController.java` 中新增：
    - `POST /api/openlist-config/{id}/tmdb-match`：TMDB匹配
    - `POST /api/openlist-config/{id}/rename`：批量重命名
    - `POST /api/openlist-config/{id}/tmdb-search`：TMDB搜索
- **Success Criteria**:
  - 所有API端点正常工作
  - 请求/响应格式正确
  - 错误处理完善
- **Test Requirements**:
  - `programmatic` TR-6.1: API返回200状态码
  - `programmatic` TR-6.2: 响应数据格式正确

---

### [ ] 任务 7：前端 - 集成所有功能到文件浏览器
- **Priority**: P0
- **Depends On**: 任务 3, 4, 5
- **Description**:
  - 在文件浏览器工具栏添加按钮
  - 集成批量重命名对话框
  - 集成TMDB匹配重命名对话框
  - 添加进度提示
  - 添加错误通知
- **Success Criteria**:
  - 所有功能入口可见
  - 对话框可以正常打开
  - 进度提示显示
  - 错误通知显示
- **Test Requirements**:
  - `human-judgement` TR-7.1: 界面布局合理
  - `human-judgement` TR-7.2: 操作流程顺畅
  - `human-judgement` TR-7.3: 反馈清晰

---

### [ ] 任务 8：测试和优化
- **Priority**: P1
- **Depends On**: 任务 1-7
- **Description**:
  - 完整功能测试
  - 边界情况测试
  - 性能优化
  - 响应式适配
  - 可访问性优化
- **Success Criteria**:
  - 所有功能正常工作
  - 边界情况处理正确
  - 性能良好
  - 响应式适配完美
- **Test Requirements**:
  - `programmatic` TR-8.1: 无控制台错误
  - `human-judgement` TR-8.2: 用户体验流畅
  - `human-judgement` TR-8.3: 界面美观

---

## 技术实现细节

### Emby命名规范
- **电影**: `{Title} ({Year})` 例如: `Inception (2010).mkv`
- **电视剧单集**: `{Series Name} - S{Season}E{Episode} - {Episode Title}` 例如: `Breaking Bad - S01E01 - Pilot.mkv`

### 文件重命名流程
1. 用户选择文件
2. 用户配置重命名规则或触发TMDB匹配
3. 系统预览新文件名
4. 用户确认
5. 执行重命名
6. 刷新目录列表

### 撤销功能实现
- 重命名前保存原始文件名到本地状态
- 提供撤销按钮
- 撤销时使用原始文件名调用重命名API

### 进度提示
- 使用进度条显示重命名进度
- 显示当前处理的文件名
- 完成后显示成功/失败统计
