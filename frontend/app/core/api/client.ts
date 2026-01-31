/**
 * API 客户端
 * 基于 ofetch 的 Nuxt 原生支持
 */

import { useAuthStore } from '~/core/stores/auth.js'

// API 基础配置
const API_BASE_URL = '/'

// Token 刷新互斥锁
let isRefreshing = false
let refreshPromise: Promise<void> | null = null
const failedQueue: Array<{ resolve: () => void; reject: (err: Error) => void }> = []

const processQueue = (error: Error | null) => {
  failedQueue.forEach(prom => {
    if (error) prom.reject(error)
    else prom.resolve()
  })
  failedQueue.length = 0
}

// 获取认证 Token - 从localStorage/sessionStorage获取，与auth store保持一致
const getToken = (): string | null => {
  if (import.meta.server) return null

  // 优先从localStorage获取（记住我模式）
  let token = null
  if (typeof localStorage !== 'undefined') {
    const stored = localStorage.getItem('auth_token')
    // localStorage.getItem()返回null或字符串，确保有效值
    if (stored && stored !== 'undefined' && stored !== 'null') {
      token = stored
    }
  }

  // 如果localStorage中没有，尝试从sessionStorage获取
  if (!token && typeof sessionStorage !== 'undefined') {
    const stored = sessionStorage.getItem('auth_token')
    if (stored && stored !== 'undefined' && stored !== 'null') {
      token = stored
    }
  }

  // 调试日志
  if (import.meta.dev && token) {
    console.log('[API Client] 获取到Token:', token.substring(0, 50) + '...')
  } else if (import.meta.dev) {
    console.log('[API Client] 未找到Token')
  }

  return token
}

// 请求拦截器
const onRequest = (context: { request: string; options: { method?: string; headers?: Record<string, string> } }) => {
  const token = getToken()

  // 确保 headers 存在
  const options = context.options
  if (!options.headers) {
    options.headers = {}
  }

  // 添加认证头
  if (token) {
    options.headers['Authorization'] = `Bearer ${token}`
    if (import.meta.dev) {
      console.log('[API Request] Authorization头已设置:', `Bearer ${token.substring(0, 30)}...`)
    }
  } else if (import.meta.dev) {
    console.log('[API Request] 无Token，未设置Authorization头')
  }

  // 添加全局请求头
  options.headers['X-Requested-With'] = 'XMLHttpRequest'
  options.headers['Accept'] = 'application/json'

  // CSRF Token 处理
  if (import.meta.client) {
    const csrfToken = useCookie('XSRF-TOKEN')
    if (csrfToken.value && options.method !== 'GET') {
      options.headers['X-XSRF-TOKEN'] = csrfToken.value
    }
  }

  // 请求日志
  if (import.meta.dev) {
    console.log(`[API] ${options.method} ${context.request}`)
  }
}

// 401 错误处理 - Token 刷新
const handleUnauthorized = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (isRefreshing) {
      failedQueue.push({ resolve, reject })
      return
    }

    isRefreshing = true

    // 尝试刷新 Token
    $fetch('/api/auth/refresh', { method: 'POST' } as object)
      .then(() => {
        processQueue(null)
        resolve()
      })
      .catch((error) => {
        processQueue(error instanceof Error ? error : new Error('Token 刷新失败'))
        reject(error)
      })
      .finally(() => {
        isRefreshing = false
      })
  })
}

// 响应错误拦截器
const onResponseError = async (context: { response: { status: number; statusText: string; json(): Promise<unknown> }; request: string; options: object }) => {
  const response = context.response

  // 401 Unauthorized - Token 过期或无效
  if (response.status === 401) {
    try {
      await handleUnauthorized()
      // 重试原始请求
      return $fetch(context.request, context.options as object)
    } catch {
      // 刷新失败，跳转到登录页
      if (import.meta.client) {
        const authStore = useAuthStore()
        authStore.logout()
        window.location.href = '/auth/login'
      }
      throw new Error('登录已过期，请重新登录')
    }
  }

  // 403 Forbidden - 无权限
  if (response.status === 403) {
    throw new Error('您没有权限执行此操作')
  }

  // 422 Unprocessable Entity - 业务验证失败
  if (response.status === 422) {
    const data = await response.json().catch(() => ({}))
    const message = (data as Record<string, unknown>).message || '请求参数验证失败'
    throw new Error(message as string)
  }

  // 429 Too Many Requests - 限流
  if (response.status === 429) {
    throw new Error('请求过于频繁，请稍后再试')
  }

  // 500 Internal Server Error - 服务器错误
  if (response.status >= 500) {
    throw new Error('服务器错误，请稍后重试')
  }

  // 其他错误
  const errorText = response.statusText || '请求失败'
  throw new Error(errorText)
}

// 创建 ofetch 实例
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const $fetchApi = $fetch.create({
  baseURL: API_BASE_URL,
  credentials: 'include',
  timeout: 30000,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  onRequest: onRequest as any,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  onResponseError: onResponseError as any
})

// 便捷 API 方法
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const $api = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  get: <T = any>(url: string, options?: object) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $fetchApi<T>(url, { method: 'GET', ...options } as any),

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  post: <T = any>(url: string, body?: unknown, options?: object) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $fetchApi<T>(url, { method: 'POST', body, ...options } as any),

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  put: <T = any>(url: string, body?: unknown, options?: object) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $fetchApi<T>(url, { method: 'PUT', body, ...options } as any),

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  patch: <T = any>(url: string, body?: unknown, options?: object) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $fetchApi<T>(url, { method: 'PATCH', body, ...options } as any),

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  delete: <T = any>(url: string, options?: object) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $fetchApi<T>(url, { method: 'DELETE', ...options } as any)
}

// 导出类型
export type ApiResponse<T> = {
  data: T
  message?: string
  code?: number
}

// 重新导出 $fetch 以便直接使用
export { $fetch }

// 便捷 API 调用函数（带自动认证）
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const authenticatedApiCall = async <T = any>(
  url: string,
  options: { method?: string; body?: unknown; headers?: Record<string, string> } = {}
): Promise<T> => {
  const { method = 'GET', body, headers } = options

  // 确保 URL 以 /api 开头（Docker 生产环境需要）
  const apiUrl = url.startsWith('/api') ? url : `/api${url.startsWith('/') ? url : '/' + url}`

  return await $fetchApi<T>(apiUrl, {
    method,
    body,
    headers: {
      'Content-Type': 'application/json',
      ...headers
    }
  } as any)
}

// 便捷 API 调用函数（无认证）
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const apiCall = async <T = any>(
  url: string,
  options: { method?: string; body?: unknown; headers?: Record<string, string> } = {}
): Promise<T> => {
  const { method = 'GET', body, headers } = options

  // 确保 URL 以 /api 开头（Docker 生产环境需要）
  const apiUrl = url.startsWith('/api') ? url : `/api${url.startsWith('/') ? url : '/' + url}`

  return await $fetch<T>(apiUrl, {
    method,
    body,
    headers: {
      'Content-Type': 'application/json',
      ...headers
    }
  } as any)
}
