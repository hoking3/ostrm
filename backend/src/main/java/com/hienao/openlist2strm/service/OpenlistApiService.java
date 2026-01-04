/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hienao.openlist2strm.constant.AppConstants;
import com.hienao.openlist2strm.entity.OpenlistConfig;
import com.hienao.openlist2strm.exception.BusinessException;
import com.hienao.openlist2strm.util.UrlEncoder;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * OpenList API服务类 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class OpenlistApiService {

  @Inject
  ObjectMapper objectMapper;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(60))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  /** OpenList API响应数据结构 */
  @Data
  public static class OpenlistApiResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private OpenlistData data;
  }

  /** OpenList数据结构 */
  @Data
  public static class OpenlistData {
    @JsonProperty("files")
    private List<OpenlistFile> files;

    @JsonProperty("readme")
    private String readme;
  }

  /** OpenList文件信息 */
  @Data
  public static class OpenlistFile {
    @JsonProperty("name")
    private String name;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("type")
    private String type;

    @JsonProperty("url")
    private String url;

    @JsonProperty("path")
    private String path;

    @JsonProperty("modified")
    private String modified;

    @JsonProperty("sign")
    private String sign;
  }

  /** Alist API响应数据结构 */
  @Data
  public static class AlistApiResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private AlistData data;
  }

  /** Alist数据结构 */
  @Data
  public static class AlistData {
    @JsonProperty("content")
    private List<AlistFile> content;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("readme")
    private String readme;

    @JsonProperty("header")
    private String header;

    @JsonProperty("write")
    private Boolean write;

    @JsonProperty("provider")
    private String provider;
  }

  /** Alist文件信息 */
  @Data
  public static class AlistFile {
    @JsonProperty("name")
    private String name;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("is_dir")
    private Boolean isDir;

    @JsonProperty("modified")
    private String modified;

    @JsonProperty("created")
    private String created;

    @JsonProperty("sign")
    private String sign;

    @JsonProperty("thumb")
    private String thumb;

    @JsonProperty("type")
    private Integer type;

    @JsonProperty("hashinfo")
    private String hashinfo;

    @JsonProperty("hash_info")
    private Object hashInfo;
  }

  public List<OpenlistFile> getAllFilesRecursively(OpenlistConfig config, String path) {
    List<OpenlistFile> allFiles = new ArrayList<>();
    getAllFilesRecursively(config, path, allFiles);
    return allFiles;
  }

  private void getAllFilesRecursively(
      OpenlistConfig config, String path, List<OpenlistFile> allFiles) {
    try {
      Log.info("正在获取目录: " + path);

      List<OpenlistFile> files = getDirectoryContents(config, path);

      for (OpenlistFile file : files) {
        allFiles.add(file);

        if ("folder".equals(file.getType())) {
          String subPath = file.getPath();
          if (subPath == null || subPath.isEmpty()) {
            subPath = path + "/" + file.getName();
          }
          getAllFilesRecursively(config, subPath, allFiles);
        }
      }

    } catch (Exception e) {
      Log.errorf("获取目录内容失败: " + path + ", 错误: " + e.getMessage(), e);
      throw new BusinessException("获取目录内容失败: " + path + ", 错误: " + e.getMessage(), e);
    }
  }

  public List<OpenlistFile> getDirectoryContents(OpenlistConfig config, String path) {
    try {
      String apiUrl = config.getBaseUrl();
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += "api/fs/list";

      String requestUrl = apiUrl + "?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8);
      Log.debug("请求URL: " + requestUrl);

      String requestBody = String.format(
          "{\"path\":\"%s\",\"password\":\"\",\"page\":1,\"per_page\":0,\"refresh\":false}",
          path);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(requestUrl))
          .header("Content-Type", "application/json")
          .header("User-Agent", AppConstants.USER_AGENT)
          .header("Authorization", config.getToken())
          .timeout(Duration.ofSeconds(30))
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("OpenList API请求失败，状态码: " + response.statusCode());
      }

      String responseBody = response.body();
      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("OpenList API返回空响应");
      }

      Log.debug("API响应: " + responseBody);

      AlistApiResponse apiResponse = objectMapper.readValue(responseBody, AlistApiResponse.class);

      if (apiResponse.getCode() == null || !apiResponse.getCode().equals(200)) {
        throw new BusinessException("OpenList API返回错误: " + apiResponse.getMessage());
      }

      if (apiResponse.getData() == null || apiResponse.getData().getContent() == null) {
        Log.warn("目录为空或无文件: " + path);
        return new ArrayList<>();
      }

      List<OpenlistFile> files = new ArrayList<>();
      for (AlistFile alistFile : apiResponse.getData().getContent()) {
        OpenlistFile file = new OpenlistFile();
        file.setName(alistFile.getName());
        file.setSize(alistFile.getSize());
        file.setType(alistFile.getIsDir() ? "folder" : "file");
        file.setModified(alistFile.getModified());
        file.setSign(alistFile.getSign());

        String filePath = path;
        if (!filePath.endsWith("/")) {
          filePath += "/";
        }
        filePath += alistFile.getName();
        file.setPath(filePath);

        String fileUrl = buildFileUrl(config.getBaseUrl(), filePath);
        file.setUrl(fileUrl);

        files.add(file);
      }

      Log.info("获取到 " + files.size() + " 个文件/目录: " + path);

      return files;

    } catch (Exception e) {
      Log.errorf("调用OpenList API失败: " + path + ", 错误: " + e.getMessage(), e);
      throw new BusinessException("调用OpenList API失败: " + e.getMessage(), e);
    }
  }

  public boolean checkFileExists(OpenlistConfig config, String filePath) {
    try {
      String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
      String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

      List<OpenlistFile> files = getDirectoryContents(config, dirPath);

      return files.stream()
          .anyMatch(file -> "file".equals(file.getType()) && fileName.equals(file.getName()));

    } catch (Exception e) {
      Log.debugf("检查文件存在性失败: " + filePath + ", 错误: " + e.getMessage());
      return false;
    }
  }

  public byte[] getFileContent(OpenlistConfig config, OpenlistFile file) {
    return getFileContent(config, file, true);
  }

  public byte[] getFileContent(OpenlistConfig config, OpenlistFile file, boolean enableUrlEncoding) {
    try {
      String fileUrl = file.getUrl();
      if (file.getSign() != null && !file.getSign().isEmpty()) {
        String completeUrl = file.getUrl() + "?sign=" + file.getSign();
        if (enableUrlEncoding) {
          fileUrl = UrlEncoder.encodeUrlSmart(completeUrl);
        } else {
          fileUrl = completeUrl;
        }
      } else {
        if (enableUrlEncoding) {
          fileUrl = UrlEncoder.encodeUrlSmart(fileUrl);
        } else {
          fileUrl = file.getUrl();
        }
      }

      Log.debug("下载文件请求 - 文件名: " + file.getName() + ", 完整URL: " + fileUrl);

      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(fileUrl))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(60))
          .GET();

      if (config.getToken() != null && !config.getToken().isEmpty()) {
        requestBuilder.header("Authorization", config.getToken());
      }

      HttpRequest request = requestBuilder.build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      Log.debug("文件下载响应 - 状态码: " + response.statusCode());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        Log.warn("文件下载失败: " + file.getName() + ", 状态码: " + response.statusCode());
        return null;
      }

      byte[] content = response.body();
      if (content == null || content.length == 0) {
        Log.warn("文件内容为空: " + file.getName());
        return null;
      }

      Log.debug("文件下载成功 - 文件名: " + file.getName() + ", 大小: " + content.length + " bytes");
      return content;

    } catch (Exception e) {
      Log.error("下载文件异常: " + file.getName() + ", 错误: " + e.getMessage(), e);
      return null;
    }
  }

  public byte[] getFileContent(OpenlistConfig config, String filePath) {
    try {
      String fileUrl = buildFileUrl(config.getBaseUrl(), filePath);

      Log.debugf("下载文件请求 - 文件路径: " + filePath + ", 完整URL: " + fileUrl);

      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(fileUrl))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(60))
          .GET();

      if (config.getToken() != null && !config.getToken().isEmpty()) {
        requestBuilder.header("Authorization", config.getToken());
      }

      HttpRequest request = requestBuilder.build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      Log.debug("文件下载响应 - 状态码: " + response.statusCode());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        Log.warnf("文件下载失败: " + filePath + ", 状态码: " + response.statusCode());
        return null;
      }

      byte[] content = response.body();
      if (content == null || content.length == 0) {
        Log.warn("文件内容为空: " + filePath);
        return null;
      }

      Log.debugf("文件下载成功 - 路径: " + filePath + ", 大小: " + content.length + " bytes");
      return content;

    } catch (Exception e) {
      Log.errorf("下载文件异常: " + filePath + ", 错误: " + e.getMessage(), e);
      return null;
    }
  }

  public ValidateConfigResult validateConfig(String baseUrl, String token) {
    try {
      String apiUrl = baseUrl;
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += "api/me";

      Log.info("验证OpenList配置: " + apiUrl);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .header("Content-Type", "application/json")
          .header("User-Agent", AppConstants.USER_AGENT)
          .header("Authorization", token)
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("OpenList API请求失败，状态码: " + response.statusCode());
      }

      String responseBody = response.body();
      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("OpenList API返回空响应");
      }

      Log.debug("验证配置响应: " + responseBody);

      MeApiResponse meResponse = objectMapper.readValue(responseBody, MeApiResponse.class);

      if (meResponse.getCode() == null || !meResponse.getCode().equals(200)) {
        throw new BusinessException("OpenList API返回错误: " + meResponse.getMessage());
      }

      if (meResponse.getData() == null) {
        throw new BusinessException("OpenList API返回数据为空");
      }

      MeData userData = meResponse.getData();

      if (userData.getDisabled() != null && userData.getDisabled()) {
        throw new BusinessException("该账号已被禁用，无法添加配置");
      }

      ValidateConfigResult result = new ValidateConfigResult();
      result.setUsername(userData.getUsername());
      result.setBasePath(userData.getBasePath() != null ? userData.getBasePath() : "/");

      Log.info("验证OpenList配置成功: username=" + result.getUsername() + ", basePath=" + result.getBasePath());
      return result;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      Log.error("验证OpenList配置失败: " + e.getMessage(), e);
      if (e.getMessage() != null && e.getMessage().contains("401")) {
        throw new BusinessException("Token无效或已过期");
      } else if (e.getMessage() != null && e.getMessage().contains("403")) {
        throw new BusinessException("没有权限访问该API");
      } else if (e.getMessage() != null && e.getMessage().contains("404")) {
        throw new BusinessException("API接口不存在，请检查Base URL");
      }
      throw new BusinessException("网络连接失败，请检查Base URL和Token: " + e.getMessage());
    }
  }

  public boolean validatePath(String baseUrl, String token, String basePath, String taskPath) {
    try {
      String fullPath = basePath;
      if (fullPath.endsWith("/") && taskPath.startsWith("/")) {
        fullPath = fullPath.substring(0, fullPath.length() - 1) + taskPath;
      } else if (!fullPath.endsWith("/") && !taskPath.startsWith("/")) {
        fullPath = fullPath + "/" + taskPath;
      } else {
        fullPath = fullPath + taskPath;
      }

      String apiUrl = baseUrl;
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/";
      }
      apiUrl += "api/fs/get";

      Log.infof("验证任务路径: " + apiUrl + ", fullPath: " + fullPath);

      String requestBody = String.format(
          "{\"path\":\"%s\",\"password\":\"\",\"page\":1,\"per_page\":0,\"refresh\":false}",
          fullPath);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .header("Content-Type", "application/json")
          .header("User-Agent", AppConstants.USER_AGENT)
          .header("Authorization", token)
          .timeout(Duration.ofSeconds(30))
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("OpenList API请求失败，状态码: " + response.statusCode());
      }

      String responseBody = response.body();
      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("OpenList API返回空响应");
      }

      Log.debug("验证路径响应: " + responseBody);

      FsGetApiResponse fsResponse = objectMapper.readValue(responseBody, FsGetApiResponse.class);

      if (fsResponse.getCode() == null || !fsResponse.getCode().equals(200)) {
        throw new BusinessException("路径验证失败: " + fsResponse.getMessage());
      }

      if (fsResponse.getData() == null) {
        throw new BusinessException("指定路径不存在");
      }

      if (fsResponse.getData().getIsDir() == null || !fsResponse.getData().getIsDir()) {
        throw new BusinessException("指定路径不是一个目录");
      }

      Log.info("验证任务路径成功: " + fullPath);
      return true;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      Log.error("验证任务路径失败: " + e.getMessage(), e);
      if (e.getMessage() != null && e.getMessage().contains("401")) {
        throw new BusinessException("OpenList Token无效或已过期");
      } else if (e.getMessage() != null && e.getMessage().contains("403")) {
        throw new BusinessException("没有权限访问该路径");
      } else if (e.getMessage() != null && e.getMessage().contains("404")) {
        throw new BusinessException("指定路径不存在");
      }
      throw new BusinessException("路径验证失败，请检查路径是否正确: " + e.getMessage());
    }
  }

  /** /api/me 接口响应结构 */
  @Data
  public static class MeApiResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private MeData data;
  }

  /** /api/me 用户数据结构 */
  @Data
  public static class MeData {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("base_path")
    private String basePath;

    @JsonProperty("role")
    private Integer role;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("permission")
    private Integer permission;

    @JsonProperty("sso_id")
    private String ssoId;

    @JsonProperty("authn")
    private String authn;
  }

  /** /api/fs/get 接口响应结构 */
  @Data
  public static class FsGetApiResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private FsGetData data;
  }

  /** /api/fs/get 数据结构 */
  @Data
  public static class FsGetData {
    @JsonProperty("name")
    private String name;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("is_dir")
    private Boolean isDir;

    @JsonProperty("modified")
    private String modified;

    @JsonProperty("created")
    private String created;

    @JsonProperty("sign")
    private String sign;

    @JsonProperty("thumb")
    private String thumb;

    @JsonProperty("type")
    private Integer type;

    @JsonProperty("hashinfo")
    private String hashinfo;

    @JsonProperty("hash_info")
    private Object hashInfo;

    @JsonProperty("raw_url")
    private String rawUrl;

    @JsonProperty("readme")
    private String readme;

    @JsonProperty("header")
    private String header;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("related")
    private Object related;
  }

  /** 验证配置结果 */
  @Data
  public static class ValidateConfigResult {
    private String username;
    private String basePath;
  }

  private String buildFileUrl(String baseUrl, String filePath) {
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    // 对路径进行 URL 编码
    String encodedPath = "";
    String[] segments = filePath.split("/");
    for (int i = 0; i < segments.length; i++) {
      if (!segments[i].isEmpty()) {
        if (i > 0 || !filePath.startsWith("/")) {
          encodedPath += "/";
        }
        encodedPath += URLEncoder.encode(segments[i], StandardCharsets.UTF_8)
            .replace("+", "%20");
      }
    }

    String result = baseUrl + "d" + encodedPath;
    Log.debug("URL构建编码: " + baseUrl + "d" + filePath + " -> " + result);
    return result;
  }
}
