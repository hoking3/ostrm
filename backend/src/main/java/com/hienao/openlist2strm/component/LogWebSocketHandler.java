/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.component;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.PathParam;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志 WebSocket 处理器 - Quarkus WebSocket Next 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@WebSocket(path = "/ws/logs/{logType}")
@ApplicationScoped
public class LogWebSocketHandler {

  private static final ConcurrentHashMap<String, WebSocketConnection> sessions = new ConcurrentHashMap<>();

  @Inject
  WebSocketConnection connection;

  @OnOpen
  public void onOpen(@PathParam("logType") String logType) {
    String sessionKey = connection.id() + "_" + logType;
    sessions.put(sessionKey, connection);
    Log.info("WebSocket连接已建立: sessionId=" + connection.id() + ", logType=" + logType);
    connection.sendTextAndAwait("连接成功，开始接收 " + logType + " 日志");
  }

  @OnTextMessage
  public void onMessage(String message, @PathParam("logType") String logType) {
    Log.debug("收到WebSocket消息: sessionId=" + connection.id() + ", message=" + message);
  }

  @OnClose
  public void onClose(@PathParam("logType") String logType) {
    String sessionId = connection.id();
    sessions.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId));
    Log.info("WebSocket连接已关闭: sessionId=" + sessionId);
  }

  @OnError
  public void onError(Throwable error, @PathParam("logType") String logType) {
    Log.error("WebSocket传输错误: sessionId=" + connection.id(), error);
    String sessionId = connection.id();
    sessions.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId));
  }

  public void broadcastToLogType(String logType, String message) {
    sessions.entrySet().stream()
        .filter(entry -> entry.getKey().endsWith("_" + logType))
        .forEach(entry -> {
          try {
            WebSocketConnection session = entry.getValue();
            if (session.isOpen()) {
              session.sendTextAndAwait(message);
            }
          } catch (Exception e) {
            Log.error("广播消息失败: sessionId=" + entry.getValue().id(), e);
          }
        });
  }

  public int getConnectionCount() {
    return sessions.size();
  }

  public long getConnectionCount(String logType) {
    return sessions.keySet().stream()
        .filter(key -> key.endsWith("_" + logType))
        .count();
  }
}
