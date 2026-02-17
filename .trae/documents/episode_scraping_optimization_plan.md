# 优化单集电视剧刮削 - 实现计划

## 问题分析
当前刮削单集电视剧时，会同时下载：
- 剧集剧照（-thumb.jpg）✅ 正确
- 电视剧海报（-poster.jpg）❌ 不需要
- 电视剧背景图（-fanart.jpg）❌ 不需要

单集只需要剧集剧照，不需要下载整个电视剧的海报和背景图。

## 实现任务

### [x] 任务 1: 修改 MediaScrapingService - 单集只下载剧照
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改 scrapFromTmdbMatch 方法，对于电视剧单集，只下载剧集剧照
  - 电视剧单集时，将 downloadPoster 和 downloadBackdrop 设为 false，只保留 stillUrl 下载
- **Success Criteria**: 刮削单集时只下载 -thumb.jpg，不下载 -poster.jpg 和 -fanart.jpg
- **Test Requirements**:
  - `programmatic` TR-1.1: 单集刮削只生成 -thumb.jpg 文件
  - `programmatic` TR-1.2: 电影刮削仍正常下载 poster 和 backdrop

## 修改文件列表
1. `backend/src/main/java/com/hienao/openlist2strm/service/MediaScrapingService.java`

## 完成总结
- ✅ 已完成修改：在 MediaScrapingService 中添加单集检测逻辑
- ✅ 当检测到是电视剧单集（type=tv，且有 season 和 episode 都有值时），强制将 downloadPoster 和 downloadBackdrop 设为 false
- ✅ 这样单集刮削只会下载剧集剧照（-thumb.jpg）
- ✅ 电影刮削和电视剧整体刮削仍正常工作！
