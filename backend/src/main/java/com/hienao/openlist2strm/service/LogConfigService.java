/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志配置服务 - Quarkus CDI 版本
 * 
 * 注意：Quarkus 使用 JBoss Logging 和 java.util.logging，
 * 不使用 Logback，因此动态日志级别配置方式不同
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class LogConfigService {

  @Inject
  SystemConfigService systemConfigService;

  /**
   * 应用日志级别配置
   * 根据系统配置动态调整日志级别
   */
  public void applyLogLevelConfig() {
    try {
      Map<String, Object> logConfig = systemConfigService.getLogConfig();
      String logLevel = (String) logConfig.getOrDefault("level", "info");

      Log.info("应用日志级别配置: " + logLevel);

      // 使用 java.util.logging 设置日志级别
      setJulLogLevel(logLevel);

      Log.info("日志级别配置已应用: " + logLevel);

    } catch (Exception e) {
      Log.error("应用日志级别配置失败: " + e.getMessage(), e);
    }
  }

  private void setJulLogLevel(String levelStr) {
    try {
      Level level = parseLogLevel(levelStr);
      Logger rootLogger = Logger.getLogger("");
      rootLogger.setLevel(level);

      // 设置应用包的日志级别
      Logger appLogger = Logger.getLogger("com.hienao.openlist2strm");
      appLogger.setLevel(level);

      Log.debug("日志级别已设置为: " + level);
    } catch (Exception e) {
      Log.error("设置日志级别失败: " + e.getMessage(), e);
    }
  }

  private Level parseLogLevel(String levelStr) {
    if (levelStr == null || levelStr.trim().isEmpty()) {
      return Level.INFO;
    }

    String upperLevelStr = levelStr.trim().toUpperCase();

    return switch (upperLevelStr) {
      case "DEBUG", "TRACE" -> Level.FINE;
      case "INFO" -> Level.INFO;
      case "WARN", "WARNING" -> Level.WARNING;
      case "ERROR" -> Level.SEVERE;
      case "OFF" -> Level.OFF;
      case "ALL" -> Level.ALL;
      default -> {
        Log.warnf("未知的日志级别: " + levelStr + ", 使用默认级别 INFO");
        yield Level.INFO;
      }
    };
  }

  public String getCurrentLogLevel() {
    try {
      Logger rootLogger = Logger.getLogger("");
      Level level = rootLogger.getLevel();
      if (level == null) {
        return "info";
      }
      return switch (level.getName()) {
        case "FINE", "FINER", "FINEST" -> "debug";
        case "INFO" -> "info";
        case "WARNING" -> "warn";
        case "SEVERE" -> "error";
        default -> "info";
      };
    } catch (Exception e) {
      Log.error("获取当前日志级别失败: " + e.getMessage(), e);
      return "info";
    }
  }

  public boolean isValidLogLevel(String levelStr) {
    if (levelStr == null || levelStr.trim().isEmpty()) {
      return false;
    }

    String upperLevelStr = levelStr.trim().toUpperCase();

    return "DEBUG".equals(upperLevelStr)
        || "INFO".equals(upperLevelStr)
        || "WARN".equals(upperLevelStr)
        || "WARNING".equals(upperLevelStr)
        || "ERROR".equals(upperLevelStr)
        || "OFF".equals(upperLevelStr)
        || "ALL".equals(upperLevelStr)
        || "TRACE".equals(upperLevelStr);
  }

  public boolean isValidRetentionDays(Integer retentionDays) {
    if (retentionDays == null) {
      return false;
    }
    return retentionDays == 1
        || retentionDays == 3
        || retentionDays == 5
        || retentionDays == 7
        || retentionDays == 30;
  }
}
