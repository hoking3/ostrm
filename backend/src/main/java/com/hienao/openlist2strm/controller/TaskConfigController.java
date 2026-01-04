/*
 * OStrm - Stream Management System
 * Copyright (C) 2024 OStrm Project
 *
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.task.TaskConfigDto;
import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.service.TaskConfigService;
import com.hienao.openlist2strm.service.TaskExecutionService;
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
 * 任务配置管理控制器 - Quarkus JAX-RS 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/task-config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "任务配置管理", description = "任务配置的增删改查接口")
public class TaskConfigController {

  private static final String CONFIG_ID_PARAM = "配置ID";

  @Inject
  TaskConfigService taskConfigService;

  @Inject
  TaskExecutionService taskExecutionService;

  /** 查询所有配置 */
  @GET
  @Operation(summary = "查询所有配置", description = "获取所有任务配置列表")
  public Response getAllConfigs() {
    List<TaskConfig> configs = taskConfigService.getAllConfigs();
    List<TaskConfigDto> configDtos = configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return Response.ok(ApiResponse.success(configDtos)).build();
  }

  /** 查询启用的配置 */
  @GET
  @Path("/active")
  @Operation(summary = "查询启用的配置", description = "获取所有启用状态的任务配置")
  public Response getActiveConfigs() {
    List<TaskConfig> configs = taskConfigService.getActiveConfigs();
    List<TaskConfigDto> configDtos = configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return Response.ok(ApiResponse.success(configDtos)).build();
  }

  /** 查询有定时任务的配置 */
  @GET
  @Path("/scheduled")
  @Operation(summary = "查询有定时任务的配置", description = "获取所有配置了定时任务的任务配置")
  public Response getScheduledConfigs() {
    List<TaskConfig> configs = taskConfigService.getScheduledConfigs();
    List<TaskConfigDto> configDtos = configs.stream().map(this::convertToDto).collect(Collectors.toList());
    return Response.ok(ApiResponse.success(configDtos)).build();
  }

  /** 根据ID查询配置 */
  @GET
  @Path("/{id}")
  @Operation(summary = "根据ID查询配置", description = "根据配置ID获取任务配置详情")
  public Response getConfigById(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id) {
    TaskConfig config = taskConfigService.getById(id);
    if (config == null) {
      return Response.ok(ApiResponse.error(404, "配置不存在")).build();
    }
    return Response.ok(ApiResponse.success(convertToDto(config))).build();
  }

  /** 根据任务名称查询配置 */
  @GET
  @Path("/task-name/{taskName}")
  @Operation(summary = "根据任务名称查询配置", description = "根据任务名称获取任务配置")
  public Response getConfigByTaskName(
      @Parameter(description = "任务名称", required = true) @PathParam("taskName") String taskName) {
    TaskConfig config = taskConfigService.getByTaskName(taskName);
    if (config == null) {
      return Response.ok(ApiResponse.error(404, "配置不存在")).build();
    }
    return Response.ok(ApiResponse.success(convertToDto(config))).build();
  }

  /** 根据路径查询配置 */
  @GET
  @Path("/path")
  @Operation(summary = "根据路径查询配置", description = "根据路径获取任务配置")
  public Response getConfigByPath(
      @Parameter(description = "任务路径", required = true) @QueryParam("path") String path) {
    TaskConfig config = taskConfigService.getByPath(path);
    if (config == null) {
      return Response.ok(ApiResponse.error(404, "配置不存在")).build();
    }
    return Response.ok(ApiResponse.success(convertToDto(config))).build();
  }

  /** 创建配置 */
  @POST
  @Operation(summary = "创建配置", description = "创建新的任务配置")
  public Response createConfig(@Valid TaskConfigDto configDto) {
    // 转换Cron表达式格式
    if (configDto.getCron() != null && !configDto.getCron().isEmpty()) {
      String convertedCron = convertToQuartzFormat(configDto.getCron());
      if (convertedCron != null) {
        configDto.setCron(convertedCron);
      }
    }
    TaskConfig config = convertToEntity(configDto);
    TaskConfig createdConfig = taskConfigService.createConfig(config);
    return Response.ok(ApiResponse.success(convertToDto(createdConfig))).build();
  }

  /** 更新配置 */
  @PUT
  @Path("/{id}")
  @Operation(summary = "更新配置", description = "更新指定ID的任务配置")
  public Response updateConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id,
      @Valid TaskConfigDto configDto) {
    configDto.setId(id);
    // 转换Cron表达式格式
    if (configDto.getCron() != null && !configDto.getCron().isEmpty()) {
      String convertedCron = convertToQuartzFormat(configDto.getCron());
      if (convertedCron != null) {
        configDto.setCron(convertedCron);
      }
    }
    TaskConfig config = convertToEntity(configDto);
    TaskConfig updatedConfig = taskConfigService.updateConfig(config);
    return Response.ok(ApiResponse.success(convertToDto(updatedConfig))).build();
  }

  /** 删除配置 */
  @DELETE
  @Path("/{id}")
  @Operation(summary = "删除配置", description = "删除指定ID的任务配置")
  public Response deleteConfig(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id) {
    taskConfigService.deleteById(id);
    return Response.ok(ApiResponse.success(null)).build();
  }

  /** 更新配置启用状态 */
  @PATCH
  @Path("/{id}/status")
  @Operation(summary = "更新配置启用状态", description = "启用或禁用指定ID的任务配置")
  public Response updateConfigStatus(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id,
      UpdateStatusRequest request) {
    taskConfigService.updateActiveStatus(id, request.getIsActive());
    return Response.ok(ApiResponse.success(null)).build();
  }

  /** 更新最后执行时间 */
  @PATCH
  @Path("/{id}/last-exec-time")
  @Operation(summary = "更新最后执行时间", description = "更新指定ID任务配置的最后执行时间")
  public Response updateLastExecTime(
      @Parameter(description = CONFIG_ID_PARAM, required = true) @PathParam("id") Long id,
      UpdateLastExecTimeRequest request) {
    taskConfigService.updateLastExecTime(id, request.getLastExecTime());
    return Response.ok(ApiResponse.success(null)).build();
  }

  /** 提交任务执行 */
  @POST
  @Path("/{id}/submit")
  @Operation(summary = "提交任务执行", description = "将指定ID的任务提交到线程池中执行")
  public Response submitTask(
      @Parameter(description = "任务配置ID", required = true) @PathParam("id") Long id,
      TaskSubmitRequest request) {
    Boolean isIncremental = request != null ? request.getIsIncremental() : null;
    taskExecutionService.submitTask(id, isIncremental);
    return Response.ok(ApiResponse.success("任务已提交执行")).build();
  }

  /** 更新状态请求体 */
  public static class UpdateStatusRequest {
    private Boolean isActive;

    public Boolean getIsActive() {
      return isActive;
    }

    public void setIsActive(Boolean isActive) {
      this.isActive = isActive;
    }
  }

  /** 更新最后执行时间请求体 */
  public static class UpdateLastExecTimeRequest {
    private Long lastExecTime;

    public Long getLastExecTime() {
      return lastExecTime;
    }

    public void setLastExecTime(Long lastExecTime) {
      this.lastExecTime = lastExecTime;
    }
  }

  /** 任务提交请求体 */
  public static class TaskSubmitRequest {
    private Boolean isIncremental;

    public Boolean getIsIncremental() {
      return isIncremental;
    }

    public void setIsIncremental(Boolean isIncremental) {
      this.isIncremental = isIncremental;
    }
  }

  /** 实体转DTO */
  private TaskConfigDto convertToDto(TaskConfig config) {
    TaskConfigDto dto = new TaskConfigDto();
    dto.setId(config.getId());
    dto.setTaskName(config.getTaskName());
    dto.setOpenlistConfigId(config.getOpenlistConfigId());
    dto.setPath(config.getPath());
    dto.setStrmPath(config.getStrmPath());
    dto.setIsActive(config.getIsActive());
    dto.setIsIncrement(config.getIsIncrement());
    dto.setNeedScrap(config.getNeedScrap());
    dto.setRenameRegex(config.getRenameRegex());
    dto.setCron(config.getCron());
    dto.setLastExecTime(config.getLastExecTime());
    dto.setCreatedAt(config.getCreatedAt());
    dto.setUpdatedAt(config.getUpdatedAt());
    return dto;
  }

  /** DTO转实体 */
  private TaskConfig convertToEntity(TaskConfigDto dto) {
    TaskConfig config = new TaskConfig();
    config.setId(dto.getId());
    config.setTaskName(dto.getTaskName());
    config.setOpenlistConfigId(dto.getOpenlistConfigId());
    config.setPath(dto.getPath());
    config.setStrmPath(dto.getStrmPath());
    config.setIsActive(dto.getIsActive());
    config.setIsIncrement(dto.getIsIncrement());
    config.setNeedScrap(dto.getNeedScrap());
    config.setRenameRegex(dto.getRenameRegex());
    config.setCron(dto.getCron());
    return config;
  }

  /**
   * 将 Unix Cron 格式转换为 Quartz Cron 格式 Unix Cron: 分 时 日 月 周 (5个字段) Quartz Cron: 秒 分
   * 时 日 月 周 (6个字段)
   */
  private String convertToQuartzFormat(String cronExpression) {
    if (cronExpression == null || cronExpression.trim().isEmpty()) {
      return null;
    }

    String[] parts = cronExpression.trim().split("\\s+");

    // 如果是 5 个字段，转换为 6 个字段的 Quartz 格式
    if (parts.length == 5) {
      String minute = parts[0];
      String hour = parts[1];
      String day = parts[2];
      String month = parts[3];
      String week = parts[4];

      // 在 Quartz 中，如果指定了周几，日期字段应该用 ?
      if (!week.equals("*")) {
        return "0 " + minute + " " + hour + " ? " + month + " " + week;
      } else {
        return "0 " + minute + " " + hour + " " + day + " " + month + " ?";
      }
    }

    // 如果已经是 6 个字段的 Quartz 格式，直接返回
    if (parts.length == 6) {
      return cronExpression;
    }

    return null;
  }
}
