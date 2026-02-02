# Ostrm 统一 UI 设计规范

本文档定义了 Ostrm 应用的统一 UI/UX 设计标准，所有新增或修改页面时应遵循此规范。

---

## 1. 设计概述

### 1.1 设计定位
- **产品类型**: 媒体管理 SaaS 仪表板
- **设计风格**: 现代简约玻璃拟态 (Modern Glassmorphism)
- **目标用户**: 媒体管理员、流媒体技术运维人员
- **核心价值**: 专业、功能清晰、跨设备一致体验

### 1.2 设计原则
1. **一致性** - 全应用使用相同的视觉语言和交互模式
2. **可用性** - 简洁直观的界面，降低学习成本
3. **响应式** - PC 端和移动端都能良好展示
4. **可访问性** - 符合 WCAG 2.1 AA 标准
5. **性能优先** - 减少不必要的动画和视觉特效

---

## 2. 颜色系统

### 2.1 主色调 (Primary)
```
primary-500: #3B82F6  (品牌主色)
primary-600: #2563EB  (悬停状态)
primary-700: #1D4ED8  (激活状态)
```

### 2.2 辅助色 (Secondary)
```
purple-500: #8B5CF6  (强调色 - 用于高级功能/新特性)
purple-600: #7C3AED  (悬停状态)
```

### 2.3 功能色
| 用途 | 颜色值 | 类名 |
|-----|--------|------|
| 成功 | `bg-green-500` | `.status-active` |
| 危险/错误 | `bg-red-500` | `.status-inactive` |
| 警告 | `bg-yellow-500` | - |
| 信息 | `bg-blue-500` | - |

### 2.4 中性色
```
gray-50:   #F9FAFB   (背景渐变起点)
gray-100:  #F3F4F6   (背景渐变终点)
gray-200:  #E5E7EB   (边框)
gray-500:  #6B7280   (次要文字)
gray-700:  #374151   (主要文字)
gray-900:  #111827   (标题文字)
```

### 2.5 背景渐变
```css
body {
  background: linear-gradient(135deg, #F9FAFB 0%, #EFF6FF 100%);
}
```

---

## 3. 字体系统

### 3.1 字体栈
```css
font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
```

### 3.2 字重与字号
| 元素 | 字号 | 字重 | 行高 |
|-----|------|------|------|
| 页面标题 (h1) | 2rem (32px) | 700 | 1.2 |
| 卡片标题 (h2) | 1.25rem (20px) | 600 | 1.4 |
| 区块标题 (h3) | 1.125rem (18px) | 600 | 1.5 |
| 正文 | 1rem (16px) | 400 | 1.6 |
| 次要文字 | 0.875rem (14px) | 400 | 1.5 |
| 标签文字 | 0.75rem (12px) | 500 | 1.4 |

### 3.3 渐变文字
```css
.gradient-text {
  background: linear-gradient(to right, #3B82F6, #8B5CF6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
```

---

## 4. 组件设计

### 4.1 按钮 (Buttons)

#### 主按钮 `.btn-primary`
```css
.btn-primary {
  @apply bg-gradient-to-r from-blue-600 to-blue-700
         hover:from-blue-700 hover:to-blue-800
         text-white font-medium py-2.5 px-6
         rounded-xl shadow-lg hover:shadow-xl
         transition-all duration-200
         transform hover:-translate-y-0.5;
}
```

#### 次要按钮 `.btn-secondary`
```css
.btn-secondary {
  @apply bg-white hover:bg-gray-50 text-gray-700
         font-medium py-2.5 px-6
         rounded-xl border border-gray-200
         shadow-sm hover:shadow-md
         transition-all duration-200;
}
```

#### 危险按钮 `.btn-danger`
```css
.btn-danger {
  @apply bg-gradient-to-r from-red-500 to-red-600
         hover:from-red-600 hover:to-red-700
         text-white font-medium py-2.5 px-6
         rounded-xl shadow-lg hover:shadow-xl
         transition-all duration-200
         transform hover:-translate-y-0.5;
}
```

#### 成功按钮 `.btn-success`
```css
.btn-success {
  @apply bg-gradient-to-r from-green-500 to-green-600
         hover:from-green-600 hover:to-green-700
         text-white font-medium py-2.5 px-6
         rounded-xl shadow-lg hover:shadow-xl
         transition-all duration-200
         transform hover:-translate-y-0.5;
}
```

### 4.2 输入框 `.input-field`
```css
.input-field {
  @apply block w-full px-4 py-3
         border border-gray-200 rounded-xl
         shadow-sm focus:outline-none
         focus:ring-2 focus:ring-blue-500 focus:border-transparent
         bg-white transition-all duration-200;
}
```

### 4.3 卡片容器

#### 玻璃卡片 `.glass-card`
```css
.glass-card {
  @apply bg-white/70 backdrop-blur-md
         rounded-2xl shadow-xl border border-white/30 p-6;
}
```

#### 普通卡片 `.card`
```css
.card {
  @apply bg-white/80 backdrop-blur-sm
         rounded-2xl shadow-xl border border-white/20 p-6
         hover:shadow-2xl transition-all duration-300;
}
```

#### 卡片头部 `.card-header`
```css
.card-header {
  @apply bg-gradient-to-r from-blue-600 to-purple-600
         text-white rounded-t-2xl p-6 -mx-6 -mt-6 mb-6;
}
```

### 4.4 状态徽章

#### 启用状态 `.status-active`
```css
.status-active {
  @apply bg-green-100 text-green-800 border border-green-200;
}
```

#### 禁用状态 `.status-inactive`
```css
.status-inactive {
  @apply bg-red-100 text-red-800 border border-red-200;
}
```

#### 通用徽章 `.status-badge`
```css
.status-badge {
  @apply inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold;
}
```

### 4.5 模态框

#### 遮罩层 `.modal-overlay`
```css
.modal-overlay {
  @apply fixed inset-0 bg-black/50 backdrop-blur-sm z-50;
}
```

#### 内容层 `.modal-content`
```css
.modal-content {
  @apply bg-white rounded-2xl shadow-2xl
         border border-white/20
         max-w-2xl w-full mx-4 p-6;
}
```

> **重要**: 模态框应使用 `<Teleport to="body">` 确保正确的 z-index 层级

### 4.6 浮动操作按钮 `.floating-action`
```css
.floating-action {
  @apply fixed bottom-6 right-6
         bg-gradient-to-r from-blue-600 to-purple-600
         text-white p-4 rounded-full
         shadow-2xl hover:shadow-3xl
         transition-all duration-300
         transform hover:scale-110 z-50;
}
```

### 4.7 导航栏 `.nav-glass`
```css
.nav-glass {
  @apply bg-white/80 backdrop-blur-md
         border-b border-white/20 shadow-lg;
}
```

### 4.8 页脚 `.footer-glass`
```css
.footer-glass {
  @apply bg-white/60 backdrop-blur-md
         border-t border-white/20 py-6 mt-auto;
}
```

---

## 5. 布局系统

### 5.1 基础布局结构
```
┌─────────────────────────────────────────────┐
│                 AppHeader                   │
├─────────────────────────────────────────────┤
│                                             │
│              Main Content                   │
│        (max-w-7xl mx-auto w-full)           │
│                                             │
├─────────────────────────────────────────────┤
│                 AppFooter                   │
└─────────────────────────────────────────────┘
```

### 5.2 默认布局 (layouts/default.vue)
```vue
<template>
  <div class="min-h-screen flex flex-col">
    <AppHeader />
    <main class="flex-1 w-full">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>
```

### 5.3 页面容器宽度规范
| 页面类型 | 最大宽度 | 类名 |
|---------|---------|------|
| 全宽仪表板 | 80rem (1280px) | `.max-w-7xl` |
| 标准页面 | 56rem (896px) | `.max-w-4xl` |
| 登录/注册 | 24rem (384px) | `.max-w-md` |
| 模态框 | 42rem (672px) | `.max-w-2xl` |

### 5.4 间距系统
| 场景 | 上下间距 | 左右间距 |
|-----|---------|---------|
| 页面内容 | `py-8` | `px-4 sm:px-6 lg:px-8` |
| 卡片内部 | `p-6` | - |
| 卡片间距 | `gap-6` | - |
| 按钮间距 | `space-x-3` | - |

---

## 6. 响应式设计

### 6.1 断点定义
| 断点 | 前缀 | 屏幕宽度 |
|-----|------|---------|
| sm | `sm:` | >= 640px |
| md | `md:` | >= 768px |
| lg | `lg:` | >= 1024px |
| xl | `xl:` | >= 1280px |
| 2xl | `2xl:` | >= 1536px |

### 6.2 Header 响应式策略
```html
<!-- 桌面端: 显示完整导航 -->
<div class="hidden lg:flex items-center space-x-3">
  <!-- 用户信息、操作按钮 -->
</div>

<!-- 移动端: 显示汉堡菜单 -->
<div class="lg:hidden flex items-center">
  <button @click="toggleMobileMenu">...</button>
</div>
```

### 6.3 网格响应式策略
```html
<!-- 统计卡片: 1列(移动) → 3列(桌面) -->
<div class="grid grid-cols-1 md:grid-cols-3 gap-6">

<!-- 配置卡片: 1列(移动) → 2列(平板) → 3列(桌面) -->
<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
```

### 6.4 移动端交互规范
- **触摸目标**: 最小 44x44px
- **手势**: 使用点击而非复杂手势
- **滚动**: 避免水平滚动
- **导航**: 底部导航或顶部汉堡菜单

---

## 7. 动画系统

### 7.1 动画时长
| 动画类型 | 时长 | 场景 |
|---------|------|------|
| 微交互 | 150ms | 按钮悬停、焦点变化 |
| 元素出现 | 200-300ms | 菜单展开、模态框显示 |
| 页面过渡 | 300-500ms | 页面切换 |

### 7.2 关键帧定义
```css
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
```

### 7.3 动画类
```css
.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

.animate-slide-up {
  animation: slideUp 0.3s ease-out;
}

.animate-scale-in {
  animation: scaleIn 0.2s ease-out;
}
```

### 7.4 减少动画偏好
```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## 8. 图标规范

### 8.1 图标库
- **推荐**: Heroicons (与 Tailwind 配套)
- **尺寸**: 20x20 (w-5 h-5) 或 24x24 (w-6 h-6)
- **风格**: 线性图标 (outline)

### 8.2 图标尺寸规范
| 图标尺寸 | 使用场景 |
|---------|---------|
| w-5 h-5 | 列表项、操作按钮、表单项 |
| w-6 h-6 | 导航菜单、状态指示 |
| w-8 h-8 | Logo 区域、大按钮 |

### 8.3 图标颜色规范
```html
<!-- 主要操作 -->
<svg class="w-5 h-5 text-blue-600">...</svg>

<!-- 次要操作 -->
<svg class="w-5 h-5 text-gray-500">...</svg>

<!-- 危险操作 -->
<svg class="w-5 h-5 text-red-600">...</svg>
```

---

## 9. 公共组件

### 9.1 AppHeader 组件
**位置**: `components/AppHeader.vue`

Props:
- `title?: string` - 页面标题，默认 'Ostrm'
- `showBackButton?: boolean` - 显示返回按钮，默认 false
- `userInfo?: UserInfo` - 用户信息对象

Events:
- `@logout` - 退出登录事件
- `@change-password` - 修改密码事件
- `@go-back` - 返回事件
- `@open-settings` - 打开设置事件
- `@open-logs` - 打开日志事件

### 9.2 AppFooter 组件
**位置**: `components/AppFooter.vue`

功能:
- 显示应用名称和版本
- 显示版权年份
- GitHub 仓库链接

### 9.3 使用方式
```html
<!-- 页面中使用 (已集成到默认布局) -->
<div class="min-h-screen flex flex-col">
  <AppHeader />
  <main>...</main>
  <AppFooter />
</div>

<!-- 认证页面不使用 Header/Footer -->
<template>
  <div class="min-h-screen flex items-center justify-center">
    ...
  </div>
</template>

<script setup>
definePageMeta({
  layout: false,  // 不使用默认布局
  middleware: 'guest'
})
</script>
```

---

## 10. 表单设计规范

### 10.1 表单布局
```html
<form class="glass-card space-y-6">
  <div>
    <label class="block text-sm font-medium text-gray-700 mb-2">
      标签文字
    </label>
    <input class="input-field" type="text" />
  </div>
  <div class="flex justify-end space-x-3">
    <button type="button" class="btn-secondary">取消</button>
    <button type="submit" class="btn-primary">确认</button>
  </div>
</form>
```

### 10.2 表单验证状态
```html
<!-- 错误状态 -->
<input class="input-field border-red-300 focus:ring-red-500" />

<!-- 成功状态 -->
<input class="input-field border-green-300 focus:ring-green-500" />
```

### 10.3 错误提示
```html
<p class="mt-2 text-sm text-red-600" v-if="error">
  {{ error }}
</p>
```

---

## 11. 页面模板

### 11.1 标准页面布局
```vue
<template>
  <div class="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
    <div class="animate-fade-in">
      <!-- 页面标题 -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold gradient-text">页面标题</h1>
        <p class="text-gray-600">页面描述</p>
      </div>

      <!-- 内容区域 -->
      <div class="glass-card">
        ...
      </div>
    </div>
  </div>
</template>

<script setup>
// 页面逻辑
</script>
```

### 11.2 认证页面布局 (无 Header/Footer)
```vue
<template>
  <div class="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      ...
    </div>
  </div>
</template>

<script setup>
definePageMeta({
  layout: false,
  middleware: 'guest'
})
</script>
```

### 11.3 模态框模板
```vue
<Teleport to="body">
  <div v-if="showModal" class="modal-overlay animate-fade-in" @click="closeModal">
    <div class="flex items-center justify-center min-h-screen p-4">
      <div class="modal-content animate-scale-in" @click.stop>
        <div class="card-header">
          <h3 class="text-xl font-semibold">标题</h3>
        </div>
        ...
      </div>
    </div>
  </div>
</Teleport>
```

---

## 12. 最佳实践

### 12.1 可访问性
- [ ] 按钮和链接有明确的焦点状态
- [ ] 图标按钮有 `aria-label` 或 `title`
- [ ] 表单输入有对应的 `<label>`
- [ ] 颜色对比度 >= 4.5:1
- [ ] 支持键盘导航

### 12.2 性能优化
- [ ] 使用 CSS transform/opacity 做动画
- [ ] 大列表使用虚拟滚动
- [ ] 图片使用 WebP 格式
- [ ] 减少不必要的重绘

### 12.3 代码规范
- [ ] 组件文件使用 PascalCase
- [ ] 组合式函数使用 use 前缀
- [ ] 类名使用 Tailwind 组合类
- [ ] 动画时长控制在 150-300ms

---

## 13. 新增页面检查清单

当添加新页面时，请检查以下项目：

- [ ] 使用统一的 `.glass-card` 或 `.card` 容器
- [ ] 使用 `.input-field` 作为输入框
- [ ] 使用 `.btn-primary` / `.btn-secondary` 作为按钮
- [ ] 使用 `.animate-fade-in` / `.animate-slide-up` 作为入场动画
- [ ] 响应式布局 (`grid-cols-1 md:grid-cols-x`)
- [ ] 移动端菜单适配 (`lg:hidden` / `hidden lg:`)
- [ ] 错误提示使用红色文字 (`text-red-600`)
- [ ] 成功提示使用绿色文字 (`text-green-600`)
- [ ] 图标使用 Heroicons 线性风格
- [ ] 状态使用 `.status-badge` + `.status-active`/`.status-inactive`
- [ ] 模态框使用 `<Teleport to="body">`
- [ ] 页面设置正确的 `definePageMeta`

---

## 14. 文件变更记录

| 日期 | 变更内容 | 状态 |
|-----|---------|------|
| 2026-02-03 | 设计规范文档 | 创建 UI 完成 |
| 2026-02-03 | 创建 AppFooter 组件 | 完成 |
| 2026-02-03 | 更新 default.vue 布局 | 完成 |
| 2026-02-03 | 更新所有页面组件 | 完成 |
