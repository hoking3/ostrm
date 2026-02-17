# 移除刮削选项区域 - 实现计划

## 需求分析
既然有了两个独立的按钮：
- "仅重命名"：只重命名文件
- "重命名并刮削"：默认生成NFO和下载图片

完全不需要上面的"刮削选项"区域了，可以移除，让界面更简洁！

## 实现任务

### [ ] 任务 1: 移除前端UI - 去掉刮削选项区域
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 移除刮削选项的整个区域
  - 界面更简洁，只有匹配结果和两个按钮
- **Success Criteria**: 界面上没有刮削选项区域了
- **Test Requirements**:
  - `human-judgement` TR-1.1: UI更简洁清爽

### [ ] 任务 2: 修改前端逻辑 - "重命名并刮削"默认开启所有选项
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 刮削请求中，generateNfo 和 downloadPoster、downloadBackdrop 都默认设为 true
  - 移除 scrapingOptions 相关的代码
- **Success Criteria**: 重命名并刮削默认生成NFO和下载图片
- **Test Requirements**:
  - `programmatic` TR-2.1: 刮削请求中所有选项都为 true

## 修改文件列表
1. `frontend/pages/file-browser/[id].vue
