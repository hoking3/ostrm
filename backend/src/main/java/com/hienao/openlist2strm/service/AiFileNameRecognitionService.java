/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hienao.openlist2strm.dto.media.AiRecognitionResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 文件名识别服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class AiFileNameRecognitionService {

  @Inject
  SystemConfigService systemConfigService;

  @Inject
  ObjectMapper objectMapper;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(60))
      .build();

  private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> lastResetTimes = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public AiRecognitionResult recognizeFileName(String originalFileName, String directoryPath) {
    try {
      Map<String, Object> aiConfig = systemConfigService.getAiConfig();

      boolean enabled = (Boolean) aiConfig.getOrDefault("enabled", false);
      if (!enabled) {
        Log.debug("AI 识别功能未启用，跳过文件名识别: " + originalFileName);
        return null;
      }

      String baseUrl = (String) aiConfig.get("baseUrl");
      String apiKey = (String) aiConfig.get("apiKey");
      String model = (String) aiConfig.getOrDefault("model", "gpt-3.5-turbo");

      if (baseUrl == null || baseUrl.trim().isEmpty()
          || apiKey == null || apiKey.trim().isEmpty()) {
        Log.warn("AI 识别配置不完整，跳过文件名识别");
        return null;
      }

      waitForQpmLimit(aiConfig);

      String inputText = buildInputText(originalFileName, directoryPath);
      AiRecognitionResult result = callAiApi(baseUrl, apiKey, model, aiConfig, inputText);

      if (result != null && result.isSuccess()) {
        Log.info("AI 识别成功: " + originalFileName + " -> " + result);
        return result;
      } else {
        Log.info("AI 无法识别文件名: " + originalFileName);
        return result;
      }

    } catch (Exception e) {
      Log.errorf("AI 文件名识别失败: " + originalFileName, e);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void waitForQpmLimit(Map<String, Object> aiConfig) {
    int qpmLimit = (Integer) aiConfig.getOrDefault("qpmLimit", 60);
    String key = "ai_requests";

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime lastReset = lastResetTimes.get(key);

    if (lastReset == null || ChronoUnit.MINUTES.between(lastReset, now) >= 1) {
      requestCounts.put(key, new AtomicInteger(0));
      lastResetTimes.put(key, now);
      lastReset = now;
    }

    AtomicInteger count = requestCounts.get(key);
    long secondsElapsed = ChronoUnit.SECONDS.between(lastReset, now);

    if (count.get() >= qpmLimit) {
      long secondsToWait = 60 - secondsElapsed;
      if (secondsToWait > 0) {
        Log.info("已达到 QPM 限制，等待 " + secondsToWait + " 秒后继续");
        try {
          Thread.sleep(secondsToWait * 1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        requestCounts.put(key, new AtomicInteger(0));
        lastResetTimes.put(key, LocalDateTime.now());
      }
    }

    count.incrementAndGet();
  }

  private String buildInputText(String originalFileName, String directoryPath) {
    StringBuilder input = new StringBuilder();

    if (directoryPath != null && !directoryPath.trim().isEmpty()) {
      input.append("目录路径: ").append(directoryPath).append("\n");
    }

    input.append("文件名: ").append(originalFileName);

    return input.toString();
  }

  @SuppressWarnings("unchecked")
  private AiRecognitionResult callAiApi(
      String baseUrl, String apiKey, String model, Map<String, Object> aiConfig, String inputText) {
    try {
      String apiUrl = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("model", model);
      requestBody.put("max_tokens", 300);
      requestBody.put("temperature", 0.1);

      Map<String, Object> systemMessage = new HashMap<>();
      systemMessage.put("role", "system");
      systemMessage.put("content", aiConfig.get("prompt"));

      Map<String, Object> userMessage = new HashMap<>();
      userMessage.put("role", "user");
      userMessage.put("content", inputText);

      requestBody.put("messages", new Object[] { systemMessage, userMessage });

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + apiKey)
          .timeout(Duration.ofSeconds(60))
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        Log.error("AI API 请求失败，状态码: " + response.statusCode());
        return null;
      }

      JsonNode responseJson = objectMapper.readTree(response.body());
      JsonNode choices = responseJson.get("choices");

      if (choices != null && choices.isArray() && choices.size() > 0) {
        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.get("message");
        if (message != null) {
          JsonNode content = message.get("content");
          if (content != null) {
            String result = content.asText().trim();
            return parseJsonResponse(result);
          }
        }
      }

      Log.warn("AI API 响应格式异常");
      return null;

    } catch (Exception e) {
      Log.errorf("调用 AI API 失败", e);
      return null;
    }
  }

  private AiRecognitionResult parseJsonResponse(String rawResponse) {
    if (rawResponse == null || rawResponse.trim().isEmpty()) {
      return null;
    }

    try {
      String jsonContent = extractJsonFromResponse(rawResponse.trim());
      if (jsonContent == null) {
        return null;
      }

      JsonNode jsonNode = objectMapper.readTree(jsonContent);

      JsonNode successNode = jsonNode.get("success");
      if (successNode == null) {
        return null;
      }

      boolean success = successNode.asBoolean();
      AiRecognitionResult result = new AiRecognitionResult().setSuccess(success);

      JsonNode typeNode = jsonNode.get("type");
      if (typeNode != null && !typeNode.isNull()) {
        result.setType(typeNode.asText());
      }

      if (success) {
        JsonNode titleNode = jsonNode.get("title");
        if (titleNode != null && !titleNode.isNull() && !titleNode.asText().trim().isEmpty()) {
          result.setTitle(titleNode.asText().trim());

          JsonNode yearNode = jsonNode.get("year");
          if (yearNode != null && !yearNode.isNull()) {
            result.setYear(yearNode.asText().trim());
          }

          JsonNode seasonNode = jsonNode.get("season");
          if (seasonNode != null && !seasonNode.isNull()) {
            result.setSeason(seasonNode.asInt());
          }

          JsonNode episodeNode = jsonNode.get("episode");
          if (episodeNode != null && !episodeNode.isNull()) {
            result.setEpisode(episodeNode.asInt());
          }

          return result;
        } else {
          JsonNode filenameNode = jsonNode.get("filename");
          if (filenameNode != null && !filenameNode.isNull()) {
            result.setFilename(filenameNode.asText().trim());
            return result;
          }
        }
      } else {
        JsonNode reasonNode = jsonNode.get("reason");
        result.setReason(reasonNode != null ? reasonNode.asText() : "未知原因");
        return result;
      }

    } catch (Exception e) {
      Log.warn("解析 JSON 响应失败: " + e.getMessage());
    }

    return null;
  }

  protected String extractJsonFromResponse(String response) {
    if (response == null || response.trim().isEmpty()) {
      return null;
    }

    String content = response.trim();

    if (content.contains("```")) {
      int codeBlockStart = content.indexOf("```");
      if (codeBlockStart != -1) {
        int contentStart;
        String afterTicks = content.substring(codeBlockStart + 3);

        if (afterTicks.startsWith("json")) {
          contentStart = codeBlockStart + 7;
          if (contentStart < content.length() && content.charAt(contentStart) == '\n') {
            contentStart++;
          }
        } else {
          int newlinePos = content.indexOf('\n', codeBlockStart + 3);
          contentStart = newlinePos != -1 ? newlinePos + 1 : codeBlockStart + 3;
        }

        int codeBlockEnd = content.indexOf("```", contentStart);
        if (codeBlockEnd != -1) {
          content = content.substring(contentStart, codeBlockEnd).trim();
        } else {
          content = content.substring(contentStart).trim();
        }
      }
    }

    int jsonStart = content.indexOf('{');
    int jsonEnd = content.lastIndexOf('}');

    if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) {
      return null;
    }

    return content.substring(jsonStart, jsonEnd + 1);
  }

  public boolean validateAiConfig(String baseUrl, String apiKey, String model) {
    try {
      String apiUrl = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("model", model);
      requestBody.put("max_tokens", 50);
      requestBody.put("temperature", 0.1);

      Map<String, Object> systemMessage = new HashMap<>();
      systemMessage.put("role", "system");
      systemMessage.put("content", "请返回 JSON 格式: {\"test\": \"success\"}");

      Map<String, Object> userMessage = new HashMap<>();
      userMessage.put("role", "user");
      userMessage.put("content", "测试");

      requestBody.put("messages", new Object[] { systemMessage, userMessage });

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + apiKey)
          .timeout(Duration.ofSeconds(30))
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
      if (success) {
        Log.info("AI 配置验证成功: " + model);
      } else {
        Log.warn("AI 配置验证失败，状态码: " + response.statusCode());
      }

      return success;

    } catch (Exception e) {
      Log.error("验证 AI 配置失败: " + e.getMessage(), e);
      return false;
    }
  }
}
