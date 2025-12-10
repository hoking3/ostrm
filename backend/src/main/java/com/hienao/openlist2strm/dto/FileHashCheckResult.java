package com.hienao.openlist2strm.dto;

import lombok.Data;

/**
 * 文件哈希检查结果
 * 记录本地文件与API返回文件的MD5比对结果
 *
 * @author hienao
 * @since 2025-12-10
 */
@Data
public class FileHashCheckResult {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 本地文件路径
     */
    private String localFilePath;

    /**
     * 远程MD5（来自API的hash_info.md5）
     */
    private String remoteMd5;

    /**
     * 本地计算的MD5
     */
    private String localMd5;

    /**
     * 文件是否匹配（MD5相同）
     */
    private boolean filesMatch;

    /**
     * 本地文件是否存在
     */
    private boolean localFileExists;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 错误信息（如果MD5计算失败）
     */
    private String errorMessage;

    /**
     * 检查耗时（毫秒）
     */
    private long checkTimeMs;
}