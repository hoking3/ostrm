/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * 统一路径配置类 - Quarkus ConfigMapping 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ConfigMapping(prefix = "app.paths")
public interface PathConfiguration {

  @WithDefault("/maindata/log")
  String logs();

  @WithDefault("/maindata")
  String data();

  @WithDefault("/maindata/db/openlist2strm.db")
  String database();

  @WithDefault("/maindata/config")
  String config();

  @WithDefault("/app/backend/strm")
  String strm();

  @WithDefault("/maindata/config/userInfo.json")
  String userInfo();

  @WithDefault("/maindata/log/frontend")
  String frontendLogs();
}
