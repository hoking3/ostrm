/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.service.AiFileNameRecognitionService;
import com.hienao.openlist2strm.service.SystemConfigService;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 系统配置管理控制器 - Quarkus JAX-RS 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/system")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "系统配置管理", description = "系统配置的读取和保存接口")
public class SystemConfigController {

  @Inject
  SystemConfigService systemConfigService;

  @Inject
  AiFileNameRecognitionService aiFileNameRecognitionService;

  /** 获取系统配置 */
  @GET
  @Path("/config")
  @Operation(summary = "获取系统配置", description = "获取当前系统配置信息")
  public Response getSystemConfig() {
    try {
      Map<String, Object> config = systemConfigService.getSystemConfig();
      return Response.ok(ApiResponse.success(config)).build();
    } catch (Exception e) {
      Log.errorf("获取系统配置失败", e);
      return Response.ok(ApiResponse.error("获取系统配置失败: " + e.getMessage())).build();
    }
  }

  /** 保存系统配置 */
  @POST
  @Path("/config")
  @Operation(summary = "保存系统配置", description = "保存系统配置信息")
  public Response saveSystemConfig(Map<String, Object> config) {
    try {
      // 验证媒体文件后缀配置
      if (config.containsKey("mediaExtensions")) {
        Object mediaExtensions = config.get("mediaExtensions");
        if (!(mediaExtensions instanceof List)) {
          return Response.ok(ApiResponse.error("mediaExtensions必须是数组类型")).build();
        }

        @SuppressWarnings("unchecked")
        List<String> extensions = (List<String>) mediaExtensions;

        // 验证后缀格式
        for (String ext : extensions) {
          if (!ext.startsWith(".")) {
            return Response.ok(ApiResponse.error("文件后缀必须以.开头: " + ext)).build();
          }
        }
      }

      // 验证TMDB配置
      if (config.containsKey("tmdb")) {
        Object tmdbConfig = config.get("tmdb");
        if (!(tmdbConfig instanceof Map)) {
          return Response.ok(ApiResponse.error("tmdb配置必须是对象类型")).build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> tmdb = (Map<String, Object>) tmdbConfig;

        // 验证API Key
        if (tmdb.containsKey("apiKey")) {
          String apiKey = (String) tmdb.get("apiKey");
          if (apiKey != null
              && !apiKey.trim().isEmpty()
              && !systemConfigService.validateTmdbApiKey(apiKey)) {
            return Response.ok(ApiResponse.error("TMDB API Key格式不正确")).build();
          }
        }
      }

      systemConfigService.saveSystemConfig(config);
      return Response.ok(ApiResponse.success("配置保存成功")).build();
    } catch (Exception e) {
      Log.errorf("保存系统配置失败", e);
      return Response.ok(ApiResponse.error("保存系统配置失败: " + e.getMessage())).build();
    }
  }

  /** 测试 AI 配置 */
  @POST
  @Path("/test-ai-config")
  @Operation(summary = "测试 AI 配置", description = "测试 AI 识别配置是否有效")
  public Response testAiConfig(Map<String, Object> testConfig) {
    try {
      String baseUrl = (String) testConfig.get("baseUrl");
      String apiKey = (String) testConfig.get("apiKey");
      String model = (String) testConfig.get("model");

      if (baseUrl == null || baseUrl.trim().isEmpty()) {
        return Response.ok(ApiResponse.error("API 基础 URL 不能为空")).build();
      }

      if (apiKey == null || apiKey.trim().isEmpty()) {
        return Response.ok(ApiResponse.error("API Key 不能为空")).build();
      }

      if (model == null || model.trim().isEmpty()) {
        return Response.ok(ApiResponse.error("模型名称不能为空")).build();
      }

      // 调用 AI 服务验证配置
      boolean isValid = aiFileNameRecognitionService.validateAiConfig(baseUrl, apiKey, model);

      if (isValid) {
        return Response.ok(ApiResponse.success("AI 配置测试成功")).build();
      } else {
        return Response.ok(ApiResponse.error("AI 配置测试失败，请检查配置信息")).build();
      }

    } catch (Exception e) {
      Log.errorf("测试 AI 配置失败", e);
      return Response.ok(ApiResponse.error("测试 AI 配置失败: " + e.getMessage())).build();
    }
  }

  /** 获取默认 AI 提示词 */
  @GET
  @Path("/default-ai-prompt")
  @Operation(summary = "获取默认 AI 提示词", description = "获取系统默认的 AI 识别提示词")
  public Response getDefaultAiPrompt() {
    try {
      String defaultPrompt = systemConfigService.getDefaultAiPrompt();
      return Response.ok(ApiResponse.success(defaultPrompt)).build();
    } catch (Exception e) {
      Log.errorf("获取默认 AI 提示词失败", e);
      return Response.ok(ApiResponse.error("获取默认 AI 提示词失败: " + e.getMessage())).build();
    }
  }
}
