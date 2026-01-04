/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hienao.openlist2strm.config.PathConfiguration;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class SystemConfigService {

  @Inject
  ObjectMapper objectMapper;

  @Inject
  PathConfiguration pathConfiguration;

  private static final String CONFIG_FILE = "systemconf.json";

  private String getConfigDirectoryPath() {
    return pathConfiguration.config();
  }

  private String getConfigFilePath() {
    return getConfigDirectoryPath() + "/" + CONFIG_FILE;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getSystemConfig() {
    try {
      createConfigDirectoryIfNotExists();

      File configFile = new File(getConfigFilePath());
      Map<String, Object> result;
      boolean needSave = false;

      if (!configFile.exists()) {
        Log.info("系统配置文件不存在，创建默认配置: " + getConfigFilePath());
        result = getDefaultConfig();
        needSave = true;
      } else {
        String content = Files.readString(Paths.get(getConfigFilePath()));
        if (content.trim().isEmpty()) {
          result = getDefaultConfig();
          needSave = true;
        } else {
          Map<String, Object> config = objectMapper.readValue(content, Map.class);
          result = getDefaultConfig();
          result.putAll(config);

          if (!config.containsKey("mediaExtensions") || !config.containsKey("tmdb")
              || !config.containsKey("scraping") || !config.containsKey("scrapingRegex")
              || !config.containsKey("log")) {
            needSave = true;
          }
        }
      }

      if (needSave) {
        saveSystemConfigInternal(result);
      }

      return result;
    } catch (Exception e) {
      Log.errorf("读取系统配置失败", e);
      return getDefaultConfig();
    }
  }

  public void saveSystemConfig(Map<String, Object> config) {
    try {
      createConfigDirectoryIfNotExists();
      Map<String, Object> existingConfig = getSystemConfig();
      existingConfig.putAll(config);
      saveSystemConfigInternal(existingConfig);
      Log.info("系统配置已保存到: " + getConfigFilePath());
    } catch (Exception e) {
      Log.errorf("保存系统配置失败", e);
      throw new RuntimeException("保存系统配置失败", e);
    }
  }

  private void saveSystemConfigInternal(Map<String, Object> config) throws Exception {
    String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
    Files.writeString(Paths.get(getConfigFilePath()), jsonContent);
  }

  private Map<String, Object> getDefaultConfig() {
    Map<String, Object> defaultConfig = new HashMap<>();

    defaultConfig.put(
        "mediaExtensions",
        List.of(
            ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp", ".3g2", ".asf",
            ".divx", ".f4v", ".m2ts", ".m2v", ".mts", ".ogv", ".rm", ".rmvb", ".ts", ".vob",
            ".xvid"));

    Map<String, Object> tmdbConfig = new HashMap<>();
    tmdbConfig.put("apiKey", "");
    tmdbConfig.put("baseUrl", "https://api.themoviedb.org/3");
    tmdbConfig.put("imageBaseUrl", "https://image.tmdb.org/t/p");
    tmdbConfig.put("language", "zh-CN");
    tmdbConfig.put("region", "CN");
    tmdbConfig.put("timeout", 30);
    tmdbConfig.put("retryCount", 3);
    tmdbConfig.put("posterSize", "w500");
    tmdbConfig.put("backdropSize", "w1280");
    tmdbConfig.put("proxyHost", "");
    tmdbConfig.put("proxyPort", "");
    defaultConfig.put("tmdb", tmdbConfig);

    Map<String, Object> scrapConfig = new HashMap<>();
    scrapConfig.put("enabled", true);
    scrapConfig.put("generateNfo", true);
    scrapConfig.put("downloadPoster", true);
    scrapConfig.put("downloadBackdrop", false);
    scrapConfig.put("nfoFormat", "kodi");
    scrapConfig.put("keepSubtitleFiles", false);
    scrapConfig.put("useExistingScrapingInfo", false);
    scrapConfig.put("overwriteExisting", false);
    defaultConfig.put("scraping", scrapConfig);

    Map<String, Object> aiConfig = new HashMap<>();
    aiConfig.put("enabled", false);
    aiConfig.put("baseUrl", "https://api.openai.com/v1");
    aiConfig.put("apiKey", "");
    aiConfig.put("model", "gpt-3.5-turbo");
    aiConfig.put("qpmLimit", 60);
    aiConfig.put("prompt", getDefaultAiPrompt());
    defaultConfig.put("ai", aiConfig);

    Map<String, Object> scrapingRegexConfig = new HashMap<>();
    scrapingRegexConfig.put("movieRegexps",
        com.hienao.openlist2strm.util.EnhancedRegexPatterns.getEnhancedMovieRegexps());
    scrapingRegexConfig.put("tvDirRegexps",
        com.hienao.openlist2strm.util.EnhancedRegexPatterns.getEnhancedTvDirRegexps());
    scrapingRegexConfig.put("tvFileRegexps",
        com.hienao.openlist2strm.util.EnhancedRegexPatterns.getEnhancedTvFileRegexps());
    defaultConfig.put("scrapingRegex", scrapingRegexConfig);

    Map<String, Object> logConfig = new HashMap<>();
    logConfig.put("retentionDays", 7);
    logConfig.put("level", "info");
    logConfig.put("reportUsageData", true);
    defaultConfig.put("log", logConfig);

    return defaultConfig;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getTmdbConfig() {
    Map<String, Object> systemConfig = getSystemConfig();
    return (Map<String, Object>) systemConfig.getOrDefault("tmdb", new HashMap<>());
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getScrapingConfig() {
    Map<String, Object> systemConfig = getSystemConfig();
    return (Map<String, Object>) systemConfig.getOrDefault("scraping", new HashMap<>());
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getAiConfig() {
    Map<String, Object> systemConfig = getSystemConfig();
    return (Map<String, Object>) systemConfig.getOrDefault("ai", new HashMap<>());
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getScrapingRegexConfig() {
    Map<String, Object> systemConfig = getSystemConfig();
    return (Map<String, Object>) systemConfig.getOrDefault("scrapingRegex", new HashMap<>());
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getLogConfig() {
    Map<String, Object> systemConfig = getSystemConfig();
    return (Map<String, Object>) systemConfig.getOrDefault("log", new HashMap<>());
  }

  public boolean isDataReportEnabled() {
    try {
      Map<String, Object> logConfig = getLogConfig();
      Object reportUsageData = logConfig.get("reportUsageData");
      return reportUsageData == null || Boolean.TRUE.equals(reportUsageData);
    } catch (Exception e) {
      Log.warn("获取数据上报配置失败，默认禁用上报: " + e.getMessage());
      return false;
    }
  }

  public String getDefaultAiPrompt() {
    return """
        你是一个专业的影视文件名标准化工具。你的任务是将给定的文件名解析为结构化的媒体信息，以便进行 TMDB 匹配。

        输入：文件名或目录路径（可能包含杂乱字符、非标准命名等）

        输出要求：必须返回有效的 JSON 格式，推荐使用新格式（分离字段），但也支持旧格式兼容。

        === 新格式（推荐）===
        {
          "success": true/false,
          "title": "媒体标题（不含年份、季集信息）",
          "year": "年份（字符串格式，如'2010'）",
          "season": 季数（数字，仅电视剧），
          "episode": 集数（数字，仅电视剧），
          "type": "movie/tv/unknown",
          "reason": "失败原因（仅在 success 为 false 时提供）"
        }

        重要：
        - 必须返回有效的 JSON 格式
        - 不要添加任何 JSON 之外的文字
        - 确保 JSON 格式正确，可以被解析
        """;
  }

  public boolean validateTmdbApiKey(String apiKey) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      return false;
    }
    return apiKey.trim().length() >= 32;
  }

  private void createConfigDirectoryIfNotExists() {
    try {
      Path configDir = Paths.get(getConfigDirectoryPath());
      if (!Files.exists(configDir)) {
        Files.createDirectories(configDir);
        Log.info("创建配置目录: " + getConfigDirectoryPath());
      }
    } catch (IOException e) {
      Log.error("创建配置目录失败: " + getConfigDirectoryPath(), e);
      throw new RuntimeException("创建配置目录失败", e);
    }
  }
}
