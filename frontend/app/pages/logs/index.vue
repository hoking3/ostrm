<!--
  Ostrm - Stream Management System
  Copyright (C) 2024 Ostrm Project

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->

<template>
  <!-- 主要内容 -->
    <main class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0 space-y-6">
        <!-- 日志控制面板 -->
        <div class="card">
          <div class="sm:px-6 py-4 border-b border-white/6">
            <!-- 标题 -->
            <div class="mb-4 sm:mb-0">
              <h2 class="text-lg font-medium text-white">日志控制面板</h2>
              <p class="text-sm text-white/40 mt-1">查看和管理系统运行日志</p>
            </div>

            <!-- 控制项 - 移动端垂直布局，桌面端水平布局 -->
            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-4 sm:space-y-0 sm:space-x-4">
              <!-- 左侧控制组 -->
              <div class="flex flex-col sm:flex-row sm:items-center space-y-3 sm:space-y-0 sm:space-x-4">
                <!-- 日志类型选择 -->
                <div class="flex items-center space-x-2">
                  <label class="text-sm font-medium text-white/70 whitespace-nowrap">日志类型:</label>
                  <v-select
                    v-model="selectedLogType"
                    :options="logTypeOptions"
                    :reduce="(opt: any) => opt.value"
                    :clearable="false"
                    @update:modelValue="switchLogType"
                    class="vue-select-py"
                  />
                </div>

                <!-- 日志级别筛选 -->
                <div class="flex items-center space-x-2">
                  <label class="text-sm font-medium text-white/70 whitespace-nowrap">日志级别:</label>
                  <v-select
                    v-model="selectedLogLevel"
                    :options="logLevelOptions"
                    :reduce="(opt: any) => opt.value"
                    :clearable="false"
                    @update:modelValue="filterLogsByLevel"
                    class="vue-select-py"
                  />
                </div>

                <!-- 重新加载按钮 -->
                <button
                  @click="loadLogs"
                  :disabled="loading"
                  class="btn-secondary text-sm py-2"
                >
                  <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4 mr-1 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                  </svg>
                  {{ loading ? '加载中...' : '重新加载' }}
                </button>
              </div>

              <!-- 右侧操作按钮 -->
              <div class="flex flex-col sm:flex-row items-stretch sm:items-center space-y-2 sm:space-y-0 sm:space-x-2">
                <button
                  @click="downloadLogs"
                  :disabled="downloading"
                  class="btn-secondary text-sm py-2"
                >
                  <svg v-if="downloading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4 mr-1 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                  </svg>
                  {{ downloading ? '下载中...' : '下载日志' }}
                </button>
                <button
                  @click="showDeleteDialog"
                  :disabled="deleting"
                  class="btn-danger text-sm py-2"
                >
                  <svg v-if="deleting" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4 mr-1 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                  </svg>
                  {{ deleting ? '删除中...' : '删除日志' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 日志显示区域 -->
        <div class="card p-0 overflow-hidden">
          <div class="px-4 py-3 bg-[#1A1A24] border-b border-white/6">
            <div class="flex justify-between items-center">
              <h3 class="text-sm font-medium text-white">
                {{ selectedLogType === 'backend' ? '后端日志' : '前端日志' }}
                <span class="text-white/40">({{ logLines.length }} 行)</span>
              </h3>
              <div class="text-xs text-white/40">
                最后更新: {{ lastUpdateTime || '暂无数据' }}
              </div>
            </div>
          </div>

          <!-- 日志内容 (Virtual Scroll) -->
          <div
            class="bg-[#0A0A0F] text-emerald-400 font-mono text-sm"
            style="height: 500px;"
          >
            <div v-if="loading" class="flex justify-center items-center h-full">
              <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-400"></div>
              <p class="ml-3 text-emerald-400">加载日志中...</p>
            </div>

            <div v-else-if="logLines.length === 0" class="flex justify-center items-center h-full">
              <p class="text-white/30">暂无日志数据</p>
            </div>

            <DynamicScroller
              v-else
              ref="scroller"
              class="h-full scrollbar-thin"
              :items="logLines"
              :min-item-size="28"
              key-field="id"
              @update="onScrollerUpdate"
            >
              <template v-slot="{ item, index, active }">
                <DynamicScrollerItem
                  :item="item"
                  :active="active"
                  :size-dependencies="[
                    item.text,
                  ]"
                  :data-index="index"
                >
                  <div
                    class="whitespace-pre-wrap break-words px-4 py-1 hover:bg-white/5 transition-colors flex"
                    :class="item.class"
                  >
                    <span class="text-white/30 mr-2 select-none w-10 text-right flex-shrink-0">{{ item.lineNum }}</span>
                    <span>{{ item.text }}</span>
                  </div>
                </DynamicScrollerItem>
              </template>
            </DynamicScroller>
          </div>
        </div>

        <!-- 日志统计信息 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="stat-card">
            <div class="flex items-center">
              <div class="flex-shrink-0">
                <div class="w-10 h-10 bg-blue-500/10 rounded-xl flex items-center justify-center">
                  <svg class="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                  </svg>
                </div>
              </div>
              <div class="ml-4">
                <p class="text-sm text-white/40">总行数</p>
                <p class="text-2xl font-bold text-white tabular-nums">{{ logLines.length }}</p>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="flex items-center">
              <div class="flex-shrink-0">
                <div class="w-10 h-10 bg-red-500/10 rounded-xl flex items-center justify-center">
                  <svg class="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </div>
              </div>
              <div class="ml-4">
                <p class="text-sm text-white/40">错误日志</p>
                <p class="text-2xl font-bold text-red-400 tabular-nums">{{ logStats.error }}</p>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="flex items-center">
              <div class="flex-shrink-0">
                <div class="w-10 h-10 bg-amber-500/10 rounded-xl flex items-center justify-center">
                  <svg class="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
                  </svg>
                </div>
              </div>
              <div class="ml-4">
                <p class="text-sm text-white/40">警告日志</p>
                <p class="text-2xl font-bold text-amber-400 tabular-nums">{{ logStats.warn }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 删除确认对话框 -->
        <Teleport to="body">
          <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div class="bg-[#1A1A24] rounded-lg p-6 max-w-md mx-4 border border-white/10">
              <h3 class="text-lg font-medium text-white mb-4">确认删除</h3>
              <p class="text-white/70 mb-6">
                确定要删除 {{ selectedLogType === 'backend' ? '后端' : '前端' }} 日志文件吗？此操作不可恢复。
              </p>
              <div class="flex justify-end space-x-3">
                <button @click="showDeleteConfirm = false" class="btn-secondary text-sm py-2">
                  取消
                </button>
                <button @click="deleteLogs" class="btn-danger text-sm py-2">
                  确认删除
                </button>
              </div>
            </div>
          </div>
        </Teleport>
      </div>
    </main>
</template>

<script setup>
import { ref, onMounted, nextTick, reactive } from 'vue'
import { apiCall } from '~/core/api/client'
import { useAuthStore } from '~/core/stores/auth'
import logger from '~/core/utils/logger'

// 引入 vue-virtual-scroller
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

// 页面元数据
definePageMeta({
  layout: 'default',
  pageTitle: '系统日志',
  middleware: 'auth'
})

// 认证状态
const authStore = useAuthStore()

// 响应式数据
const selectedLogType = ref('backend')
const selectedLogLevel = ref('all')
const logLines = ref([]) // 显示的日志行
const originalLogLines = ref([]) // 所有日志行
const loading = ref(false)
const downloading = ref(false)
const deleting = ref(false)
const lastUpdateTime = ref('')
const scroller = ref(null)
const showDeleteConfirm = ref(false)

// Vue Select 选项数据
const logTypeOptions = [
  { label: '后端日志', value: 'backend' },
  { label: '前端日志', value: 'frontend' }
]

const logLevelOptions = [
  { label: '全部', value: 'all' },
  { label: 'Error', value: 'error' },
  { label: 'Warn', value: 'warn' },
  { label: 'Info', value: 'info' },
  { label: 'Debug', value: 'debug' },
  { label: 'Trace', value: 'trace' }
]

// 增量统计
const logStats = reactive({
  error: 0,
  warn: 0,
  info: 0,
  debug: 0,
  trace: 0
})

// 全局计数器，确保唯一ID
let logIdCounter = 0

// 定义日志级别优先级
const LOG_LEVEL_PRIORITY = {
  'trace': 0,
  'debug': 1,
  'info': 2,
  'warn': 3,
  'error': 4
}

// 解析日志级别和样式
const parseLogLine = (line) => {
  const lineStr = String(line)
  const lowerLine = lineStr.toLowerCase()
  let level = 'info'
  let cssClass = 'text-green-400'

  if (lowerLine.includes('error') || lowerLine.includes('exception') || lowerLine.includes('failed')) {
    level = 'error'
    cssClass = 'text-red-400'
  } else if (lowerLine.includes('warn') || lowerLine.includes('warning')) {
    level = 'warn'
    cssClass = 'text-yellow-400'
  } else if (lowerLine.includes('info')) {
    level = 'info'
    cssClass = 'text-blue-400'
  } else if (lowerLine.includes('debug')) {
    level = 'debug'
    cssClass = 'text-gray-400'
  } else if (lowerLine.includes('trace')) {
    level = 'trace'
    cssClass = 'text-gray-500'
  }

  return {
    id: logIdCounter++,
    text: lineStr,
    level: level,
    class: cssClass,
    lineNum: String(logIdCounter).padStart(4, '0')
  }
}

// 重置统计
const resetStats = () => {
  logStats.error = 0
  logStats.warn = 0
  logStats.info = 0
  logStats.debug = 0
  logStats.trace = 0
  logIdCounter = 0
}

// 更新统计
const updateStats = (level) => {
  if (logStats[level] !== undefined) {
    logStats[level]++
  }
}

// 批量添加日志
const batchAddLogs = (lines) => {
  const newEntries = []

  for (const line of lines) {
    const entry = parseLogLine(line)
    newEntries.push(entry)
    updateStats(entry.level)
  }

  // 更新原始数据
  originalLogLines.value.push(...newEntries)

  // 更新显示数据
  if (selectedLogLevel.value === 'all') {
    logLines.value.push(...newEntries)
  } else {
    const selectedPriority = LOG_LEVEL_PRIORITY[selectedLogLevel.value]
    const filteredEntries = newEntries.filter(entry =>
      LOG_LEVEL_PRIORITY[entry.level] >= selectedPriority
    )
    logLines.value.push(...filteredEntries)
  }

  lastUpdateTime.value = new Date().toLocaleString('zh-CN')
}

// 按日志级别筛选
const filterLogsByLevel = () => {
  if (selectedLogLevel.value === 'all') {
    logLines.value = [...originalLogLines.value]
  } else {
    const selectedPriority = LOG_LEVEL_PRIORITY[selectedLogLevel.value]
    logLines.value = originalLogLines.value.filter(entry =>
      LOG_LEVEL_PRIORITY[entry.level] >= selectedPriority
    )
  }
}

// 切换日志类型
const switchLogType = () => {
  selectedLogLevel.value = 'all'
  loadLogs()
}

// 加载日志
const loadLogs = async () => {
  loading.value = true
  logLines.value = []
  originalLogLines.value = []
  lastUpdateTime.value = ''
  resetStats()

  try {
    const response = await apiCall(`/logs/${selectedLogType.value}`, {
      method: 'GET'
    })

    if (response.code === 200) {
      if (response.data && Array.isArray(response.data)) {
        batchAddLogs(response.data)
      }
    } else {
      logger.error('获取日志失败:', response.message)
    }
  } catch (error) {
    logger.error('获取日志错误:', error)
  } finally {
    loading.value = false
  }
}

// 下载日志
const downloadLogs = async () => {
  downloading.value = true
  try {
    const config = useRuntimeConfig()
    const baseURL = config.public.apiBase || 'http://localhost:8080'
    const downloadUrl = `${baseURL}/logs/${selectedLogType.value}/download`

    const response = await fetch(downloadUrl, { method: 'GET' })

    if (response.ok) {
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${selectedLogType.value}-${new Date().toISOString().split('T')[0]}.log`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } else {
      const errorText = await response.text()
      alert('下载失败: ' + (errorText || '未知错误'))
    }
  } catch (error) {
    logger.error('下载日志错误:', error)
    alert('下载失败: ' + (error.message || '网络错误'))
  } finally {
    downloading.value = false
  }
}

// 显示删除确认对话框
const showDeleteDialog = () => {
  showDeleteConfirm.value = true
}

// 删除日志
const deleteLogs = async () => {
  deleting.value = true
  showDeleteConfirm.value = false

  try {
    const response = await apiCall(`/logs/${selectedLogType.value}`, {
      method: 'DELETE'
    })

    if (response.code === 200) {
      // 清空前端显示
      logLines.value = []
      originalLogLines.value = []
      lastUpdateTime.value = ''
      resetStats()
      alert('日志已删除')
    } else {
      alert('删除失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    logger.error('删除日志错误:', error)
    alert('删除失败: ' + (error.message || '网络错误'))
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  authStore.restoreAuth()
  loadLogs()
})
</script>

<style scoped>
/* 覆盖 vue-virtual-scroller 默认样式以适配我们的黑色主题 */
:deep(.vue-recycle-scroller__item-wrapper) {
  box-sizing: border-box;
}

:deep(.vue-recycle-scroller.ready .vue-recycle-scroller__item-view) {
  will-change: transform;
}

/* Vue Select 缩小高度样式 */
.vue-select-py {
  --vs-height: 36px;
  min-width: 120px;
  width: 120px;
}

.vue-select-py :deep(.vs__dropdown-toggle) {
  padding-top: 4px;
  padding-bottom: 4px;
  min-height: 36px;
}

.vue-select-py :deep(.vs__search) {
  font-size: 0.875rem;
  line-height: 1.25rem;
}
</style>
