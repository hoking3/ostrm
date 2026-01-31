/**
 * 访客中间件 - 仅允许未登录用户访问
 */

import { useAuthStore } from '~/core/stores/auth.js'

export default defineNuxtRouteMiddleware((_to) => {
  const authStore = useAuthStore()

  // 恢复认证状态
  authStore.restoreAuth()

  // 如果已登录，跳转到首页
  if (authStore.isAuthenticated) {
    return navigateTo('/', { replace: true })
  }
})
