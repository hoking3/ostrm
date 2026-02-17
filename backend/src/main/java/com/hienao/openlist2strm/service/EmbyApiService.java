package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.constant.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Emby API服务类
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Service
public class EmbyApiService {

  private final RestTemplate restTemplate;

  public EmbyApiService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * 刷新Emby媒体库
   *
   * @param embyServerUrl Emby服务器URL
   * @param apiKey Emby API密钥
   * @return 是否刷新成功
   */
  public boolean refreshMediaLibrary(String embyServerUrl, String apiKey) {
    try {
      log.info("开始刷新Emby媒体库: {}", embyServerUrl);

      // 构建请求URL
      String apiUrl = embyServerUrl;
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += "emby/Library/Refresh";

      // 添加API密钥参数
      if (apiKey != null && !apiKey.isEmpty()) {
        apiUrl += "?api_key=" + apiKey;
      }

      log.debug("Emby媒体库刷新请求URL: {}", apiUrl);

      // 设置请求头
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("User-Agent", AppConstants.USER_AGENT);

      HttpEntity<String> entity = new HttpEntity<>(headers);

      // 发送请求
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
      long endTime = System.currentTimeMillis();

      log.info("Emby媒体库刷新请求耗时: {}ms", endTime - startTime);

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("Emby媒体库刷新失败，状态码: {}", response.getStatusCode());
        return false;
      }

      log.info("Emby媒体库刷新成功");
      return true;

    } catch (Exception e) {
      log.error("Emby媒体库刷新异常: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * 刷新指定的Emby媒体库
   *
   * @param embyServerUrl Emby服务器URL
   * @param libraryId 媒体库ID
   * @param apiKey Emby API密钥
   * @param username Emby用户名（可选）
   * @param password Emby密码（可选）
   * @return 是否刷新成功
   */
  public boolean refreshSpecificMediaLibrary(String embyServerUrl, String libraryId, String apiKey, String username, String password) {
    try {
      log.info("开始刷新指定的Emby媒体库: {} (ID: {})", embyServerUrl, libraryId);

      // 构建请求URL
      String apiUrl = embyServerUrl;
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += "emby/Library/Refresh/" + libraryId;

      // 添加API密钥参数
      if (apiKey != null && !apiKey.isEmpty()) {
        apiUrl += "?api_key=" + apiKey;
      }

      log.debug("Emby指定媒体库刷新请求URL: {}", apiUrl);

      // 设置请求头
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("User-Agent", AppConstants.USER_AGENT);

      // 添加基本认证（如果提供了用户名和密码）
      if (username != null && !username.isEmpty() && password != null) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        log.debug("使用基本认证: {}", username);
      }

      HttpEntity<String> entity = new HttpEntity<>(headers);

      // 发送请求
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
      long endTime = System.currentTimeMillis();

      log.info("Emby指定媒体库刷新请求耗时: {}ms", endTime - startTime);

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("Emby指定媒体库刷新失败，状态码: {}", response.getStatusCode());
        return false;
      }

      log.info("Emby指定媒体库刷新成功: {}", libraryId);
      return true;

    } catch (Exception e) {
      log.error("Emby指定媒体库刷新异常: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * 测试Emby服务器连接
   *
   * @param embyServerUrl Emby服务器URL
   * @param apiKey Emby API密钥
   * @param username Emby用户名（可选）
   * @param password Emby密码（可选）
   * @return 是否连接成功
   */
  public boolean testConnection(String embyServerUrl, String apiKey, String username, String password) {
    try {
      log.info("测试Emby服务器连接: {}", embyServerUrl);

      // 构建请求URL
      String apiUrl = embyServerUrl;
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += "emby/System/Info";

      // 添加API密钥参数
      if (apiKey != null && !apiKey.isEmpty()) {
        apiUrl += "?api_key=" + apiKey;
      }

      log.debug("Emby连接测试请求URL: {}", apiUrl);

      // 设置请求头
      HttpHeaders headers = new HttpHeaders();
      headers.set("User-Agent", AppConstants.USER_AGENT);

      // 添加基本认证（如果提供了用户名和密码）
      if (username != null && !username.isEmpty() && password != null) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        log.debug("使用基本认证: {}", username);
      }

      HttpEntity<String> entity = new HttpEntity<>(headers);

      // 发送请求
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
      long endTime = System.currentTimeMillis();

      log.info("Emby连接测试请求耗时: {}ms", endTime - startTime);

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("Emby连接测试失败，状态码: {}", response.getStatusCode());
        return false;
      }

      log.info("Emby连接测试成功");
      return true;

    } catch (Exception e) {
      log.error("Emby连接测试异常: {}", e.getMessage(), e);
      return false;
    }
  }
}