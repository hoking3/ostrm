/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.entity.OpenlistConfig;
import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.exception.BusinessException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 任务执行服务类 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class TaskExecutionService {

  @Inject
  TaskConfigService taskConfigService;

  @Inject
  OpenlistConfigService openlistConfigService;

  @Inject
  OpenlistApiService openlistApiService;

  @Inject
  StrmFileService strmFileService;

  @Inject
  MediaScrapingService mediaScrapingService;

  // 使用自定义线程池
  private final ExecutorService taskSubmitExecutor = Executors.newFixedThreadPool(4);

  /**
   * 提交任务到线程池执行
   *
   * @param taskId      任务ID
   * @param isIncrement 是否增量执行（可选参数）
   */
  public void submitTask(Long taskId, Boolean isIncrement) {
    Log.infof("提交任务到线程池 - 任务ID: " + taskId + ", 增量模式: " + isIncrement);

    // 使用线程池异步执行任务
    taskSubmitExecutor.execute(
        () -> {
          try {
            executeTaskSync(taskId, isIncrement);
          } catch (Exception e) {
            Log.errorf("任务执行失败 - 任务ID: " + taskId + ", 错误信息: " + e.getMessage(), e);
          }
        });

    Log.info("任务已成功提交到线程池 - 任务ID: " + taskId);
  }

  /**
   * 同步执行任务（在线程池中调用）
   *
   * @param taskId      任务ID
   * @param isIncrement 是否增量执行（可选参数）
   */
  private void executeTaskSync(Long taskId, Boolean isIncrement) {
    try {
      Log.info(
          "开始执行任务 - 任务ID: " + taskId + ", 增量模式: " + isIncrement + ", 线程: "
              + Thread.currentThread().getName());

      // 获取任务配置
      TaskConfig taskConfig = taskConfigService.getById(taskId);
      if (taskConfig == null) {
        throw new BusinessException("任务配置不存在，ID: " + taskId);
      }

      // 检查任务是否启用
      if (!Boolean.TRUE.equals(taskConfig.getIsActive())) {
        throw new BusinessException("任务已禁用，无法执行，ID: " + taskId);
      }

      // 确定是否使用增量模式
      boolean useIncrement;
      if (isIncrement != null) {
        // 如果传了参数，以传参为主
        useIncrement = isIncrement;
        Log.info("使用传入的增量参数: " + isIncrement);
      } else {
        // 如果没传参数，以任务配置为主
        useIncrement = Boolean.TRUE.equals(taskConfig.getIsIncrement());
        Log.info("使用任务配置的增量参数: " + useIncrement);
      }

      // 更新任务开始执行时间
      taskConfigService.updateLastExecTime(taskId, LocalDateTime.now());

      // 执行具体的任务逻辑
      executeTaskLogic(taskConfig, useIncrement);

      Log.info(
          "任务执行完成 - 任务ID: " + taskId + ", 任务名称: " + taskConfig.getTaskName() + ", 增量模式: "
              + useIncrement);

    } catch (Exception e) {
      Log.errorf("任务执行失败 - 任务ID: " + taskId + ", 错误信息: " + e.getMessage(), e);
      throw new BusinessException("任务执行失败: " + e.getMessage(), e);
    }
  }

  /**
   * 异步执行任务（保留原有方法以兼容其他调用）
   *
   * @param taskId      任务ID
   * @param isIncrement 是否增量执行（可选参数）
   * @return CompletableFuture<Void>
   */
  public CompletableFuture<Void> executeTask(Long taskId, Boolean isIncrement) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            Log.info(
                "开始执行任务 - 任务ID: " + taskId + ", 增量模式: " + isIncrement + ", 线程: "
                    + Thread.currentThread().getName());

            // 获取任务配置
            TaskConfig taskConfig = taskConfigService.getById(taskId);
            if (taskConfig == null) {
              throw new BusinessException("任务配置不存在，ID: " + taskId);
            }

            // 检查任务是否启用
            if (!Boolean.TRUE.equals(taskConfig.getIsActive())) {
              throw new BusinessException("任务已禁用，无法执行，ID: " + taskId);
            }

            // 确定是否使用增量模式
            boolean useIncrement;
            if (isIncrement != null) {
              useIncrement = isIncrement;
              Log.info("使用传入的增量参数: " + isIncrement);
            } else {
              useIncrement = Boolean.TRUE.equals(taskConfig.getIsIncrement());
              Log.info("使用任务配置的增量参数: " + useIncrement);
            }

            // 更新任务开始执行时间
            taskConfigService.updateLastExecTime(taskId, LocalDateTime.now());

            // 执行具体的任务逻辑
            executeTaskLogic(taskConfig, useIncrement);

            Log.info(
                "任务执行完成 - 任务ID: " + taskId + ", 任务名称: " + taskConfig.getTaskName() + ", 增量模式: "
                    + useIncrement);

          } catch (Exception e) {
            Log.errorf("任务执行失败 - 任务ID: " + taskId + ", 错误信息: " + e.getMessage(), e);
            throw new BusinessException("任务执行失败: " + e.getMessage(), e);
          }
        },
        taskSubmitExecutor);
  }

  /**
   * 执行具体的任务逻辑
   *
   * @param taskConfig  任务配置
   * @param isIncrement 是否增量执行
   */
  private void executeTaskLogic(TaskConfig taskConfig, boolean isIncrement) {
    Log.info("开始执行任务逻辑: " + taskConfig.getTaskName() + ", 增量模式: " + isIncrement);

    try {
      // 1. 获取OpenList配置
      OpenlistConfig openlistConfig = getOpenlistConfig(taskConfig);

      // 2. 如果是全量执行，先清空STRM目录
      if (!isIncrement) {
        Log.info("全量执行模式，开始清理STRM目录: " + taskConfig.getStrmPath());
        strmFileService.clearStrmDirectory(taskConfig.getStrmPath());
      }

      // 3. 使用内存优化的文件处理方式
      Log.info("开始处理文件，使用内存优化策略");

      boolean needScrap = Boolean.TRUE.equals(taskConfig.getNeedScrap());

      List<OpenlistApiService.OpenlistFile> allFiles = processFilesWithMemoryOptimization(openlistConfig, taskConfig,
          isIncrement, needScrap);

      Log.info("处理完成，共处理 " + allFiles.size() + " 个文件/目录");

      // 4. 过滤并处理视频文件
      int processedCount = 0;
      int scrapSkippedCount = 0;

      for (OpenlistApiService.OpenlistFile file : allFiles) {
        if ("file".equals(file.getType()) && strmFileService.isVideoFile(file.getName())) {
          try {
            String relativePath = strmFileService.calculateRelativePath(taskConfig.getPath(), file.getPath());
            String fileUrlWithSign = buildFileUrlWithSign(file.getUrl(), file.getSign());

            strmFileService.generateStrmFile(
                taskConfig.getStrmPath(),
                relativePath,
                file.getName(),
                fileUrlWithSign,
                isIncrement,
                taskConfig.getRenameRegex(),
                openlistConfig);

            if (needScrap) {
              try {
                String saveDirectory = buildScrapSaveDirectory(taskConfig.getStrmPath(), relativePath);

                boolean needScrapFile = needScrapFile(
                    file.getName(),
                    taskConfig.getRenameRegex(),
                    taskConfig.getStrmPath(),
                    relativePath,
                    isIncrement);

                if (needScrapFile) {
                  if (isIncrement && mediaScrapingService.isDirectoryFullyScraped(saveDirectory)) {
                    Log.debug("目录已完全刮削，跳过: " + saveDirectory);
                    scrapSkippedCount++;
                  } else {
                    String currentDirectory = file.getPath().substring(0, file.getPath().lastIndexOf('/') + 1);
                    List<OpenlistApiService.OpenlistFile> currentDirFiles = allFiles.stream()
                        .filter(
                            f -> f.getPath().startsWith(currentDirectory)
                                && f.getPath()
                                    .substring(currentDirectory.length())
                                    .indexOf('/') == -1)
                        .collect(Collectors.toList());

                    mediaScrapingService.scrapMedia(
                        openlistConfig,
                        file.getName(),
                        taskConfig.getStrmPath(),
                        relativePath,
                        currentDirFiles,
                        file.getPath());
                  }
                } else {
                  Log.debug("NFO文件已存在，跳过刮削: " + file.getName());
                  scrapSkippedCount++;
                }
              } catch (Exception scrapException) {
                Log.error(
                    "刮削文件失败: " + file.getName() + ", 错误: " + scrapException.getMessage(),
                    scrapException);
              }
            }

            processedCount++;

          } catch (Exception e) {
            Log.error("处理文件失败: " + file.getName() + ", 错误: " + e.getMessage(), e);
          }
        }
      }

      if (needScrap && scrapSkippedCount > 0) {
        Log.info("跳过了 " + scrapSkippedCount + " 个已刮削目录中的文件");
      }

      // 5. 如果是增量执行，清理孤立的STRM文件
      if (isIncrement) {
        Log.info("增量执行模式，开始清理孤立的STRM文件");
        int cleanedCount = strmFileService.cleanOrphanedStrmFiles(
            taskConfig.getStrmPath(),
            allFiles,
            taskConfig.getPath(),
            taskConfig.getRenameRegex(),
            openlistConfig);
        Log.info("清理了 " + cleanedCount + " 个孤立的STRM文件");
      }

      Log.info(
          "任务执行完成: " + taskConfig.getTaskName() + ", 处理了 " + processedCount + " 个视频文件");

    } catch (Exception e) {
      Log.error("任务执行失败: " + taskConfig.getTaskName() + ", 错误: " + e.getMessage(), e);
      throw new BusinessException("任务执行失败: " + e.getMessage(), e);
    }
  }

  private OpenlistConfig getOpenlistConfig(TaskConfig taskConfig) {
    if (taskConfig.getOpenlistConfigId() == null) {
      throw new BusinessException("任务配置中未指定OpenList配置ID");
    }

    OpenlistConfig openlistConfig = openlistConfigService.getById(taskConfig.getOpenlistConfigId());
    if (openlistConfig == null) {
      throw new BusinessException("OpenList配置不存在，ID: " + taskConfig.getOpenlistConfigId());
    }

    if (!Boolean.TRUE.equals(openlistConfig.getIsActive())) {
      throw new BusinessException("OpenList配置已禁用，ID: " + taskConfig.getOpenlistConfigId());
    }

    return openlistConfig;
  }

  private String buildFileUrlWithSign(String originalUrl, String sign) {
    if (originalUrl == null) {
      return null;
    }

    String processedUrl = originalUrl;

    if (sign != null && !sign.trim().isEmpty()) {
      String separator = processedUrl.contains("?") ? "&" : "?";
      processedUrl = processedUrl + separator + "sign=" + sign;
    }

    return processedUrl;
  }

  public String processUrlWithBaseUrlReplacement(String originalUrl, OpenlistConfig openlistConfig) {
    if (originalUrl == null || openlistConfig == null) {
      return originalUrl;
    }

    if (openlistConfig.getStrmBaseUrl() == null
        || openlistConfig.getStrmBaseUrl().trim().isEmpty()) {
      Log.debug("未配置strmBaseUrl，直接使用原始URL: " + originalUrl);
      return originalUrl;
    }

    try {
      java.net.URL url = new java.net.URL(originalUrl);
      String path = url.getPath();
      String query = url.getQuery();
      String ref = url.getRef();

      String newBaseUrl = openlistConfig.getStrmBaseUrl();
      if (!newBaseUrl.endsWith("/")) {
        newBaseUrl += "/";
      }

      if (path.startsWith("/")) {
        path = path.substring(1);
      }

      String newUrl = newBaseUrl + path;

      if (query != null && !query.isEmpty()) {
        newUrl += "?" + query;
      }

      if (ref != null && !ref.isEmpty()) {
        newUrl += "#" + ref;
      }

      Log.info("URL替换: " + originalUrl + " -> " + newUrl);
      return newUrl;

    } catch (Exception e) {
      Log.warnf("URL替换失败，使用原始URL: " + originalUrl + ", 错误: " + e.getMessage());
      return originalUrl;
    }
  }

  private String buildScrapSaveDirectory(String strmDirectory, String relativePath) {
    if (relativePath == null || relativePath.trim().isEmpty()) {
      return strmDirectory;
    }

    String directoryPath = relativePath;
    int lastSlashIndex = relativePath.lastIndexOf('/');
    if (lastSlashIndex > 0) {
      directoryPath = relativePath.substring(0, lastSlashIndex);
    } else if (lastSlashIndex == 0) {
      directoryPath = "";
    }

    if (directoryPath.isEmpty()) {
      return strmDirectory;
    }

    return strmDirectory + "/" + directoryPath;
  }

  private List<OpenlistApiService.OpenlistFile> processFilesWithMemoryOptimization(
      OpenlistConfig openlistConfig,
      TaskConfig taskConfig,
      boolean isIncrement,
      boolean needScrap) {

    List<OpenlistApiService.OpenlistFile> allFiles = new ArrayList<>();

    try {
      processDirectoryBatch(
          openlistConfig, taskConfig.getPath(), taskConfig, isIncrement, needScrap, allFiles);

    } catch (Exception e) {
      Log.error("内存优化文件处理失败: " + e.getMessage(), e);
      Log.info("降级使用原始文件处理方法");
      allFiles = openlistApiService.getAllFilesRecursively(openlistConfig, taskConfig.getPath());
    }

    return allFiles;
  }

  private void processDirectoryBatch(
      OpenlistConfig openlistConfig,
      String path,
      TaskConfig taskConfig,
      boolean isIncrement,
      boolean needScrap,
      List<OpenlistApiService.OpenlistFile> allFiles) {

    try {
      List<OpenlistApiService.OpenlistFile> files = openlistApiService.getDirectoryContents(openlistConfig, path);

      for (OpenlistApiService.OpenlistFile file : files) {
        allFiles.add(file);

        if ("folder".equals(file.getType())) {
          String subPath = file.getPath();
          if (subPath == null || subPath.isEmpty()) {
            subPath = path + "/" + file.getName();
          }
          processDirectoryBatch(
              openlistConfig, subPath, taskConfig, isIncrement, needScrap, allFiles);
        }
      }

    } catch (Exception e) {
      Log.errorf("处理目录失败: " + path + ", 错误: " + e.getMessage(), e);
    }
  }

  private boolean needScrapFile(
      String fileName,
      String renameRegex,
      String strmPath,
      String relativePath,
      boolean isIncrement) {
    if (!isIncrement) {
      return true;
    }

    try {
      String finalFileName = processFileNameForScraping(fileName, renameRegex);
      java.nio.file.Path strmFilePath = strmFileService.buildStrmFilePath(strmPath, relativePath, finalFileName);

      java.nio.file.Path nfoFilePath = strmFilePath.resolveSibling(
          strmFilePath.getFileName().toString().replace(".strm", ".nfo"));

      return !java.nio.file.Files.exists(nfoFilePath);
    } catch (Exception e) {
      Log.warn("检查NFO文件是否存在时发生错误: " + e.getMessage() + ", 默认进行刮削");
      return true;
    }
  }

  private String processFileNameForScraping(String originalFileName, String renameRegex) {
    String processedName = originalFileName;

    if (renameRegex != null && !renameRegex.trim().isEmpty()) {
      try {
        if (renameRegex.contains("|")) {
          String[] parts = renameRegex.split("\\|", 2);
          String pattern = parts[0];
          String replacement = parts[1];
          processedName = processedName.replaceAll(pattern, replacement);
          Log.debug("文件重命名: " + originalFileName + " -> " + processedName);
        }
      } catch (Exception e) {
        Log.warnf("重命名规则应用失败: " + renameRegex + ", 使用原始文件名");
      }
    }

    int lastDotIndex = processedName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      processedName = processedName.substring(0, lastDotIndex);
    }
    processedName += ".strm";

    return processedName;
  }
}
