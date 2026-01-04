/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.DataReportRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 数据上报服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class DataReportService {

  @Inject
  SystemConfigService systemConfigService;

  @Inject
  SignService signService;

  @Inject
  ObjectMapper objectMapper;

  private static final String POSTHOG_API_URL = "https://us.i.posthog.com/capture/";

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();

  public void reportEvent(String event, Map<String, Object> customProperties) {
    try {
      if (!isDataReportEnabled()) {
        Log.debug("数据上报已禁用，跳过事件上报: " + event);
        return;
      }

      Map<String, Object> properties = buildEventProperties(customProperties);
      DataReportRequest request = new DataReportRequest(event, properties);
      sendReportRequest(request);

      Log.debug("事件数据上报成功: " + event);
    } catch (Exception e) {
      Log.warnf("事件数据上报失败: " + event + ", 错误: " + e.getMessage());
    }
  }

  public void reportEvent(String event) {
    reportEvent(event, null);
  }

  private boolean isDataReportEnabled() {
    return systemConfigService.isDataReportEnabled();
  }

  private Map<String, Object> buildEventProperties(Map<String, Object> customProperties) {
    Map<String, Object> properties = new HashMap<>();

    properties.put("distinct_id", getContainerInstanceId());

    Map<String, String> imageInfo = getContainerImage();
    properties.put("image", imageInfo.get("image"));
    properties.put("image_version", imageInfo.get("version"));

    if (customProperties != null) {
      for (Map.Entry<String, Object> entry : customProperties.entrySet()) {
        String key = entry.getKey();
        if (!"distinct_id".equals(key) && !"image".equals(key) && !"version".equals(key)) {
          properties.put(key, entry.getValue());
        }
      }
    }

    return properties;
  }

  private String getContainerInstanceId() {
    try {
      String containerId = signService.getContainerInstanceId();
      if (containerId != null && !containerId.trim().isEmpty()) {
        return containerId;
      }
      return "unknown-instance";
    } catch (Exception e) {
      Log.warn("获取容器实例ID失败，使用默认值: " + e.getMessage());
      return "unknown-instance";
    }
  }

  private Map<String, String> getContainerImage() {
    Map<String, String> result = new HashMap<>();
    result.put("image", "ostrm");

    String version = System.getenv("APP_VERSION");
    if (version == null || version.trim().isEmpty()) {
      version = "dev";
    }
    result.put("version", version);

    return result;
  }

  private void sendReportRequest(DataReportRequest request) {
    try {
      String jsonBody = objectMapper.writeValueAsString(request);

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(POSTHOG_API_URL))
          .header("Content-Type", "application/json")
          .timeout(Duration.ofSeconds(30))
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        Log.warn("数据上报请求失败，状态码: " + response.statusCode());
      }
    } catch (Exception e) {
      Log.warn("发送数据上报请求异常: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
