# 单次任务中字幕文件重复下载问题分析

## 问题现象

在一次任务执行过程中，同一个字幕文件会被下载两次。从日志中可以看到：
```
[INFO] 已复制字幕文件: movie.srt -> /strm/path/movie.srt (大小: 12345 bytes)
[INFO] 已复制字幕文件: movie.srt -> /strm/path/movie.srt (大小: 12345 bytes)
```

## 问题根源分析

### 根本原因：目录内多个视频文件导致字幕文件被多次处理

当一个目录包含多个视频文件和共享的字幕文件时，会发生以下情况：

**目录结构示例：**
```
/movies/
├── movie1.mp4
├── movie2.mp4
├── movie3.mp4
├── movie.srt          ← 字幕文件（会被多次下载）
└── poster.jpg
```

### 详细代码分析

#### 1. 文件遍历逻辑（TaskExecutionService.java:516-548）

在 `processDirectoryBatch` 方法中：

```java
// 获取当前目录内容
List<OpenlistApiService.OpenlistFile> files = openlistApiService.getDirectoryContents(openlistConfig, path);

for (OpenlistApiService.OpenlistFile file : files) {
    allFiles.add(file); // 将所有文件添加到allFiles列表，包括字幕文件

    if ("file".equals(file.getType()) && strmFileService.isVideoFile(file.getName())) {
        // 立即处理视频文件
        processVideoFile(openlistConfig, file, taskConfig, isIncrement, needScrap, files, ...);
    }
}
```

**关键问题1：** 字幕文件被添加到 `allFiles` 列表中，且每次处理视频文件时都会传递当前目录的所有文件。

#### 2. 视频文件处理逻辑（TaskExecutionService.java:559-627）

在 `processVideoFile` 方法中：

```java
// 如果启用了刮削功能，执行媒体刮削
if (needScrap) {
    String saveDirectory = buildScrapSaveDirectory(taskConfig.getStrmPath(), relativePath);

    // 检查是否需要刮削
    boolean needScrapFile = needScrapFile(...);

    if (needScrapFile) {
        // 过滤出当前视频文件所在目录的文件
        String currentDirectory = file.getPath().substring(0, file.getPath().lastIndexOf('/') + 1);
        List<OpenlistApiService.OpenlistFile> currentDirFiles = allFiles.stream()
            .filter(
                f -> f.getPath().startsWith(currentDirectory)
                    && f.getPath()
                        .substring(currentDirectory.length())
                        .indexOf('/') == -1)
            .collect(java.util.stream.Collectors.toList());

        mediaScrapingService.scrapMedia(
            openlistConfig,
            file.getName(),
            taskConfig.getStrmPath(),
            relativePath,
            currentDirFiles, // ← 传递当前目录的所有文件（包括字幕文件）
            file.getPath());
    }
}
```

**关键问题2：** 每次处理视频文件时，都会构建当前目录的文件列表并传递给 `scrapMedia`。

#### 3. 字幕文件复制逻辑（MediaScrapingService.java:83-88）

在 `scrapMedia` 方法中：

```java
// 构建保存目录
String saveDirectory = buildSaveDirectory(strmDirectory, relativePath);

// 处理字幕文件复制（在解析媒体之前执行）
if (keepSubtitleFiles) {
    String[] subtitleExtensions = { ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx" };
    copyRelatedFiles(openlistConfig, saveDirectory, directoryFiles, subtitleExtensions, "字幕文件");
}
```

**关键问题3：** `copyRelatedFiles` 没有检查目标文件是否已存在，直接下载并覆盖。

#### 4. 文件复制实现（MediaScrapingService.java:820-874）

在 `copyRelatedFiles` 方法中：

```java
private boolean copyRelatedFiles(...) {
    for (OpenlistApiService.OpenlistFile file : directoryFiles) {
        if ("file".equals(file.getType())) {
            String fileName_lower = file.getName().toLowerCase();
            boolean isMatch = false;
            for (String ext : allowedExtensions) {
                if (fileName_lower.endsWith(ext)) {
                    isMatch = true;
                    break;
                }
            }

            if (isMatch) {
                // 直接下载并写入，不检查目标文件是否存在
                byte[] content = openlistApiService.getFileContent(openlistConfig, file, false);

                if (content != null && content.length > 0) {
                    Path targetFile = Paths.get(saveDirectory, file.getName());
                    Files.createDirectories(targetFile.getParent());
                    Files.write(targetFile, content); // 每次都写入，覆盖原文件
                }
            }
        }
    }
}
```

**关键问题4：** 没有文件存在性检查，每次都执行下载操作。

## 复现步骤

### 场景复现

1. **目录结构：**
   ```
   /movies/
   ├── movie1.mp4
   ├── movie2.mp4
   ├── movie.srt
   ```

2. **执行流程：**

   **第一次处理 movie1.mp4：**
   ```
   processDirectoryBatch("/movies")
   ├── 遍历文件：movie1.mp4, movie2.mp4, movie.srt
   ├── 处理 movie1.mp4
   │   ├── 调用 scrapMedia(openlistConfig, "movie1.mp4", ..., currentDirFiles=[movie1.mp4, movie2.mp4, movie.srt])
   │   ├── copyRelatedFiles 发现 movie.srt 匹配字幕扩展名
   │   ├── 下载 movie.srt → /strm/movies/movie.srt ✅ 第一次下载
   │   └── 继续刮削 movie1.mp4
   ```

   **第二次处理 movie2.mp4：**
   ```
   ├── 处理 movie2.mp4
   │   ├── 调用 scrapMedia(openlistConfig, "movie2.mp4", ..., currentDirFiles=[movie1.mp4, movie2.mp4, movie.srt])
   │   ├── copyRelatedFiles 发现 movie.srt 匹配字幕扩展名
   │   ├── 下载 movie.srt → /strm/movies/movie.srt ✅ 第二次下载（重复）
   │   └── 继续刮削 movie2.mp4
   ```

3. **结果：**
   - movie.srt 被下载了两次
   - 日志显示两次相同的复制操作
   - 文件被覆盖两次（内容相同）

## 日志验证

从日志中可以看到类似的输出：

```
[INFO] 开始处理媒体文件: movie1.mp4
[DEBUG] 准备复制字幕文件: movie.srt
[INFO] 已复制字幕文件: movie.srt -> /strm/movies/movie.srt (大小: 12345 bytes)

[INFO] 开始处理媒体文件: movie2.mp4
[DEBUG] 准备复制字幕文件: movie.srt
[INFO] 已复制字幕文件: movie.srt -> /strm/movies/movie.srt (大小: 12345 bytes)
```

两次日志的唯一区别是处理的视频文件名，字幕文件是同一个。

## 影响范围

### 受影响的场景

1. **多个视频文件共享字幕文件**
   - 同一目录下的多个视频文件使用相同的字幕文件
   - 例如：电影的不同版本（720p、1080p、4K）共享同一字幕

2. **电视剧目录结构**
   ```
   /tv_show/
   ├── S01E01.mp4
   ├── S01E02.mp4
   ├── S01E03.mp4
   ├── subtitles.srt  ← 会被多次下载
   └── poster.jpg
   ```

3. **批量处理任务**
   - 在全量执行模式下，所有视频文件都会被处理
   - 每次处理都会触发字幕文件下载

### 不受影响的场景

1. **单个视频文件 + 字幕文件**
   - 目录只有一个视频文件时不存在重复下载问题

2. **字幕文件已禁用**
   - `keepSubtitleFiles = false` 时不会下载字幕文件

## 解决方案

### 方案1：添加文件存在性检查（最简单有效）

修改 `copyRelatedFiles` 方法，添加文件存在性检查：

```java
private boolean copyRelatedFiles(...) {
    for (OpenlistApiService.OpenlistFile file : directoryFiles) {
        if ("file".equals(file.getType())) {
            String fileName_lower = file.getName().toLowerCase();
            boolean isMatch = false;
            for (String ext : allowedExtensions) {
                if (fileName_lower.endsWith(ext)) {
                    isMatch = true;
                    break;
                }
            }

            if (isMatch) {
                Path targetFile = Paths.get(saveDirectory, file.getName());

                // 检查文件是否已存在
                if (Files.exists(targetFile)) {
                    log.debug("{}文件已存在，跳过下载: {}", fileTypeDescription, file.getName());
                    continue;
                }

                // 下载文件内容
                byte[] content = openlistApiService.getFileContent(openlistConfig, file, false);

                if (content != null && content.length > 0) {
                    Files.createDirectories(targetFile.getParent());
                    Files.write(targetFile, content);
                    foundAndCopied = true;
                }
            }
        }
    }
}
```

**优点：**
- 实现简单，只需添加几行代码
- 适用于所有场景
- 不影响现有逻辑

**缺点：**
- 不会减少API调用次数（只在本地检查）

### 方案2：优化调用时机（从目录级别优化）

在目录级别优化字幕文件复制，而不是在每个视频文件处理时复制：

```java
// 在 TaskExecutionService.processDirectoryBatch 中
if (needScrap && keepSubtitleFiles) {
    // 只复制一次字幕文件
    String[] subtitleExtensions = { ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx" };
    copySubtitleFilesOnce(openlistConfig, taskConfig.getStrmPath(), allFiles, subtitleExtensions);
}

for (OpenlistApiService.OpenlistFile file : files) {
    allFiles.add(file);

    if ("file".equals(file.getType()) && strmFileService.isVideoFile(file.getName())) {
        // 不再传递 directoryFiles，让 scrapMedia 自行处理
        processVideoFile(openlistConfig, file, taskConfig, isIncrement, needScrap, null, ...);
    }
}
```

**优点：**
- 彻底解决问题，减少API调用
- 性能最优

**缺点：**
- 需要重构现有逻辑
- 可能影响其他功能

### 方案3：结合方案1和方案2（推荐）

既添加文件存在性检查，又优化调用时机：

```java
// 在目录级别处理字幕文件
if (needScrap && keepSubtitleFiles) {
    String[] subtitleExtensions = { ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx" };
    copySubtitleFilesOnce(openlistConfig, taskConfig.getStrmPath(), allFiles, subtitleExtensions);
}

// 在 scrapMedia 中不再复制字幕文件
// if (keepSubtitleFiles) {
//     String[] subtitleExtensions = { ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx" };
//     copyRelatedFiles(openlistConfig, saveDirectory, directoryFiles, subtitleExtensions, "字幕文件");
// }
```

**优点：**
- 彻底解决问题
- 性能最佳
- 代码变更最小

## 结论

单次任务中字幕文件重复下载的根本原因是：
1. **目录内多个视频文件共享字幕文件**
2. **每次处理视频文件时都会复制字幕文件**
3. **复制操作没有文件存在性检查**

**推荐解决方案：** 方案1（添加文件存在性检查）+ 方案2（优化调用时机）

这样既保证了向后兼容性，又解决了性能问题。

---
*分析时间: 2026-01-26*
*涉及代码版本: v2.2.6*
