/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.config;

import com.hienao.openlist2strm.service.DataReportService;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 应用启动日志输出 - Quarkus 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class ApplicationStartupLogger {

  @ConfigProperty(name = "quarkus.log.file.path", defaultValue = "./logs/backend.log")
  String logFilePath;

  @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
  String serverPort;

  @Inject
  DataReportService dataReportService;

  void onStart(@Observes StartupEvent ev) {
    Log.info("=".repeat(60));
    Log.info("🚀 应用启动完成！");
    Log.info("=".repeat(60));

    Log.info("🌐 服务端口: " + serverPort);
    Log.info("📁 工作目录: " + System.getProperty("user.dir"));
    Log.info("☕ Java版本: " + System.getProperty("java.version"));
    Log.info("🖥️  操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));

    // 输出日志配置信息
    Log.info("📝 日志配置:");
    Log.info("   配置路径: " + logFilePath);

    Path logFile = Paths.get(logFilePath);
    Path logDir = logFile.getParent();
    if (logDir != null) {
      Log.info("   日志目录: " + logDir.toAbsolutePath());
      Log.info("   目录存在: " + Files.exists(logDir));
    }

    // 输出访问地址
    Log.info("🔗 访问地址:");
    Log.info("   本地: http://localhost:" + serverPort);
    Log.info("   API文档: http://localhost:" + serverPort + "/q/swagger-ui");

    // 输出环境变量
    Log.info("🔧 关键环境变量:");
    logEnvVar("LOG_PATH");
    logEnvVar("DATABASE_PATH");

    Log.info("=".repeat(60));
    Log.info("✅ 应用已就绪，可以开始处理请求");
    Log.info("=".repeat(60));

    // 上报应用启动事件
    try {
      dataReportService.reportEvent("app_use", new HashMap<>());
      Log.debug("应用启动事件上报成功");
    } catch (Exception reportException) {
      Log.warn("应用启动事件上报失败，错误: " + reportException.getMessage());
    }
  }

  private void logEnvVar(String name) {
    String value = System.getenv(name);
    if (value != null) {
      Log.info("   " + name + ": " + value);
    } else {
      Log.info("   " + name + ": (未设置)");
    }
  }
}
