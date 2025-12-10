package com.hienao.openlist2strm.dto;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 下载优化统计信息
 * 跟踪文件下载优化的各种指标
 *
 * @author hienao
 * @since 2025-12-10
 */
@Data
public class DownloadStatistics {

    /**
     * 总检查文件数
     */
    private final AtomicInteger totalFilesChecked = new AtomicInteger(0);

    /**
     * 跳过的文件数（MD5匹配）
     */
    private final AtomicInteger filesSkipped = new AtomicInteger(0);

    /**
     * 下载的文件数
     */
    private final AtomicInteger filesDownloaded = new AtomicInteger(0);

    /**
     * 节省的字节数
     */
    private final AtomicLong bytesSaved = new AtomicLong(0);

    /**
     * MD5计算耗时（毫秒）
     */
    private final AtomicLong md5CalculationTimeMs = new AtomicLong(0);

    /**
     * 跳过原因统计
     */
    private final ConcurrentHashMap<String, AtomicInteger> skipReasons = new ConcurrentHashMap<>();

    /**
     * 增加检查文件计数
     */
    public void incrementTotalFilesChecked() {
        totalFilesChecked.incrementAndGet();
    }

    /**
     * 增加跳过文件计数
     *
     * @param bytesSaved 节省的字节数
     */
    public void incrementFilesSkipped(long bytesSaved) {
        filesSkipped.incrementAndGet();
        this.bytesSaved.addAndGet(bytesSaved);
        skipReasons.computeIfAbsent("MD5_MATCH", k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 增加跳过文件计数（带原因）
     *
     * @param reason    跳过原因
     * @param bytesSaved 节省的字节数
     */
    public void incrementFilesSkipped(String reason, long bytesSaved) {
        filesSkipped.incrementAndGet();
        this.bytesSaved.addAndGet(bytesSaved);
        skipReasons.computeIfAbsent(reason, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 增加下载文件计数
     */
    public void incrementFilesDownloaded() {
        filesDownloaded.incrementAndGet();
    }

    /**
     * 增加MD5计算耗时
     *
     * @param timeMs 耗时（毫秒）
     */
    public void addMd5CalculationTime(long timeMs) {
        md5CalculationTimeMs.addAndGet(timeMs);
    }

    /**
     * 获取跳过率
     *
     * @return 跳过率（0-1之间）
     */
    public double getSkipRate() {
        int total = totalFilesChecked.get();
        return total > 0 ? (double) filesSkipped.get() / total : 0.0;
    }

    /**
     * 重置所有统计信息
     */
    public void reset() {
        totalFilesChecked.set(0);
        filesSkipped.set(0);
        filesDownloaded.set(0);
        bytesSaved.set(0);
        md5CalculationTimeMs.set(0);
        skipReasons.clear();
    }

    /**
     * 格式化节省的字节数
     *
     * @return 格式化后的字节数字符串
     */
    public String getFormattedBytesSaved() {
        long bytes = bytesSaved.get();
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}