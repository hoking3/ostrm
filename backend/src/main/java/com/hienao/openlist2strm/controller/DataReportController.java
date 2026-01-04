/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.service.DataReportService;
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
 * 数据上报控制器 - Quarkus JAX-RS 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/data-report")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "数据上报", description = "数据上报相关接口")
public class DataReportController {

  @Inject
  DataReportService dataReportService;

  /**
   * 上报事件数据
   *
   * @param event      事件名称
   * @param properties 自定义属性（可选）
   * @return 响应结果
   */
  @POST
  @Path("/event")
  @Operation(summary = "上报事件数据", description = "上报单个事件数据")
  public Response reportEvent(
      @QueryParam("event") String event, Map<String, Object> properties) {
    try {
      dataReportService.reportEvent(event, properties);
      return Response.ok(Map.of("success", true, "message", "事件数据上报成功")).build();
    } catch (Exception e) {
      Log.errorf("事件数据上报失败: " + event + ", 错误: " + e.getMessage(), e);
      return Response.ok(Map.of("success", false, "message", "事件数据上报失败: " + e.getMessage()))
          .build();
    }
  }

  /**
   * 批量上报事件数据
   *
   * @param request 批量上报请求
   * @return 响应结果
   */
  @POST
  @Path("/events")
  @Operation(summary = "批量上报事件数据", description = "批量上报多个事件数据")
  public Response reportEvents(BatchReportRequest request) {
    try {
      int successCount = 0;
      int failCount = 0;

      for (EventData eventData : request.getEvents()) {
        try {
          dataReportService.reportEvent(eventData.getEvent(), eventData.getProperties());
          successCount++;
        } catch (Exception e) {
          Log.warn("批量上报中单个事件失败: " + eventData.getEvent() + ", 错误: " + e.getMessage());
          failCount++;
        }
      }

      return Response.ok(
          Map.of(
              "success",
              true,
              "message",
              String.format("批量上报完成，成功: %d, 失败: %d", successCount, failCount),
              "successCount",
              successCount,
              "failCount",
              failCount))
          .build();
    } catch (Exception e) {
      Log.errorf("批量事件数据上报失败, 错误: " + e.getMessage(), e);
      return Response.ok(Map.of("success", false, "message", "批量事件数据上报失败: " + e.getMessage()))
          .build();
    }
  }

  /** 批量上报请求DTO */
  public static class BatchReportRequest {
    private List<EventData> events;

    public List<EventData> getEvents() {
      return events;
    }

    public void setEvents(List<EventData> events) {
      this.events = events;
    }
  }

  /** 事件数据DTO */
  public static class EventData {
    private String event;
    private Map<String, Object> properties;

    public String getEvent() {
      return event;
    }

    public void setEvent(String event) {
      this.event = event;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }
  }
}
