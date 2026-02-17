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
  <div class="min-h-screen">
    <AppHeader
      title="文件浏览器"
      :user-info="userInfo"
      :show-back-button="true"
      @logout="logout"
      @change-password="changePassword"
      @open-settings="openSettings"
      @open-logs="openLogs"
      @go-back="goBack"
    />

    <main class="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div class="glass-card mb-6 animate-fade-in">
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center space-x-3 flex-1 min-w-0">
            <div class="flex-shrink-0 w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-500 rounded-xl flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
              </svg>
            </div>
            <div class="min-w-0">
              <h2 class="text-xl font-bold gradient-text truncate" :title="config?.username || '加载中...'">{{ config?.username || '加载中...' }}</h2>
              <p class="text-sm text-gray-500 truncate" :title="config?.baseUrl || ''">{{ config?.baseUrl || '' }}</p>
            </div>
          </div>

          <div class="flex items-center flex-wrap gap-2">
            <template v-if="filteredFiles.length > 0">
              <label class="flex items-center space-x-2 px-3 py-2 bg-gray-100 rounded-lg">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleSelectAll"
                  class="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                />
                <span class="text-sm text-gray-600">全选</span>
              </label>
            </template>
            <template v-if="selectedFiles.size > 0">
              <span class="text-sm text-gray-600 bg-gray-100 px-3 py-2 rounded-lg">
                已选择 {{ selectedFiles.size }} 个文件
              </span>
              <button
                @click="clearSelection"
                class="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-all duration-200"
              >
                取消选择
              </button>
            </template>
            <button
              @click="openBatchRenameDialog"
              :disabled="selectedFiles.size === 0"
              :class="[
                'px-4 py-2 rounded-lg transition-all duration-200 flex items-center space-x-2',
                selectedFiles.size > 0 
                  ? 'bg-blue-600 text-white hover:bg-blue-700' 
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              ]"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
              </svg>
              <span>批量重命名</span>
            </button>
            <button
              @click="openTmdbMatchDialog"
              :disabled="selectedFiles.size === 0"
              :class="[
                'px-4 py-2 rounded-lg transition-all duration-200 flex items-center space-x-2',
                selectedFiles.size > 0 
                  ? 'bg-green-600 text-white hover:bg-green-700' 
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              ]"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"></path>
              </svg>
              <span>TMDB识别</span>
            </button>
            <div class="relative flex-1 min-w-[200px]">
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索当前目录..."
                class="input-field pl-10"
                @keyup.enter="handleSearch"
              />
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
              </svg>
            </div>
            
            <div class="flex items-center bg-gray-100 rounded-xl p-1" role="group" aria-label="视图切换">
              <button
                @click="setViewMode('grid')"
                :class="[
                  'p-2 rounded-lg transition-all duration-200',
                  viewMode === 'grid' 
                    ? 'bg-white text-blue-600 shadow-sm' 
                    : 'text-gray-500 hover:text-gray-700 hover:bg-gray-200'
                ]"
                :aria-pressed="viewMode === 'grid'"
                title="网格视图"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H14a2 2 0 01-2-2V14zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V16zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H14a2 2 0 01-2-2V16z"></path>
                </svg>
              </button>
              <button
                @click="setViewMode('list')"
                :class="[
                  'p-2 rounded-lg transition-all duration-200',
                  viewMode === 'list' 
                    ? 'bg-white text-blue-600 shadow-sm' 
                    : 'text-gray-500 hover:text-gray-700 hover:bg-gray-200'
                ]"
                :aria-pressed="viewMode === 'list'"
                title="列表视图"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16"></path>
                </svg>
              </button>
            </div>
            
            <button
              @click="refresh"
              class="p-2 text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-xl transition-all duration-200"
              title="刷新"
            >
              <svg :class="{ 'animate-spin': loading }" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div class="glass-card mb-6 animate-fade-in">
        <nav class="flex items-center space-x-2 text-sm overflow-x-auto pb-2">
          <button
            @click="navigateTo('/')"
            class="flex items-center space-x-1 text-blue-600 hover:text-blue-700 transition-colors whitespace-nowrap"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
            </svg>
            <span>根目录</span>
          </button>
          <template v-for="(segment, index) in breadcrumbSegments" :key="index">
            <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
            </svg>
            <button
              @click="navigateTo(breadcrumbPaths[index])"
              class="text-gray-600 hover:text-blue-600 transition-colors whitespace-nowrap font-medium"
            >
              {{ segment }}
            </button>
          </template>
        </nav>
      </div>

      <div v-if="loading" class="flex justify-center items-center py-20">
        <div class="text-center">
          <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent"></div>
          <p class="mt-4 text-gray-600 text-lg">加载中...</p>
        </div>
      </div>

      <div v-else-if="error" class="glass-card text-center py-12 animate-fade-in">
        <div class="w-20 h-20 bg-gradient-to-r from-red-500 to-red-600 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <h3 class="text-xl font-semibold text-gray-900 mb-2">加载失败</h3>
        <p class="text-gray-600 mb-6">{{ error }}</p>
        <button @click="refresh" class="btn-primary">
          重试
        </button>
      </div>

      <div v-else-if="filteredFiles.length === 0" class="glass-card text-center py-12 animate-fade-in">
        <div class="w-20 h-20 bg-gradient-to-r from-gray-400 to-gray-500 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
          </svg>
        </div>
        <h3 class="text-xl font-semibold text-gray-900 mb-2">{{ isSearching ? '未找到文件' : '目录为空' }}</h3>
        <p class="text-gray-600">{{ isSearching ? '尝试使用其他关键词搜索' : '该目录下没有文件或文件夹' }}</p>
      </div>

      <div v-else class="animate-slide-up">
        <div v-if="viewMode === 'grid'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 transition-all duration-300">
          <div
            v-for="file in filteredFiles"
            :key="file.path || file.name"
            :class="[
              'card group cursor-pointer transform hover:scale-102 transition-all duration-300',
              isFileSelected(file) ? 'ring-2 ring-blue-500 bg-blue-50' : ''
            ]"
            @click="handleFileClick(file)"
          >
            <div class="flex items-start space-x-4">
              <div class="flex-shrink-0">
                <input
                  type="checkbox"
                  :checked="isFileSelected(file)"
                  @click.stop="toggleFileSelection(file)"
                  class="w-5 h-5 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                />
              </div>
              <div class="flex-shrink-0">
                <div v-if="file.type === 'folder'" class="w-12 h-12 bg-gradient-to-r from-yellow-400 to-yellow-500 rounded-xl flex items-center justify-center">
                  <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                  </svg>
                </div>
                <div v-else class="w-12 h-12 bg-gradient-to-r from-blue-400 to-blue-500 rounded-xl flex items-center justify-center">
                  <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                  </svg>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-sm font-semibold text-gray-900 truncate" :title="file.name">{{ file.name }}</h3>
                <p class="text-xs text-gray-500 mt-1">
                  {{ formatFileSize(file.size) }}
                </p>
                <p v-if="file.modified" class="text-xs text-gray-400 mt-1">
                  {{ formatDate(file.modified) }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="space-y-2 transition-all duration-300">
          <div class="card flex items-center space-x-4 px-4 py-3 bg-gray-50 rounded-xl text-sm font-semibold text-gray-600">
            <div class="flex-shrink-0 w-10"></div>
            <div class="flex-shrink-0 w-10"></div>
            <div class="flex-1 min-w-0">
              <button
                @click="setSortBy('name')"
                class="flex items-center space-x-1 hover:text-blue-600 transition-colors"
              >
                <span>文件名</span>
                <svg v-if="sortBy === 'name'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path v-if="sortOrder === 'asc'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"></path>
                  <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                </svg>
              </button>
            </div>
            <div class="hidden sm:flex items-center space-x-6">
              <button
                @click="setSortBy('size')"
                class="flex items-center space-x-1 hover:text-blue-600 transition-colors"
              >
                <span>大小</span>
                <svg v-if="sortBy === 'size'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path v-if="sortOrder === 'asc'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"></path>
                  <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                </svg>
              </button>
              <button
                @click="setSortBy('modified')"
                class="flex items-center space-x-1 hover:text-blue-600 transition-colors"
              >
                <span>修改时间</span>
                <svg v-if="sortBy === 'modified'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path v-if="sortOrder === 'asc'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"></path>
                  <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                </svg>
              </button>
            </div>
            <div class="flex-shrink-0 w-10"></div>
          </div>
          <div
            v-for="file in filteredFiles"
            :key="file.path || file.name"
            :class="[
              'card group cursor-pointer flex items-center space-x-4 p-4 transition-all duration-200 hover:bg-gray-50',
              isFileSelected(file) ? 'ring-2 ring-blue-500 bg-blue-50' : ''
            ]"
            @click="handleFileClick(file)"
          >
            <div class="flex-shrink-0">
              <input
                type="checkbox"
                :checked="isFileSelected(file)"
                @click.stop="toggleFileSelection(file)"
                class="w-5 h-5 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
              />
            </div>
            <div class="flex-shrink-0">
              <div v-if="file.type === 'folder'" class="w-10 h-10 bg-gradient-to-r from-yellow-400 to-yellow-500 rounded-xl flex items-center justify-center">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                </svg>
              </div>
              <div v-else class="w-10 h-10 bg-gradient-to-r from-blue-400 to-blue-500 rounded-xl flex items-center justify-center">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                </svg>
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-sm font-semibold text-gray-900 truncate" :title="file.name">{{ file.name }}</h3>
            </div>
            <div class="hidden sm:flex items-center space-x-6 text-sm text-gray-500">
              <span class="whitespace-nowrap">{{ formatFileSize(file.size) }}</span>
              <span v-if="file.modified" class="whitespace-nowrap">{{ formatDate(file.modified) }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 批量重命名对话框 -->
    <div v-if="showBatchRenameDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-2xl max-w-5xl w-full max-h-[90vh] overflow-hidden">
        <div class="p-6 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-gray-900">批量重命名</h3>
            <button
              @click="closeBatchRenameDialog"
              class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>

        <div class="p-6 overflow-y-auto max-h-[60vh]">
          <div class="flex items-center gap-4 mb-6 flex-wrap">
            <div class="flex items-center gap-2">
              <label class="text-sm font-medium text-gray-700">模式</label>
              <select v-model="renameConfig.mode" class="input-field" @change="updateRenamePreview">
                <option value="magic">魔法变量</option>
                <option value="regex">正则替换</option>
                <option value="sequence">顺序命名</option>
              </select>
            </div>
            <template v-if="renameConfig.mode === 'magic'">
              <div class="flex items-center gap-2">
                <label class="text-sm font-medium text-gray-700">命名模板</label>
                <select v-model="renameConfig.template" class="input-field" @change="updateRenamePreview">
                  <option value="auto">自动</option>
                  <option value="movie">电影</option>
                  <option value="tv">剧集</option>
                </select>
              </div>
              <input
                v-model="renameConfig.customPattern"
                type="text"
                placeholder="例如: S{season}E{episode}.{ext}"
                class="input-field flex-1 min-w-[200px]"
                @input="updateRenamePreview"
              />
            </template>
            <button
              @click="updateRenamePreview"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all"
            >
              预览
            </button>
            <button
              @click="executeBatchRename"
              :disabled="renameProcessing"
              class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-all flex items-center space-x-2"
            >
              <svg v-if="renameProcessing" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
              </svg>
              <span>应用</span>
            </button>
          </div>

          <template v-if="renameConfig.mode === 'magic' || renameConfig.mode === 'sequence'">
            <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">前缀</label>
                <input
                  v-model="renameConfig.prefix"
                  type="text"
                  placeholder="例如: Movie_"
                  class="input-field"
                  @input="updateRenamePreview"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">后缀</label>
                <input
                  v-model="renameConfig.suffix"
                  type="text"
                  placeholder="例如: _HD"
                  class="input-field"
                  @input="updateRenamePreview"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">序号格式</label>
                <input
                  v-model="renameConfig.numberFormat"
                  type="text"
                  placeholder="例如: %03d"
                  class="input-field"
                  @input="updateRenamePreview"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">起始序号</label>
                <input
                  v-model.number="renameConfig.startNumber"
                  type="number"
                  min="1"
                  class="input-field"
                  @input="updateRenamePreview"
                />
              </div>
            </div>
          </template>

          <template v-if="renameConfig.mode === 'regex'">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">查找正则</label>
                <input
                  v-model="renameConfig.regexPattern"
                  type="text"
                  placeholder="例如: (.*?)\\.S(\\d+)E(\\d+)"
                  class="input-field font-mono"
                  @input="updateRenamePreview"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">替换为</label>
                <input
                  v-model="renameConfig.regexReplacement"
                  type="text"
                  placeholder="例如: $1 - S$2E$3"
                  class="input-field font-mono"
                  @input="updateRenamePreview"
                />
              </div>
            </div>
          </template>

          <div class="mb-6">
            <h4 class="text-sm font-medium text-gray-700 mb-3">重命名预览</h4>
            <div class="bg-gray-50 rounded-xl overflow-hidden">
              <div class="grid grid-cols-2 gap-4 p-4 bg-gray-100 text-sm font-medium text-gray-700">
                <span>原文件名</span>
                <span>新文件名</span>
              </div>
              <div class="divide-y divide-gray-200 max-h-64 overflow-y-auto">
                <div
                  v-for="(preview, index) in renamePreviewList"
                  :key="index"
                  class="grid grid-cols-2 gap-4 p-4 text-sm"
                >
                  <span class="text-gray-600 truncate" :title="preview.original">{{ preview.original }}</span>
                  <span :class="['truncate', preview.original !== preview.new ? 'text-blue-600 font-medium' : 'text-gray-400']" :title="preview.new">{{ preview.new }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 border-t border-gray-200 flex items-center justify-between">
          <button
            v-if="canUndoRename"
            @click="undoRename"
            class="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-all"
          >
            撤销上一次重命名
          </button>
          <div class="flex items-center space-x-3 ml-auto">
            <button
              @click="closeBatchRenameDialog"
              class="px-6 py-2.5 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-all"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- TMDB匹配对话框 -->
    <div v-if="showTmdbMatchDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-2xl max-w-6xl w-full max-h-[90vh] overflow-hidden">
        <div class="p-6 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-gray-900">TMDB智能识别</h3>
            <button
              @click="closeTmdbMatchDialog"
              class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>

        <div class="p-6 overflow-y-auto max-h-[60vh]">
          <div v-if="tmdbMatching" class="flex justify-center items-center py-12">
            <div class="text-center">
              <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent"></div>
              <p class="mt-4 text-gray-600">正在识别文件...</p>
            </div>
          </div>

          <div v-else>
            <div v-if="!tmdbSearchCompleted" class="mb-6 p-4 bg-blue-50 rounded-xl border border-blue-200">
              <h4 class="font-semibold text-gray-800 mb-3">搜索设置</h4>
              <p class="text-sm text-gray-600 mb-4">
                请输入影视名称和年份（可选）进行搜索。<br/>
                电影格式：<code class="bg-gray-200 px-1 rounded">电影名 (年份)</code><br/>
                电视剧格式：<code class="bg-gray-200 px-1 rounded">电视剧名 - S01E01</code>
              </p>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">
                    影视名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="tmdbSearchConfig.title"
                    type="text"
                    placeholder="例如: 白色橄榄树"
                    class="input-field"
                    @input="validateTmdbSearch"
                  />
                  <p v-if="tmdbSearchErrors.title" class="text-sm text-red-500 mt-1">
                    {{ tmdbSearchErrors.title }}
                  </p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">
                    年份 <span class="text-gray-400">(可选)</span>
                  </label>
                  <input
                    v-model="tmdbSearchConfig.year"
                    type="number"
                    min="1900"
                    :max="new Date().getFullYear() + 5"
                    placeholder="例如: 2025"
                    class="input-field"
                    @input="validateTmdbSearch"
                  />
                  <p v-if="tmdbSearchErrors.year" class="text-sm text-red-500 mt-1">
                    {{ tmdbSearchErrors.year }}
                  </p>
                </div>
              </div>
              <div class="mt-4">
                <button
                  @click="startTmdbMatchWithConfig"
                  :disabled="!isTmdbSearchValid || tmdbMatching"
                  class="btn-primary flex items-center space-x-2"
                >
                  <svg v-if="tmdbMatching" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                  </svg>
                  <span>开始识别</span>
                </button>
              </div>
            </div>

            <div v-else class="mb-4">
              <div class="flex items-center justify-between mb-4">
                <span class="text-sm text-gray-600">
                  共 {{ tmdbMatchResults.length }} 个文件，{{ matchedCount }} 个匹配成功
                </span>
                <div class="flex items-center space-x-2">
                  <button
                    @click="resetTmdbSearch"
                    class="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-all text-sm"
                  >
                    重新设置
                  </button>
                  <button
                    @click="startTmdbMatch"
                    class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all text-sm"
                  >
                    重新识别
                  </button>
                </div>
              </div>

              <div class="space-y-3">
                <div
                  v-for="(result, index) in tmdbMatchResults"
                  :key="index"
                  class="p-4 rounded-xl border"
                  :class="result.matched ? 'border-green-200 bg-green-50' : 'border-gray-200 bg-gray-50'"
                >
                  <div class="flex items-start justify-between">
                    <div class="flex items-start space-x-4 flex-1">
                      <div class="flex-shrink-0">
                        <input
                          v-if="result.matched"
                          v-model="result.apply"
                          type="checkbox"
                          class="w-5 h-5 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                        />
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center space-x-2 mb-1">
                          <span class="font-medium text-gray-900 truncate" :title="result.fileName">
                            {{ result.fileName }}
                          </span>
                          <span
                            v-if="result.matched"
                            class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800"
                          >
                            匹配成功
                          </span>
                          <span
                            v-else
                            class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800"
                          >
                            未找到匹配
                          </span>
                        </div>
                        <div v-if="result.matched" class="space-y-1">
                          <div class="flex items-center space-x-4 text-sm">
                            <span class="text-gray-600">
                              {{ result.title }}
                            </span>
                            <span v-if="result.season !== null && result.episode !== null" class="text-blue-600 font-medium">
                              S{{ String(result.season).padStart(2, '0') }}E{{ String(result.episode).padStart(2, '0') }}
                            </span>
                            <span v-if="result.matchScore" class="text-purple-600">
                              得分: {{ result.matchScore }}
                            </span>
                          </div>
                          <div class="text-sm text-gray-500">
                            新文件名: <span class="text-gray-900">{{ result.newFileName }}</span>
                          </div>
                        </div>
                        <div v-else class="text-sm text-red-500">
                          {{ result.error || '未找到匹配的影视内容' }}
                        </div>
                      </div>
                    </div>
                    <div class="flex-shrink-0 ml-4">
                      <button
                        v-if="!result.matched"
                        @click="openManualSearch(result, index)"
                        class="text-sm text-blue-600 hover:text-blue-800"
                      >
                        手动搜索
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 border-t border-gray-200 flex items-center justify-end space-x-3">
          <button
            @click="closeTmdbMatchDialog"
            class="px-6 py-2.5 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-all"
          >
            取消
          </button>
          <button
            @click="applyTmdbRenameOnly"
            :disabled="tmdbRenameOnlyProcessing || tmdbRenameAndScrapProcessing || applyCount === 0"
            class="px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all flex items-center space-x-2"
          >
            <svg v-if="tmdbRenameOnlyProcessing" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <span>仅重命名 {{ applyCount }} 个</span>
          </button>
          <button
            @click="applyTmdbRenameAndScrap"
            :disabled="tmdbRenameOnlyProcessing || tmdbRenameAndScrapProcessing || applyCount === 0"
            class="btn-primary flex items-center space-x-2"
          >
            <svg v-if="tmdbRenameAndScrapProcessing" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <span>重命名并刮削 {{ applyCount }} 个</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 手动搜索对话框 -->
    <div v-if="showManualSearchDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-2xl max-w-3xl w-full max-h-[90vh] overflow-hidden">
        <div class="p-6 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-gray-900">手动选择</h3>
            <button
              @click="closeManualSearchDialog"
              class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-all"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>

        <div class="p-6">
          <div class="mb-6">
            <div class="flex space-x-3">
              <input
                v-model="manualSearchQuery"
                type="text"
                placeholder="搜索影视内容..."
                class="input-field flex-1"
                @keyup.enter="executeManualSearch"
              />
              <select v-model="manualSearchType" class="input-field w-32">
                <option value="all">全部</option>
                <option value="movie">电影</option>
                <option value="tv">电视剧</option>
              </select>
              <button
                @click="executeManualSearch"
                :disabled="manualSearching"
                class="btn-primary"
              >
                {{ manualSearching ? '搜索中...' : '搜索' }}
              </button>
            </div>
          </div>

          <div v-if="manualSearchResults.length > 0" class="space-y-3 max-h-96 overflow-y-auto">
            <div
              v-for="(item, index) in manualSearchResults"
              :key="index"
              class="p-4 rounded-xl border border-gray-200 hover:border-blue-300 hover:bg-blue-50 cursor-pointer transition-all"
              @click="selectManualSearchResult(item)"
            >
              <div class="flex items-start space-x-4">
                <div v-if="item.posterUrl" class="flex-shrink-0">
                  <img :src="item.posterUrl" :alt="item.title" class="w-16 h-24 object-cover rounded-lg" />
                </div>
                <div v-else class="flex-shrink-0 w-16 h-24 bg-gray-200 rounded-lg"></div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-gray-900">{{ item.title }}</h4>
                  <p class="text-sm text-gray-500">{{ item.year || item.releaseYear }}</p>
                  <p v-if="item.overview" class="text-sm text-gray-600 mt-2 line-clamp-2">{{ item.overview }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '~/components/AppHeader.vue'
import { authenticatedApiCall } from '~/utils/api.js'
import { useAuthStore } from '~/stores/auth.js'
import logger from '~/utils/logger.js'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const configId = computed(() => route.params.id)
const userInfo = computed(() => authStore.getUserInfo || { username: '用户' })

const config = ref(null)
const currentPath = ref('/')
const files = ref([])
const searchKeyword = ref('')
const isSearching = ref(false)
const loading = ref(false)
const error = ref(null)

const STORAGE_KEY = 'file-browser-view-mode'
const viewMode = ref(localStorage.getItem(STORAGE_KEY) || 'grid')
const sortBy = ref('name')
const sortOrder = ref('asc')

const selectedFiles = ref(new Map())

const showBatchRenameDialog = ref(false)
const showTmdbMatchDialog = ref(false)
const showManualSearchDialog = ref(false)

const renameConfig = ref({
  mode: 'magic',
  template: 'auto',
  customPattern: '',
  prefix: '',
  suffix: '',
  numberFormat: '%03d',
  startNumber: 1,
  regexPattern: '',
  regexReplacement: ''
})
const renamePreviewList = ref([])
const renameProcessing = ref(false)
const lastRenameHistory = ref(null)
const canUndoRename = ref(false)

const tmdbMatching = ref(false)
const tmdbMatchResults = ref([])
const tmdbRenameOnlyProcessing = ref(false)
const tmdbRenameAndScrapProcessing = ref(false)
const currentEditingResult = ref(null)
const currentEditingIndex = ref(null)
const tmdbSearchCompleted = ref(false)

const tmdbSearchConfig = ref({
  title: '',
  year: ''
})
const tmdbSearchErrors = ref({
  title: '',
  year: ''
})

const manualSearchQuery = ref('')
const manualSearchType = ref('all')
const manualSearching = ref(false)
const manualSearchResults = ref([])

const setViewMode = (mode) => {
  viewMode.value = mode
  localStorage.setItem(STORAGE_KEY, mode)
}

const setSortBy = (field) => {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortOrder.value = 'asc'
  }
}

const breadcrumbSegments = computed(() => {
  if (currentPath.value === '/') return []
  return currentPath.value.split('/').filter(s => s)
})

const breadcrumbPaths = computed(() => {
  const segments = breadcrumbSegments.value
  const paths = []
  let path = ''
  for (const segment of segments) {
    path += '/' + segment
    paths.push(path)
  }
  return paths
})

const filteredFiles = computed(() => {
  let result = files.value
  if (searchKeyword.value && searchKeyword.value.trim() !== '') {
    const keyword = searchKeyword.value.toLowerCase()
    result = files.value.filter(f => f.name && f.name.toLowerCase().includes(keyword))
  }
  
  return [...result].sort((a, b) => {
    if (a.type === 'folder' && b.type !== 'folder') return -1
    if (a.type !== 'folder' && b.type === 'folder') return 1
    
    let comparison = 0
    switch (sortBy.value) {
      case 'name':
        comparison = (a.name || '').localeCompare(b.name || '')
        break
      case 'size':
        comparison = (a.size || 0) - (b.size || 0)
        break
      case 'modified':
        comparison = new Date(a.modified) - new Date(b.modified)
        break
    }
    
    return sortOrder.value === 'desc' ? -comparison : comparison
  })
})

const matchedCount = computed(() => {
  return tmdbMatchResults.value.filter(r => r.matched).length
})

const applyCount = computed(() => {
  return tmdbMatchResults.value.filter(r => r.matched && r.apply).length
})

const isTmdbSearchValid = computed(() => {
  return tmdbSearchConfig.value.title.trim().length > 0 && 
         tmdbSearchErrors.value.title === '' && 
         tmdbSearchErrors.value.year === ''
})

const isAllSelected = computed(() => {
  const visibleFiles = filteredFiles.value.filter(f => true)
  if (visibleFiles.length === 0) return false
  return visibleFiles.every(f => isFileSelected(f))
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    clearSelection()
  } else {
    for (const file of filteredFiles.value) {
      const key = file.path || file.name
      selectedFiles.value.set(key, file)
    }
  }
}

const loadConfig = async () => {
  try {
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}`)
    if (response.code === 200) {
      config.value = response.data
    }
  } catch (err) {
    logger.error('加载配置失败:', err)
  }
}

const loadDirectory = async (path = '/') => {
  loading.value = true
  error.value = null
  isSearching.value = false
  searchKeyword.value = ''
  selectedFiles.value.clear()
  
  try {
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/browse?path=${encodeURIComponent(path)}`)
    
    if (response.code === 200) {
      currentPath.value = path
      files.value = response.data || []
    } else {
      error.value = response.message || '加载目录失败'
    }
  } catch (err) {
    logger.error('加载目录失败:', err)
    error.value = err.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

const navigateTo = (path) => {
  loadDirectory(path)
}

const handleFileClick = (file) => {
  if (file.type === 'folder') {
    const newPath = file.path || (currentPath.value === '/' ? '' : currentPath.value) + '/' + file.name
    loadDirectory(newPath)
  }
}

const downloadFile = async (file) => {
  try {
    const filePath = file.path || (currentPath.value === '/' ? '' : currentPath.value) + '/' + file.name
    const url = `/api/openlist-config/${configId.value}/download?path=${encodeURIComponent(filePath)}`
    
    const link = document.createElement('a')
    link.href = url
    link.download = file.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    logger.info('开始下载文件:', file.name)
  } catch (err) {
    logger.error('下载文件失败:', err)
    alert('下载失败: ' + (err.message || '未知错误'))
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value || searchKeyword.value.trim() === '') {
    loadDirectory(currentPath.value)
    return
  }
  
  loading.value = true
  error.value = null
  isSearching.value = true
  
  try {
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/search`, {
      method: 'POST',
      body: {
        path: currentPath.value,
        keyword: searchKeyword.value.trim()
      }
    })
    
    if (response.code === 200) {
      files.value = response.data || []
    } else {
      error.value = response.message || '搜索失败'
    }
  } catch (err) {
    logger.error('搜索失败:', err)
    error.value = err.message || '搜索失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  if (isSearching.value) {
    handleSearch()
  } else {
    loadDirectory(currentPath.value)
  }
}

const formatFileSize = (bytes) => {
  if (bytes === null || bytes === undefined) return '-'
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const isFileSelected = (file) => {
  const key = file.path || file.name
  return selectedFiles.value.has(key)
}

const toggleFileSelection = (file) => {
  const key = file.path || file.name
  if (selectedFiles.value.has(key)) {
    selectedFiles.value.delete(key)
  } else {
    selectedFiles.value.set(key, file)
  }
}

const clearSelection = () => {
  selectedFiles.value.clear()
}

const openBatchRenameDialog = () => {
  showBatchRenameDialog.value = true
  updateRenamePreview()
}

const closeBatchRenameDialog = () => {
  showBatchRenameDialog.value = false
}

const updateRenamePreview = () => {
  const list = []
  let index = renameConfig.value.startNumber
  
  for (const file of selectedFiles.value.values()) {
    const original = file.name
    const ext = getFileExtension(original)
    const baseName = getBaseName(original)
    
    let newName = original
    
    try {
      if (renameConfig.value.mode === 'magic') {
        const mediaInfo = parseMediaInfo(original)
        newName = applyMagicPattern(baseName, ext, mediaInfo, index)
      } else if (renameConfig.value.mode === 'regex') {
        newName = applyRegexReplace(original)
      } else if (renameConfig.value.mode === 'sequence') {
        const numStr = formatNumber(index, renameConfig.value.numberFormat)
        newName = renameConfig.value.prefix + numStr + renameConfig.value.suffix + ext
      }
    } catch (e) {
      logger.error('预览重命名失败:', e)
      newName = original
    }
    
    list.push({
      original,
      new: newName,
      file
    })
    
    index++
  }
  
  renamePreviewList.value = list
}

const parseMediaInfo = (fileName) => {
  const info = {
    title: '',
    season: null,
    episode: null,
    year: null
  }
  
  const seasonEpisodeMatch = fileName.match(/[S,s](\d+)[E,e](\d+)/)
  if (seasonEpisodeMatch) {
    info.season = parseInt(seasonEpisodeMatch[1])
    info.episode = parseInt(seasonEpisodeMatch[2])
  }
  
  const yearMatch = fileName.match(/[(\[](\d{4})[)\]]/)
  if (yearMatch) {
    info.year = parseInt(yearMatch[1])
  }
  
  let title = fileName
  title = title.replace(/[S,s]\d+[E,e]\d+/g, '')
  title = title.replace(/[(\[](\d{4})[)\]]/g, '')
  title = title.replace(/\.[^.]+$/, '')
  title = title.replace(/[._-]/g, ' ').trim()
  info.title = title
  
  return info
}

const applyMagicPattern = (baseName, ext, mediaInfo, index) => {
  let pattern = renameConfig.value.customPattern
  
  if (!pattern) {
    if (renameConfig.value.template === 'movie') {
      pattern = '{title} ({year})'
    } else if (renameConfig.value.template === 'tv') {
      pattern = '{title} - S{season}E{episode}'
    } else {
      pattern = '{base}'
    }
  }
  
  let result = pattern
  result = result.replace(/\{title\}/g, mediaInfo.title || baseName)
  result = result.replace(/\{season\}/g, mediaInfo.season ? String(mediaInfo.season).padStart(2, '0') : '')
  result = result.replace(/\{episode\}/g, mediaInfo.episode ? String(mediaInfo.episode).padStart(2, '0') : '')
  result = result.replace(/\{year\}/g, mediaInfo.year || '')
  result = result.replace(/\{base\}/g, baseName)
  result = result.replace(/\{index\}/g, formatNumber(index, renameConfig.value.numberFormat))
  result = result.replace(/\{ext\}/g, ext)
  
  if (!result.endsWith(ext) && !result.includes('{ext}')) {
    result = result + ext
  }
  
  return renameConfig.value.prefix + result + renameConfig.value.suffix
}

const applyRegexReplace = (fileName) => {
  try {
    if (!renameConfig.value.regexPattern) return fileName
    
    const pattern = new RegExp(renameConfig.value.regexPattern)
    return fileName.replace(pattern, renameConfig.value.regexReplacement)
  } catch (e) {
    logger.error('正则替换失败:', e)
    return fileName
  }
}

const getFileExtension = (fileName) => {
  const lastDot = fileName.lastIndexOf('.')
  return lastDot > 0 ? fileName.substring(lastDot) : ''
}

const getBaseName = (fileName) => {
  const lastDot = fileName.lastIndexOf('.')
  return lastDot > 0 ? fileName.substring(0, lastDot) : fileName
}

const formatNumber = (num, format) => {
  try {
    const match = format.match(/%0?(\d+)d/)
    if (match) {
      const width = parseInt(match[1])
      return num.toString().padStart(width, '0')
    }
  } catch (e) {
    // ignore
  }
  return num.toString()
}

const executeBatchRename = async () => {
  if (renamePreviewList.value.length === 0) return
  
  renameProcessing.value = true
  
  try {
    const renameList = []
    const history = []
    
    for (const preview of renamePreviewList.value) {
      if (preview.original !== preview.new) {
        const srcPath = preview.file.path || (currentPath.value === '/' ? '' : currentPath.value) + '/' + preview.original
        const dstPath = currentPath.value + '/' + preview.new
        
        renameList.push({ srcPath, dstPath })
        history.push({ srcPath, dstPath, original: preview.original, new: preview.new })
      }
    }
    
    if (renameList.length === 0) {
      alert('没有需要重命名的文件')
      return
    }
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/batch-rename`, {
      method: 'POST',
      body: { renameList }
    })
    
    if (response.code === 200) {
      lastRenameHistory.value = history
      canUndoRename.value = true
      
      alert(`重命名成功：成功 ${response.data.successCount} 个，失败 ${response.data.failedCount} 个`)
      closeBatchRenameDialog()
      loadDirectory(currentPath.value)
    } else {
      alert('重命名失败: ' + response.message)
    }
  } catch (err) {
    logger.error('批量重命名失败:', err)
    alert('重命名失败: ' + (err.message || '未知错误'))
  } finally {
    renameProcessing.value = false
  }
}

const undoRename = async () => {
  if (!lastRenameHistory.value) return
  
  renameProcessing.value = true
  
  try {
    const renameList = lastRenameHistory.value.map(item => ({
      srcPath: item.dstPath,
      dstPath: item.srcPath
    }))
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/batch-rename`, {
      method: 'POST',
      body: { renameList }
    })
    
    if (response.code === 200) {
      alert('撤销成功')
      lastRenameHistory.value = null
      canUndoRename.value = false
      closeBatchRenameDialog()
      loadDirectory(currentPath.value)
    } else {
      alert('撤销失败: ' + response.message)
    }
  } catch (err) {
    logger.error('撤销失败:', err)
    alert('撤销失败: ' + (err.message || '未知错误'))
  } finally {
    renameProcessing.value = false
  }
}

const openTmdbMatchDialog = async () => {
  showTmdbMatchDialog.value = true
  tmdbMatchResults.value = []
  tmdbSearchConfig.value = { title: '', year: '' }
  tmdbSearchErrors.value = { title: '', year: '' }
  tmdbSearchCompleted.value = false
  
  for (const file of selectedFiles.value.values()) {
    if (file.type !== 'folder') {
      tmdbMatchResults.value.push({
        fileName: file.name,
        path: file.path,
        file,
        matched: false,
        apply: true
      })
    }
  }
  
  try {
    logger.info('开始从目录路径提取标题:', currentPath.value)
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/extract-title-from-path`, {
      method: 'POST',
      body: { path: currentPath.value }
    })
    
    if (response.code === 200 && response.data) {
      if (response.data.bestTitle) {
        tmdbSearchConfig.value.title = response.data.bestTitle
        logger.info('自动填入目录标题:', response.data.bestTitle)
      }
    }
  } catch (err) {
    logger.error('提取目录标题失败:', err)
  }
}

const validateTmdbSearch = () => {
  tmdbSearchErrors.value.title = ''
  tmdbSearchErrors.value.year = ''
  
  if (!tmdbSearchConfig.value.title.trim()) {
    tmdbSearchErrors.value.title = '请输入影视名称'
  } else if (tmdbSearchConfig.value.title.trim().length > 200) {
    tmdbSearchErrors.value.title = '影视名称不能超过200个字符'
  } else if (/[<>:"/\\|?*]/.test(tmdbSearchConfig.value.title)) {
    tmdbSearchErrors.value.title = '影视名称不能包含非法字符：<>:"/\\|?*'
  }
  
  if (tmdbSearchConfig.value.year) {
    const year = parseInt(tmdbSearchConfig.value.year)
    const currentYear = new Date().getFullYear()
    if (isNaN(year) || year < 1900 || year > currentYear + 5) {
      tmdbSearchErrors.value.year = `年份必须在 1900 到 ${currentYear + 5} 之间`
    }
  }
}

const resetTmdbSearch = () => {
  tmdbSearchConfig.value = { title: '', year: '' }
  tmdbSearchErrors.value = { title: '', year: '' }
  tmdbSearchCompleted.value = false
  tmdbMatchResults.value = []
  
  for (const file of selectedFiles.value.values()) {
    if (file.type !== 'folder') {
      tmdbMatchResults.value.push({
        fileName: file.name,
        path: file.path,
        file,
        matched: false,
        apply: true
      })
    }
  }
}

const startTmdbMatchWithConfig = async () => {
  validateTmdbSearch()
  if (!isTmdbSearchValid.value) return
  
  tmdbMatching.value = true
  
  try {
    const filesToMatch = tmdbMatchResults.value.map(r => ({
      fileName: r.fileName,
      directoryPath: currentPath.value,
      path: r.path
    }))
    
    logger.info('开始TMDB匹配，配置：', tmdbSearchConfig.value)
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/tmdb-batch-match-with-config`, {
      method: 'POST',
      body: {
        files: filesToMatch,
        searchConfig: {
          title: tmdbSearchConfig.value.title.trim(),
          year: tmdbSearchConfig.value.year ? parseInt(tmdbSearchConfig.value.year) : null
        }
      }
    })
    
    logger.info('TMDB匹配响应：', response)
    
    if (response.code === 200) {
      tmdbSearchCompleted.value = true
      for (let i = 0; i < response.data.length; i++) {
        tmdbMatchResults.value[i] = {
          ...tmdbMatchResults.value[i],
          ...response.data[i]
        }
      }
    }
  } catch (err) {
    logger.error('TMDB匹配失败:', err)
  } finally {
    tmdbMatching.value = false
  }
}

const startTmdbMatch = async () => {
  tmdbMatching.value = true
  
  try {
    const filesToMatch = tmdbMatchResults.value.map(r => ({
      fileName: r.fileName,
      directoryPath: currentPath.value,
      path: r.path
    }))
    
    logger.info('开始TMDB重新识别')
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/tmdb-batch-match`, {
      method: 'POST',
      body: { files: filesToMatch }
    })
    
    logger.info('TMDB重新识别响应：', response)
    
    if (response.code === 200) {
      tmdbSearchCompleted.value = true
      for (let i = 0; i < response.data.length; i++) {
        tmdbMatchResults.value[i] = {
          ...tmdbMatchResults.value[i],
          ...response.data[i]
        }
      }
    }
  } catch (err) {
    logger.error('TMDB匹配失败:', err)
  } finally {
    tmdbMatching.value = false
  }
}

const closeTmdbMatchDialog = () => {
  showTmdbMatchDialog.value = false
}

const openManualSearch = (result, index) => {
  currentEditingResult.value = result
  currentEditingIndex.value = index
  manualSearchQuery.value = result.title || getBaseName(result.fileName)
  manualSearchResults.value = []
  showManualSearchDialog.value = true
}

const closeManualSearchDialog = () => {
  showManualSearchDialog.value = false
  currentEditingResult.value = null
  currentEditingIndex.value = null
}

const executeManualSearch = async () => {
  if (!manualSearchQuery.value.trim()) return
  
  manualSearching.value = true
  manualSearchResults.value = []
  
  try {
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/tmdb-search`, {
      method: 'POST',
      body: {
        query: manualSearchQuery.value,
        type: manualSearchType.value
      }
    })
    
    if (response.code === 200 && response.data.success) {
      const results = []
      
      if (response.data.type === 'all') {
        if (response.data.movieResults) {
          results.push(...response.data.movieResults.map(r => ({ ...r, type: 'movie' })))
        }
        if (response.data.tvResults) {
          results.push(...response.data.tvResults.map(r => ({ ...r, type: 'tv' })))
        }
      } else if (response.data.type === 'movie' && response.data.results) {
        results.push(...response.data.results.map(r => ({ ...r, type: 'movie' })))
      } else if (response.data.type === 'tv' && response.data.results) {
        results.push(...response.data.results.map(r => ({ ...r, type: 'tv' })))
      }
      
      for (const item of results) {
        if (item.posterPath) {
          item.posterUrl = `https://image.tmdb.org/t/p/w500${item.posterPath}`
        }
        if (!item.year && item.releaseDate) {
          item.year = item.releaseDate.substring(0, 4)
        }
      }
      
      manualSearchResults.value = results
    }
  } catch (err) {
    logger.error('手动搜索失败:', err)
  } finally {
    manualSearching.value = false
  }
}

const selectManualSearchResult = async (item) => {
  if (!currentEditingResult.value || currentEditingIndex.value === null) return
  
  try {
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/tmdb-get-by-id`, {
      method: 'POST',
      body: {
        tmdbId: item.id,
        type: item.type,
        season: currentEditingResult.value.season,
        episode: currentEditingResult.value.episode,
        originalFileName: currentEditingResult.value.fileName
      }
    })
    
    if (response.code === 200 && response.data.matched) {
      tmdbMatchResults.value[currentEditingIndex.value] = {
        ...tmdbMatchResults.value[currentEditingIndex.value],
        ...response.data,
        matched: true,
        apply: true
      }
    }
    
    closeManualSearchDialog()
  } catch (err) {
    logger.error('选择结果失败:', err)
  }
}

const applyTmdbRenameOnly = async () => {
  const filesToProcess = tmdbMatchResults.value.filter(r => r.matched && r.apply)
  if (filesToProcess.length === 0) return
  
  tmdbRenameOnlyProcessing.value = true
  
  try {
    // 仅执行重命名
    const renameList = []
    const history = []
    
    for (const result of filesToProcess) {
      const srcPath = result.path || (currentPath.value === '/' ? '' : currentPath.value) + '/' + result.fileName
      const dstPath = currentPath.value + '/' + result.newFileName
      
      renameList.push({ srcPath, dstPath })
      history.push({ srcPath, dstPath, original: result.fileName, new: result.newFileName })
    }
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/batch-rename`, {
      method: 'POST',
      body: { renameList }
    })
    
    if (response.code === 200) {
      lastRenameHistory.value = history
      canUndoRename.value = true
      
      alert(`重命名成功：成功 ${response.data.successCount} 个，失败 ${response.data.failedCount} 个`)
      closeTmdbMatchDialog()
      loadDirectory(currentPath.value)
    } else {
      alert('重命名失败: ' + response.message)
    }
  } catch (err) {
    logger.error('应用TMDB重命名失败:', err)
    alert('操作失败: ' + (err.message || '未知错误'))
  } finally {
    tmdbRenameOnlyProcessing.value = false
  }
}

const applyTmdbRenameAndScrap = async () => {
  const filesToProcess = tmdbMatchResults.value.filter(r => r.matched && r.apply)
  if (filesToProcess.length === 0) return
  
  tmdbRenameAndScrapProcessing.value = true
  
  try {
    // 先执行刮削
    logger.info('开始执行影视信息刮削...')
    
    const scrapingItems = filesToProcess.map(result => ({
      filePath: result.path || (currentPath.value === '/' ? '' : currentPath.value) + '/' + result.fileName,
      tmdbId: result.tmdbId,
      type: result.type,
      season: result.season,
      episode: result.episode,
      targetFileName: result.newFileName
    }))
    
    const scrapingResponse = await authenticatedApiCall(`/openlist-config/${configId.value}/scraping`, {
      method: 'POST',
      body: {
        items: scrapingItems,
        options: {
          generateNfo: true,
          downloadPoster: true,
          downloadBackdrop: true
        }
      }
    })
    
    let scrapingSuccessCount = 0
    let scrapingFailedCount = 0
    if (scrapingResponse.code === 200) {
      logger.info('刮削完成:', scrapingResponse.data)
      scrapingSuccessCount = scrapingResponse.data.successCount || 0
      scrapingFailedCount = scrapingResponse.data.failedCount || 0
    }
    
    // 再执行重命名
    const renameList = []
    const history = []
    
    for (const result of filesToProcess) {
      const srcPath = result.path || (currentPath.value === '/' ? '' : currentPath.value) + '/' + result.fileName
      const dstPath = currentPath.value + '/' + result.newFileName
      
      renameList.push({ srcPath, dstPath })
      history.push({ srcPath, dstPath, original: result.fileName, new: result.newFileName })
    }
    
    const response = await authenticatedApiCall(`/openlist-config/${configId.value}/batch-rename`, {
      method: 'POST',
      body: { renameList }
    })
    
    if (response.code === 200) {
      lastRenameHistory.value = history
      canUndoRename.value = true
      
      // 合并提示
      let message = `刮削：成功 ${scrapingSuccessCount} 个，失败 ${scrapingFailedCount} 个\n`
      message += `重命名：成功 ${response.data.successCount} 个，失败 ${response.data.failedCount} 个`
      alert(message)
      
      closeTmdbMatchDialog()
      loadDirectory(currentPath.value)
    } else {
      alert('重命名失败: ' + response.message)
    }
  } catch (err) {
    logger.error('应用TMDB重命名和刮削失败:', err)
    alert('操作失败: ' + (err.message || '未知错误'))
  } finally {
    tmdbRenameAndScrapProcessing.value = false
  }
}

const logout = async () => {
  try {
    await authenticatedApiCall('/auth/sign-out', { method: 'POST' })
  } catch (error) {
    logger.error('登出失败:', error)
  } finally {
    authStore.clearAuth()
    await router.push('/login')
  }
}

const changePassword = () => {
  router.push('/change-password')
}

const openSettings = () => {
  router.push('/settings')
}

const openLogs = () => {
  router.push('/logs')
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  authStore.restoreAuth()
  loadConfig()
  loadDirectory('/')
})
</script>

<style scoped>
.hover\:scale-102:hover {
  transform: scale(1.02);
}
</style>
