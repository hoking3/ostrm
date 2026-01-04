/*
 * OStrm - Stream Management System
 * Copyright (C) 2024 OStrm Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.sign.ChangePasswordDto;
import com.hienao.openlist2strm.dto.sign.SignInDto;
import com.hienao.openlist2strm.dto.sign.SignUpDto;
import com.hienao.openlist2strm.security.JwtService;
import com.hienao.openlist2strm.service.SignService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 认证管理控制器 - Quarkus JAX-RS 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "认证管理", description = "用户认证相关接口")
public class SignController {

    @Inject
    SignService signService;

    @Inject
    JwtService jwtService;

    @POST
    @Path("/sign-in")
    @Operation(summary = "用户登录", description = "用户登录接口，成功后返回JWT token")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "登录成功", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiResponse.class))),
            @APIResponse(responseCode = "400", description = "登录失败")
    })
    public Response signIn(@Valid SignInDto signInDto) {
        String username = signService.signIn(signInDto);
        String token = jwtService.generateToken(username);
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("token", token);
        data.put("expiresAt", jwtService.getExpiresAt(token));
        return Response.ok(ApiResponse.success(data, "登录成功")).build();
    }

    @POST
    @Path("/sign-up")
    @Operation(summary = "用户注册", description = "用户注册接口")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "注册成功"),
            @APIResponse(responseCode = "400", description = "注册失败")
    })
    public Response signUp(@Valid SignUpDto signUpDto) {
        signService.signUp(signUpDto);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(null, "注册成功"))
                .build();
    }

    @GET
    @Path("/check-user")
    @Operation(summary = "检查用户是否存在", description = "检查用户是否存在的接口")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "用户存在"),
            @APIResponse(responseCode = "404", description = "用户不存在")
    })
    public Response checkUser() {
        boolean exists = signService.checkUserExists();
        Map<String, Object> data = new HashMap<>();
        data.put("exists", exists);
        if (exists) {
            return Response.ok(ApiResponse.success(data, "用户存在")).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponse<>(404, "用户不存在", data))
                    .build();
        }
    }

    @POST
    @Path("/sign-out")
    @Authenticated
    @Operation(summary = "用户登出", description = "用户登出接口，需要JWT认证")
    @SecurityRequirement(name = "Bearer Authentication")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "登出成功"),
            @APIResponse(responseCode = "401", description = "未授权")
    })
    public Response signOut() {
        // Quarkus JWT 是无状态的，客户端只需删除 token 即可
        return Response.ok(ApiResponse.success(null, "登出成功")).build();
    }

    @POST
    @Path("/refresh")
    @Authenticated
    @Operation(summary = "刷新Token", description = "刷新JWT token接口，需要JWT认证")
    @SecurityRequirement(name = "Bearer Authentication")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Token刷新成功"),
            @APIResponse(responseCode = "401", description = "Token无效")
    })
    public Response refresh(@Context HttpHeaders headers) {
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String oldToken = authHeader.substring(7);
            if (jwtService.verify(oldToken)) {
                String username = jwtService.getSubject(oldToken);
                String newToken = jwtService.generateToken(username);
                Map<String, Object> data = new HashMap<>();
                data.put("token", newToken);
                data.put("expiresAt", jwtService.getExpiresAt(newToken));
                return Response.ok(ApiResponse.success(data, "Token刷新成功")).build();
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error(401, "Token无效，无法刷新"))
                .build();
    }

    @GET
    @Path("/validate")
    @Authenticated
    @Operation(summary = "验证Token", description = "验证JWT token是否有效，需要JWT认证")
    @SecurityRequirement(name = "Bearer Authentication")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Token有效"),
            @APIResponse(responseCode = "401", description = "Token无效")
    })
    public Response validate(@Context HttpHeaders headers) {
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        Map<String, Object> data = new HashMap<>();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.verify(token)) {
                String username = jwtService.getSubject(token);
                data.put("valid", true);
                data.put("username", username);
                data.put("expiresAt", jwtService.getExpiresAt(token));
                data.put("issuedAt", jwtService.getIssuedAt(token));
                return Response.ok(ApiResponse.success(data, "Token有效")).build();
            }
        }
        data.put("valid", false);
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error(401, "Token无效"))
                .build();
    }

    @POST
    @Path("/change-password")
    @Authenticated
    @Operation(summary = "修改密码", description = "修改用户密码接口，需要JWT认证")
    @SecurityRequirement(name = "Bearer Authentication")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "密码修改成功"),
            @APIResponse(responseCode = "400", description = "密码修改失败"),
            @APIResponse(responseCode = "401", description = "未授权")
    })
    public Response changePassword(@Valid ChangePasswordDto changePasswordDto) {
        signService.changePassword(changePasswordDto);
        return Response.ok(ApiResponse.success(null, "密码修改成功")).build();
    }
}
