# TMDB智能识别功能分离 - 实现计划

## 需求分析
1. 在TMDB智能识别中添加是否需要生成NFO和下载图片的清晰选项
2. 把TMDB识别重命名和TMDB识别刮削完全分开，提供两个独立的功能入口

## 实现任务

### [x] 任务 1: 修改前端UI - 分离刮削和重命名功能
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改TMDB智能识别对话框，将刮削选项和重命名功能分开展示
  - 添加两个独立的按钮："仅重命名"和"重命名并刮削"
  - 优化刮削选项的展示，使其更清晰独立
- **Success Criteria**: UI上清晰区分重命名和刮削功能，用户可以独立选择执行
- **Test Requirements**:
  - `programmatic` TR-1.1: 两个按钮都能正常显示
  - `human-judgement` TR-1.2: UI布局清晰，功能区分明确

### [x] 任务 2: 修改前端逻辑 - 添加独立的重命名和刮削方法
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 添加 `applyTmdbRenameOnly` 方法：仅执行重命名
  - 修改 `applyTmdbRename` 方法：保留原有的重命名+刮削逻辑
  - 确保刮削选项在需要时才显示
- **Success Criteria**: 两种功能都能正常独立工作
- **Test Requirements**:
  - `programmatic` TR-2.1: 仅重命名功能正常执行
  - `programmatic` TR-2.2: 重命名+刮削功能正常执行

## 修改文件列表
1. `frontend/pages/file-browser/[id].vue` - 主要修改文件

## 完成总结
- ✅ 已完成UI修改，移除了"启用影视信息刮削"复选框，将刮削选项直接展示
- ✅ 添加了两个独立按钮："仅重命名"和"重命名并刮削"
- ✅ 添加了 `applyTmdbRenameOnly` 方法，仅执行重命名
- ✅ 添加了 `applyTmdbRenameAndScrap` 方法，执行重命名和刮削
- ✅ 移除了不再需要的 `enableScraping` 变量
- ✅ 刮削选项（生成NFO、下载海报、下载背景图）始终可见，用户可以自由选择
