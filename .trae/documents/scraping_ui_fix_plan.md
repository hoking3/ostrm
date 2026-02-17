# 修复刮削UI和提示 - 实现计划

## 需求分析
1. 刮削时只生成nfo，添加是否下载图片选项
2. 在重命名刮削后提示成功两次，修改成功提示

## 实现任务

### [x] 任务 1: 修改前端UI - 添加图片下载选项
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 重新添加"下载图片"选项（一个统一的复选框，而不是分开的海报和背景图）
  - 保留"生成NFO文件"选项
- **Success Criteria**: UI上有两个选项：生成NFO、下载图片
- **Test Requirements**:
  - `human-judgement` TR-1.1: UI选项清晰简洁

### [x] 任务 2: 修改前端逻辑 - 处理图片下载选项
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 添加 `downloadImages` 选项到 scrapingOptions
  - 根据该选项决定是否传递 downloadPoster 和 downloadBackdrop
- **Success Criteria**: 图片下载选项正常工作

### [x] 任务 3: 修改成功提示 - 避免两次提示
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改 `applyTmdbRenameAndScrap` 方法，将刮削和重命名的提示合并
  - 只显示一个最终成功提示，而不是分开两个
- **Success Criteria**: 只显示一个成功提示
- **Test Requirements**:
  - `programmatic` TR-3.1: 只显示一个提示

## 修改文件列表
1. `frontend/pages/file-browser/[id].vue

## 完成总结
- ✅ 已完成UI修改：刮削选项为两列，分别是"生成NFO文件"和"下载图片"
- ✅ 已完成逻辑修改：scrapingOptions 增加 downloadImages，根据该选项传递 downloadPoster 和 downloadBackdrop
- ✅ 已完成成功提示修改：刮削和重命名的提示合并为一个，只显示一次
- ✅ 现在界面更简洁，用户体验更好！
