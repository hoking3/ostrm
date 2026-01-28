## Why

在任务执行过程中，视频目录中的字幕文件和图片文件没有被下载。根本原因是 `FileProcessorChain` 在处理视频文件时，只会调用处理 `VIDEO` 类型的处理器，而 `SubtitleCopyHandler`（处理 `SUBTITLE` 类型）和 `ImageDownloadHandler`（处理 `IMAGE` 类型）被跳过。此外，`SubtitleCopyHandler` 依赖 `scrapingConfig.keepSubtitleFiles` 配置项来判断是否下载字幕，但该配置可能未被正确保存或传递（用户选择了"保留字幕文件"但配置值为 `false`）。`ImageDownloadHandler` 也假设图片文件名遵循 `{baseFileName}-poster.jpg` 格式，无法匹配用户实际使用的任意命名图片文件。

## What Changes

- 修改 `FileProcessorChain.supports()` 方法，允许 `SubtitleCopyHandler` 和 `ImageDownloadHandler` 在处理视频文件时也被调用
- 修改 `SubtitleCopyHandler`，移除对 `scrapingConfig.keepSubtitleFiles` 配置的依赖，默认或根据任务配置下载所有字幕文件
- 修改 `ImageDownloadHandler`，支持下载目录中任意命名的图片文件：
  - 优先查找 `{baseFileName}-poster.jpg` 等特定格式
  - 如果不存在，下载同目录下任意命名的图片文件（如 `FomalhautABC.jpeg`）
- 添加配置验证日志，确保配置正确读取

## Capabilities

### New Capabilities
- `subtitle-download`: 在处理视频文件时，自动下载同目录下的字幕文件
- `local-image-download`: 在处理视频文件时，自动下载同目录下的任意图片文件

### Modified Capabilities
- 无（这是一个功能修复，不涉及需求层面的变化）

## Impact

- **修改文件**：
  - `FileProcessorChain.java`: 修改 supports() 方法的处理器匹配逻辑
  - `SubtitleCopyHandler.java`: 修改 process() 方法，移除配置依赖，扩展处理范围
  - `ImageDownloadHandler.java`: 修改文件查找逻辑，支持任意命名的图片文件

- **影响功能**：
  - 字幕文件下载：处理视频文件时自动下载同目录的字幕
  - 图片文件下载：处理视频文件时自动下载同目录的图片文件
  - 不再依赖特定的图片命名格式
