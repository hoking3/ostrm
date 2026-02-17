# 增强的 TMDB 匹配逻辑实现计划

## 问题分析

当前问题：
1. 系统仍在搜索文件名，而不是直接使用给定的影视名称搜索
2. 对于编号的文件名（如 "26 王者中轴.strm"），没有正确提取集数编号
3. 匹配逻辑需要重新设计：先搜索 TMDB 获取完整剧集信息，再逐个匹配文件

## 实现计划

### [ ] Task 1: 重新设计匹配逻辑 - 先搜索 TMDB 获取剧集信息
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 修改批量匹配方法，先使用 searchTitle 搜索 TMDB
  - 获取剧集的完整信息（包括季数、集数等）
  - 然后再逐个匹配文件
- **Success Criteria**:
  - 优先使用用户给定的 searchTitle 搜索，而不是从文件名提取关键词
  - 能够获取到完整的剧集信息
- **Test Requirements**:
  - `programmatic` TR-1.1: 使用 searchTitle "故宫100" 搜索，能够找到正确的剧集
  - `human-judgement` TR-1.2: 不再依赖文件名中的关键词进行搜索

### [ ] Task 2: 添加从文件名提取集数编号的功能
- **Priority**: P0
- **Depends On**: None
- **Description**:
  - 支持从文件名开头提取数字作为集数编号（如 "26 王者中轴.strm" → E26）
  - 支持多种编号格式：纯数字、"第X集"、"EPX"、"SXXEXX" 等
  - 改进 extractEpisodeFromFileName() 方法
- **Success Criteria**:
  - "26 王者中轴.strm" → 识别为 E26
  - "第9期上：林更新.strm" → 识别为 E9
  - "EP12.strm" → 识别为 E12
- **Test Requirements**:
  - `programmatic` TR-2.1: 测试 "26 王者中轴.strm" → E26
  - `programmatic` TR-2.2: 测试 "第9期上.strm" → E9
  - `programmatic` TR-2.3: 测试 "EP12.strm" → E12

### [ ] Task 3: 修改批量匹配方法
- **Priority**: P0
- **Depends On**: Task 1, Task 2
- **Description**:
  - 修改 batchMatchFilesToTmdbWithConfig() 方法
  - 第一步：使用 searchTitle 搜索 TMDB，获取剧集信息
  - 第二步：遍历每个文件，提取集数编号
  - 第三步：为每个文件生成正确的 Emby 格式文件名
- **Success Criteria**:
  - 文件 "26 王者中轴.strm" 应该重命名为 "故宫100.S01E26.strm"
  - 文件 "27 九五之尊.strm" 应该重命名为 "故宫100.S01E27.strm"
- **Test Requirements**:
  - `programmatic` TR-3.1: 批量匹配 2 个文件，验证文件名生成正确
  - `human-judgement` TR-3.2: 检查匹配结果是否符合预期

### [ ] Task 4: 完整测试和验证
- **Priority**: P1
- **Depends On**: Task 3
- **Description**:
  - 编译并测试完整流程
  - 验证各种边界情况
  - 确保向后兼容性
- **Success Criteria**:
  - 后端编译成功
  - 所有功能正常工作
- **Test Requirements**:
  - `programmatic` TR-4.1: 后端编译成功
  - `human-judgement` TR-4.2: 完整流程测试通过

## 技术要点

### 新的匹配流程
```
1. 接收批量匹配请求（包含多个文件和 searchTitle）
2. 使用 searchTitle 搜索 TMDB，获取剧集详情
3. 对于每个文件：
   a. 提取集数编号（优先从文件名开头数字提取）
   b. 生成 Emby 格式文件名：{剧名}.S{季}E{集}.{ext}
4. 返回所有匹配结果
```

### 集数编号提取优先级
1. 从文件名开头提取纯数字（如 "26 王者中轴.strm" → 26）
2. 匹配 "第X期"、"第X集" 格式
3. 匹配 "EPX"、"EX" 格式
4. 如果都无法提取，按文件顺序自动分配
