# 修复重命名路径问题 - 实施计划

## 问题分析
从日志看到：
- 批量匹配成功，生成了正确的文件名：`26 王者中轴.strm` → `故宫100.S01E26.strm`
- 但重命名失败，错误：`using relative path is not allowed`
- 路径转换后仍然是完整路径：`/飞牛/strm/移动云盘1/纪录片/故宫100/第二辑/26 王者中轴.strm`

## [x] Task 1: 修复 stripBasePath 方法
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修复 `stripBasePath` 方法，确保返回正确的相对路径格式
  - 当 basePath 为 "/" 时，需要去掉前导的 "/"
  - 确保返回的路径符合 Alist API 的要求
- **Success Criteria**:
  - `stripBasePath("/飞牛/...", "/")` 返回 `"飞牛/..."`（无前置 "/"）
- **Test Requirements**:
  - `programmatic` TR-1.1: 验证 stripBasePath("/a/b/c", "/") 返回 "a/b/c"
  - `programmatic` TR-1.2: 验证 stripBasePath("/a/b/c", "/a") 返回 "b/c"

## [x] Task 2: 测试修复后的重命名功能
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 编译并测试修复后的代码
  - 确保重命名能够正常工作
- **Success Criteria**:
  - 重命名操作成功，不再出现 "using relative path is not allowed" 错误
- **Test Requirements**:
  - `programmatic` TR-2.1: 后端代码编译成功 ✓
  - `human-judgement` TR-2.2: 实际测试重命名功能正常工作
