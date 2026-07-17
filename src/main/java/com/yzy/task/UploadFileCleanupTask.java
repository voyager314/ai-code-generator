package com.yzy.task;

import com.yzy.common.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;

@Slf4j
@Component
public class UploadFileCleanupTask {

    @Scheduled(cron = "${file.cleanup.cron:0 0 3 * * ?}")
    public void cleanExpiredUploadFiles() {
        Instant expirationThreshold = ZonedDateTime.now().minusMonths(3).toInstant();
        int deletedFileCount = cleanupExpiredFiles(Path.of(AppConstant.FILE_DIR), expirationThreshold);
        if (deletedFileCount > 0) {
            log.info("已清理 {} 个超过 3 个月的上传文件", deletedFileCount);
        }
    }

    int cleanupExpiredFiles(Path rootDirectory, Instant expirationThreshold) {
        if (Files.notExists(rootDirectory)) {
            return 0;
        }

        int deletedFileCount = 0;
        try (var paths = Files.walk(rootDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isRegularFile(path)) {
                    if (Files.getLastModifiedTime(path).toInstant().isBefore(expirationThreshold)
                            && deletePath(path)) {
                        deletedFileCount++;
                    }
                } else if (!path.equals(rootDirectory) && Files.isDirectory(path)) {
                    deleteEmptyDirectory(path);
                }
            }
        } catch (IOException e) {
            log.error("扫描上传文件目录失败: {}", rootDirectory, e);
        }
        return deletedFileCount;
    }

    private boolean deletePath(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除过期上传文件失败: {}", path, e);
            return false;
        }
    }

    private void deleteEmptyDirectory(Path directory) {
        try (var children = Files.list(directory)) {
            if (children.findAny().isEmpty()) {
                Files.deleteIfExists(directory);
            }
        } catch (IOException e) {
            log.warn("清理空上传目录失败: {}", directory, e);
        }
    }
}
