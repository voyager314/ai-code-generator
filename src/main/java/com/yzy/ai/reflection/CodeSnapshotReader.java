package com.yzy.ai.reflection;

import cn.hutool.core.io.FileUtil;
import com.yzy.ai.tools.WorkspaceResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class CodeSnapshotReader {

    @Autowired
    private WorkspaceResolver workspaceResolver;

    private static final List<String> CODE_EXTENSIONS = List.of(
            ".html", ".htm", ".css", ".js", ".ts", ".tsx", ".jsx", ".json", ".vue"
    );

    public String read(Long appId, int maxChars) {
        String root = workspaceResolver.getRoot(appId).toString();
        File rootDir = new File(root);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.warn("工作目录不存在或不是目录: {}", root);
            return "";
        }

        StringBuilder content = new StringBuilder();
        content.append("## 项目文件结构和代码\n");

        FileUtil.walkFiles(rootDir, file -> {
            if (maxChars > 0 && content.length() >= maxChars) {
                return;
            }
            if (shouldSkip(rootDir, file)) {
                return;
            }
            String lowerName = file.getName().toLowerCase();
            if (CODE_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
                String subPath = FileUtil.subPath(root, file);
                content.append("### 文件：").append(subPath).append("\n");
                content.append(FileUtil.readUtf8String(file)).append("\n\n");
            }
        });

        if (maxChars > 0 && content.length() > maxChars) {
            return content.substring(0, maxChars);
        }
        return content.toString();
    }

    private boolean shouldSkip(File root, File file) {
        String subPath = FileUtil.subPath(root.getAbsolutePath(), file);
        if (subPath.startsWith(".")) {
            return true;
        }
        return subPath.contains("node_modules" + File.separator)
                || subPath.contains("dist" + File.separator)
                || subPath.contains(".git")
                || subPath.contains("target");
    }
}
