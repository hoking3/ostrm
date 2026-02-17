<template>
  <div class="min-h-screen">
    <AppHeader
      title="系统设置"
      :show-back-button="true"
      @logout="handleLogout"
      @change-password="handleChangePassword"
      @go-back="goBack"
      @open-settings="handleOpenSettings"
      @open-logs="handleOpenLogs"
    />
    
    <div class="max-w-4xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div class="animate-fade-in">
        <div class="text-center mb-8">
          <div class="w-16 h-16 bg-gradient-to-r from-purple-600 to-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572-1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826 3.31-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 0 11-6 0 3 3 0 016 0z"></path>
            </svg>
          </div>
          <h1 class="text-3xl font-bold gradient-text mb-2">系统设置</h1>
          <p class="text-gray-600">配置系统参数和功能选项</p>
        </div>

        <div class="space-y-8">
          <div class="glass-card">
            <div class="card-header">
              <h3 class="text-xl font-semibold">媒体文件设置</h3>
            </div>
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-3">
                  生成 STRM 媒体文件后缀
                </label>
                <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                  <div v-for="extension in availableExtensions" :key="extension" class="flex items-center">
                    <input
                      :id="extension"
                      v-model="selectedExtensions"
                      :value="extension"
                      type="checkbox"
                      class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    >
                    <label :for="extension" class="ml-2 block text-sm text-gray-900 font-medium">
                      {{ extension }}
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="card-header">
              <h3 class="text-xl font-semibold">TMDB API 配置</h3>
            </div>
            <div class="space-y-6">
              <div>
                <label for="tmdbApiKey" class="block text-sm font-semibold text-gray-700 mb-2">
                  TMDB API Key
                </label>
                <div class="flex rounded-xl shadow-sm">
                  <input
                    id="tmdbApiKey"
                    v-model="tmdbConfig.apiKey"
                    :type="showApiKey ? 'text' : 'password'"
                    class="input-field rounded-r-none"
                    placeholder="请输入 TMDB API Key"
                  />
                  <button
                    type="button"
                    @click="toggleApiKeyVisibility"
                    class="inline-flex items-center px-4 py-3 border border-l-0 border-gray-200 bg-gray-50 text-gray-700 rounded-r-xl hover:bg-gray-100 transition-colors"
                  >
                    {{ showApiKey ? '隐藏' : '显示' }}
                  </button>
                </div>
                <p class="mt-2 text-sm text-gray-500">
                  请在 <a href="https://www.themoviedb.org/settings/api" target="_blank" class="text-blue-600 hover:text-blue-700 font-medium">TMDB 官网</a> 申请 API Key
                </p>
              </div>

              <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div>
                  <label for="tmdbLanguage" class="block text-sm font-semibold text-gray-700 mb-2">
                    语言设置
                  </label>
                  <select
                    id="tmdbLanguage"
                    v-model="tmdbConfig.language"
                    class="input-field"
                  >
                    <option value="zh-CN">中文（简体）</option>
                    <option value="zh-TW">中文（繁体）</option>
                    <option value="en-US">English</option>
                  </select>
                </div>

                <div>
                  <label for="tmdbRegion" class="block text-sm font-semibold text-gray-700 mb-2">
                    地区设置
                  </label>
                  <select
                    id="tmdbRegion"
                    v-model="tmdbConfig.region"
                    class="input-field"
                  >
                    <option value="CN">中国</option>
                    <option value="TW">台湾</option>
                    <option value="HK">香港</option>
                    <option value="US">美国</option>
                  </select>
                </div>
              </div>

              <div class="bg-gray-50 rounded-xl p-4">
                <h4 class="text-md font-semibold text-gray-900 mb-3">HTTP 代理配置</h4>
                <p class="text-sm text-gray-600 mb-4">
                  如果需要通过代理访问 TMDB API，请配置以下选项（可选）
                </p>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label for="tmdbProxyHost" class="block text-sm font-semibold text-gray-700 mb-2">
                      代理主机地址
                    </label>
                    <input
                      id="tmdbProxyHost"
                      v-model="tmdbConfig.proxyHost"
                      type="text"
                      class="input-field"
                      placeholder="例如: 127.0.0.1"
                    />
                  </div>

                  <div>
                    <label for="tmdbProxyPort" class="block text-sm font-semibold text-gray-700 mb-2">
                      代理端口
                    </label>
                    <input
                      id="tmdbProxyPort"
                      v-model="tmdbConfig.proxyPort"
                      type="text"
                      class="input-field"
                      placeholder="例如: 7890"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="card-header">
              <h3 class="text-xl font-semibold">文件复制设置</h3>
            </div>
            <div class="space-y-4">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="flex items-center p-4 bg-gray-50 rounded-xl">
                  <input
                    id="copyExistingScrapingInfo"
                    v-model="fileCopyConfig.copyExistingScrapingInfo"
                    type="checkbox"
                    class="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <div class="ml-3">
                    <label for="copyExistingScrapingInfo" class="block text-sm font-semibold text-gray-900">
                      复制已存在的刮削信息
                    </label>
                    <p class="text-xs text-gray-500 mt-1">在STRM生成时复制媒体文件同级目录的NFO文件和刮削图片</p>
                  </div>
                </div>

                <div class="flex items-center p-4 bg-gray-50 rounded-xl">
                  <input
                    id="keepSubtitleFiles"
                    v-model="fileCopyConfig.keepSubtitleFiles"
                    type="checkbox"
                    class="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <div class="ml-3">
                    <label for="keepSubtitleFiles" class="block text-sm font-semibold text-gray-900">
                      保留字幕文件
                    </label>
                    <p class="text-xs text-gray-500 mt-1">复制媒体文件同级目录的.srt、.ass字幕文件到STRM目录</p>
                  </div>
                </div>

                <div class="flex items-center p-4 bg-gray-50 rounded-xl">
                  <input
                    id="overwriteExistingNfo"
                    v-model="fileCopyConfig.overwriteExistingNfo"
                    type="checkbox"
                    class="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <div class="ml-3">
                    <label for="overwriteExistingNfo" class="block text-sm font-semibold text-gray-900">
                      覆盖已存在的NFO文件
                    </label>
                    <p class="text-xs text-gray-500 mt-1">当NFO文件已存在时，是否覆盖</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="card-header">
              <h3 class="text-xl font-semibold">Emby 媒体库配置</h3>
            </div>
            <div class="space-y-6">
              <div>
                <label for="embyServerUrl" class="block text-sm font-semibold text-gray-700 mb-2">
                  Emby 服务器地址
                </label>
                <input
                  id="embyServerUrl"
                  v-model="embyConfig.serverUrl"
                  type="url"
                  placeholder="http://localhost:8096"
                  class="input-field"
                />
              </div>

              <div>
                <label for="embyApiKey" class="block text-sm font-semibold text-gray-700 mb-2">
                  API Key
                </label>
                <input
                  id="embyApiKey"
                  v-model="embyConfig.apiKey"
                  type="password"
                  placeholder="Emby API Key"
                  class="input-field"
                />
              </div>

              <p class="text-sm text-gray-500">
                配置全局 Emby 服务器信息，任务中可以选择使用此全局配置
              </p>
            </div>
          </div>

          <div class="glass-card">
            <div class="card-header">
              <h3 class="text-xl font-semibold">日志同级配置设置</h3>
            </div>
            <div class="space-y-6">
              <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div>
                  <label for="logRetentionDays" class="block text-sm font-semibold text-gray-700 mb-2">
                    日志保留时间
                  </label>
                  <select
                    id="logRetentionDays"
                    v-model.number="logConfig.retentionDays"
                    class="input-field"
                  >
                    <option :value="1">1天</option>
                    <option :value="3">3天</option>
                    <option :value="5">5天</option>
                    <option :value="7">7天</option>
                    <option :value="30">30天</option>
                  </select>
                  <p class="mt-1 text-xs text-gray-500">系统将在每天凌晨1:30自动清理过期日志文件</p>
                </div>

                <div>
                  <label for="logLevel" class="block text-sm font-semibold text-gray-700 mb-2">
                    日志级别
                  </label>
                  <select
                    id="logLevel"
                    v-model="logConfig.level"
                    class="input-field"
                  >
                    <option value="debug">Debug</option>
                    <option value="info">Info</option>
                    <option value="warn">Warn</option>
                    <option value="error">Error</option>
                  </select>
                  <p class="mt-1 text-xs text-gray-500">系统仅保留等于或高于所选级别的日志记录</p>
                </div>
              </div>

              <div class="border-t border-gray-200 pt-6">
                <div class="flex items-start space-x-3">
                  <input
                    id="reportUsageData"
                    v-model="logConfig.reportUsageData"
                    type="checkbox"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded mt-1"
                  >
                  <div class="flex-1">
                    <label for="reportUsageData" class="block text-sm font-semibold text-gray-700">
                      上报使用数据
                    </label>
                    <p class="mt-1 text-xs text-gray-500">
                      帮助我们改进产品体验。即使勾选此选项，也不会上报任何用户隐私信息，仅收集匿名的功能使用统计数据。
                    </p>
                  </div>
                </div>
              </div>

              <div class="p-4 bg-yellow-50 border border-yellow-200 rounded-xl">
                <div class="flex items-start">
                  <svg class="flex-shrink-0 h-5 w-5 text-yellow-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                  <div class="ml-3">
                    <h4 class="text-sm font-semibold text-yellow-800">注意事项</h4>
                    <div class="mt-1 text-sm text-yellow-700">
                      <ul class="list-disc list-inside space-y-1">
                        <li>日志级别变更将在下次应用重启后生效</li>
                        <li>Debug级别会产生大量日志，建议仅在调试时使用</li>
                        <li>日志清理任务会同时清理前端和后端的过期日志文件</li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="flex justify-end space-x-4">
            <button
              @click="goBack"
              type="button"
              class="btn-secondary"
            >
              取消
            </button>
            <button
              @click="saveSettings"
              type="button"
              class="btn-primary"
              :disabled="saving"
            >
              <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ saving ? '保存中...' : '保存设置' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="showSuccess" class="fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-xl shadow-2xl z-50 animate-slide-up">
      <div class="flex items-center">
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>
        设置保存成功！
      </div>
    </div>
    
    <div v-if="errorMessage" class="fixed top-4 right-4 bg-red-500 text-white px-6 py-3 rounded-xl shadow-2xl z-50 animate-slide-up">
      <div class="flex items-center">
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '~/components/AppHeader.vue'
import { authenticatedApiCall } from '~/utils/api.js'
import { useAuthStore } from '~/stores/auth.js'

const router = useRouter()
const authStore = useAuthStore()
const tokenCookie = useCookie('token')

const userInfo = computed(() => {
  return authStore.getUserInfo || { username: '用户' }
})

const availableExtensions = ref([])
const selectedExtensions = ref([])
const tmdbConfig = ref({
  apiKey: '',
  language: 'zh-CN',
  region: 'CN',
  proxyHost: '',
  proxyPort: ''
})
const fileCopyConfig = ref({
  copyExistingScrapingInfo: false,
  keepSubtitleFiles: false,
  overwriteExistingNfo: false
})
const logConfig = ref({
  retentionDays: 7,
  level: 'info',
  reportUsageData: true
})
const embyConfig = ref({
  serverUrl: '',
  apiKey: ''
})
const showApiKey = ref(false)
const saving = ref(false)
const showSuccess = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  await loadCurrentSettings()
})

const loadCurrentSettings = async () => {
  availableExtensions.value = ['.mp4', '.avi', '.mkv', '.mov', '.wmv', '.flv', '.webm', '.m4v', '.3gp', '.3g2', '.asf', '.divx', '.f4v', '.m2ts', '.m2v', '.mts', '.ogv', '.rm', '.rmvb', '.ts', '.vob', '.xvid', '.iso']

  try {
    const response = await authenticatedApiCall('/system/config')

    if (response && response.code === 200 && response.data) {
      const config = response.data

      if (config.mediaExtensions && Array.isArray(config.mediaExtensions)) {
        selectedExtensions.value = [...config.mediaExtensions]
      } else {
        selectedExtensions.value = ['.mp4', '.avi', '.rmvb', '.mkv']
      }

      if (config.tmdb && typeof config.tmdb === 'object') {
        tmdbConfig.value = {
          apiKey: config.tmdb.apiKey || '',
          language: config.tmdb.language || 'zh-CN',
          region: config.tmdb.region || 'CN',
          proxyHost: config.tmdb.proxyHost || '',
          proxyPort: config.tmdb.proxyPort || ''
        }
      }

      fileCopyConfig.value = {
        copyExistingScrapingInfo: config.copyExistingScrapingInfo === true,
        keepSubtitleFiles: config.keepSubtitleFiles === true,
        overwriteExistingNfo: config.overwriteExistingNfo === true
      }

      if (config.log && typeof config.log === 'object') {
        logConfig.value = {
          retentionDays: config.log.retentionDays || 7,
          level: config.log.level || 'info',
          reportUsageData: config.log.reportUsageData !== undefined ? config.log.reportUsageData : true
        }
      }

      if (config.emby && typeof config.emby === 'object') {
        embyConfig.value = {
          serverUrl: config.emby.serverUrl || '',
          apiKey: config.emby.apiKey || ''
        }
      }

    } else {
      selectedExtensions.value = ['.mp4', '.avi', '.rmvb', '.mkv', '.iso']
    }
  } catch (error) {
    selectedExtensions.value = ['.mp4', '.avi', '.rmvb', '.mkv', '.iso']
  }
}

const saveSettings = async () => {
  if (selectedExtensions.value.length === 0) {
    errorMessage.value = '请至少选择一个媒体文件后缀'
    setTimeout(() => {
      errorMessage.value = ''
    }, 3000)
    return
  }

  saving.value = true
  errorMessage.value = ''

  try {
    const configData = {
      mediaExtensions: selectedExtensions.value,
      tmdb: {
        apiKey: tmdbConfig.value.apiKey,
        language: tmdbConfig.value.language,
        region: tmdbConfig.value.region,
        proxyHost: tmdbConfig.value.proxyHost,
        proxyPort: tmdbConfig.value.proxyPort
      },
      copyExistingScrapingInfo: fileCopyConfig.value.copyExistingScrapingInfo,
      keepSubtitleFiles: fileCopyConfig.value.keepSubtitleFiles,
      overwriteExistingNfo: fileCopyConfig.value.overwriteExistingNfo,
      log: {
        retentionDays: logConfig.value.retentionDays,
        level: logConfig.value.level,
        reportUsageData: logConfig.value.reportUsageData
      },
      emby: {
        serverUrl: embyConfig.value.serverUrl,
        apiKey: embyConfig.value.apiKey
      }
    }

    const response = await authenticatedApiCall('/system/config', {
      method: 'POST',
      body: configData
    })

    if (response && response.code === 200) {
      showSuccess.value = true
      setTimeout(() => {
        showSuccess.value = false
      }, 3000)
    } else {
      errorMessage.value = response?.message || '保存设置失败'
      setTimeout(() => {
        errorMessage.value = ''
      }, 3000)
    }
  } catch (error) {
    errorMessage.value = error.data?.message || '保存设置失败'
    setTimeout(() => {
      errorMessage.value = ''
    }, 3000)
  } finally {
    saving.value = false
  }
}

const toggleApiKeyVisibility = () => {
  showApiKey.value = !showApiKey.value
}

const goBack = () => {
  router.back()
}

const handleLogout = () => {
  authStore.clearAuth()
  router.push('/login')
}

const handleChangePassword = () => {
  router.push('/change-password')
}

const handleOpenSettings = () => {
}

const handleOpenLogs = () => {
  router.push('/logs')
}
</script>

<style scoped>
</style>
