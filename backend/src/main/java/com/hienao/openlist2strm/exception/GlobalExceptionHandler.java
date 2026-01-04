/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.exception;

import com.hienao.openlist2strm.dto.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * 全局业务异常处理器 - Quarkus JAX-RS 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<BusinessException> {

  @Override
  public Response toResponse(BusinessException ex) {
    Log.error("Business Error Handled ===> " + ex.getMessage(), ex);
    ApiResponse<Void> response = ApiResponse.error(500, ex.getMessage());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(response)
        .build();
  }
}
