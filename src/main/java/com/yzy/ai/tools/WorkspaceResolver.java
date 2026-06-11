package com.yzy.ai.tools;

import com.yzy.common.AppConstant;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkspaceResolver {
    private final ConcurrentHashMap<Long, Path> workspaces = new ConcurrentHashMap<>();

    public void registerWorkspace(Long appId, String path) {
        workspaces.put(appId, Paths.get(path));
    }

    public Path getRoot(Long appId) {
        return workspaces.getOrDefault(appId,
                Paths.get(AppConstant.OUTPUT_DIR, "vue_project_" + appId));
    }

    public Path resolve(Long appId, String relativePath) {
        Path path = Paths.get(relativePath);
        if (path.isAbsolute()) {
            return path;
        }
        return getRoot(appId).resolve(relativePath);
    }
}
