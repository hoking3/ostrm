/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.version.VersionCheckResponse;
import com.hienao.openlist2strm.service.GitHubVersionService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 版本检查控制器 - Quarkus JAX-RS 版本
 * 注：本控制器不需要认证
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "版本管理", description = "版本检查和更新相关接口")
public class VersionController {

  @Inject
  GitHubVersionService gitHubVersionService;

  /** 检查版本更新 */
  @GET
  @Path("/check")
  @Operation(summary = "检查版本更新", description = "检查当前版本是否有新版本可用")
  public Response checkVersion(
      @QueryParam("currentVersion") @DefaultValue("dev") String currentVersion) {
    try {
      Log.debug("检查版本更新请求: " + currentVersion);

      VersionCheckResponse response = gitHubVersionService.checkVersionUpdate(currentVersion);

      if (response.getError() != null) {
        Log.warn("版本检查失败: " + response.getError());
        return Response.ok(ApiResponse.error(response.getError())).build();
      }

      return Response.ok(ApiResponse.success(response)).build();
    } catch (Exception e) {
      Log.errorf("检查版本更新失败", e);
      return Response.ok(ApiResponse.error("检查版本更新失败: " + e.getMessage())).build();
    }
  }

  /** 获取最新版本信息 */
  @GET
  @Path("/latest")
  @Operation(summary = "获取最新版本信息", description = "获取GitHub上的最新版本信息")
  public Response getLatestVersion() {
    try {
      Log.debug("获取最新版本信息请求");

      VersionCheckResponse response = gitHubVersionService.checkVersionUpdate("dev");

      if (response.getError() != null) {
        Log.warn("获取最新版本失败: " + response.getError());
        return Response.ok(ApiResponse.error(response.getError())).build();
      }

      return Response.ok(ApiResponse.success(response)).build();
    } catch (Exception e) {
      Log.errorf("获取最新版本失败", e);
      return Response.ok(ApiResponse.error("获取最新版本失败: " + e.getMessage())).build();
    }
  }

  /** 清除版本检查缓存 */
  @DELETE
  @Path("/cache/clear")
  @Operation(summary = "清除版本检查缓存", description = "清除版本检查相关的缓存数据")
  public Response clearVersionCache() {
    try {
      Log.debug("清除版本检查缓存请求");
      // Quarkus 缓存会自动管理
      return Response.ok(ApiResponse.success("版本检查缓存已清除")).build();
    } catch (Exception e) {
      Log.errorf("清除版本检查缓存失败", e);
      return Response.ok(ApiResponse.error("清除版本检查缓存失败: " + e.getMessage())).build();
    }
  }
}
