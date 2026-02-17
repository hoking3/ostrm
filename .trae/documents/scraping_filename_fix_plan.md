# 修复刮削文件与重命名后文件不一致问题 - 实现计划

## 问题分析
当前流程问题：
1. 使用原始文件名执行刮削（生成NFO、下载图片）
2. 然后再重命名原视频文件
3. 结果导致：刮削文件（NFO、图片）保持原始文件名，而视频文件已重命名，两者不一致

## 正确的流程应该是
1. 重命名视频文件
2. 使用新文件名执行刮削
或者
1. 刮削时直接使用新文件名

## 实现任务

### [x] 任务 1: 修改后端刮削接口 - 支持传入目标文件名
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改 ScrapingRequest 或 ScrapingItem，添加 targetFileName 字段
  - 修改 scrapFromTmdbMatch 方法，支持使用 targetFileName 来生成刮削文件
- **Success Criteria**: 后端可以根据传入的目标文件名来生成对应的刮削文件
- **Test Requirements**:
  - `programmatic` TR-1.1: 新增字段可以正常传递和使用
  - `programmatic` TR-1.2: 使用目标文件名生成的刮削文件名正确

### [x] 任务 2: 修改前端 - 刮削时传递重命名后的文件名
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 修改前端刮削请求，传递 newFileName 作为目标文件名
  - 保持重命名和刮削的执行顺序不变（先刮削再重命名，但刮削用新文件名）
- **Success Criteria**: 刮削文件使用新文件名生成，与重命名后的视频文件一致
- **Test Requirements**:
  - `programmatic` TR-2.1: 刮削请求包含目标文件名字段
  - `human-judgement` TR-2.2: 刮削后的文件名与重命名后的视频文件名一致

## 修改文件列表
1. `backend/src/main/java/com/hienao/openlist2strm/controller/OpenlistConfigController.java`
2. `backend/src/main/java/com/hienao/openlist2strm/service/MediaScrapingService.java`
3. `frontend/pages/file-browser/[id].vue`

## 完成总结
- ✅ 已完成后端修改：在 ScrapingItem 中添加 targetFileName 字段
- ✅ 已完成 MediaScrapingService 修改：scrapFromTmdbMatch 方法支持使用 targetFileName
- ✅ 已完成前端修改：刮削请求中传递 newFileName 作为 targetFileName
- ✅ 现在刮削文件会直接使用重命名后的文件名生成，与视频文件保持一致！
