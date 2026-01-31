/**
 * 认证中间件 - 需要登录才能访问
 */

import { useAuthStore } from '~/core/stores/auth.js'

export default defineNuxtRouteMiddleware((_to) => {
  const authStore = useAuthStore()

  // 恢复认证状态
  authStore.restoreAuth()

  console.log('[Auth Middleware] 认证状态检查:')
  console.log('- isLoggedIn:', authStore.isLoggedIn)
  console.log('- token存在:', !!authStore.getToken)
  console.log('- isAuthenticated:', authStore.isAuthenticated)

  // 检查是否已登录
  if (!authStore.isAuthenticated) {
    // 未登录，跳转到登录页
    console.log('[Auth Middleware] 未认证，跳转登录页')
    return navigateTo('/auth/login', { replace: true })
  }

  console.log('[Auth Middleware] 认证通过，允许访问')
})
