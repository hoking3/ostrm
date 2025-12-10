package com.hienao.openlist2strm.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * MD5计算工具类
 * 用于计算文件的MD5哈希值
 *
 * @author hienao
 * @since 2025-12-10
 */
@Slf4j
@Component
public class MD5Calculator {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 计算文件的MD5哈希值
     *
     * @param filePath 文件路径
     * @return MD5哈希值，如果计算失败则返回Optional.empty()
     */
    public Optional<String> calculateFileMD5(Path filePath) {
        if (filePath == null) {
            log.debug("File path is null");
            return Optional.empty();
        }

        if (!Files.exists(filePath)) {
            log.debug("File does not exist: {}", filePath);
            return Optional.empty();
        }

        if (!Files.isRegularFile(filePath)) {
            log.debug("Path is not a regular file: {}", filePath);
            return Optional.empty();
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath));
                 DigestInputStream dis = new DigestInputStream(is, md)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                // 读取文件内容以更新MessageDigest
                while (dis.read(buffer) != -1) {
                    // 继续读取
                }
            }

            byte[] digest = md.digest();
            String md5Hash = bytesToHex(digest);

            log.debug("Calculated MD5 for file {}: {}", filePath.getFileName(), md5Hash);
            return Optional.of(md5Hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not available", e);
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Failed to calculate MD5 for file: {}", filePath, e);
            return Optional.empty();
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}