package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.OpenlistApiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 图片文件下载处理器
 *
 * <p>负责图片文件（海报、背景图、缩略图）的三级优先级处理：</p>
 * <ol>
 *   <li>优先级 1 - 本地文件：检查本地是否存在对应图片文件</li>
 *   <li>优先级 2 - OpenList 文件：本地不存在时从 OpenList 同级目录下载</li>
 *   <li>优先级 3 - 刮削文件：前两级都不存在时执行 TMDB 刮削</li>
 * </ol>
 *
 * <p>Order: 41</p>
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(41)
@RequiredArgsConstructor
public class ImageDownloadHandler implements FileProcessorHandler {

  private final FilePriorityResolver priorityResolver;
  private final OpenlistApiService openlistApiService;

  // 图片文件扩展名
  private static final Set<String> IMAGE_EXTENSIONS = Set.of(
      ".jpg", ".jpeg", ".png", ".webp", ".bmp"
  );

  // ==================== 接口实现 ====================

  @Override
  public ProcessingResult process(FileProcessingContext context) {
    try {
      // 1. 检查配置是否启用
      if (!isImageScrapingEnabled(context)) {
        log.debug("图片刮削未启用，跳过");
        context.getStats().incrementSkipped();
        return ProcessingResult.SKIPPED;
      }

      // 2. 处理海报文件
      ProcessingResult posterResult = processImage(
          context, "-poster.jpg", "海报");

      // 3. 处理背景图文件
      ProcessingResult backdropResult = processImage(
          context, "-fanart.jpg", "背景图");

      // 4. 处理缩略图文件
      ProcessingResult thumbResult = processImage(
          context, "-thumb.jpg", "缩略图");

      // 综合结果
      if (posterResult == ProcessingResult.FAILED
          || backdropResult == ProcessingResult.FAILED
          || thumbResult == ProcessingResult.FAILED) {
        return ProcessingResult.FAILED;
      }

      return ProcessingResult.SUCCESS;

    } catch (Exception e) {
      log.error("图片文件处理失败: {}", context.getBaseFileName(), e);
      context.getStats().incrementFailed();
      return ProcessingResult.FAILED;
    }
  }

  @Override
  public Set<FileType> getHandledTypes() {
    return Set.of(FileType.IMAGE);
  }

  // ==================== 图片处理 ====================

  /**
   * 处理单个图片文件
   */
  private ProcessingResult processImage(
      FileProcessingContext context,
      String suffix,
      String description) {

    String imageFileName = context.getBaseFileName() + suffix;

    // 1. 检查本地文件是否存在
    Path localPath = Paths.get(context.getSaveDirectory(), imageFileName);
    if (Files.exists(localPath)) {
      log.debug("本地{}文件已存在，跳过: {}", description, imageFileName);
      context.getStats().incrementSkipped();
      return ProcessingResult.SKIPPED;
    }

    // 2. 从 OpenList 下载
    OpenlistApiService.OpenlistFile openlistImage = findImageInDirectory(
        context.getDirectoryFiles(), imageFileName);

    if (openlistImage != null) {
      try {
        byte[] content = openlistApiService.getFileContent(
            context.getOpenlistConfig(), openlistImage, false);

        if (content != null && content.length > 0) {
          Files.createDirectories(localPath.getParent());
          Files.write(localPath, content);

          log.info("从 OpenList 下载{}文件成功: {}", description, imageFileName);
          context.getStats().incrementProcessed();
          return ProcessingResult.SUCCESS;
        }
      } catch (Exception e) {
        log.warn("从 OpenList 下载{}文件失败: {}", description, imageFileName, e);
      }
    }

    // 3. 图片文件不执行刮削（由 MediaScrapingHandler 处理）
    log.debug("本地和 OpenList 都不存在{}文件，跳过: {}", description, imageFileName);
    context.getStats().incrementSkipped();
    return ProcessingResult.SKIPPED;
  }

  // ==================== 电视剧共用图片处理 ====================

  /**
   * 处理电视剧共用图片（poster.jpg, fanart.jpg）
   */
  public ProcessingResult processTvShowSharedImages(FileProcessingContext context) {
    try {
      String saveDirectory = context.getSaveDirectory();

      // 处理 tvshow.nfo
      processSharedFile(context, saveDirectory, "tvshow.nfo");

      // 处理 poster.jpg
      processSharedFile(context, saveDirectory, "poster.jpg");

      // 处理 fanart.jpg
      processSharedFile(context, saveDirectory, "fanart.jpg");

      return ProcessingResult.SUCCESS;

    } catch (Exception e) {
      log.error("处理电视剧共用图片失败: {}", context.getBaseFileName(), e);
      return ProcessingResult.FAILED;
    }
  }

  private void processSharedFile(
      FileProcessingContext context, String saveDirectory, String fileName) {

    Path localPath = Paths.get(saveDirectory, fileName);

    if (Files.exists(localPath)) {
      log.debug("本地共用文件已存在，跳过: {}", fileName);
      return;
    }

    // 从 OpenList 下载
    OpenlistApiService.OpenlistFile openlistFile = findImageInDirectory(
        context.getDirectoryFiles(), fileName);

    if (openlistFile != null) {
      try {
        byte[] content = openlistApiService.getFileContent(
            context.getOpenlistConfig(), openlistFile, false);

        if (content != null && content.length > 0) {
          Files.createDirectories(localPath.getParent());
          Files.write(localPath, content);
          log.info("从 OpenList 下载共用文件成功: {}", fileName);
        }
      } catch (Exception e) {
        log.warn("从 OpenList 下载共用文件失败: {}", fileName, e);
      }
    }
  }

  // ==================== 工具方法 ====================

  /**
   * 检查图片刮削是否启用
   */
  private boolean isImageScrapingEnabled(FileProcessingContext context) {
    var scrapingConfig = context.getAttribute("scrapingConfig");
    if (scrapingConfig != null && scrapingConfig instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> config = (java.util.Map<String, Object>) scrapingConfig;
      return Boolean.TRUE.equals(config.get("useExistingScrapingInfo"));
    }
    return false;
  }

  /**
   * 在目录文件中查找图片文件
   */
  private OpenlistApiService.OpenlistFile findImageInDirectory(
      java.util.List<OpenlistApiService.OpenlistFile> files, String fileName) {
    if (files == null) {
      return null;
    }

    return files.stream()
        .filter(f -> "file".equals(f.getType()))
        .filter(f -> f.getName().equalsIgnoreCase(fileName))
        .findFirst()
        .orElse(null);
  }
}
