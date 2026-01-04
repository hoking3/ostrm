/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.constant.AppConstants;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

/**
 * 封面图片下载服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class CoverImageService {

  @Inject
  SystemConfigService systemConfigService;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  public String downloadPoster(String posterUrl, String saveDirectory, String fileName) {
    if (posterUrl == null || posterUrl.trim().isEmpty()) {
      Log.warn("海报URL为空，跳过下载");
      return null;
    }

    String posterFileName = fileName + "-poster.jpg";
    String posterFilePath = Paths.get(saveDirectory, posterFileName).toString();

    try {
      downloadImage(posterUrl, posterFilePath);
      Log.info("海报下载成功: " + posterFilePath);
      return posterFilePath;
    } catch (Exception e) {
      Log.error("下载海报失败: " + e.getMessage(), e);
      return null;
    }
  }

  public String downloadBackdrop(String backdropUrl, String saveDirectory, String fileName) {
    if (backdropUrl == null || backdropUrl.trim().isEmpty()) {
      Log.warn("背景图片URL为空，跳过下载");
      return null;
    }

    String backdropFileName = fileName + "-backdrop.jpg";
    String backdropFilePath = Paths.get(saveDirectory, backdropFileName).toString();

    try {
      downloadImage(backdropUrl, backdropFilePath);
      Log.info("背景图片下载成功: " + backdropFilePath);
      return backdropFilePath;
    } catch (Exception e) {
      Log.error("下载背景图片失败: " + e.getMessage(), e);
      return null;
    }
  }

  public String downloadThumb(String thumbUrl, String saveDirectory, String fileName) {
    if (thumbUrl == null || thumbUrl.trim().isEmpty()) {
      Log.warn("缩略图URL为空，跳过下载");
      return null;
    }

    String thumbFileName = fileName + "-thumb.jpg";
    String thumbFilePath = Paths.get(saveDirectory, thumbFileName).toString();

    try {
      downloadImage(thumbUrl, thumbFilePath);
      Log.info("缩略图下载成功: " + thumbFilePath);
      return thumbFilePath;
    } catch (Exception e) {
      Log.error("下载缩略图失败: " + e.getMessage(), e);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void downloadImage(String imageUrl, String saveFilePath) throws IOException {
    Path savePath = Paths.get(saveFilePath);

    Path parentDir = savePath.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }

    if (Files.exists(savePath)) {
      Map<String, Object> scrapingConfig = systemConfigService.getScrapingConfig();
      boolean overwriteExisting = (Boolean) scrapingConfig.getOrDefault("overwriteExisting", false);

      if (!overwriteExisting) {
        Log.info("检测到同名图片文件已存在，跳过下载: " + saveFilePath);
        return;
      } else {
        Log.info("同名图片文件已存在，但允许覆盖，继续下载: " + saveFilePath);
      }
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(imageUrl))
          .header("User-Agent", AppConstants.USER_AGENT)
          .header("Accept", "image/*")
          .timeout(Duration.ofSeconds(60))
          .GET()
          .build();

      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IOException("HTTP请求失败，状态码: " + response.statusCode());
      }

      byte[] imageData = response.body();
      if (imageData == null || imageData.length == 0) {
        throw new IOException("下载的图片数据为空");
      }

      Files.write(savePath, imageData);
      Log.debug("图片下载完成: " + imageUrl + " -> " + saveFilePath);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("下载被中断: " + e.getMessage(), e);
    } catch (Exception e) {
      Log.errorf("下载图片失败: " + imageUrl + " -> " + saveFilePath, e);
      throw new IOException("下载图片失败: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public void downloadImages(
      String posterUrl, String backdropUrl, String saveDirectory, String fileName) {
    Map<String, Object> scrapingConfig = systemConfigService.getScrapingConfig();
    boolean downloadPoster = (Boolean) scrapingConfig.getOrDefault("downloadPoster", true);
    boolean downloadBackdrop = (Boolean) scrapingConfig.getOrDefault("downloadBackdrop", false);

    if (downloadPoster && posterUrl != null && !posterUrl.trim().isEmpty()) {
      downloadPoster(posterUrl, saveDirectory, fileName);
    }

    if (downloadBackdrop && backdropUrl != null && !backdropUrl.trim().isEmpty()) {
      downloadBackdrop(backdropUrl, saveDirectory, fileName);
    }
  }

  public String getStandardizedFileName(String originalFileName) {
    if (originalFileName == null || originalFileName.isEmpty()) {
      return "unknown";
    }

    String nameWithoutExt = originalFileName;
    int lastDotIndex = nameWithoutExt.lastIndexOf('.');
    if (lastDotIndex > 0) {
      nameWithoutExt = nameWithoutExt.substring(0, lastDotIndex);
    }

    return sanitizeFileName(nameWithoutExt);
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return "unknown";
    }
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "").trim();
  }
}
