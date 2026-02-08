# fix-logs-domain-routing

## Why

日志页面在使用域名访问时返回 "Page not found"，但 localhost 可以正常访问。这表明存在路由配置差异，需要诊断并修复域名访问场景下的路由问题。

## What Changes

- 诊断 `/logs` 页面域名访问 "Page not found" 的根本原因
- 修复路由配置，确保域名访问与 localhost 行为一致
- 验证其他页面是否存在类似问题

## Capabilities

### New Capabilities
- `domain-routing-fix`: 诊断并修复域名访问时的路由配置问题

### Modified Capabilities
- 无

## Impact

- **前端路由**: Nuxt.js 页面路由配置
- **Web 服务器**: Nginx/Caddy 代理配置
- **环境配置**: 基础 URL 配置
