# fix-logs-domain-routing

## Context

### 问题描述
- `http://localhost:3111/logs` 可以正常打开
- 使用域名访问 `/logs` 时显示 "Page not found"
- 其他页面（首页、设置页等）没有这个问题

### 环境分析

**Web 服务器配置**：
- Caddyfile: SPA 路由使用 `@spa` matcher + `rewrite * /index.html`
- nginx.conf: 使用 `try_files $uri $uri/ /index.html`
- 两者都应该正确处理 `/logs` 路由

**页面配置对比**：
| 页面 | 文件位置 | auth middleware |
|------|----------|-----------------|
| 首页 | `pages/index.vue` | ✅ 有 |
| 设置页 | `pages/settings/index.vue` | ❓ 未知 |
| 日志页 | `pages/logs/index.vue` | ❌ **缺失** |
| 修改密码 | `pages/auth/change-password.vue` | ✅ 有 |

### 可能原因
1. **日志页面缺少 auth middleware** - 可能导致路由守卫异常
2. **浏览器缓存** - 域名可能缓存了旧的 404 响应
3. **Nuxt 路由注册问题** - 页面组件可能未正确注册

## Goals / Non-Goals

**Goals**:
- 诊断并修复 `/logs` 页面域名访问 "Page not found" 问题
- 确保日志页面与其他受保护页面行为一致
- 验证修复后 localhost 和域名访问均正常工作

**Non-Goals**:
- 不修改 Caddyfile/nginx.conf 配置（当前配置正确）
- 不进行大规模架构变更

## Decisions

### 1. 添加 auth middleware 到日志页面

**决策**: 在日志页面的 `definePageMeta` 中添加 `middleware: 'auth'`

**原因**:
- 其他受保护页面（首页、修改密码）都有 auth middleware
- 日志页面是管理功能，应该需要登录才能访问
- 缺少 middleware 可能导致 Nuxt 路由守卫异常

**代码位置**: `frontend/app/pages/logs/index.vue:258-261`

```javascript
// 修改前
definePageMeta({
  layout: 'default',
  pageTitle: '系统日志'
})

// 修改后
definePageMeta({
  layout: 'default',
  pageTitle: '系统日志',
  middleware: 'auth'
})
```

### 2. 验证设置页面配置

**决策**: 检查设置页面是否有正确的 middleware 配置

**原因**:
- 确保所有受保护页面有一致的配置
- 防止类似问题在其他页面出现

## Risks / Trade-offs

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 修复后问题仍存在 | 低 | 需要进一步排查 web 服务器配置 | 准备备用方案：检查 Caddyfile 路由规则 |
| 添加 middleware 后重定向循环 | 极低 | 用户无法访问日志页面 | middleware 逻辑简单，不会导致循环 |

## Open Questions

1. **域名访问 vs localhost 访问的本质差异是什么？**
   - 可能需要用户提供更多环境信息（是否使用反向代理、CDN 等）
2. **浏览器缓存是否需要清理？**
   - 建议用户清除浏览器缓存或使用无痕模式测试
