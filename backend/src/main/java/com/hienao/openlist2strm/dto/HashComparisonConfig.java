package com.hienao.openlist2strm.dto;

import lombok.Data;

/**
 * MD5比对配置
 * 控制文件下载优化行为
 *
 * @author hienao
 * @since 2025-12-10
 */
@Data
public class HashComparisonConfig {

    /**
     * 是否启用MD5比对功能（主开关）
     */
    private boolean enabled = true;

    /**
     * 跳过MD5计算的文件大小限制（字节）
     * 大于此值的文件不计算MD5，直接下载
     */
    private long maxFileSizeForMd5 = 104857600L; // 100MB

    /**
     * 强制下载所有文件
     * 为true时跳过MD5检查，下载所有文件
     */
    private boolean forceDownloadAll = false;

    /**
     * 记录跳过的文件日志
     */
    private boolean logSkippedFiles = true;

    /**
     * 并行MD5计算线程数
     */
    private int parallelMd5Threads = 4;

    /**
     * 验证配置是否有效
     *
     * @return 配置是否有效
     */
    public boolean isValid() {
        return maxFileSizeForMd5 > 0 &&
               parallelMd5Threads >= 1 &&
               parallelMd5Threads <= 32;
    }
}