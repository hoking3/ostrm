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
  <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <div class="text-center">
        <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent"></div>
        <p class="mt-4 text-gray-600 text-lg">加载中...</p>
      </div>
    </div>

    <template v-else>
      <!-- 配置信息卡片 -->
      <div class="glass-card mb-6 animate-fade-in">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-lg leading-6 font-medium text-gray-900">配置信息</h3>
            <p class="mt-1 max-w-2xl text-sm text-gray-500">当前 OpenList 配置详情</p>
          </div>
          <span :class="configInfo?.isActive ? 'status-active' : 'status-inactive'"
                class="status-badge">
            {{ configInfo?.isActive ? '启用' : '禁用' }}
          </span>
        </div>

        <div class="mt-5 border-t border-gray-200 pt-5" v-if="configInfo">
          <dl class="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
            <div>
              <dt class="text-sm font-medium text-gray-500">用户名</dt>
              <dd class="mt-1 text-sm text-gray-900">{{ configInfo.username }}</dd>
            </div>
            <div>
              <dt class="text-sm font-medium text-gray-500">Base URL</dt>
              <dd class="mt-1 text-sm text-gray-900 break-all">{{ configInfo.baseUrl }}</dd>
            </div>
            <div>
              <dt class="text-sm font-medium text-gray-500">Base Path</dt>
              <dd class="mt-1 text-sm text-gray-900">{{ configInfo.basePath || '/' }}</dd>
            </div>
            <div>
              <dt class="text-sm font-medium text-gray-500">创建时间</dt>
              <dd class="mt-1 text-sm text-gray-900">{{ formatDate(configInfo.createdAt) }}</dd>
            </div>
          </dl>
        </div>
      </div>

      <!-- 任务管理区域 -->
      <div class="glass-card animate-fade-in" style="animation-delay: 0.1s">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-lg leading-6 font-medium text-gray-900">任务管理</h3>
            <p class="mt-1 text-sm text-gray-500">管理您的 STRM 生成任务</p>
          </div>
          <button type="button" class="btn-primary" @click="showCreateTaskModal = true">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
            </svg>
            创建任务
          </button>
        </div>

        <!-- 任务列表 -->
        <div class="space-y-4" v-if="tasks.length > 0">
          <div class="card" v-for="task in tasks" :key="task.id">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between mb-4 gap-3">
              <h4 class="text-lg font-medium text-gray-900">{{ task.taskName }}</h4>
              <div class="flex items-center space-x-2 flex-wrap gap-y-2">
                <span :class="task.isActive ? 'status-active' : 'status-inactive'" class="status-badge">
                  {{ task.isActive ? '启用' : '禁用' }}
                </span>
                <button class="p-2 text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition-all duration-200"
                        @click="editTask(task)" title="编辑">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                  </svg>
                </button>
                <button class="p-2 text-green-600 hover:text-green-700 hover:bg-green-50 rounded-lg transition-all duration-200"
                        @click="showExecuteModal(task.id)" :disabled="generatingStrm[task.id]" title="立即执行">
                  <svg v-if="generatingStrm[task.id]" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </button>
                <button class="p-2 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-all duration-200"
                        @click="deleteTask(task.id)" title="删除">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                  </svg>
                </button>
              </div>
            </div>

            <div class="grid grid-cols-1 gap-x-4 gap-y-3 sm:grid-cols-2">
              <div>
                <dt class="text-sm font-medium text-gray-500">路径</dt>
                <dd class="mt-1 text-sm text-gray-900 break-all">{{ task.path }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">STRM路径</dt>
                <dd class="mt-1 text-sm text-gray-900 break-all">{{ task.strmPath }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">定时任务</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ task.cron || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">上次执行</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ formatDate(task.lastExecTime) }}</dd>
              </div>
            </div>

            <div class="mt-3 flex items-center space-x-4 flex-wrap gap-y-2">
              <label class="flex items-center text-sm" :class="task.needScrap ? 'text-gray-600' : 'text-gray-500'">
                <input type="checkbox" :checked="task.needScrap" disabled class="mr-2 h-4 w-4 text-blue-600 rounded">
                需要刮削
              </label>
              <label class="flex items-center text-sm text-gray-600">
                <input type="checkbox" :checked="task.isIncrement" disabled class="mr-2 h-4 w-4 text-blue-600 rounded">
                增量更新
              </label>
            </div>

            <div class="mt-3" v-if="task.renameRegex">
              <dt class="text-sm font-medium text-gray-500">重命名正则表达式</dt>
              <dd class="mt-1 text-sm text-gray-900 font-mono bg-gray-50 px-3 py-2 rounded break-all">{{ task.renameRegex }}</dd>
            </div>

            <div class="mt-3 text-xs text-gray-500">
              创建时间: {{ formatDate(task.createdAt) }}
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div class="text-center py-12" v-else>
          <div class="w-16 h-16 bg-gradient-to-r from-gray-400 to-gray-500 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path>
            </svg>
          </div>
          <h3 class="text-lg font-medium text-gray-900 mb-2">暂无任务配置</h3>
          <p class="text-gray-500 mb-6">创建您的第一个任务配置</p>
          <button type="button" class="btn-primary" @click="showCreateTaskModal = true">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
            </svg>
            创建第一个任务
          </button>
        </div>
      </div>
    </template>

    <!-- 创建/编辑任务模态框 -->
    <Teleport to="body">
      <div v-if="showCreateTaskModal || showEditTaskModal" class="modal-overlay animate-fade-in" @click="closeModal">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in w-full max-w-lg" @click.stop>
            <div class="card-header">
              <div class="flex items-center justify-between">
                <h3 class="text-xl font-semibold">
                  {{ showCreateTaskModal ? '创建任务' : '编辑任务' }}
                </h3>
                <button @click="closeModal" class="text-white/80 hover:text-white transition-colors">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                  </svg>
                </button>
              </div>
            </div>

            <form @submit.prevent="submitTask" class="space-y-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">任务名称 *</label>
                <input v-model="taskForm.taskName" type="text" required class="input-field">
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">任务路径 *</label>
                <input v-model="taskForm.path" type="text" required class="input-field">
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">STRM路径</label>
                <div class="flex">
                  <span class="inline-flex items-center px-3 rounded-l-xl border border-r-0 border-gray-200 bg-gray-50 text-gray-500 text-sm">
                    /app/backend/strm/
                  </span>
                  <input v-model="strmSubPath" type="text" placeholder="子路径（可选）"
                         class="input-field rounded-l-none">
                </div>
                <p class="mt-1 text-xs text-gray-500">前缀 /app/backend/strm/ 固定不可修改</p>
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">定时任务表达式</label>
                <input v-model="taskForm.cron" type="text" placeholder="例如: 0 15 10 ? * *"
                       class="input-field">
                <p class="mt-1 text-xs text-gray-500">Cron表达式格式，留空表示不启用定时任务</p>
              </div>

              <div>
                <div class="flex items-center justify-between mb-2">
                  <label class="block text-sm font-semibold text-gray-700">
                    重命名正则表达式
                  </label>
                  <button type="button"
                          @click="showRenameRegexHelp = !showRenameRegexHelp"
                          class="text-gray-400 hover:text-blue-600 transition-colors"
                          :class="{ 'text-blue-500': showRenameRegexHelp }">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                  </button>
                </div>
                <input v-model="taskForm.renameRegex" type="text" placeholder="留空表示不需要重命名"
                       class="input-field">
                <p class="mt-1 text-xs text-gray-500">用于文件重命名的正则表达式，留空表示不需要重命名</p>

                <!-- 帮助提示框 -->
                <div v-if="showRenameRegexHelp"
                     class="mt-3 p-4 bg-blue-50 border border-blue-200 rounded-xl">
                  <h4 class="text-sm font-semibold text-blue-800 mb-2">使用说明</h4>
                  <div class="text-xs text-blue-700 space-y-2">
                    <p><strong>格式：</strong>原始模式|替换内容</p>
                    <p><strong>示例：</strong></p>
                    <ul class="list-disc list-inside ml-2 space-y-1">
                      <li>移除方括号和圆括号： <code class="bg-blue-100 px-1 rounded">[\[\]()]|</code></li>
                      <li>空格替换为下划线： <code class="bg-blue-100 px-1 rounded">\s+|_</code></li>
                      <li>添加前缀： <code class="bg-blue-100 px-1 rounded">^|Movie_</code></li>
                      <li>移除分辨率： <code class="bg-blue-100 px-1 rounded">\s*\d{3,4}p\s*|</code></li>
                    </ul>
                    <p class="mt-2"><strong>注意：</strong>如果正则表达式格式错误，将使用原始文件名</p>
                  </div>
                </div>
              </div>

              <div class="space-y-3">
                <label class="flex items-start">
                  <input v-model="taskForm.needScrap" type="checkbox"
                         class="mt-1 h-4 w-4 text-blue-600 rounded border-gray-300">
                  <span class="ml-2 text-sm text-gray-700">
                    需要刮削
                    <span class="block text-xs text-gray-500 mt-0.5">启用TMDB刮削功能，生成NFO和封面</span>
                  </span>
                </label>

                <label class="flex items-center">
                  <input v-model="taskForm.isIncrement" type="checkbox"
                         class="h-4 w-4 text-blue-600 rounded border-gray-300">
                  <span class="ml-2 text-sm text-gray-700">增量更新</span>
                </label>

                <label class="flex items-center">
                  <input v-model="taskForm.isActive" type="checkbox"
                         class="h-4 w-4 text-blue-600 rounded border-gray-300">
                  <span class="ml-2 text-sm text-gray-700">启用任务</span>
                </label>
              </div>

              <div class="flex justify-end space-x-3 pt-4">
                <button type="button" @click="closeModal" class="btn-secondary">
                  取消
                </button>
                <button type="submit" :disabled="submitting" class="btn-primary">
                  <svg v-if="submitting" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ submitting ? '保存中...' : (showCreateTaskModal ? '创建' : '保存') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 执行模式选择模态框 -->
    <Teleport to="body">
      <div v-if="showExecuteTaskModal" class="modal-overlay animate-fade-in" @click="closeExecuteModal">
        <div class="flex items-center justify-center min-h-screen p-4">
          <div class="modal-content animate-scale-in w-full max-w-md" @click.stop>
            <div class="card-header">
              <div class="flex items-center justify-between">
                <h3 class="text-xl font-semibold">选择执行模式</h3>
                <button @click="closeExecuteModal" class="text-white/80 hover:text-white transition-colors">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                  </svg>
                </button>
              </div>
            </div>

            <div class="space-y-4">
              <p class="text-sm text-gray-600">请选择任务执行模式：</p>

              <button @click="executeTask(currentTaskId, false)"
                      class="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-blue-50 border border-gray-200 hover:border-blue-300 rounded-xl transition-all duration-200">
                <div class="text-left">
                  <div class="font-medium text-gray-900">全量执行</div>
                  <div class="text-sm text-gray-500">清空STRM目录，重新生成所有文件</div>
                </div>
                <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                </svg>
              </button>

              <button @click="executeTask(currentTaskId, true)"
                      class="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-blue-50 border border-gray-200 hover:border-blue-300 rounded-xl transition-all duration-200">
                <div class="text-left">
                  <div class="font-medium text-gray-900">增量执行</div>
                  <div class="text-sm text-gray-500">只处理变化的文件，清理孤立文件</div>
                </div>
                <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import logger from '~/core/utils/logger'
import { useRoute, useRouter } from 'vue-router'
import { apiCall, authenticatedApiCall } from '~/core/api/client'

// 路由相关
const route = useRoute()
const router = useRouter()
const configId = route.params.id

// 响应式数据
const configInfo = ref(null)
const tasks = ref([])
const loading = ref(true)
const showCreateTaskModal = ref(false)
const showEditTaskModal = ref(false)
const showExecuteTaskModal = ref(false)
const submitting = ref(false)
const editingTaskId = ref(null)
const currentTaskId = ref(null)
const generatingStrm = ref({})
const taskForm = ref({
  taskName: '',
  path: '',
  strmPath: '/app/backend/strm',
  cron: '',
  needScrap: false,
  renameRegex: '',
  isIncrement: true,
  isActive: true
})
const strmSubPath = ref('')
const showRenameRegexHelp = ref(false)

// 获取配置信息
const getConfigInfo = async () => {
  try {
    loading.value = true

    const response = await authenticatedApiCall(`/openlist-config/${configId}`, {
      method: 'GET'
    })

    if (response.code === 200) {
      configInfo.value = response.data
    } else {
      logger.error('获取配置信息失败:', response.message)
      await navigateTo('/')
    }
  } catch (error) {
    logger.error('获取配置信息时发生错误:', error)
    await navigateTo('/')
  } finally {
    loading.value = false
  }
}

// 获取任务列表
const fetchTasks = async () => {
  try {
    const response = await authenticatedApiCall('/task-config', {
      method: 'GET'
    })

    if (response.code === 200) {
      // 过滤出属于当前配置的任务
      tasks.value = response.data.filter(task => task.openlistConfigId == configId)
    }
  } catch (error) {
    logger.error('获取任务列表失败:', error)
  }
}

// 重置任务表单
const resetTaskForm = () => {
  taskForm.value = {
    taskName: '',
    path: '',
    strmPath: '/app/backend/strm',
    cron: '',
    needScrap: false,
    renameRegex: '',
    isIncrement: true,
    isActive: true
  }
  strmSubPath.value = ''
  showRenameRegexHelp.value = false
}

// 编辑任务
const editTask = (task) => {
  editingTaskId.value = task.id
  taskForm.value = {
    taskName: task.taskName,
    path: task.path,
    strmPath: task.strmPath,
    cron: task.cron || '',
    needScrap: task.needScrap || false,
    renameRegex: task.renameRegex || '',
    isIncrement: task.isIncrement,
    isActive: task.isActive
  }
  // 解析STRM路径，提取子路径
  const prefix = '/app/backend/strm/'
  if (task.strmPath && task.strmPath.startsWith(prefix)) {
    strmSubPath.value = task.strmPath.substring(prefix.length)
  } else {
    strmSubPath.value = ''
  }
  showEditTaskModal.value = true
}

// 验证任务路径是否存在（通过后端代理，避免CORS问题）
const validateTaskPath = async (taskPath) => {
  try {
    if (!configInfo.value) {
      throw new Error('配置信息未加载')
    }

    // 调用后端代理接口进行路径验证
    const response = await authenticatedApiCall('/openlist-config/validate-path', {
      method: 'POST',
      body: {
        baseUrl: configInfo.value.baseUrl,
        token: configInfo.value.token,
        basePath: configInfo.value.basePath,
        taskPath: taskPath
      }
    })

    if (response.code === 200) {
      return true
    } else {
      throw new Error(response.message || '路径验证失败')
    }
  } catch (error) {
    throw new Error(error.message || '路径验证失败，请检查路径是否正确')
  }
}

// 提交任务
const submitTask = async () => {
  try {
    submitting.value = true

    // 先验证任务路径是否存在
    if (taskForm.value.path) {
      await validateTaskPath(taskForm.value.path)
    }

    // 合并STRM路径
    const fullStrmPath = '/app/backend/strm/' + (strmSubPath.value || '')

    const taskData = {
      ...taskForm.value,
      strmPath: fullStrmPath,
      openlistConfigId: parseInt(configId)
    }

    let response
    if (showCreateTaskModal.value) {
      // 创建任务
      response = await authenticatedApiCall('/task-config', {
        method: 'POST',
        body: taskData
      })
    } else {
      // 更新任务
      response = await authenticatedApiCall(`/task-config/${editingTaskId.value}`, {
        method: 'PUT',
        body: taskData
      })
    }

    if (response.code === 200) {
      await fetchTasks()
      closeModal()
    } else {
      throw new Error(response.message || '操作失败')
    }
  } catch (error) {
    logger.error('提交任务失败:', error)
    alert(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 删除任务
const deleteTask = async (taskId) => {
  if (!confirm('确定要删除这个任务吗？')) {
    return
  }

  try {
    const response = await authenticatedApiCall(`/task-config/${taskId}`, {
      method: 'DELETE'
    })

    if (response.code === 200) {
      await fetchTasks()
    } else {
      throw new Error(response.message || '删除失败')
    }
  } catch (error) {
    logger.error('删除任务失败:', error)
  }
}

// 关闭模态框
const closeModal = () => {
  showCreateTaskModal.value = false
  showEditTaskModal.value = false
  editingTaskId.value = null
  resetTaskForm()
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp || timestamp === 0) {
    return '未执行'
  }
  return new Date(timestamp).toLocaleString('zh-CN')
}

// 显示执行模式选择弹窗
const showExecuteModal = (taskId) => {
  currentTaskId.value = taskId
  showExecuteTaskModal.value = true
}

// 关闭执行模式选择弹窗
const closeExecuteModal = () => {
  showExecuteTaskModal.value = false
  currentTaskId.value = null
}

// 执行任务
const executeTask = async (taskId, isIncremental) => {
  try {
    generatingStrm.value[taskId] = true
    closeExecuteModal()

    const response = await authenticatedApiCall(`/task-config/${taskId}/submit`, {
      method: 'POST',
      body: {
        isIncremental: isIncremental
      }
    })

    if (response.code === 200) {
      const modeText = isIncremental ? '增量' : '全量'
      alert(`任务已提交，正在后台执行${modeText}生成STRM文件...`)
    } else {
      throw new Error(response.message || '提交任务失败')
    }
  } catch (error) {
    logger.error('提交任务失败:', error)
    alert(error.message || '提交任务失败，请稍后重试')
  } finally {
    generatingStrm.value[taskId] = false
  }
}

// 组件挂载时获取配置信息和任务列表
onMounted(() => {
  getConfigInfo()
  fetchTasks()
})
</script>
