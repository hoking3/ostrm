# OStrm - API集成实现计划

## 功能增强需求
1. **集成OpenList API**，在STRM文件生成前自动执行OpenList数据刷新操作，确保获取最新的媒体资源信息
2. **集成Emby API**，在STRM文件成功生成后自动触发Emby媒体库刷新功能，确保新生成的STRM文件能被Emby系统及时识别和索引

## 实现计划

### [ ] 任务1: 为OpenList API添加数据刷新功能
- **优先级**: P0
- **Depends On**: None
- **Description**:
  - 在OpenlistApiService中添加刷新数据的方法
  - 实现对OpenList API的刷新接口调用
  - 确保异常处理机制完善
  - 添加详细的日志记录
- **Success Criteria**:
  - 能够成功调用OpenList API的刷新接口
  - 刷新操作失败时能正确处理异常
  - 日志记录完整清晰
- **Test Requirements**:
  - `programmatic` TR-1.1: 调用刷新接口后能获取到最新的文件列表
  - `programmatic` TR-1.2: 网络错误时能正确捕获并处理异常
  - `human-judgement` TR-1.3: 日志记录包含调用时间、请求参数、响应状态

### [ ] 任务2: 创建Emby API服务类
- **优先级**: P0
- **Depends On**: None
- **Description**:
  - 创建EmbyApiService类，负责与Emby API交互
  - 实现媒体库刷新功能
  - 确保异常处理机制完善
  - 添加详细的日志记录
- **Success Criteria**:
  - 能够成功调用Emby API的刷新接口
  - 刷新操作失败时能正确处理异常
  - 日志记录完整清晰
- **Test Requirements**:
  - `programmatic` TR-2.1: 调用刷新接口后Emby能识别新生成的STRM文件
  - `programmatic` TR-2.2: 网络错误时能正确捕获并处理异常
  - `human-judgement` TR-2.3: 日志记录包含调用时间、请求参数、响应状态

### [ ] 任务3: 修改TaskExecutionService集成API调用
- **优先级**: P1
- **Depends On**: 任务1, 任务2
- **Description**:
  - 在TaskExecutionService的executeTaskLogic方法中，在文件处理前添加OpenList数据刷新调用
  - 在STRM文件生成完成后添加Emby媒体库刷新调用
  - 确保调用顺序正确，不影响原有流程
- **Success Criteria**:
  - OpenList数据刷新在文件处理前执行
  - Emby媒体库刷新在STRM文件生成后执行
  - 原有STRM文件生成流程不受影响
- **Test Requirements**:
  - `programmatic` TR-3.1: 任务执行时能按顺序调用两个API
  - `programmatic` TR-3.2: API调用失败时任务能继续执行
  - `human-judgement` TR-3.3: 执行流程清晰，日志记录完整

### [ ] 任务4: 添加功能开关配置
- **优先级**: P1
- **Depends On**: None
- **Description**:
  - 在TaskConfig实体中添加两个新字段：enableOpenlistRefresh和enableEmbyRefresh
  - 更新数据库迁移脚本
  - 在前端页面添加相应的配置选项
  - 在后端服务中读取配置并根据配置决定是否执行刷新操作
- **Success Criteria**:
  - 用户能在前端启用/禁用两个自动刷新功能
  - 后端能正确读取配置并执行相应操作
  - 配置变更能正确保存到数据库
- **Test Requirements**:
  - `programmatic` TR-4.1: 禁用配置时API调用不执行
  - `programmatic` TR-4.2: 启用配置时API调用正常执行
  - `human-judgement` TR-4.3: 前端配置界面清晰易用

### [ ] 任务5: 完善异常处理和日志记录
- **优先级**: P2
- **Depends On**: 任务1, 任务2, 任务3
- **Description**:
  - 为所有API调用添加完善的异常处理
  - 添加详细的日志记录，包括调用时间、请求参数、响应状态和结果
  - 提供清晰的错误提示，当API调用失败时能明确告知用户具体原因
- **Success Criteria**:
  - 所有API调用异常都能被正确捕获和处理
  - 日志记录完整清晰，便于排查问题
  - 错误提示明确，用户能理解失败原因
- **Test Requirements**:
  - `programmatic` TR-5.1: 网络错误时能正确捕获并记录异常
  - `programmatic` TR-5.2: 认证失败时能正确捕获并记录异常
  - `human-judgement` TR-5.3: 日志记录包含所有必要信息
  - `human-judgement` TR-5.4: 错误提示清晰明确

### [ ] 任务6: 测试和验证
- **优先级**: P2
- **Depends On**: 所有任务
- **Description**:
  - 测试完整的流程，确保两个API集成功能正常工作
  - 验证功能开关配置的有效性
  - 测试异常情况的处理
  - 确保整个系统的稳定性和兼容性
- **Success Criteria**:
  - 完整流程测试通过
  - 功能开关配置有效
  - 异常情况处理正确
  - 系统稳定运行，无性能问题
- **Test Requirements**:
  - `programmatic` TR-6.1: 完整流程测试通过
  - `programmatic` TR-6.2: 功能开关配置测试通过
  - `programmatic` TR-6.3: 异常情况测试通过
  - `human-judgement` TR-6.4: 系统运行稳定，无明显性能问题

## 技术实现要点

1. **OpenList API集成**:
   - 使用现有的OpenlistApiService类
   - 添加refreshDirectory方法，调用OpenList API的刷新接口
   - 确保使用正确的认证头和请求参数

2. **Emby API集成**:
   - 创建新的EmbyApiService类
   - 实现refreshMediaLibrary方法，调用Emby API的刷新接口
   - 支持基本认证和API密钥认证

3. **配置管理**:
   - 使用现有的TaskConfig实体
   - 添加两个新的布尔字段
   - 更新数据库迁移脚本
   - 在前端页面添加配置选项

4. **异常处理**:
   - 使用try-catch块捕获所有可能的异常
   - 记录详细的错误信息
   - 提供清晰的错误提示
   - 确保API调用失败不影响主流程

5. **日志记录**:
   - 使用Slf4j记录详细的日志
   - 包括API调用时间、请求参数、响应状态和结果
   - 使用不同级别的日志（info, debug, error）

6. **性能考虑**:
   - API调用使用异步方式，避免阻塞主流程
   - 添加超时设置，避免长时间等待
   - 实现重试机制，提高可靠性

## 风险评估

1. **API兼容性风险**:
   - OpenList和Emby API可能会有版本差异
   - 解决方案：实现灵活的API调用方式，支持不同版本的API

2. **网络依赖风险**:
   - 网络不稳定可能导致API调用失败
   - 解决方案：添加完善的异常处理和重试机制

3. **性能影响风险**:
   - API调用可能会增加任务执行时间
   - 解决方案：使用异步调用，优化API调用时机

4. **配置复杂性风险**:
   - 新增配置选项可能会增加用户使用复杂度
   - 解决方案：提供合理的默认值，添加清晰的说明文档

## 预期成果

1. **功能增强**:
   - 自动执行OpenList数据刷新，确保获取最新的媒体资源信息
   - 自动触发Emby媒体库刷新，确保新生成的STRM文件能被及时识别

2. **用户体验**:
   - 提供功能开关配置，允许用户根据需要启用/禁用自动刷新功能
   - 提供清晰的错误提示，当API调用失败时能明确告知用户具体原因

3. **系统稳定性**:
   - 完善的异常处理机制，确保API调用失败不影响主流程
   - 详细的日志记录，便于排查问题

4. **兼容性**:
   - 保证功能的兼容性和稳定性，不影响原有的STRM文件生成流程
   - 支持不同版本的OpenList和Emby API