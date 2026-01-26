package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import java.util.Set;

/**
 * 文件处理器接口
 *
 * <p>定义了文件处理管道中每个处理器的通用行为。
 * 所有的文件处理器都需要实现此接口。</p>
 *
 * @author hienao
 * @since 2024-01-01
 */
public interface FileProcessorHandler {

  /**
   * 处理文件
   *
   * @param context 处理上下文，包含当前处理的文件信息和配置
   * @return 处理结果
   */
  ProcessingResult process(FileProcessingContext context);

  /**
   * 获取处理器执行顺序
   *
   * <p>数值越小越先执行。
   * 建议的顺序值：</p>
   * <ul>
   *   <li>10-19: 文件发现 (FileDiscoveryHandler)</li>
   *   <li>20-29: 文件过滤 (FileFilterHandler)</li>
   *   <li>30-39: STRM生成 (StrmGenerationHandler)</li>
   *   <li>40-49: 文件下载 (NfoDownloadHandler, ImageDownloadHandler, SubtitleCopyHandler)</li>
   *   <li>50-59: 媒体刮削 (MediaScrapingHandler)</li>
   *   <li>60-69: 清理 (OrphanCleanupHandler)</li>
   * </ul>
   *
   * @return 执行顺序值
   */
  default int getOrder() {
    return 0;
  }

  /**
   * 获取处理器支持的文件类型
   *
   * @return 支持的文件类型集合
   */
  default Set<FileType> getHandledTypes() {
    return Set.of(FileType.ALL);
  }
}
