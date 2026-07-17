package com.yzy.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadFileCleanupTaskTest {

    @TempDir
    Path tempDirectory;

    private final UploadFileCleanupTask cleanupTask = new UploadFileCleanupTask();

    @Test
    void shouldDeleteOnlyFilesOlderThanExpirationThreshold() throws Exception {
        Path appDirectory = Files.createDirectories(tempDirectory.resolve("1").resolve("100"));
        Path expiredFile = Files.writeString(appDirectory.resolve("expired.txt"), "expired");
        Path retainedFile = Files.writeString(appDirectory.resolve("retained.txt"), "retained");
        Instant expirationThreshold = Instant.now().minus(90, ChronoUnit.DAYS);
        Files.setLastModifiedTime(expiredFile, FileTime.from(expirationThreshold.minus(1, ChronoUnit.DAYS)));
        Files.setLastModifiedTime(retainedFile, FileTime.from(expirationThreshold.plus(1, ChronoUnit.DAYS)));

        int deletedFileCount = cleanupTask.cleanupExpiredFiles(tempDirectory, expirationThreshold);

        assertEquals(1, deletedFileCount);
        assertFalse(Files.exists(expiredFile));
        assertTrue(Files.exists(retainedFile));
    }

    @Test
    void shouldRemoveEmptyDirectoriesAfterDeletingExpiredFiles() throws Exception {
        Path appDirectory = Files.createDirectories(tempDirectory.resolve("1").resolve("100"));
        Path expiredFile = Files.writeString(appDirectory.resolve("expired.txt"), "expired");
        Instant expirationThreshold = Instant.now().minus(90, ChronoUnit.DAYS);
        Files.setLastModifiedTime(expiredFile, FileTime.from(expirationThreshold.minus(1, ChronoUnit.DAYS)));

        cleanupTask.cleanupExpiredFiles(tempDirectory, expirationThreshold);

        assertFalse(Files.exists(appDirectory));
        assertFalse(Files.exists(appDirectory.getParent()));
        assertTrue(Files.exists(tempDirectory));
    }
}
