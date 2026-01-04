/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * 缓存服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class CacheService {

  @CacheResult(cacheName = "verify-code")
  public String getVerifyCodeBy(String identify) {
    Log.debug("缓存未命中: " + identify);
    return null;
  }

  @CacheResult(cacheName = "verify-code")
  public String upsertVerifyCodeBy(String identify, String value) {
    return value;
  }

  @CacheInvalidate(cacheName = "verify-code")
  public void removeVerifyCodeBy(String identify) {
    Log.debug("移除验证码缓存: " + identify);
  }

  @CacheInvalidateAll(cacheName = "verify-code")
  public void clearAllVerifyCode() {
    Log.debug("清除所有验证码缓存");
  }
}
