# 修复TMDB识别UI和图片下载问题 - 实施计划

## [x] 任务1: 修复两个按钮都显示转圈的问题
- **Priority**: P0
- **Depends On**: None
- **Description**: 两个按钮都使用了相同的 `tmdbApplyProcessing` 变量，导致点击任何一个按钮都会让两个按钮同时显示加载状态。需要为每个按钮创建独立的状态变量。
- **Success Criteria**: 点击"仅重命名"时只有该按钮显示转圈，点击"重命名并刮削"时只有该按钮显示转圈
- **Test Requirements**:
  - `programmatic` TR-1.1: 验证两个按钮使用不同的状态变量 ✓
  - `human-judgement` TR-1.2: 手动测试两个按钮的加载状态是否独立

## [x] 任务2: 修复不生成图片的问题
- **Priority**: P0
- **Depends On**: None
- **Description**: 检查为什么现在不生成图片了，确保前端传递正确的参数并且后端正确处理。
- **Success Criteria**: 选择"重命名并刮削"时能够正常生成NFO和下载图片
- **Test Requirements**:
  - `programmatic` TR-2.1: 验证前端传递的 options 参数是否正确 ✓
  - `programmatic` TR-2.2: 验证后端 scrapFromTmdbMatch 方法是否正确处理参数 ✓
  - `human-judgement` TR-2.3: 手动测试"重命名并刮削"功能是否生成NFO和图片
