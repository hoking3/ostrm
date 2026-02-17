package com.hienao.openlist2strm.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 从目录路径中提取影视标题的服务
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DirectoryTitleExtractorService {

  private static final List<String> EXCLUDE_KEYWORDS = Arrays.asList(
      "season", "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9",
      "第", "季", "集", "期", "episode", "ep", "part", "pt",
      "download", "下载", "temp", "临时", "backup", "备份",
      "complete", "完整", "final", "最终", "uncut", "未删减",
      "bluray", "bdrip", "dvdrip", "webrip", "web-dl", "hdtv",
      "1080p", "720p", "480p", "2160p", "4k", "uhd",
      "x264", "x265", "h264", "h265", "hevc", "avc",
      "chinese", "english", "mandarin", "cantonese",
      "简体", "繁体", "中字", "英字", "双语", "国语", "粤语", "原声",
      "movies", "电影", "tv", "电视剧", "综艺", "documentary", "纪录片",
      "video", "视频", "media", "媒体", "my", "我的",
      "new", "新", "old", "旧", "folder", "目录"
  );

  private static final Pattern SEASON_EPISODE_PATTERN = Pattern.compile(
      "(?i)s\\d+|season\\s*\\d+|第\\s*\\d+\\s*季|第\\s*\\d+\\s*集|ep\\s*\\d+|e\\d+",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

  private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[._\\-\\[\\]()]+");

  /**
   * 从目录路径中提取可能的影视标题
   *
   * @param currentPath 当前目录路径
   * @return 标题候选列表，按可能性从高到低排序
   */
  public List<String> extractTitlesFromPath(String currentPath) {
    List<String> candidates = new ArrayList<>();

    if (currentPath == null || currentPath.trim().isEmpty()) {
      return candidates;
    }

    log.info("开始从路径提取标题: {}", currentPath);

    String[] segments = currentPath.split("/");

    for (int i = segments.length - 1; i >= 0; i--) {
      String segment = segments[i].trim();
      if (segment.isEmpty()) {
        continue;
      }

      String cleanedTitle = cleanAndValidateTitle(segment);
      if (cleanedTitle != null && !cleanedTitle.isEmpty()) {
        if (!candidates.contains(cleanedTitle)) {
          candidates.add(cleanedTitle);
          log.debug("找到候选标题: {} (来自目录段: {})", cleanedTitle, segment);
        }
      }
    }

    log.info("提取到 {} 个标题候选: {}", candidates.size(), candidates);
    return candidates;
  }

  /**
   * 清理并验证目录名称是否可能是影视标题
   *
   * @param dirName 目录名称
   * @return 清理后的标题，如果不是有效标题则返回 null
   */
  private String cleanAndValidateTitle(String dirName) {
    String originalName = dirName.toLowerCase();

    for (String exclude : EXCLUDE_KEYWORDS) {
      if (originalName.equals(exclude) || originalName.contains(exclude + " ") || 
          originalName.contains(" " + exclude)) {
        log.debug("目录名包含排除关键词: {}", dirName);
        return null;
      }
    }

    if (SEASON_EPISODE_PATTERN.matcher(originalName).find()) {
      log.debug("目录名是季/集标识: {}", dirName);
      return null;
    }

    String cleaned = dirName;
    cleaned = SPECIAL_CHARS_PATTERN.matcher(cleaned).replaceAll(" ");
    cleaned = YEAR_PATTERN.matcher(cleaned).replaceAll("");
    cleaned = cleaned.trim();
    cleaned = cleaned.replaceAll("\\s+", " ");

    if (cleaned.length() < 2 || cleaned.length() > 100) {
      log.debug("清理后标题长度不符合要求: {}", cleaned);
      return null;
    }

    return cleaned;
  }

  /**
   * 获取最佳标题（第一个候选）
   *
   * @param currentPath 当前目录路径
   * @return 最佳标题，如果没有则返回 null
   */
  public String getBestTitle(String currentPath) {
    List<String> candidates = extractTitlesFromPath(currentPath);
    return candidates.isEmpty() ? null : candidates.get(0);
  }
}
