# 字幕文件重复下载场景分析

## 问题概述

在OpenList文件树遍历流程中，字幕文件存在重复下载的问题，即相同的字幕文件会被多次下载并写入本地目录，造成网络资源和磁盘I/O的浪费。

## 字幕文件处理逻辑

### 1. 字幕文件复制触发点

在`MediaScrapingService.scrapMedia()`方法中，第83-88行处理字幕文件复制：

```java
// 构建保存目录（在解析之前就需要知道保存位置）
String saveDirectory = buildSaveDirectory(strmDirectory, relativePath);

// 处理字幕文件复制（在解析媒体之前执行）
if (keepSubtitleFiles) {
    String[] subtitleExtensions = { ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx" };
    copyRelatedFiles(openlistConfig, saveDirectory, directoryFiles, subtitleExtensions, "字幕文件");
}
```

**关键配置项：**
- `keepSubtitleFiles`：是否保留字幕文件（默认false）
- 支持的字幕格式：`.srt`, `.ass`, `.vtt`, `.ssa`, `.sub`, `.idx`

### 2. 字幕文件复制实现

`copyRelatedFiles()`方法（第820-874行）负责复制字幕文件：

```java
private boolean copyRelatedFiles(
    OpenlistConfig openlistConfig,
    String saveDirectory,
    List<OpenlistApiService.OpenlistFile> directoryFiles,
    String[] allowedExtensions,
    String fileTypeDescription) {

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
                    Files.write(targetFile, content); // 直接写入，覆盖原文件
                }
            }
        }
    }
}
```

## 字幕文件重复下载场景分析

### 场景1：全量执行模式下重复下载

**问题描述：**
在全量执行模式下，每次任务执行都会重新下载字幕文件，即使字幕文件已经存在于本地目录。

**根本原因：**
`copyRelatedFiles()`方法没有检查目标文件是否已经存在，每次都会执行以下流程：
1. 从OpenList API下载字幕文件内容
2. 直接写入本地目录，覆盖原文件

**代码流程：**
```
TaskExecutionService.executeTaskLogic()
└── 全量执行模式
    ├── 清理STRM目录
    ├── 递归遍历OpenList文件树
    │   └── 对每个视频文件
    │       └── MediaScrapingService.scrapMedia()
    │           └── if (keepSubtitleFiles)
    │               └── copyRelatedFiles() // 每次都下载
    │                   ├── openlistApiService.getFileContent() // API下载
    │                   └── Files.write() // 直接写入
```

**问题影响：**
- 浪费网络带宽
- 增加OpenList服务器负载
- 重复磁盘I/O操作
- 字幕文件时间戳被更新

### 场景2：增量执行模式下重复下载

**问题描述：**
在增量执行模式下，字幕文件仍然会被重复下载，没有利用增量模式的优势。

**根本原因：**
增量模式下，系统只检查NFO文件是否存在来决定是否跳过刮削，但**字幕文件复制逻辑不在刮削检查范围内**。

**代码分析：**
在`scrapMedia()`方法中（第285-289行）：
```java
// 检查是否已刮削（增量模式下跳过已刮削的文件）
if (isAlreadyScraped(saveDirectory, baseFileName, mediaInfo)) {
    log.info("文件已刮削，跳过: {}", fileName);
    return;
}
```

`isAlreadyScraped()`方法只检查：
- NFO文件（`.nfo`）
- 海报文件（`-poster.jpg`）
- 背景图文件（`-fanart.jpg`或`-thumb.jpg`）

**但字幕文件不在检查范围内**，所以即使字幕文件已存在，仍会重复下载。

### 场景3：目录级别优化失效

**问题描述：**
系统实现了目录级别优化（`isDirectoryFullyScraped()`），当目录完全刮削时会跳过整个目录的处理，但**字幕文件复制仍然会执行**。

**代码分析：**
在`TaskExecutionService.processVideoFile()`方法中（第600-602行）：
```java
if (isIncrement && mediaScrapingService.isDirectoryFullyScraped(saveDirectory)) {
    log.debug("目录已完全刮削，跳过: {}", saveDirectory);
    scrapSkippedCount++;
} else {
    mediaScrapingService.scrapMedia(...); // 这里仍会复制字幕文件
}
```

即使目录已完全刮削，字幕文件仍然会被重复下载。

### 场景4：刮削失败重试导致重复下载

**问题描述：**
当刮削过程中某个步骤失败时，系统会重试，导致字幕文件被重复下载。

**错误处理逻辑：**
在`TaskExecutionService.executeTaskLogic()`方法中（第280-287行）：
```java
} catch (Exception scrapException) {
    log.error("刮削文件失败: {}, 错误: {}", file.getName(), scrapException.getMessage(), scrapException);
    // 刮削失败不影响STRM文件生成，继续处理
}
```

当刮削失败但不影响主流程时，字幕文件可能已经被复制，如果重试，会再次复制。

## 问题根本原因

### 1. 缺少文件存在性检查

**核心问题：** `copyRelatedFiles()`方法没有检查目标文件是否已存在。

**当前实现：**
```java
// 直接下载并写入，不检查目标文件是否存在
byte[] content = openlistApiService.getFileContent(openlistConfig, file, false);
if (content != null && content.length > 0) {
    Path targetFile = Paths.get(saveDirectory, file.getName());
    Files.createDirectories(targetFile.getParent());
    Files.write(targetFile, content); // 每次都写入
}
```

**应该实现的逻辑：**
```java
Path targetFile = Paths.get(saveDirectory, file.getName());
if (!Files.exists(targetFile)) {
    // 仅在文件不存在时下载
    byte[] content = openlistApiService.getFileContent(openlistConfig, file, false);
    if (content != null && content.length > 0) {
        Files.createDirectories(targetFile.getParent());
        Files.write(targetFile, content);
    }
} else {
    log.debug("{}文件已存在，跳过下载: {}", fileTypeDescription, file.getName());
}
```

### 2. 增量模式检查范围不完整

**问题：** 增量模式检查（`isAlreadyScraped()`）没有包含字幕文件。

**影响：** 即使启用了增量模式，字幕文件仍会被重复下载。

### 3. 没有幂等性保证

**问题：** 下载操作不具备幂等性，每次执行都会产生副作用（更新文件时间戳）。

## 解决方案建议

### 方案1：添加文件存在性检查（推荐）

在`copyRelatedFiles()`方法中添加文件存在性检查：

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

                // 下载文件内容 (不使用URL编码)
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

### 方案2：在增量模式检查中包含字幕文件

修改`isAlreadyScraped()`方法，增加字幕文件检查：

```java
private boolean isAlreadyScraped(String saveDirectory, String baseFileName, MediaInfo mediaInfo) {
    // ... 现有检查 ...

    // 检查字幕文件（如果启用了keepSubtitleFiles）
    Map<String, Object> scrapingConfig = systemConfigService.getScrapingConfig();
    boolean keepSubtitleFiles = (Boolean) scrapingConfig.getOrDefault("keepSubtitleFiles", false);

    if (keepSubtitleFiles) {
        String[] subtitleExtensions = { ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx" };
        for (String ext : subtitleExtensions) {
            String subtitlePath = saveDirectory + "/" + baseFileName + ext;
            if (!new File(subtitlePath).exists()) {
                log.debug("字幕文件不存在，需要复制: {}", subtitlePath);
                return false;
            }
        }
    }

    return true;
}
```

### 方案3：添加配置项控制是否覆盖字幕文件

参考`overwriteExisting`配置，为字幕文件添加覆盖控制：

```java
boolean overwriteExisting = (Boolean) scrapingConfig.getOrDefault("overwriteSubtitleFiles", false);

if (!Files.exists(targetFile) || overwriteExisting) {
    // 下载并写入文件
}
```

## 性能影响评估

### 当前问题的影响

1. **网络带宽浪费**：每次执行任务都会重新下载所有字幕文件
2. **API服务器负载**：增加OpenList服务器的请求压力
3. **磁盘I/O开销**：重复写入相同文件
4. **任务执行时间**：不必要的下载延长了任务执行时间

### 优化后的收益

以一个典型的场景为例：
- 1000个视频文件
- 平均每个视频有2个字幕文件（.srt + .ass）
- 每次执行任务

**优化前：**
- 下载次数：2000次
- 网络传输：假设每个字幕文件50KB，总计100MB
- 磁盘写入：100MB

**优化后（文件已存在）：**
- 下载次数：0次
- 网络传输：0MB
- 磁盘写入：0次

## 结论

字幕文件重复下载是一个典型的**缺少幂等性保证**的问题。根本原因是`copyRelatedFiles()`方法没有检查目标文件是否存在，且增量模式的检查范围不包含字幕文件。

**建议优先级：**
1. **高优先级**：方案1 - 添加文件存在性检查（简单有效）
2. **中优先级**：方案2 - 在增量模式检查中包含字幕文件（完善增量逻辑）
3. **低优先级**：方案3 - 添加配置项控制是否覆盖（提供灵活性）

**最佳实践：**
结合方案1和方案2，既能解决重复下载问题，又能与增量模式良好集成，实现真正的增量处理。

---
*分析时间: 2026-01-26*
*涉及代码版本: v2.2.6*
