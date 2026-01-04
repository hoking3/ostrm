/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.FrontendLogRequest;
import com.hienao.openlist2strm.service.LogService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 日志管理控制器 - Quarkus JAX-RS 版本
 * 注：本控制器不需要认证
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "日志管理", description = "系统日志相关接口")
public class LogController {

  @Inject
  LogService logService;

  @GET
  @Path("/{logType}")
  @Operation(summary = "获取日志内容", description = "获取指定类型的日志内容（无需认证）")
  public Response getLogs(
      @Parameter(description = "日志类型", example = "backend") @PathParam("logType") String logType,
      @Parameter(description = "获取行数", example = "1000") @QueryParam("lines") @DefaultValue("1000") int lines) {
    try {
      List<String> logLines = logService.getLogLines(logType, lines);
      return Response.ok(ApiResponse.success(logLines)).build();
    } catch (IllegalArgumentException e) {
      Log.warn("获取日志失败，参数错误: " + e.getMessage());
      return Response.ok(ApiResponse.error(400, e.getMessage())).build();
    } catch (Exception e) {
      Log.errorf("获取日志失败", e);
      return Response.ok(ApiResponse.error(500, "获取日志失败: " + e.getMessage())).build();
    }
  }

  @GET
  @Path("/{logType}/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(summary = "下载日志文件", description = "下载指定类型的完整日志文件（无需认证）")
  public Response downloadLog(
      @Parameter(description = "日志类型", example = "backend") @PathParam("logType") String logType) {
    try {
      InputStream inputStream = logService.getLogFileAsStream(logType);

      if (inputStream == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      String filename = logType + "-" + java.time.LocalDate.now() + ".log";

      return Response.ok(inputStream)
          .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
          .build();

    } catch (IllegalArgumentException e) {
      Log.warn("下载日志失败，参数错误: " + e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (Exception e) {
      Log.errorf("下载日志失败", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path("/{logType}/stats")
  @Operation(summary = "获取日志统计信息", description = "获取日志的统计信息（无需认证）")
  public Response getLogStats(
      @Parameter(description = "日志类型", example = "backend") @PathParam("logType") String logType) {
    try {
      Object stats = logService.getLogStats(logType);
      return Response.ok(ApiResponse.success(stats)).build();
    } catch (IllegalArgumentException e) {
      Log.warn("获取日志统计失败，参数错误: " + e.getMessage());
      return Response.ok(ApiResponse.error(400, e.getMessage())).build();
    } catch (Exception e) {
      Log.errorf("获取日志统计失败", e);
      return Response.ok(ApiResponse.error(500, "获取日志统计失败: " + e.getMessage())).build();
    }
  }

  @POST
  @Path("/frontend")
  @Operation(summary = "接收前端日志", description = "接收前端发送的日志数据（无需认证）")
  public Response receiveFrontendLogs(
      @Parameter(description = "前端日志数据") FrontendLogRequest request) {
    try {
      logService.processFrontendLogs(request);
      return Response.ok(ApiResponse.success("日志接收成功")).build();
    } catch (IllegalArgumentException e) {
      Log.warn("接收前端日志失败，参数错误: " + e.getMessage());
      return Response.ok(ApiResponse.error(400, e.getMessage())).build();
    } catch (Exception e) {
      Log.errorf("接收前端日志失败", e);
      return Response.ok(ApiResponse.error(500, "接收前端日志失败: " + e.getMessage())).build();
    }
  }
}
