/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.FrontendLogRequest;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 日志服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class LogService {

  @ConfigProperty(name = "quarkus.log.file.path", defaultValue = "./logs/backend.log")
  String logFilePath;

  void onStart(@Observes StartupEvent ev) {
    Log.info("=== LogService 初始化 ===");
    Log.info("配置的日志路径: " + getActualLogPath());
    Log.info("工作目录: " + System.getProperty("user.dir"));
    Log.info("支持的日志类型: " + getSupportedLogTypes());
  }

  private String getActualLogPath() {
    // 从日志文件路径提取目录
    Path path = Paths.get(logFilePath);
    Path parent = path.getParent();
    if (parent != null) {
      return parent.toString();
    }
    return "./logs";
  }

  private static final Map<String, String> LOG_FILE_MAPPING = Map.of(
      "backend", "backend.log",
      "frontend", "frontend.log");

  private Path getLogFilePath(String logType) {
    String fileName = LOG_FILE_MAPPING.get(logType.toLowerCase());
    if (fileName == null) {
      throw new IllegalArgumentException("不支持的日志类型: " + logType);
    }

    return Paths.get(getActualLogPath(), fileName);
  }

  public List<String> getLogLines(String logType, int maxLines) {
    Path logFile = getLogFilePath(logType);

    if (!Files.exists(logFile)) {
      Log.warn("日志文件不存在: " + logFile);
      return Collections.emptyList();
    }

    try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile.toFile(), StandardCharsets.UTF_8)) {

      LinkedList<String> lines = new LinkedList<>();
      String line;
      int count = 0;

      while ((line = reader.readLine()) != null && count < maxLines) {
        lines.addFirst(line);
        count++;
      }

      return lines;

    } catch (IOException e) {
      Log.errorf("读取日志文件失败: " + logFile, e);
      throw new RuntimeException("读取日志文件失败", e);
    }
  }

  public InputStream getLogFileAsStream(String logType) {
    Path logFile = getLogFilePath(logType);

    if (!Files.exists(logFile)) {
      Log.warn("日志文件不存在: " + logFile);
      return null;
    }

    try {
      return Files.newInputStream(logFile);
    } catch (IOException e) {
      Log.errorf("获取日志文件流失败: " + logFile, e);
      return null;
    }
  }

  public Map<String, Object> getLogStats(String logType) {
    Path logFile = getLogFilePath(logType);
    Map<String, Object> stats = new HashMap<>();

    if (!Files.exists(logFile)) {
      stats.put("exists", false);
      stats.put("totalLines", 0);
      stats.put("fileSize", 0);
      stats.put("lastModified", null);
      return stats;
    }

    try {
      stats.put("exists", true);
      stats.put("fileSize", Files.size(logFile));
      stats.put("lastModified", Files.getLastModifiedTime(logFile).toString());

      List<String> lines = Files.readAllLines(logFile);
      stats.put("totalLines", lines.size());

      long errorCount = lines.stream()
          .filter(
              line -> line.toLowerCase().contains("error")
                  || line.toLowerCase().contains("exception")
                  || line.toLowerCase().contains("failed"))
          .count();

      long warnCount = lines.stream()
          .filter(
              line -> line.toLowerCase().contains("warn") || line.toLowerCase().contains("warning"))
          .count();

      long infoCount = lines.stream().filter(line -> line.toLowerCase().contains("info")).count();
      long debugCount = lines.stream().filter(line -> line.toLowerCase().contains("debug")).count();

      stats.put("errorCount", errorCount);
      stats.put("warnCount", warnCount);
      stats.put("infoCount", infoCount);
      stats.put("debugCount", debugCount);

      int previewLines = Math.min(10, lines.size());
      if (previewLines > 0) {
        List<String> recentLines = lines.subList(lines.size() - previewLines, lines.size());
        stats.put("recentLines", recentLines);
      } else {
        stats.put("recentLines", Collections.emptyList());
      }

    } catch (IOException e) {
      Log.errorf("获取日志统计信息失败: " + logFile, e);
      throw new RuntimeException("获取日志统计信息失败", e);
    }

    return stats;
  }

  public Set<String> getSupportedLogTypes() {
    return LOG_FILE_MAPPING.keySet();
  }

  public void processFrontendLogs(FrontendLogRequest request) {
    if (request == null || request.getLogs() == null || request.getLogs().isEmpty()) {
      Log.warn("接收到空的前端日志请求");
      return;
    }

    Path frontendLogFile = getLogFilePath("frontend");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    try {
      Files.createDirectories(frontendLogFile.getParent());

      List<String> logLines = new ArrayList<>();
      for (FrontendLogRequest.LogEntry entry : request.getLogs()) {
        Instant instant = (entry.getTimestamp() != null)
            ? Instant.ofEpochMilli(entry.getTimestamp())
            : Instant.now();
        String timestamp = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter);

        StringBuilder logLine = new StringBuilder();
        logLine.append(timestamp).append(" [FRONTEND]");

        if (entry.getLevel() != null) {
          logLine.append(" ").append(entry.getLevel().toUpperCase());
        }

        if (entry.getUserId() != null) {
          logLine.append(" [User:").append(entry.getUserId()).append("]");
        }

        if (entry.getUrl() != null) {
          logLine.append(" [URL:").append(entry.getUrl()).append("]");
        }

        logLine.append(" - ").append(entry.getMessage());

        if (entry.getExtra() != null) {
          logLine.append(" [Extra:").append(entry.getExtra().toString()).append("]");
        }

        logLines.add(logLine.toString());
      }

      Files.write(frontendLogFile, logLines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      Log.debug("成功写入 " + logLines.size() + " 条前端日志到文件: " + frontendLogFile);

    } catch (Exception e) {
      Log.errorf("写入前端日志失败: " + frontendLogFile, e);
      throw new RuntimeException("写入前端日志失败", e);
    }
  }
}
