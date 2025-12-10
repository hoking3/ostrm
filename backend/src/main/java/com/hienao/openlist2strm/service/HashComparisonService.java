package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.FileHashCheckResult;
import com.hienao.openlist2strm.dto.HashComparisonConfig;
import com.hienao.openlist2strm.util.MD5Calculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 哈希比对服务
 * 负责比对本地文件与API返回文件的MD5值
 *
 * @author hienao
 * @since 2025-12-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HashComparisonService {

    private final MD5Calculator md5Calculator;

    /**
     * 检查文件哈希值
     *
     * @param apiFile    API返回的文件信息
     * @param localPath  本地文件路径
     * @param config     MD5比对配置
     * @return 哈希检查结果
     */
    public FileHashCheckResult checkFileHash(
            OpenlistApiService.OpenlistFile apiFile,
            Path localPath,
            HashComparisonConfig config) {

        FileHashCheckResult result = new FileHashCheckResult();
        result.setFileName(apiFile.getName());
        result.setLocalFilePath(localPath.toString());

        long startTime = System.currentTimeMillis();

        try {
            // 检查本地文件是否存在
            boolean localExists = Files.exists(localPath);
            result.setLocalFileExists(localExists);

            if (!localExists) {
                log.debug("Local file does not exist: {}", localPath);
                result.setFilesMatch(false);
                return result;
            }

            // 获取文件大小
            long fileSize = Files.size(localPath);
            result.setFileSize(fileSize);

            // 检查是否超过MD5计算大小限制
            if (fileSize > config.getMaxFileSizeForMd5()) {
                if (config.isLogSkippedFiles()) {
                    log.info("File too large for MD5 check ({}MB), will download: {}",
                            fileSize / (1024 * 1024), apiFile.getName());
                }
                result.setFilesMatch(false);
                result.setErrorMessage("File too large for MD5 check");
                return result;
            }

            // 获取API返回的MD5
            if (!apiFile.hasMd5Hash()) {
                log.debug("API does not provide MD5 for file: {}", apiFile.getName());
                result.setFilesMatch(false);
                result.setErrorMessage("No MD5 provided by API");
                return result;
            }

            String remoteMd5 = apiFile.getMd5Hash();
            result.setRemoteMd5(remoteMd5);

            // 计算本地文件MD5
            long md5Start = System.currentTimeMillis();
            String localMd5 = md5Calculator.calculateFileMD5(localPath)
                    .orElse(null);
            long md5Time = System.currentTimeMillis() - md5Start;

            if (localMd5 == null) {
                log.warn("Failed to calculate MD5 for local file: {}", localPath);
                result.setFilesMatch(false);
                result.setErrorMessage("Failed to calculate local MD5");
                return result;
            }

            result.setLocalMd5(localMd5);

            // 比较MD5值
            boolean matches = remoteMd5.equalsIgnoreCase(localMd5);
            result.setFilesMatch(matches);

            if (matches) {
                if (config.isLogSkippedFiles()) {
                    log.info("MD5 matches, skipping download: {} (saved {} bytes)",
                            apiFile.getName(), fileSize);
                }
            } else {
                log.debug("MD5 mismatch, will download: {} (remote: {}, local: {})",
                        apiFile.getName(), remoteMd5, localMd5);
            }

        } catch (Exception e) {
            log.error("Error checking file hash for: {}", apiFile.getName(), e);
            result.setFilesMatch(false);
            result.setErrorMessage("Error during hash check: " + e.getMessage());
        } finally {
            result.setCheckTimeMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

  }