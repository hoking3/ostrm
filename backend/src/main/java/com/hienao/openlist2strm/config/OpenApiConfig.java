/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * OpenAPI 配置 - Quarkus MicroProfile OpenAPI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@OpenAPIDefinition(info = @Info(title = "OStrm API", version = "1.0", description = "OStrm 单用户系统 API 文档"), security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "请在此处输入JWT token")
public class OpenApiConfig extends Application {
}
