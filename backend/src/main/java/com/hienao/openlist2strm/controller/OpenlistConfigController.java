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
 *
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.openlist.OpenlistConfigDto;
import com.hienao.openlist2strm.entity.OpenlistConfig;
import com.hienao.openlist2strm.service.OpenlistApiService;
import com.hienao.openlist2strm.service.OpenlistConfigService;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 配置管理控制器 - Quarkus JAX-RS 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/openlist-config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "OpenList配置管理", description = "OpenList配置的增删改查接口")
public class OpenlistConfigController {

  private static final String CONFIG_ID_PARAM = "配置ID";

  @Inject
  OpenlistConfigService openlistConfigService;

  @Inject
  OpenlistApiService openlistApiService;

  /** 查询所有配置 */
  @GET
  @Operation(summary = "查询所有配置", description = "获取所有OpenList配置列表")
  public Response getAllConfigs() {
    List<OpenlistConfig> configs = openlistConfigService.getAllConfigs();
    List<OpenlistConfigDto> configDtos = configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return Response.ok(ApiResponse.success(configDtos)).build();
  }

  /** 查询启用的配置 */
  @GET
  @Path("/active")
  @Operation(summary = "查询启用的配置", description = "获取所有启用状态的OpenList配置")
  public Response getActiveConfigs() {
    List<OpenlistConfig> configs = openlistConfigService.getActiveConfigs();
    List<OpenlistConfigDto> configDtos = configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return Response.ok(ApiResponse.success(configDtos)).build();
  }

  /** 根据ID查询配置 */
  @GET
  @Path("/{id}")
  @Operation(summary = "根据ID查询配置", description = "根据配置ID获取OpenList配置详情")
  public Response getConfigById(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id) {
    OpenlistConfig config = openlistConfigService.getById(id);
    if (config == null) {
      return Response.ok(ApiResponse.error(404, "配置不存在")).build();
    }
    return Response.ok(ApiResponse.success(convertToDto(config))).build();
  }

  /** 根据用户名查询配置 */
  @GET
  @Path("/username/{username}")
  @Operation(summary = "根据用户名查询配置", description = "根据用户名获取OpenList配置")
  public Response getConfigByUsername(
      @Parameter(description = "用户名", required = true) @PathParam("username") String username) {
    OpenlistConfig config = openlistConfigService.getByUsername(username);
    if (config == null) {
      return Response.ok(ApiResponse.error(404, "配置不存在")).build();
    }
    return Response.ok(ApiResponse.success(convertToDto(config))).build();
  }

  /** 创建配置 */
  @POST
  @Operation(summary = "创建配置", description = "创建新的OpenList配置")
  public Response createConfig(
      @Parameter(description = "配置信息", required = true) @Valid OpenlistConfigDto configDto) {
    OpenlistConfig config = convertToEntity(configDto);
    OpenlistConfig createdConfig = openlistConfigService.createConfig(config);
    return Response.ok(ApiResponse.success(convertToDto(createdConfig))).build();
  }

  /** 更新配置 */
  @PUT
  @Path("/{id}")
  @Operation(summary = "更新配置", description = "更新指定ID的配置")
  public Response updateConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id,
      @Parameter(description = "配置信息", required = true) @Valid OpenlistConfigDto configDto) {
    configDto.setId(id);
    OpenlistConfig config = convertToEntity(configDto);
    OpenlistConfig updatedConfig = openlistConfigService.updateConfig(config);
    return Response.ok(ApiResponse.success(convertToDto(updatedConfig))).build();
  }

  /** 删除配置 */
  @DELETE
  @Path("/{id}")
  @Operation(summary = "删除配置", description = "删除指定ID的配置")
  public Response deleteConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id) {
    openlistConfigService.deleteConfig(id);
    return Response.ok(ApiResponse.success(null)).build();
  }

  /** 启用/禁用配置 */
  @PATCH
  @Path("/{id}/status")
  @Operation(summary = "启用/禁用配置", description = "更新指定配置的启用状态")
  public Response updateConfigStatus(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id,
      @Parameter(description = "状态更新请求", required = true) UpdateStatusRequest request) {
    openlistConfigService.updateActiveStatus(id, request.getIsActive());
    return Response.ok(ApiResponse.success(null)).build();
  }

  /** 验证OpenList配置（用于前端保存配置前的验证，避免CORS问题） */
  @POST
  @Path("/validate")
  @Operation(summary = "验证OpenList配置", description = "验证OpenList的baseUrl和token是否有效")
  public Response validateConfig(
      @Parameter(description = "验证配置请求", required = true) ValidateConfigRequest request) {
    try {
      OpenlistApiService.ValidateConfigResult result = openlistApiService.validateConfig(request.getBaseUrl(),
          request.getToken());

      ValidateConfigResponse response = new ValidateConfigResponse();
      response.setUsername(result.getUsername());
      response.setBasePath(result.getBasePath());

      return Response.ok(ApiResponse.success(response)).build();
    } catch (Exception e) {
      Log.error("验证OpenList配置失败: " + e.getMessage());
      return Response.ok(ApiResponse.error(400, e.getMessage())).build();
    }
  }

  /** 验证任务路径（用于前端创建任务时验证路径是否存在，避免CORS问题） */
  @POST
  @Path("/validate-path")
  @Operation(summary = "验证任务路径", description = "验证指定的任务路径在OpenList中是否存在且是目录")
  public Response validatePath(
      @Parameter(description = "验证路径请求", required = true) ValidatePathRequest request) {
    try {
      openlistApiService.validatePath(
          request.getBaseUrl(), request.getToken(), request.getBasePath(), request.getTaskPath());
      return Response.ok(ApiResponse.success(null)).build();
    } catch (Exception e) {
      Log.error("验证任务路径失败: " + e.getMessage());
      return Response.ok(ApiResponse.error(400, e.getMessage())).build();
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
    dto.setId(config.getId());
    // dto.setConfigName(config.getConfigName()); // configName 不存在
    dto.setBaseUrl(config.getBaseUrl());
    dto.setUsername(config.getUsername());
    dto.setToken(config.getToken());
    dto.setBasePath(config.getBasePath());
    dto.setIsActive(config.getIsActive());
    dto.setCreatedAt(config.getCreatedAt());
    dto.setUpdatedAt(config.getUpdatedAt());
    return dto;
  }

  /** DTO转实体 */
  private OpenlistConfig convertToEntity(OpenlistConfigDto dto) {
    OpenlistConfig config = new OpenlistConfig();
    config.setId(dto.getId());
    // config.setConfigName(dto.getConfigName()); // configName 不存在
    config.setBaseUrl(dto.getBaseUrl());
    config.setUsername(dto.getUsername());
    config.setToken(dto.getToken());
    config.setBasePath(dto.getBasePath());
    config.setIsActive(dto.getIsActive());
    // createdAt 和 updatedAt 通常由 JPA 自动处理，或者在 Service 层设置
    // 但如果 DTO 传了且需要更新，可以使用 setter
    // 注意：OpenlistConfig 没有 setCreateTime，只有 setCreatedAt
    return config;
  }
}
