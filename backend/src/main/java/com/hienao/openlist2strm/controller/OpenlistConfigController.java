/*
 * OStrm - Stream Management System
 * Copyright (C) 2024 OStrm Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.openlist.OpenlistConfigDto;
import com.hienao.openlist2strm.entity.OpenlistConfig;
import com.hienao.openlist2strm.service.FileRenamingService;
import com.hienao.openlist2strm.service.OpenlistApiService;
import com.hienao.openlist2strm.service.OpenlistConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 配置管理控制器
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/openlist-config")
@RequiredArgsConstructor
@Tag(name = "OpenList配置管理", description = "OpenList配置的增删改查接口")
public class OpenlistConfigController {

  private static final String CONFIG_ID_PARAM = "配置ID";

  private final OpenlistConfigService openlistConfigService;
  private final OpenlistApiService openlistApiService;
  private final FileRenamingService fileRenamingService;
  private final com.hienao.openlist2strm.service.MediaScrapingService mediaScrapingService;
  private final com.hienao.openlist2strm.service.DirectoryTitleExtractorService directoryTitleExtractorService;

  /** 查询所有配置 */
  @GetMapping
  @Operation(summary = "查询所有配置", description = "获取所有OpenList配置列表")
  public ResponseEntity<ApiResponse<List<OpenlistConfigDto>>> getAllConfigs() {
    List<OpenlistConfig> configs = openlistConfigService.getAllConfigs();
    List<OpenlistConfigDto> configDtos =
        configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(configDtos));
  }

  /** 查询启用的配置 */
  @GetMapping("/active")
  @Operation(summary = "查询启用的配置", description = "获取所有启用状态的OpenList配置")
  public ResponseEntity<ApiResponse<List<OpenlistConfigDto>>> getActiveConfigs() {
    List<OpenlistConfig> configs = openlistConfigService.getActiveConfigs();
    List<OpenlistConfigDto> configDtos =
        configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(configDtos));
  }

  /** 根据ID查询配置 */
  @GetMapping("/{id}")
  @Operation(summary = "根据ID查询配置", description = "根据配置ID获取OpenList配置详情")
  public ResponseEntity<ApiResponse<OpenlistConfigDto>> getConfigById(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id) {
    OpenlistConfig config = openlistConfigService.getById(id);
    if (config == null) {
      return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
    }
    return ResponseEntity.ok(ApiResponse.success(convertToDto(config)));
  }

  /** 根据用户名查询配置 */
  @GetMapping("/username/{username}")
  @Operation(summary = "根据用户名查询配置", description = "根据用户名获取OpenList配置")
  public ResponseEntity<ApiResponse<OpenlistConfigDto>> getConfigByUsername(
      @Parameter(description = "用户名", required = true) @PathVariable String username) {
    OpenlistConfig config = openlistConfigService.getByUsername(username);
    if (config == null) {
      return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
    }
    return ResponseEntity.ok(ApiResponse.success(convertToDto(config)));
  }

  /** 创建配置 */
  @PostMapping
  @Operation(summary = "创建配置", description = "创建新的OpenList配置")
  public ResponseEntity<ApiResponse<OpenlistConfigDto>> createConfig(
      @Parameter(description = "配置信息", required = true) @Valid @RequestBody
          OpenlistConfigDto configDto) {
    OpenlistConfig config = convertToEntity(configDto);
    OpenlistConfig createdConfig = openlistConfigService.createConfig(config);
    return ResponseEntity.ok(ApiResponse.success(convertToDto(createdConfig)));
  }

  /** 更新配置 */
  @PutMapping("/{id}")
  @Operation(summary = "更新配置", description = "更新指定ID的配置")
  public ResponseEntity<ApiResponse<OpenlistConfigDto>> updateConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "配置信息", required = true) @Valid @RequestBody
          OpenlistConfigDto configDto) {
    configDto.setId(id);
    OpenlistConfig config = convertToEntity(configDto);
    OpenlistConfig updatedConfig = openlistConfigService.updateConfig(config);
    return ResponseEntity.ok(ApiResponse.success(convertToDto(updatedConfig)));
  }

  /** 删除配置 */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除配置", description = "删除指定ID的配置")
  public ResponseEntity<ApiResponse<Void>> deleteConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id) {
    openlistConfigService.deleteConfig(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  /** 启用/禁用配置 */
  @PatchMapping("/{id}/status")
  @Operation(summary = "启用/禁用配置", description = "更新指定配置的启用状态")
  public ResponseEntity<ApiResponse<Void>> updateConfigStatus(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "状态更新请求", required = true) @RequestBody
          UpdateStatusRequest request) {
    openlistConfigService.updateActiveStatus(id, request.getIsActive());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  /** 验证OpenList配置（用于前端保存配置前的验证，避免CORS问题） */
  @PostMapping("/validate")
  @Operation(summary = "验证OpenList配置", description = "验证OpenList的baseUrl和token是否有效")
  public ResponseEntity<ApiResponse<ValidateConfigResponse>> validateConfig(
      @Parameter(description = "验证配置请求", required = true) @RequestBody
          ValidateConfigRequest request) {
    try {
      OpenlistApiService.ValidateConfigResult result =
          openlistApiService.validateConfig(request.getBaseUrl(), request.getToken());
      
      ValidateConfigResponse response = new ValidateConfigResponse();
      response.setUsername(result.getUsername());
      response.setBasePath(result.getBasePath());
      
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (Exception e) {
      log.error("验证OpenList配置失败: {}", e.getMessage());
      return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
    }
  }

  /** 验证任务路径（用于前端创建任务时验证路径是否存在，避免CORS问题） */
  @PostMapping("/validate-path")
  @Operation(summary = "验证任务路径", description = "验证指定的任务路径在OpenList中是否存在且是目录")
  public ResponseEntity<ApiResponse<Void>> validatePath(
      @Parameter(description = "验证路径请求", required = true) @RequestBody
          ValidatePathRequest request) {
    try {
      openlistApiService.validatePath(
          request.getBaseUrl(),
          request.getToken(),
          request.getBasePath(),
          request.getTaskPath());
      return ResponseEntity.ok(ApiResponse.success(null));
    } catch (Exception e) {
      log.error("验证任务路径失败: {}", e.getMessage());
      return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
    }
  }

  /** 状态更新请求DTO */
  public static class UpdateStatusRequest {
    private Boolean isActive;

    public Boolean getIsActive() {
      return isActive;
    }

    public void setIsActive(Boolean isActive) {
      this.isActive = isActive;
    }
  }

  /** 验证配置请求DTO */
  public static class ValidateConfigRequest {
    private String baseUrl;
    private String token;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }
  }

  /** 验证配置响应DTO */
  public static class ValidateConfigResponse {
    private String username;
    private String basePath;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getBasePath() {
      return basePath;
    }

    public void setBasePath(String basePath) {
      this.basePath = basePath;
    }
  }

  /** 验证路径请求DTO */
  public static class ValidatePathRequest {
    private String baseUrl;
    private String token;
    private String basePath;
    private String taskPath;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getBasePath() {
      return basePath;
    }

    public void setBasePath(String basePath) {
      this.basePath = basePath;
    }

    public String getTaskPath() {
      return taskPath;
    }

    public void setTaskPath(String taskPath) {
      this.taskPath = taskPath;
    }
  }

  /** 实体转DTO */
  private OpenlistConfigDto convertToDto(OpenlistConfig config) {
    OpenlistConfigDto dto = new OpenlistConfigDto();
    BeanUtils.copyProperties(config, dto);
    return dto;
  }

  /** 浏览目录 */
  @GetMapping("/{id}/browse")
  @Operation(summary = "浏览目录", description = "获取指定路径下的文件和目录列表")
  public ResponseEntity<ApiResponse<List<OpenlistApiService.OpenlistFile>>> browseDirectory(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "目录路径", required = false) @RequestParam(defaultValue = "/") String path) {
    try {
      OpenlistConfig config = openlistConfigService.getById(id);
      if (config == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
      }
      
      log.info("浏览目录: configId={}, path={}", id, path);
      
      String fullPath = buildFullPath(config.getBasePath(), path);
      List<OpenlistApiService.OpenlistFile> files = openlistApiService.getDirectoryContents(config, fullPath);
      
      return ResponseEntity.ok(ApiResponse.success(files));
    } catch (Exception e) {
      log.error("浏览目录失败: id={}, path={}, error={}", id, path, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "浏览目录失败: " + e.getMessage()));
    }
  }

  /** 下载文件 */
  @GetMapping("/{id}/download")
  @Operation(summary = "下载文件", description = "下载指定路径的文件")
  public ResponseEntity<byte[]> downloadFile(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "文件路径", required = true) @RequestParam String path) {
    try {
      OpenlistConfig config = openlistConfigService.getById(id);
      if (config == null) {
        return ResponseEntity.notFound().build();
      }
      
      log.info("下载文件: configId={}, path={}", id, path);
      
      String fullPath = buildFullPath(config.getBasePath(), path);
      
      OpenlistApiService.OpenlistFile fileInfo = new OpenlistApiService.OpenlistFile();
      fileInfo.setPath(fullPath);
      fileInfo.setName(path.substring(path.lastIndexOf('/') + 1));
      
      List<OpenlistApiService.OpenlistFile> directoryFiles = openlistApiService.getDirectoryContents(
          config, fullPath.substring(0, Math.max(0, fullPath.lastIndexOf('/'))));
      
      for (OpenlistApiService.OpenlistFile f : directoryFiles) {
        if (f.getPath() != null && f.getPath().equals(fullPath)) {
          fileInfo = f;
          break;
        }
      }
      
      byte[] fileContent = openlistApiService.getFileContent(config, fileInfo, false);
      if (fileContent == null) {
        return ResponseEntity.notFound().build();
      }
      
      String fileName = fileInfo.getName();
      String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
      
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentDispositionFormData("attachment", encodedFileName);
      headers.setContentLength(fileContent.length);
      
      return ResponseEntity.ok().headers(headers).body(fileContent);
    } catch (Exception e) {
      log.error("下载文件失败: id={}, path={}, error={}", id, path, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 搜索文件 */
  @PostMapping("/{id}/search")
  @Operation(summary = "搜索文件", description = "在指定目录下搜索文件")
  public ResponseEntity<ApiResponse<List<OpenlistApiService.OpenlistFile>>> searchFiles(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "搜索请求", required = true) @RequestBody SearchRequest request) {
    try {
      OpenlistConfig config = openlistConfigService.getById(id);
      if (config == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
      }
      
      log.info("搜索文件: configId={}, path={}, keyword={}", id, request.getPath(), request.getKeyword());
      
      String fullPath = buildFullPath(config.getBasePath(), request.getPath());
      List<OpenlistApiService.OpenlistFile> allFiles = openlistApiService.getAllFilesRecursively(config, fullPath);
      
      String keyword = request.getKeyword().toLowerCase();
      List<OpenlistApiService.OpenlistFile> results = new ArrayList<>();
      
      for (OpenlistApiService.OpenlistFile file : allFiles) {
        if (file.getName() != null && file.getName().toLowerCase().contains(keyword)) {
          results.add(file);
        }
      }
      
      return ResponseEntity.ok(ApiResponse.success(results));
    } catch (Exception e) {
      log.error("搜索文件失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "搜索失败: " + e.getMessage()));
    }
  }

  /** 构建完整路径 */
  private String buildFullPath(String basePath, String path) {
    if (basePath == null || basePath.isEmpty()) {
      basePath = "/";
    }
    if (path == null || path.isEmpty()) {
      path = "/";
    }
    
    if (basePath.endsWith("/") && path.startsWith("/")) {
      return basePath + path.substring(1);
    } else if (!basePath.endsWith("/") && !path.startsWith("/")) {
      return basePath + "/" + path;
    } else {
      return basePath + path;
    }
  }

  /** 搜索请求DTO */
  @lombok.Data
  public static class SearchRequest {
    private String path;
    private String keyword;
  }

  /** 单个重命名请求DTO */
  @lombok.Data
  public static class RenameRequest {
    private String srcPath;
    private String dstPath;
  }

  /** 批量重命名请求DTO */
  @lombok.Data
  public static class BatchRenameRequest {
    private List<Map<String, String>> renameList;
  }

  /** 重命名文件 */
  @PostMapping("/{id}/rename")
  @Operation(summary = "重命名文件", description = "重命名单个文件或目录")
  public ResponseEntity<ApiResponse<Map<String, Object>>> renameFile(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "重命名请求", required = true) @RequestBody RenameRequest request) {
    try {
      OpenlistConfig config = openlistConfigService.getById(id);
      if (config == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
      }

      log.info("重命名文件: configId={}, srcPath={}, dstPath={}", id, request.getSrcPath(), request.getDstPath());
      log.info("配置basePath: {}", config.getBasePath());

      String srcPath = stripBasePath(request.getSrcPath(), config.getBasePath());
      String dstPath = stripBasePath(request.getDstPath(), config.getBasePath());
      
      log.info("重命名路径转换 - 源: {} -> {}, 目标: {} -> {}", 
          request.getSrcPath(), srcPath, request.getDstPath(), dstPath);

      boolean success = openlistApiService.renameFile(config, srcPath, dstPath);

      Map<String, Object> result = new HashMap<>();
      result.put("success", success);
      result.put("srcPath", srcPath);
      result.put("dstPath", dstPath);

      if (success) {
        return ResponseEntity.ok(ApiResponse.success(result));
      } else {
        return ResponseEntity.ok(ApiResponse.error(500, "重命名失败"));
      }
    } catch (Exception e) {
      log.error("重命名文件失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "重命名失败: " + e.getMessage()));
    }
  }

  /** 批量重命名文件 */
  @PostMapping("/{id}/batch-rename")
  @Operation(summary = "批量重命名文件", description = "批量重命名多个文件或目录")
  public ResponseEntity<ApiResponse<Map<String, Object>>> batchRenameFiles(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "批量重命名请求", required = true) @RequestBody BatchRenameRequest request) {
    try {
      OpenlistConfig config = openlistConfigService.getById(id);
      if (config == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
      }

      log.info("批量重命名文件: configId={}, count={}", id, request.getRenameList().size());
      log.info("配置basePath: {}", config.getBasePath());

      List<Map<String, String>> relativePathList = new ArrayList<>();
      for (Map<String, String> item : request.getRenameList()) {
        String srcPath = stripBasePath(item.get("srcPath"), config.getBasePath());
        String dstPath = stripBasePath(item.get("dstPath"), config.getBasePath());
        
        log.info("重命名路径转换 - 源: {} -> {}, 目标: {} -> {}", 
            item.get("srcPath"), srcPath, item.get("dstPath"), dstPath);
        
        Map<String, String> relativeItem = new HashMap<>();
        relativeItem.put("srcPath", srcPath);
        relativeItem.put("dstPath", dstPath);
        relativePathList.add(relativeItem);
      }

      Map<String, Object> result = openlistApiService.batchRenameFiles(config, relativePathList);

      return ResponseEntity.ok(ApiResponse.success(result));
    } catch (Exception e) {
      log.error("批量重命名文件失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "批量重命名失败: " + e.getMessage()));
    }
  }
  
  /**
   * 从路径中去除 basePath 前缀
   *
   * @param fullPath 完整路径
   * @param basePath 基础路径
   * @return 去除 basePath 后的相对路径（符合 Alist API 要求）
   */
  private String stripBasePath(String fullPath, String basePath) {
    if (fullPath == null || fullPath.isEmpty()) {
      return fullPath;
    }
    
    if (basePath == null || basePath.isEmpty() || basePath.equals("/")) {
      String result = fullPath;
      if (result.startsWith("/")) {
        result = result.substring(1);
      }
      log.debug("stripBasePath - basePath 为 /, 返回: {} -> {}", fullPath, result);
      return result;
    }
    
    String normalizedBasePath = basePath;
    if (!normalizedBasePath.endsWith("/")) {
      normalizedBasePath = normalizedBasePath + "/";
    }
    
    if (fullPath.startsWith(normalizedBasePath)) {
      String result = fullPath.substring(normalizedBasePath.length());
      if (result.startsWith("/")) {
        result = result.substring(1);
      }
      log.debug("stripBasePath - 去除 basePath 后: {} -> {}", fullPath, result);
      return result;
    }
    
    if (fullPath.equals(basePath)) {
      log.debug("stripBasePath - 路径等于 basePath, 返回空字符串");
      return "";
    }
    
    String result = fullPath;
    if (result.startsWith("/")) {
      result = result.substring(1);
    }
    log.debug("stripBasePath - 未匹配 basePath, 返回: {} -> {}", fullPath, result);
    return result;
  }

  /** TMDB匹配请求DTO */
  @lombok.Data
  public static class TmdbMatchRequest {
    private String fileName;
    private String directoryPath;
    private String path;
  }

  /** 批量TMDB匹配请求DTO */
  @lombok.Data
  public static class BatchTmdbMatchRequest {
    private List<Map<String, String>> files;
  }

  /** 带搜索配置的批量TMDB匹配请求DTO */
  @lombok.Data
  public static class BatchTmdbMatchWithConfigRequest {
    private List<Map<String, String>> files;
    private SearchConfig searchConfig;
    
    @lombok.Data
    public static class SearchConfig {
      private String title;
      private Integer year;
    }
  }

  /** TMDB搜索请求DTO */
  @lombok.Data
  public static class TmdbSearchRequest {
    private String query;
    private String type;
    private String year;
  }

  /** 根据TMDB ID获取详情请求DTO */
  @lombok.Data
  public static class TmdbGetByIdRequest {
    private Integer tmdbId;
    private String type;
    private Integer season;
    private Integer episode;
    private String originalFileName;
  }

  /** TMDB匹配文件 */
  @PostMapping("/{id}/tmdb-match")
  @Operation(summary = "TMDB匹配文件", description = "将单个文件匹配到TMDB")
  public ResponseEntity<ApiResponse<Map<String, Object>>> tmdbMatchFile(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "匹配请求", required = true) @RequestBody TmdbMatchRequest request) {
    try {
      log.info("TMDB匹配文件: configId={}, fileName={}", id, request.getFileName());

      Map<String, Object> result = fileRenamingService.matchFileToTmdb(
          request.getFileName(), request.getDirectoryPath());

      return ResponseEntity.ok(ApiResponse.success(result));
    } catch (Exception e) {
      log.error("TMDB匹配文件失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "匹配失败: " + e.getMessage()));
    }
  }

  /** 批量TMDB匹配文件 */
  @PostMapping("/{id}/tmdb-batch-match")
  @Operation(summary = "批量TMDB匹配文件", description = "批量将文件匹配到TMDB")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tmdbBatchMatchFiles(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "批量匹配请求", required = true) @RequestBody BatchTmdbMatchRequest request) {
    try {
      log.info("批量TMDB匹配文件: configId={}, count={}", id, request.getFiles().size());

      List<Map<String, Object>> results = fileRenamingService.batchMatchFilesToTmdb(request.getFiles());

      return ResponseEntity.ok(ApiResponse.success(results));
    } catch (Exception e) {
      log.error("批量TMDB匹配文件失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "批量匹配失败: " + e.getMessage()));
    }
  }

  /** 带搜索配置的批量TMDB匹配文件 */
  @PostMapping("/{id}/tmdb-batch-match-with-config")
  @Operation(summary = "带搜索配置的批量TMDB匹配文件", description = "使用指定的影视名和年份配置批量匹配文件到TMDB")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tmdbBatchMatchFilesWithConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "带搜索配置的批量匹配请求", required = true) @RequestBody BatchTmdbMatchWithConfigRequest request) {
    try {
      log.info("带搜索配置的批量TMDB匹配文件: configId={}, count={}, title={}, year={}", 
          id, request.getFiles().size(), 
          request.getSearchConfig() != null ? request.getSearchConfig().getTitle() : null,
          request.getSearchConfig() != null ? request.getSearchConfig().getYear() : null);

      List<Map<String, Object>> results = fileRenamingService.batchMatchFilesToTmdbWithConfig(
          request.getFiles(), 
          request.getSearchConfig() != null ? request.getSearchConfig().getTitle() : null,
          request.getSearchConfig() != null ? request.getSearchConfig().getYear() : null);

      return ResponseEntity.ok(ApiResponse.success(results));
    } catch (Exception e) {
      log.error("带搜索配置的批量TMDB匹配文件失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "批量匹配失败: " + e.getMessage()));
    }
  }

  /** 搜索TMDB */
  @PostMapping("/{id}/tmdb-search")
  @Operation(summary = "搜索TMDB", description = "在TMDB中搜索影视内容")
  public ResponseEntity<ApiResponse<Map<String, Object>>> tmdbSearch(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "搜索请求", required = true) @RequestBody TmdbSearchRequest request) {
    try {
      log.info("搜索TMDB: configId={}, query={}, type={}", id, request.getQuery(), request.getType());

      Map<String, Object> result = fileRenamingService.searchTmdb(
          request.getQuery(), request.getType(), request.getYear());

      return ResponseEntity.ok(ApiResponse.success(result));
    } catch (Exception e) {
      log.error("搜索TMDB失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "搜索失败: " + e.getMessage()));
    }
  }

  /** 根据TMDB ID获取详情 */
  @PostMapping("/{id}/tmdb-get-by-id")
  @Operation(summary = "根据TMDB ID获取详情", description = "根据TMDB ID获取影视详情并生成文件名")
  public ResponseEntity<ApiResponse<Map<String, Object>>> tmdbGetById(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "获取详情请求", required = true) @RequestBody TmdbGetByIdRequest request) {
    try {
      log.info("根据TMDB ID获取详情: configId={}, tmdbId={}, type={}", id, request.getTmdbId(), request.getType());

      Map<String, Object> result;
      if ("movie".equals(request.getType())) {
        result = fileRenamingService.getMovieByIdAndGenerateName(
            request.getTmdbId(), request.getOriginalFileName());
      } else {
        result = fileRenamingService.getTvByIdAndGenerateName(
            request.getTmdbId(), request.getSeason(), request.getEpisode(), request.getOriginalFileName());
      }

      return ResponseEntity.ok(ApiResponse.success(result));
    } catch (Exception e) {
      log.error("根据TMDB ID获取详情失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "获取详情失败: " + e.getMessage()));
    }
  }

  /** DTO转实体 */
  private OpenlistConfig convertToEntity(OpenlistConfigDto dto) {
    OpenlistConfig config = new OpenlistConfig();
    BeanUtils.copyProperties(dto, config);
    return config;
  }

  /** 刮削请求DTO */
  @lombok.Data
  public static class ScrapingRequest {
    private List<ScrapingItem> items;
    private ScrapingOptions options;
    
    @lombok.Data
    public static class ScrapingItem {
      private String filePath;
      private Integer tmdbId;
      private String type;
      private Integer season;
      private Integer episode;
      private String targetFileName;
    }
    
    @lombok.Data
    public static class ScrapingOptions {
      private Boolean generateNfo;
      private Boolean downloadPoster;
      private Boolean downloadBackdrop;
    }
  }

  /** 从目录路径提取标题 */
  @PostMapping("/{id}/extract-title-from-path")
  @Operation(summary = "从目录路径提取影视标题", description = "从当前目录逐级向上遍历，提取可能的影视标题")
  public ResponseEntity<ApiResponse<Map<String, Object>>> extractTitleFromPath(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "提取标题请求", required = true) @RequestBody ExtractTitleRequest request) {
    try {
      log.info("从路径提取标题: configId={}, path={}", id, request.getPath());
      
      List<String> titles = directoryTitleExtractorService.extractTitlesFromPath(request.getPath());
      String bestTitle = directoryTitleExtractorService.getBestTitle(request.getPath());
      
      Map<String, Object> result = new HashMap<>();
      result.put("titles", titles);
      result.put("bestTitle", bestTitle);
      
      return ResponseEntity.ok(ApiResponse.success(result));
    } catch (Exception e) {
      log.error("提取标题失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "提取标题失败: " + e.getMessage()));
    }
  }
  
  /** 提取标题请求DTO */
  @lombok.Data
  public static class ExtractTitleRequest {
    private String path;
  }

  /** 批量刮削 */
  @PostMapping("/{id}/scraping")
  @Operation(summary = "批量刮削影视信息", description = "根据TMDB匹配结果批量生成NFO文件和下载图片")
  public ResponseEntity<ApiResponse<Map<String, Object>>> batchScraping(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathVariable Long id,
      @Parameter(description = "刮削请求", required = true) @RequestBody ScrapingRequest request) {
    try {
      OpenlistConfig config = openlistConfigService.getById(id);
      if (config == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
      }

      log.info("开始批量刮削: configId={}, count={}", id, request.getItems().size());

      Map<String, Boolean> options = new HashMap<>();
      if (request.getOptions() != null) {
        options.put("generateNfo", request.getOptions().getGenerateNfo() != null ? request.getOptions().getGenerateNfo() : true);
        options.put("downloadPoster", request.getOptions().getDownloadPoster() != null ? request.getOptions().getDownloadPoster() : true);
        options.put("downloadBackdrop", request.getOptions().getDownloadBackdrop() != null ? request.getOptions().getDownloadBackdrop() : false);
      } else {
        options.put("generateNfo", true);
        options.put("downloadPoster", true);
        options.put("downloadBackdrop", false);
      }

      List<Map<String, Object>> results = new ArrayList<>();
      int successCount = 0;
      int failedCount = 0;

      for (ScrapingRequest.ScrapingItem item : request.getItems()) {
        Map<String, Object> result = mediaScrapingService.scrapFromTmdbMatch(
            config,
            item.getFilePath(),
            item.getTmdbId(),
            item.getType(),
            item.getSeason(),
            item.getEpisode(),
            item.getTargetFileName(),
            options);
        
        results.add(result);
        
        if ((Boolean) result.getOrDefault("success", false)) {
          successCount++;
        } else {
          failedCount++;
        }
      }

      Map<String, Object> response = new HashMap<>();
      response.put("results", results);
      response.put("total", request.getItems().size());
      response.put("successCount", successCount);
      response.put("failedCount", failedCount);

      log.info("批量刮削完成: 成功 {} 个，失败 {} 个", successCount, failedCount);
      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (Exception e) {
      log.error("批量刮削失败: id={}, error={}", id, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error(500, "批量刮削失败: " + e.getMessage()));
    }
  }
}
