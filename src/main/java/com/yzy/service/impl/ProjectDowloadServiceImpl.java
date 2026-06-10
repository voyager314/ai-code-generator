package com.yzy.service.impl;

import cn.hutool.core.util.ZipUtil;
import com.yzy.exception.ErrorCode;
import com.yzy.exception.ThrowUtil;
import com.yzy.service.ProjectDownLoadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDowloadServiceImpl implements ProjectDownLoadService {
    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    /**
     * 检查路径是否允许包含在压缩包中
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);
        // 检查路径中的每一部分
        for (Path part : relativePath) {
            String partName = part.toString();
            // 检查是否在忽略名称列表中
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // 检查文件扩展名
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public void downLoadAsZip(String projectPath, String fileName, HttpServletResponse response) {
        File projectFile = new File(projectPath);
        ThrowUtil.throwIf(!projectFile.exists(), ErrorCode.OPERATION_ERROR,"项目文件不存在！");
        //过滤文件
        FileFilter fileFilter=file->isPathAllowed(projectFile.toPath(), file.toPath());
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", fileName));
        try {
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8,false,fileFilter,projectFile);
        } catch (IOException e) {
            log.error("项目打包下载异常{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
