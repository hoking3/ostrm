# 改进剧集识别和集数提取 - 实施计划

## 问题分析
当前问题：
1. "2024-10-19 出发团出发前 上集..." 被错误识别为 S01E2024（把年份 2024 当作集数）
2. 无法识别特别篇（Season 0）
3. 没有利用 TMDB 剧集详情中的集数信息进行匹配

## [ ] Task 1: 重写集数提取逻辑，设置正确的优先级
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改提取逻辑的优先级：
    1. 优先：SxxExx 格式（如 S01E01）
    2. 其次："第X期"、"第X集" 格式
    3. 最后：文件名开头数字（但要排除 4 位年份）
  - 4 位数字开头如果在 1900-2030 范围内，当作年份处理，不当作集数
- **Success Criteria**:
  - "2024-10-19..." 不会把 2024 当作集数
- **Test Requirements**:
  - `programmatic` TR-1.1: 验证 extractEpisodeFromFileName 对各种格式的处理
  - `programmatic` TR-1.2: 验证 4 位年份不会被误识别为集数

## [ ] Task 2: 获取 TMDB 剧集的详细集数列表
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 调用 TMDB API 获取剧集的季数和每集的详细信息
  - 包括：每集的标题、首播日期、集数编号等
- **Success Criteria**:
  - 能够获取到完整的剧集集数信息
- **Test Requirements**:
  - `programmatic` TR-2.1: 验证能成功获取 TMDB 剧集详情和集数列表

## [ ] Task 3: 实现智能集数匹配算法
- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 对比文件名和 TMDB 每集信息：
    1. 标题匹配度
    2. 日期匹配（如果文件名包含日期）
    3. 关键词匹配（如 "第1期"、"上集" 等）
  - 计算匹配置信度，选择置信度最高的
- **Success Criteria**:
  - 能通过文件名信息智能匹配到正确的集数
- **Test Requirements**:
  - `programmatic` TR-3.1: 验证匹配算法对示例文件的匹配正确性
  - `human-judgement` TR-3.2: 验证匹配结果的合理性

## [ ] Task 4: 测试完整的识别流程
- **Priority**: P1
- **Depends On**: Task 3
- **Description**: 
  - 完整测试从搜索到匹配到命名的完整流程
  - 测试各种边界情况
- **Success Criteria**:
  - 完整流程工作正常
- **Test Requirements**:
  - `human-judgement` TR-4.1: 实际测试用户提供的两个示例文件
