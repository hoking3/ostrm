/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.component.LogWebSocketHandler;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 日志文件监控服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class LogFileMonitorService {

  @Inject
  LogWebSocketHandler webSocketHandler;

  @ConfigProperty(name = "quarkus.log.file.path", defaultValue = "./logs/backend.log")
  String logFilePath;

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final ConcurrentHashMap<String, Future<?>> monitorTasks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> lastReadPositions = new ConcurrentHashMap<>();

  private String getActualLogPath() {
    Path path = Paths.get(logFilePath);
    Path parent = path.getParent();
    if (parent != null) {
      return parent.toString();
    }
    return "./logs";
  }

  void onStart(@Observes StartupEvent ev) {
    Log.info("=== 初始化日志文件监控服务 ===");
    Log.info("配置的日志路径: " + logFilePath);
    Log.info("实际日志路径: " + getActualLogPath());
    Log.info("工作目录: " + System.getProperty("user.dir"));

    Path logDir = Paths.get(getActualLogPath());
    Log.info("日志目录: " + logDir.toAbsolutePath() + " (存在: " + Files.exists(logDir) + ")");

    startMonitoring("backend");
    startMonitoring("frontend");
  }

  void onStop(@Observes ShutdownEvent ev) {
    Log.info("关闭日志文件监控服务");
    monitorTasks.values().forEach(task -> task.cancel(true));
    executorService.shutdown();
  }

  public void startMonitoring(String logType) {
    if (monitorTasks.containsKey(logType)) {
      Log.warn("日志类型 " + logType + " 已在监控中");
      return;
    }

    Future<?> task = executorService.submit(() -> monitorLogFile(logType));
    monitorTasks.put(logType, task);
    Log.info("开始监控日志文件: " + logType);
  }

  public void stopMonitoring(String logType) {
    Future<?> task = monitorTasks.remove(logType);
    if (task != null) {
      task.cancel(true);
      Log.info("停止监控日志文件: " + logType);
    }
  }

  private void monitorLogFile(String logType) {
    Path logFilePath = getLogFilePath(logType);

    try {
      Log.info("开始监控日志文件 (轮询): " + logFilePath);

      while (!Files.exists(logFilePath)) {
        Thread.sleep(2000);
        if (Thread.currentThread().isInterrupted()) {
          return;
        }
      }

      try {
        long lastPosition = Files.size(logFilePath);
        lastReadPositions.put(logType, lastPosition);
        Log.infof("日志文件已找到: " + logFilePath + ", 初始大小: " + lastPosition);
      } catch (IOException e) {
        Log.errorf("获取初始文件大小失败: " + logFilePath, e);
      }

      while (!Thread.currentThread().isInterrupted()) {
        try {
          if (Files.exists(logFilePath)) {
            readAndBroadcastNewLines(logType, logFilePath);
          } else {
            lastReadPositions.put(logType, 0L);
            Log.warn("日志文件被删除: " + logFilePath);
          }

          Thread.sleep(500);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          Log.errorf("轮询日志文件异常: " + logType, e);
          Thread.sleep(5000);
        }
      }

    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      Log.errorf("监控日志文件致命错误: " + logType, e);
    }
  }

  private void readAndBroadcastNewLines(String logType, Path logFilePath) {
    try {
      long currentSize = Files.size(logFilePath);
      long lastPosition = lastReadPositions.getOrDefault(logType, 0L);

      if (currentSize < lastPosition) {
        Log.info("日志文件大小变小(轮转?), 重置读取位置: " + lastPosition + " -> 0");
        lastPosition = 0;
      }

      if (currentSize > lastPosition) {
        try (RandomAccessFile file = new RandomAccessFile(logFilePath.toFile(), "r")) {
          file.seek(lastPosition);

          byte[] buffer = new byte[(int) (currentSize - lastPosition)];
          int bytesRead = file.read(buffer);

          if (bytesRead > 0) {
            String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);

            String[] lines = content.split("\r?\n");
            for (int i = 0; i < lines.length; i++) {
              String line = lines[i];

              if (i < lines.length - 1 || content.endsWith("\n") || content.endsWith("\r\n")) {
                if (!line.isEmpty()) {
                  webSocketHandler.broadcastToLogType(logType, line);
                }
              } else {
                byte[] lineBytes = line.getBytes(StandardCharsets.UTF_8);
                file.seek(currentSize - lineBytes.length);
                break;
              }
            }
          }

          lastReadPositions.put(logType, file.getFilePointer());
        }
      }

    } catch (IOException e) {
      Log.errorf("读取日志文件失败: " + logFilePath, e);
    }
  }

  private Path getLogFilePath(String logType) {
    String fileName = switch (logType.toLowerCase()) {
      case "backend" -> "backend.log";
      case "frontend" -> "frontend.log";
      default -> throw new IllegalArgumentException("不支持的日志类型: " + logType);
    };
    return Paths.get(getActualLogPath(), fileName);
  }

  public boolean isMonitoring(String logType) {
    Future<?> task = monitorTasks.get(logType);
    return task != null && !task.isDone() && !task.isCancelled();
  }

  public Map<String, Boolean> getAllMonitoringStatus() {
    Map<String, Boolean> status = new HashMap<>();
    status.put("backend", isMonitoring("backend"));
    status.put("frontend", isMonitoring("frontend"));
    return status;
  }
}
