# 智能重命名逻辑重构计划

## 一、问题分析

### 当前问题
1. **重命名失败**：错误信息 "using relative path is not allowed" 仍然存在
2. **命名不规范**：新文件名不符合 Emby 命名规范（如 `故宫100.strm` 而不是 `故宫100.S01E26.strm`）
3. **缺少目录识别**：未利用上级目录信息来辅助识别
4. **匹配不够精准**：缺少基于日期、简介等多维度的匹配逻辑

### 用户需求
1. 使用上级目录或逐级目录查询提取剧集名称关键词
2. 自动预填入影视名称中，减少人工输入
3. 通过比对 TMDB 剧集名、简介、日期等信息实现精准识别
4. 严格按照 Emby 命名规范重命名

## 二、实现计划

### 任务 1：修复重命名路径处理逻辑
**目标**：彻底解决 "using relative path is not allowed" 错误

**实现细节**：
- 深入分析 OpenList/Alist API 的路径要求
- 确保 srcPath 和 dstPath 都是正确的相对路径格式
- 增加详细的日志记录，方便调试路径问题
- 测试边界情况（根目录、多级目录等）

**修改文件**：
- `backend/src/main/java/com/hienao/openlist2strm/controller/OpenlistConfigController.java`
- `backend/src/main/java/com/hienao/openlist2strm/service/OpenlistApiService.java`

---

### 任务 2：实现目录关键词提取功能
**目标**：从上级目录逐级提取可能的剧集名称

**实现细节**：
1. **新增 API 端点**：`/{id}/extract-title-from-path`
2. **提取逻辑**：
   - 从当前目录开始，逐级向上遍历
   - 识别目录名中可能的影视名称关键词
   - 过滤掉常见的非影视名称（如 "Season 1", "S01", "下载" 等）
   - 返回最可能的影视名称候选列表
3. **支持配置**：允许用户自定义排除关键词列表

**修改/新增文件**：
- `backend/src/main/java/com/hienao/openlist2strm/service/DirectoryTitleExtractorService.java` (新建)
- `backend/src/main/java/com/hienao/openlist2strm/controller/OpenlistConfigController.java`

---

### 任务 3：优化 TMDB 匹配逻辑
**目标**：实现多维度精准匹配

**实现细节**：
1. **获取剧集详情**：
   - 获取电视剧的所有季集信息
   - 包括每集的标题、简介、播出日期
2. **匹配算法**：
   - **日期匹配**：从文件名中提取日期（如 "2024-12-21"），与剧集播出日期比对
   - **关键词匹配**：从文件名中提取关键词，与剧集标题、简介比对
   - **期数匹配**：识别 "第9期"、"EP9" 等格式，映射到集数
   - **综合评分**：给每个维度分配权重，计算总匹配度
3. **选择最佳匹配**：选择匹配度最高的结果

**修改文件**：
- `backend/src/main/java/com/hienao/openlist2strm/service/FileRenamingService.java`
- `backend/src/main/java/com/hienao/openlist2strm/service/TmdbApiService.java` (新增获取剧集详情的方法)
- `backend/src/main/java/com/hienao/openlist2strm/dto/tmdb/TmdbTvDetail.java` (扩展字段)

---

### 任务 4：重新实现 Emby 命名规范
**目标**：生成符合 Emby 标准的文件名

**实现细节**：
1. **电影格式**：`{Title} ({Year}).{ext}`
   - 例如：`阿凡达 (2009).strm`
2. **电视剧格式**：
   - 格式 1：`{Series Name} - S{Season}E{Episode}.{ext}`
   - 格式 2：`{Series Name}.S{Season}E{Episode}.{ext}` (用户可选)
   - 例如：`故宫100.S01E26.strm`
3. **季数和集数处理**：
   - 如果无法确定季数，默认 S01
   - 根据文件顺序自动分配集数（当无法从文件名识别时）
   - 支持自定义起始集数

**修改文件**：
- `backend/src/main/java/com/hienao/openlist2strm/service/FileRenamingService.java`

---

### 任务 5：更新前端界面
**目标**：实现自动提取关键词和更好的用户体验

**实现细节**：
1. **自动提取关键词**：
   - 打开 TMDB 匹配对话框时，自动调用后端 API 提取目录关键词
   - 将提取的关键词预填入影视名称输入框
   - 显示提取的候选列表，用户可选择
2. **命名格式选项**：
   - 添加电视剧命名格式选择（连字符或点分隔）
   - 显示预览示例
3. **匹配结果展示**：
   - 显示匹配度评分
   - 显示匹配依据（日期匹配、关键词匹配等）

**修改文件**：
- `frontend/pages/file-browser/[id].vue`

---

### 任务 6：完整测试
**目标**：确保所有功能正常工作

**测试内容**：
1. 路径处理测试（根目录、多级目录）
2. 目录关键词提取测试
3. TMDB 匹配测试（日期、关键词、期数）
4. 文件名生成测试（电影、电视剧各种格式）
5. 重命名功能测试
6. 刮削功能测试

## 三、技术要点

### 目录遍历逻辑
```java
// 伪代码示例
List<String> extractTitlesFromPath(String currentPath) {
    List<String> candidates = new ArrayList<>();
    String[] segments = currentPath.split("/");
    
    for (int i = segments.length - 1; i >= 0; i--) {
        String dirName = segments[i];
        if (isLikelyTitle(dirName)) {
            candidates.add(dirNameNameclean);
        }
    }
    return candidates;
}
```

### 多维度匹配评分
| 维度 | 权重 | 说明 |
|------|------|------|
| 日期匹配 | 40% | 文件名中的日期与剧集播出日期匹配 |
| 标题匹配 | 30% | 关键词与剧集标题匹配 |
| 简介匹配 | 20% | 关键词与剧集简介匹配 |
| 期数匹配 | 10% | 期数/集数匹配 |

## 四、时间估算

| 任务 | 预计时间 |
|------|----------|
| 任务 1：修复路径问题 | 1小时 |
| 任务 2：目录关键词提取 | 2小时 |
| 任务 3：优化 TMDB 匹配 | 3小时 |
| 任务 4：Emby 命名规范 | 1.5小时 |
| 任务 5：前端更新 | 2小时 |
| 任务 6：测试 | 1.5小时 |
| **总计** | **11小时** |
