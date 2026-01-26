package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.OpenlistApiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 字幕文件复制处理器
 *
 * <p>负责字幕文件的三级优先级处理：</p>
 * <ol>
 *   <li>优先级 1 - 本地文件：检查本地是否存在对应字幕文件</li>
 *   <li>优先级 2 - OpenList 文件：本地不存在时从 OpenList 同级目录下载</li>
 *   <li>优先级 3 - 无刮削选项：字幕文件不支持 API 刮削</li>
 * </ol>
 *
 * <p>Order: 42</p>
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(42)
@RequiredArgsConstructor
public class SubtitleCopyHandler implements FileProcessorHandler {

  private final FilePriorityResolver priorityResolver;
  private final OpenlistApiService openlistApiService;

  /**
   * 已下载的字幕文件集合（用于防止重复下载）
   */
  private final Set<String> downloadedSubtitles = new HashSet<>();

  // ==================== 支持的字幕文件扩展名 ====================

  private static final Set<String> SUBTITLE_EXTENSIONS = Set.of(
      ".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx"
  );

  // ==================== 接口实现 ====================

  @Override
  public ProcessingResult process(FileProcessingContext context) {
    try {
      // 1. 检查配置是否启用
      if (!isKeepSubtitleEnabled(context)) {
        log.debug("保留字幕文件未启用，跳过");
        context.getStats().incrementSkipped();
        return ProcessingResult.SKIPPED;
      }

      // 2. 获取当前目录的所有字幕文件
      String currentDirectory = context.getCurrentFile().getPath()
          .substring(0, context.getCurrentFile().getPath().lastIndexOf('/') + 1);

      java.util.List<OpenlistApiService.OpenlistFile> subtitleFiles =
          context.getDirectoryFiles().stream()
              .filter(f -> "file".equals(f.getType()))
              .filter(f -> isSubtitleFile(f.getName()))
              .filter(f -> f.getPath().startsWith(currentDirectory))
              .filter(f -> {
                String fileName = f.getName();
                return !downloadedSubtitles.contains(fileName.toLowerCase());
              })
              .collect(Collectors.toList());

      if (subtitleFiles.isEmpty()) {
        log.debug("没有需要处理的字幕文件");
        context.getStats().incrementSkipped();
        return ProcessingResult.SKIPPED;
      }

      // 3. 处理每个字幕文件
      int successCount = 0;
      for (OpenlistApiService.OpenlistFile subtitleFile : subtitleFiles) {
        if (copySubtitleFile(context, subtitleFile)) {
          downloadedSubtitles.add(subtitleFile.getName().toLowerCase());
          successCount++;
        }
      }

      if (successCount > 0) {
        log.info("成功复制 {} 个字幕文件", successCount);
        context.getStats().incrementProcessed();
        return ProcessingResult.SUCCESS;
      }

      context.getStats().incrementSkipped();
      return ProcessingResult.SKIPPED;

    } catch (Exception e) {
      log.error("字幕文件处理失败: {}", context.getBaseFileName(), e);
      context.getStats().incrementFailed();
      return ProcessingResult.FAILED;
    }
  }

  @Override
  public Set<FileType> getHandledTypes() {
    return Set.of(FileType.SUBTITLE);
  }

  // ==================== 字幕复制逻辑 ====================

  /**
   * 复制单个字幕文件
   */
  private boolean copySubtitleFile(
      FileProcessingContext context,
      OpenlistApiService.OpenlistFile subtitleFile) {

    String saveDirectory = context.getSaveDirectory();
    String fileName = subtitleFile.getName();

    try {
      // 1. 检查本地是否已存在
      Path localPath = Paths.get(saveDirectory, fileName);
      if (Files.exists(localPath)) {
        log.debug("本地字幕文件已存在，跳过: {}", fileName);
        downloadedSubtitles.add(fileName.toLowerCase());
        return true;
      }

      // 2. 从 OpenList 下载
      byte[] content = openlistApiService.getFileContent(
          context.getOpenlistConfig(), subtitleFile, false);

      if (content != null && content.length > 0) {
        // 3. 保存到本地
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, content);

        log.info("已复制字幕文件: {} -> {} (大小: {} bytes)",
            fileName, localPath, content.length);
        return true;
      }

      log.debug("字幕文件内容为空: {}", fileName);
      return false;

    } catch (Exception e) {
      log.warn("复制字幕文件失败: {}, 错误: {}", fileName, e.getMessage());
      return false;
    }
  }

  // ==================== 配置检查 ====================

  /**
   * 检查是否启用保留字幕文件
   */
  private boolean isKeepSubtitleEnabled(FileProcessingContext context) {
    var scrapingConfig = context.getAttribute("scrapingConfig");
    if (scrapingConfig != null && scrapingConfig instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> config = (java.util.Map<String, Object>) scrapingConfig;
      return Boolean.TRUE.equals(config.get("keepSubtitleFiles"));
    }
    return false;
  }

  // ==================== 工具方法 ====================

  /**
   * 检查是否为字幕文件
   */
  public boolean isSubtitleFile(String fileName) {
    if (fileName == null) {
      return false;
    }
    String lower = fileName.toLowerCase();
    for (String ext : SUBTITLE_EXTENSIONS) {
      if (lower.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 获取字幕文件扩展名列表
   */
  public Set<String> getSubtitleExtensions() {
    return new HashSet<>(SUBTITLE_EXTENSIONS);
  }

  /**
   * 清空已下载字幕文件记录（用于新任务开始时）
   */
  public void clearDownloadedSubtitles() {
    downloadedSubtitles.clear();
  }

  /**
   * 获取已下载字幕文件数量
   */
  public int getDownloadedSubtitleCount() {
    return downloadedSubtitles.size();
  }
}
