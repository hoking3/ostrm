/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Quarkus 应用入口
 *
 * @author hienao
 * @since 2025-12-31
 */
@QuarkusMain
public class ApplicationService {

  public static void main(String... args) {
    Quarkus.run(args);
  }
}
