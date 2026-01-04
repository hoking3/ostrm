/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hienao.openlist2strm.constant.AppConstants;
import com.hienao.openlist2strm.dto.version.GitHubRelease;
import com.hienao.openlist2strm.dto.version.VersionCheckResponse;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * GitHub版本检查服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class GitHubVersionService {

  @Inject
  ObjectMapper objectMapper;

  @ConfigProperty(name = "github.repo.owner", defaultValue = "hienao")
  String repoOwner;

  @ConfigProperty(name = "github.repo.name", defaultValue = "ostrm")
  String repoName;

  @ConfigProperty(name = "github.api.timeout", defaultValue = "30")
  int apiTimeout;

  private static final String GITHUB_API_URL = "https://api.github.com";

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();

  @CacheResult(cacheName = "version-check")
  public VersionCheckResponse checkVersionUpdate(String currentVersion) {
    try {
      Log.info("检查版本更新: " + currentVersion);

      GitHubRelease latestRelease = getLatestRelease();
      if (latestRelease == null) {
        Log.warn("无法获取最新版本信息，返回错误响应");
        return createErrorResponse("无法获取最新版本信息");
      }

      return buildVersionCheckResponse(currentVersion, latestRelease);
    } catch (Exception e) {
      Log.errorf("检查版本更新失败", e);
      return createErrorResponse("检查版本更新失败: " + e.getMessage());
    }
  }

  @CacheResult(cacheName = "github-releases")
  public GitHubRelease getLatestRelease() {
    try {
      String url = String.format("%s/repos/%s/%s/releases", GITHUB_API_URL, repoOwner, repoName);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Accept", "application/vnd.github.v3+json")
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(apiTimeout))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      Log.info("GitHub API响应状态: " + response.statusCode());

      GitHubRelease[] releases = objectMapper.readValue(response.body(), GitHubRelease[].class);

      if (releases == null || releases.length == 0) {
        Log.warn("未找到任何release");
        return null;
      }

      Log.info("获取到 " + releases.length + " 个release");

      List<GitHubRelease> filteredReleases = Arrays.stream(releases)
          .filter(release -> !release.isDraft() && !release.isPrerelease())
          .sorted(Comparator.comparing(GitHubRelease::getPublishedAt).reversed())
          .toList();

      if (filteredReleases.isEmpty()) {
        Log.warn("未找到有效的release (过滤后为空)");
        return null;
      }

      GitHubRelease latestRelease = filteredReleases.get(0);
      Log.info("获取到最新release: " + latestRelease.getTagName());

      return latestRelease;
    } catch (Exception e) {
      Log.errorf("获取GitHub release失败", e);
      return null;
    }
  }

  public int compareVersions(String version1, String version2) {
    try {
      if (version2 == null) {
        return 1;
      }

      String v1 = version1.startsWith("v") ? version1.substring(1) : version1;
      String v2 = version2.startsWith("v") ? version2.substring(1) : version2;

      if (isDevelopmentVersion(v1) && !isDevelopmentVersion(v2)) {
        return -1;
      }
      if (!isDevelopmentVersion(v1) && isDevelopmentVersion(v2)) {
        return 1;
      }
      if (isDevelopmentVersion(v1) && isDevelopmentVersion(v2)) {
        return 0;
      }

      String[] parts1 = v1.split("\\.");
      String[] parts2 = v2.split("\\.");

      int length = Math.max(parts1.length, parts2.length);

      for (int i = 0; i < length; i++) {
        int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
        int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

        if (num1 > num2) {
          return 1;
        } else if (num1 < num2) {
          return -1;
        }
      }

      return 0;
    } catch (Exception e) {
      Log.warn("版本号比较失败: " + version1 + " vs " + version2);
      return 0;
    }
  }

  private boolean isDevelopmentVersion(String version) {
    if (version == null || version.trim().isEmpty()) {
      return true;
    }

    String lowerVersion = version.toLowerCase();
    return lowerVersion.equals("dev")
        || lowerVersion.equals("development")
        || lowerVersion.equals("snapshot")
        || lowerVersion.equals("alpha")
        || lowerVersion.equals("beta")
        || lowerVersion.equals("rc")
        || lowerVersion.contains("-dev")
        || lowerVersion.contains("-snapshot")
        || lowerVersion.contains("-alpha")
        || lowerVersion.contains("-beta")
        || lowerVersion.contains("-rc");
  }

  private VersionCheckResponse buildVersionCheckResponse(
      String currentVersion, GitHubRelease release) {
    VersionCheckResponse response = new VersionCheckResponse();
    response.setCurrentVersion(currentVersion);

    String tagName = release.getTagName();
    response.setLatestVersion(tagName);
    response.setReleaseUrl(release.getHtmlUrl());
    response.setReleaseNotes(release.getBody());
    response.setPrerelease(release.isPrerelease());
    response.setPublishedAt(release.getPublishedAt());
    response.setCheckTime(LocalDateTime.now());

    int comparison = compareVersions(currentVersion, tagName);
    response.setHasUpdate(comparison < 0);

    Log.infof("版本检查结果: 当前=" + currentVersion + ", 最新=" + tagName + ", 有更新=" + response.isHasUpdate());

    return response;
  }

  private VersionCheckResponse createErrorResponse(String error) {
    VersionCheckResponse response = new VersionCheckResponse();
    response.setError(error);
    response.setCheckTime(LocalDateTime.now());
    return response;
  }
}
