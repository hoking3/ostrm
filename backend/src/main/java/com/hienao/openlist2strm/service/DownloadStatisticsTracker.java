package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.DownloadStatistics;
import com.hienao.openlist2strm.dto.FileHashCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下载统计追踪器
 * 跟踪任务执行期间的下载优化统计信息
 *
 * @author hienao
 * @since 2025-12-10
 */
@Slf4j
@Component
public class DownloadStatisticsTracker {

    /**
     * 任务ID到统计信息的映射
     */
    private final Map<String, DownloadStatistics> taskStats = new ConcurrentHashMap<>();

    /**
     * 开始追踪任务
     *
     * @param taskId 任务ID
     */
    public void startTracking(String taskId) {
        taskStats.put(taskId, new DownloadStatistics());
        log.info("Started tracking download statistics for task: {}", taskId);
    }

    /**
     * 停止追踪任务
     *
     * @param taskId 任务ID
     * @return 任务统计信息
     */
    public DownloadStatistics stopTracking(String taskId) {
        DownloadStatistics stats = taskStats.remove(taskId);
        if (stats != null) {
            log.info("Stopped tracking download statistics for task: {}", taskId);
            log.info("Final stats - Checked: {}, Skipped: {}, Downloaded: {}, Saved: {}",
                    stats.getTotalFilesChecked(),
                    stats.getFilesSkipped(),
                    stats.getFilesDownloaded(),
                    stats.getFormattedBytesSaved());
        }
        return stats;
    }

    /**
     * 记录文件检查结果
     *
     * @param taskId 任务ID
     * @param result 文件哈希检查结果
     */
    public void recordFileCheck(String taskId, FileHashCheckResult result) {
        DownloadStatistics stats = taskStats.get(taskId);
        if (stats == null) {
            log.warn("No statistics tracking for task: {}", taskId);
            return;
        }

        stats.incrementTotalFilesChecked();
        stats.addMd5CalculationTime(result.getCheckTimeMs());

        if (result.isLocalFileExists()) {
            if (result.isFilesMatch()) {
                // 文件MD5匹配，跳过下载
                stats.incrementFilesSkipped("MD5_MATCH", result.getFileSize());
            } else {
                // 文件需要更新，会触发下载
                stats.incrementFilesDownloaded();
            }
        } else {
            // 文件不存在，会触发下载
            stats.incrementFilesDownloaded();
        }
    }

    /**
     * 记录文件跳过（非MD5匹配原因）
     *
     * @param taskId     任务ID
     * @param reason     跳过原因
     * @param bytesSaved 节省的字节数
     */
    public void recordFileSkipped(String taskId, String reason, long bytesSaved) {
        DownloadStatistics stats = taskStats.get(taskId);
        if (stats == null) {
            log.warn("No statistics tracking for task: {}", taskId);
            return;
        }

        stats.incrementTotalFilesChecked();
        stats.incrementFilesSkipped(reason, bytesSaved);
    }

    /**
     * 获取任务统计信息
     *
     * @param taskId 任务ID
     * @return 统计信息，如果不存在则返回null
     */
    public DownloadStatistics getStatistics(String taskId) {
        return taskStats.get(taskId);
    }

    /**
     * 获取所有活跃任务的统计信息
     *
     * @return 任务ID到统计信息的映射
     */
    public Map<String, DownloadStatistics> getAllStatistics() {
        return new ConcurrentHashMap<>(taskStats);
    }

    /**
     * 清理所有统计信息
     */
    public void clearAll() {
        taskStats.clear();
        log.info("Cleared all download statistics");
    }

    /**
     * 清理指定任务的统计信息
     *
     * @param taskId 任务ID
     */
    public void clearTask(String taskId) {
        taskStats.remove(taskId);
        log.info("Cleared download statistics for task: {}", taskId);
    }
}