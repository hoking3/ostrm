/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.entity.TaskConfig;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调度服务 - Quarkus Scheduler 实现（替代原有的 QuartzSchedulerService）
 *
 * <p>
 * 注意：Quarkus Scheduler 不支持动态添加/删除任务，因此这里使用一个简化的实现。
 * 对于需要动态调度的任务，使用 Map 存储任务配置，在定时任务中检查并执行。
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class SchedulerService {

    /** 存储动态任务配置 */
    private final Map<Long, TaskConfig> scheduledTasks = new ConcurrentHashMap<>();

    /** 存储暂停的任务 */
    private final Map<Long, Boolean> pausedTasks = new ConcurrentHashMap<>();

    @Inject
    TaskExecutionService taskExecutionService;

    /**
     * 添加定时任务
     *
     * @param taskConfig 任务配置
     */
    public void addScheduledTask(TaskConfig taskConfig) {
        if (taskConfig == null || taskConfig.getId() == null) {
            Log.warn("无法添加任务：任务配置为空");
            return;
        }

        scheduledTasks.put(taskConfig.getId(), taskConfig);
        pausedTasks.remove(taskConfig.getId()); // 确保不在暂停列表中
        Log.info("添加定时任务成功，任务ID: " + taskConfig.getId() + ", 任务名称: " + taskConfig.getTaskName());
    }

    /**
     * 更新定时任务
     *
     * @param taskConfig 任务配置
     */
    public void updateScheduledTask(TaskConfig taskConfig) {
        if (taskConfig == null || taskConfig.getId() == null) {
            Log.warn("无法更新任务：任务配置为空");
            return;
        }

        if (taskConfig.getCron() != null && !taskConfig.getCron().trim().isEmpty() && taskConfig.getIsActive()) {
            scheduledTasks.put(taskConfig.getId(), taskConfig);
            pausedTasks.remove(taskConfig.getId());
        } else {
            // 如果没有 cron 表达式或已禁用，移除任务
            scheduledTasks.remove(taskConfig.getId());
        }
        Log.info("更新定时任务成功，任务ID: " + taskConfig.getId());
    }

    /**
     * 移除定时任务
     *
     * @param taskId 任务ID
     */
    public void removeScheduledTask(Long taskId) {
        scheduledTasks.remove(taskId);
        pausedTasks.remove(taskId);
        Log.info("移除定时任务成功，任务ID: " + taskId);
    }

    /**
     * 暂停定时任务
     *
     * @param taskId 任务ID
     */
    public void pauseScheduledTask(Long taskId) {
        pausedTasks.put(taskId, true);
        Log.info("暂停定时任务成功，任务ID: " + taskId);
    }

    /**
     * 恢复定时任务
     *
     * @param taskId 任务ID
     */
    public void resumeScheduledTask(Long taskId) {
        pausedTasks.remove(taskId);
        Log.info("恢复定时任务成功，任务ID: " + taskId);
    }

    /**
     * 检查定时任务是否存在
     *
     * @param taskId 任务ID
     * @return true 如果存在
     */
    public boolean isScheduledTaskExists(Long taskId) {
        return scheduledTasks.containsKey(taskId);
    }

    /**
     * 检查任务是否暂停
     *
     * @param taskId 任务ID
     * @return true 如果已暂停
     */
    public boolean isTaskPaused(Long taskId) {
        return pausedTasks.getOrDefault(taskId, false);
    }

    /**
     * 每分钟检查需要执行的任务
     *
     * <p>
     * 由于 Quarkus Scheduler 不支持动态 cron 表达式，这里使用固定的每分钟检查策略，
     * 然后根据任务的 cron 表达式判断是否需要执行。
     */
    @Scheduled(every = "1m", identity = "task-scheduler-check")
    void checkAndExecuteTasks(ScheduledExecution execution) {
        if (scheduledTasks.isEmpty()) {
            return;
        }

        Log.debug("检查定时任务，当前任务数: " + scheduledTasks.size());

        for (Map.Entry<Long, TaskConfig> entry : scheduledTasks.entrySet()) {
            Long taskId = entry.getKey();
            TaskConfig config = entry.getValue();

            // 跳过暂停的任务
            if (isTaskPaused(taskId)) {
                continue;
            }

            // 检查是否应该执行（简化实现：每次都执行，实际应该根据 cron 表达式判断）
            // TODO: 实现更精确的 cron 表达式匹配
            if (shouldExecute(config)) {
                try {
                    Log.info("执行定时任务: " + config.getTaskName());
                    taskExecutionService.submitTask(taskId, config.getIsIncrement());
                } catch (Exception e) {
                    Log.errorf("执行定时任务失败，任务ID: " + taskId + ", 错误: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 判断任务是否应该执行（简化实现）
     *
     * @param config 任务配置
     * @return true 如果应该执行
     */
    private boolean shouldExecute(TaskConfig config) {
        // 简化实现：根据 cron 表达式的分钟字段判断
        // 完整实现应该使用 cron 表达式解析库
        String cron = config.getCron();
        if (cron == null || cron.trim().isEmpty()) {
            return false;
        }

        // 解析 cron 表达式（6 字段格式：秒 分 时 日 月 周）
        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 6) {
            return false;
        }

        // 获取当前时间
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int currentMinute = now.getMinute();
        int currentHour = now.getHour();
        int currentSecond = now.getSecond();

        // 仅在每分钟的前 10 秒内检查，避免重复执行
        if (currentSecond > 10) {
            return false;
        }

        // 简单匹配分钟和小时
        String minuteExpr = parts[1];
        String hourExpr = parts[2];

        return matchesCronField(minuteExpr, currentMinute) && matchesCronField(hourExpr, currentHour);
    }

    /**
     * 匹配 cron 字段（简化实现）
     *
     * @param expr  cron 表达式字段
     * @param value 当前值
     * @return true 如果匹配
     */
    private boolean matchesCronField(String expr, int value) {
        if ("*".equals(expr)) {
            return true;
        }

        if (expr.contains("/")) {
            // 步长表达式，如 */5
            String[] parts = expr.split("/");
            int step = Integer.parseInt(parts[1]);
            return value % step == 0;
        }

        if (expr.contains(",")) {
            // 多值表达式，如 0,30
            String[] values = expr.split(",");
            for (String v : values) {
                if (Integer.parseInt(v.trim()) == value) {
                    return true;
                }
            }
            return false;
        }

        try {
            return Integer.parseInt(expr) == value;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
