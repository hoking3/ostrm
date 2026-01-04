/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.config.PathConfiguration;
import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.exception.BusinessException;
import com.hienao.openlist2strm.mapper.TaskConfigMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 任务配置服务类 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class TaskConfigService {

  private static final String TASK_CONFIG_ID_NULL_ERROR = "任务配置ID不能为空";

  @Inject
  TaskConfigMapper taskConfigMapper;

  @Inject
  SchedulerService schedulerService;

  @Inject
  PathConfiguration pathConfiguration;

  /**
   * 根据ID查询任务配置
   *
   * @param id 主键ID
   * @return 任务配置信息
   */
  public TaskConfig getById(Long id) {
    if (id == null) {
      throw new BusinessException(TASK_CONFIG_ID_NULL_ERROR);
    }
    return taskConfigMapper.selectById(id);
  }

  /**
   * 根据任务名称查询配置
   *
   * @param taskName 任务名称
   * @return 任务配置信息
   */
  public TaskConfig getByTaskName(String taskName) {
    if (taskName == null || taskName.trim().isEmpty()) {
      throw new BusinessException("任务名称不能为空");
    }
    return taskConfigMapper.selectByTaskName(taskName);
  }

  /**
   * 根据路径查询配置
   *
   * @param path 任务路径
   * @return 任务配置信息
   */
  public TaskConfig getByPath(String path) {
    if (path == null || path.trim().isEmpty()) {
      throw new BusinessException("任务路径不能为空");
    }
    return taskConfigMapper.selectByPath(path);
  }

  /**
   * 查询所有启用的任务配置
   *
   * @return 任务配置列表
   */
  public List<TaskConfig> getActiveConfigs() {
    return taskConfigMapper.selectActiveConfigs();
  }

  /**
   * 查询所有任务配置
   *
   * @return 任务配置列表
   */
  public List<TaskConfig> getAllConfigs() {
    return taskConfigMapper.selectAll();
  }

  /**
   * 查询有定时任务的配置
   *
   * @return 任务配置列表
   */
  public List<TaskConfig> getConfigsWithCron() {
    return taskConfigMapper.selectWithCron();
  }

  /**
   * 查询有定时任务的配置（别名方法）
   *
   * @return 任务配置列表
   */
  public List<TaskConfig> getScheduledConfigs() {
    return getConfigsWithCron();
  }

  /**
   * 创建任务配置
   *
   * @param taskConfig 任务配置信息
   * @return 创建的任务配置
   */
  @Transactional
  public TaskConfig createConfig(TaskConfig taskConfig) {
    validateConfig(taskConfig);

    // 检查任务名称是否已存在
    TaskConfig existingConfig = taskConfigMapper.selectByTaskName(taskConfig.getTaskName());
    if (existingConfig != null) {
      throw new BusinessException("任务名称已存在: " + taskConfig.getTaskName());
    }

    // 检查路径是否已存在
    TaskConfig existingPathConfig = taskConfigMapper.selectByPath(taskConfig.getPath());
    if (existingPathConfig != null) {
      throw new BusinessException("任务路径已存在: " + taskConfig.getPath());
    }

    // 设置默认值
    setDefaultValues(taskConfig);

    int result = taskConfigMapper.insert(taskConfig);
    if (result <= 0) {
      throw new BusinessException("创建任务配置失败");
    }

    // 如果设置了定时任务表达式，添加到调度器
    if (taskConfig.getCron() != null && !taskConfig.getCron().trim().isEmpty() && taskConfig.getIsActive()) {
      try {
        schedulerService.addScheduledTask(taskConfig);
        Log.info("添加定时任务到调度器成功，任务名称: " + taskConfig.getTaskName());
      } catch (Exception e) {
        Log.error("添加定时任务到调度器失败，任务名称: " + taskConfig.getTaskName() + ", 错误: " + e.getMessage(), e);
      }
    }

    Log.info("创建任务配置成功，任务名称: " + taskConfig.getTaskName() + ", ID: " + taskConfig.getId());
    return taskConfig;
  }

  /**
   * 更新任务配置
   *
   * @param taskConfig 任务配置信息
   * @return 更新的任务配置
   */
  @Transactional
  public TaskConfig updateConfig(TaskConfig taskConfig) {
    if (taskConfig.getId() == null) {
      throw new BusinessException(TASK_CONFIG_ID_NULL_ERROR);
    }

    // 检查配置是否存在
    TaskConfig existingConfig = taskConfigMapper.selectById(taskConfig.getId());
    if (existingConfig == null) {
      throw new BusinessException("任务配置不存在，ID: " + taskConfig.getId());
    }

    // 如果更新了任务名称，检查是否与其他配置冲突
    if (taskConfig.getTaskName() != null && !taskConfig.getTaskName().trim().isEmpty()
        && !taskConfig.getTaskName().equals(existingConfig.getTaskName())) {
      TaskConfig conflictConfig = taskConfigMapper.selectByTaskName(taskConfig.getTaskName());
      if (conflictConfig != null && !conflictConfig.getId().equals(taskConfig.getId())) {
        throw new BusinessException("任务名称已存在: " + taskConfig.getTaskName());
      }
    }

    // 如果更新了路径，检查是否与其他配置冲突
    if (taskConfig.getPath() != null && !taskConfig.getPath().trim().isEmpty()
        && !taskConfig.getPath().equals(existingConfig.getPath())) {
      TaskConfig conflictConfig = taskConfigMapper.selectByPath(taskConfig.getPath());
      if (conflictConfig != null && !conflictConfig.getId().equals(taskConfig.getId())) {
        throw new BusinessException("任务路径已存在: " + taskConfig.getPath());
      }
    }

    int result = taskConfigMapper.updateById(taskConfig);
    if (result <= 0) {
      throw new BusinessException("更新任务配置失败");
    }

    // 更新定时任务
    try {
      TaskConfig updatedConfig = taskConfigMapper.selectById(taskConfig.getId());
      schedulerService.updateScheduledTask(updatedConfig);
      Log.info("更新调度器定时任务成功，任务ID: " + taskConfig.getId());
    } catch (Exception e) {
      Log.error("更新调度器定时任务失败，任务ID: " + taskConfig.getId() + ", 错误: " + e.getMessage(), e);
    }

    Log.info("更新任务配置成功，ID: " + taskConfig.getId());
    return taskConfigMapper.selectById(taskConfig.getId());
  }

  /**
   * 删除任务配置
   *
   * @param id 主键ID
   */
  @Transactional
  public void deleteConfig(Long id) {
    if (id == null) {
      throw new BusinessException(TASK_CONFIG_ID_NULL_ERROR);
    }

    TaskConfig existingConfig = taskConfigMapper.selectById(id);
    if (existingConfig == null) {
      throw new BusinessException("任务配置不存在，ID: " + id);
    }

    int result = taskConfigMapper.deleteById(id);
    if (result <= 0) {
      throw new BusinessException("删除任务配置失败");
    }

    // 删除定时任务
    try {
      schedulerService.removeScheduledTask(id);
      Log.info("删除调度器定时任务成功，任务ID: " + id);
    } catch (Exception e) {
      Log.errorf("删除调度器定时任务失败，任务ID: " + id + ", 错误: " + e.getMessage(), e);
    }

    Log.infof("删除任务配置成功，ID: " + id + ", 任务名称: " + existingConfig.getTaskName());
  }

  /**
   * 删除任务配置（别名方法）
   *
   * @param id 主键ID
   */
  @Transactional
  public void deleteById(Long id) {
    deleteConfig(id);
  }

  /**
   * 启用/禁用任务配置
   *
   * @param id       主键ID
   * @param isActive 是否启用
   */
  @Transactional
  public void updateActiveStatus(Long id, Boolean isActive) {
    if (id == null) {
      throw new BusinessException(TASK_CONFIG_ID_NULL_ERROR);
    }
    if (isActive == null) {
      throw new BusinessException("启用状态不能为空");
    }

    TaskConfig existingConfig = taskConfigMapper.selectById(id);
    if (existingConfig == null) {
      throw new BusinessException("任务配置不存在，ID: " + id);
    }

    int result = taskConfigMapper.updateActiveStatus(id, isActive);
    if (result <= 0) {
      throw new BusinessException("更新任务配置状态失败");
    }

    // 根据状态暂停或恢复定时任务
    try {
      if (isActive) {
        // 如果启用且有cron表达式，恢复或添加定时任务
        if (existingConfig.getCron() != null && !existingConfig.getCron().trim().isEmpty()) {
          if (schedulerService.isScheduledTaskExists(id)) {
            schedulerService.resumeScheduledTask(id);
            Log.info("恢复调度器定时任务成功，任务ID: " + id);
          } else {
            // 重新获取最新的任务配置
            TaskConfig updatedConfig = taskConfigMapper.selectById(id);
            schedulerService.addScheduledTask(updatedConfig);
            Log.info("添加调度器定时任务成功，任务ID: " + id);
          }
        }
      } else {
        // 如果禁用，暂停定时任务
        if (schedulerService.isScheduledTaskExists(id)) {
          schedulerService.pauseScheduledTask(id);
          Log.info("暂停调度器定时任务成功，任务ID: " + id);
        }
      }
    } catch (Exception e) {
      Log.errorf("更新调度器定时任务状态失败，任务ID: " + id + ", 错误: " + e.getMessage(), e);
    }

    Log.infof("更新任务配置状态成功，ID: " + id + ", 状态: " + (isActive ? "启用" : "禁用"));
  }

  /**
   * 更新最后执行时间
   *
   * @param id           主键ID
   * @param lastExecTime 最后执行时间戳
   */
  @Transactional
  public void updateLastExecTime(Long id, Long lastExecTime) {
    if (id == null) {
      throw new BusinessException(TASK_CONFIG_ID_NULL_ERROR);
    }
    if (lastExecTime == null) {
      throw new BusinessException("执行时间不能为空");
    }

    int result = taskConfigMapper.updateLastExecTime(id, lastExecTime);
    if (result <= 0) {
      throw new BusinessException("更新任务执行时间失败");
    }

    Log.debugf("更新任务执行时间成功，ID: " + id + ", 时间: " + lastExecTime);
  }

  /**
   * 更新最后执行时间（LocalDateTime版本）
   *
   * @param id           主键ID
   * @param lastExecTime 最后执行时间
   */
  @Transactional
  public void updateLastExecTime(Long id, LocalDateTime lastExecTime) {
    if (id == null) {
      throw new BusinessException(TASK_CONFIG_ID_NULL_ERROR);
    }
    if (lastExecTime == null) {
      throw new BusinessException("执行时间不能为空");
    }

    // 将LocalDateTime转换为时间戳
    Long timestamp = lastExecTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    updateLastExecTime(id, timestamp);
  }

  /**
   * 验证任务配置
   *
   * @param taskConfig 任务配置
   */
  private void validateConfig(TaskConfig taskConfig) {
    if (taskConfig == null) {
      throw new BusinessException("任务配置不能为空");
    }
    if (taskConfig.getTaskName() == null || taskConfig.getTaskName().trim().isEmpty()) {
      throw new BusinessException("任务名称不能为空");
    }
    if (taskConfig.getPath() == null || taskConfig.getPath().trim().isEmpty()) {
      throw new BusinessException("任务路径不能为空");
    }
  }

  /**
   * 设置默认值
   *
   * @param taskConfig 任务配置
   */
  private void setDefaultValues(TaskConfig taskConfig) {
    if (taskConfig.getNeedScrap() == null) {
      taskConfig.setNeedScrap(false);
    }
    if (taskConfig.getRenameRegex() == null) {
      taskConfig.setRenameRegex("");
    }
    if (taskConfig.getCron() == null) {
      taskConfig.setCron("");
    }
    if (taskConfig.getIsIncrement() == null) {
      taskConfig.setIsIncrement(true);
    }
    if (taskConfig.getStrmPath() == null || taskConfig.getStrmPath().trim().isEmpty()) {
      taskConfig.setStrmPath(pathConfiguration.strm());
    }
    if (taskConfig.getLastExecTime() == null) {
      taskConfig.setLastExecTime(0L);
    }
    if (taskConfig.getIsActive() == null) {
      taskConfig.setIsActive(true);
    }
  }
}
