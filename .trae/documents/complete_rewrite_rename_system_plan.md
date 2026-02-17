# 完全重写重命名系统 - 实施计划

## 问题分析
从日志分析：
- 前端传递：`/飞牛/strm/移动云盘1/纪录片/故宫100/第二辑/26 王者中轴.strm`
- 后端转换后仍然有问题
- 错误：`using relative path is not allowed`

需要完全重写整个系统，简化路径处理！

## [ ] Task 1: 修改前端 - 传递目录和文件名，而不是完整路径
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改前端，不再传递完整路径
  - 改为传递：目录路径 + 源文件名 + 目标文件名
  - 让后端自己处理路径拼接
- **Success Criteria**:
  - 前端传递数据结构简化
- **Test Requirements**:
  - `programmatic` TR-1.1: 前端代码修改完成

## [ ] Task 2: 重写后端控制器 - 简化路径处理
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 修改批量重命名接口，接受新的数据结构
  - 不再使用 stripBasePath 方法
  - 直接使用目录和文件名拼接
- **Success Criteria**:
  - 路径处理逻辑简化
- **Test Requirements**:
  - `programmatic` TR-2.1: 后端控制器代码重写完成

## [ ] Task 3: 重写 OpenlistApiService - 正确处理 Alist API 路径
- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 重写 renameFile 方法
  - 正确处理 Alist API 期望的路径格式
  - 添加更详细的日志
- **Success Criteria**:
  - 路径格式符合 Alist API 要求
- **Test Requirements**:
  - `programmatic` TR-3.1: OpenlistApiService 代码重写完成
  - `programmatic` TR-3.2: 代码编译成功

## [ ] Task 4: 测试整个重命名流程
- **Priority**: P0
- **Depends On**: Task 3
- **Description**: 
  - 完整测试从前端到后端的重命名流程
  - 确保 TMDB 匹配后的重命名也能工作
- **Success Criteria**:
  - 重命名操作成功，不再报错
- **Test Requirements**:
  - `human-judgement` TR-4.1: 实际测试重命名功能正常工作
