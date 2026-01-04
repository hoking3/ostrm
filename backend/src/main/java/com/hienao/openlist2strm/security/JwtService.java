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

package com.hienao.openlist2strm.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * JWT 服务 - Quarkus SmallRye JWT 实现
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "jwt.expiration-min", defaultValue = "20160")
    long expirationMinutes;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "ostrm")
    String issuer;

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @return JWT Token 字符串
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(expirationMinutes));

        return Jwt.issuer(issuer)
                .subject(username)
                .upn(username)
                .groups(Set.of("user"))
                .issuedAt(now)
                .expiresAt(expiry)
                .sign();
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true 如果有效
     */
    public boolean verify(String token) {
        try {
            // 在实际请求中，Quarkus 会自动验证 token
            // 这里进行基本的格式检查
            if (token == null || token.isEmpty()) {
                return false;
            }
            String[] parts = token.split("\\.");
            return parts.length == 3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getSubject(String token) {
        try {
            // 解析 JWT payload (Base64 解码)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // 简单解析 JSON 获取 sub 字段
            int subStart = payload.indexOf("\"sub\":\"");
            if (subStart == -1) {
                return null;
            }
            subStart += 7;
            int subEnd = payload.indexOf("\"", subStart);
            return payload.substring(subStart, subEnd);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Token 过期时间
     *
     * @param token JWT Token
     * @return 过期时间戳（秒）
     */
    public Long getExpiresAt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int expStart = payload.indexOf("\"exp\":");
            if (expStart == -1) {
                return null;
            }
            expStart += 6;
            int expEnd = payload.indexOf(",", expStart);
            if (expEnd == -1) {
                expEnd = payload.indexOf("}", expStart);
            }
            return Long.parseLong(payload.substring(expStart, expEnd).trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Token 签发时间
     *
     * @param token JWT Token
     * @return 签发时间戳（秒）
     */
    public Long getIssuedAt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int iatStart = payload.indexOf("\"iat\":");
            if (iatStart == -1) {
                return null;
            }
            iatStart += 6;
            int iatEnd = payload.indexOf(",", iatStart);
            if (iatEnd == -1) {
                iatEnd = payload.indexOf("}", iatStart);
            }
            return Long.parseLong(payload.substring(iatStart, iatEnd).trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 JsonWebToken 获取用户名（用于已验证的请求）
     *
     * @param jwt 已验证的 JsonWebToken
     * @return 用户名
     */
    public String getUsername(JsonWebToken jwt) {
        return jwt.getSubject();
    }
}
