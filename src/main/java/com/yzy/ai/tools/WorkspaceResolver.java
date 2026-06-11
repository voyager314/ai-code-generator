package com.yzy.ai.tools;

import com.yzy.common.AppConstant;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作空间路径解析器
 * <p>
 * 管理 appId 到工作空间根目录的映射，供所有文件操作工具统一使用。
 * Agent 模式下通过 registerWorkspace() 注册 agent_xxx 目录；
 * 未注册的 appId 回退到旧的 vue_project_xxx 路径，保证向后兼容。
 */
@Component
public class WorkspaceResolver {
    private final ConcurrentHashMap<Long, Path> workspaces = new ConcurrentHashMap<>();

    /**
     * 为指定 appId 注册工作空间根目录
     */
    public void registerWorkspace(Long appId, String path) {
        workspaces.put(appId, Paths.get(path));
    }

    /**
     * 获取工作空间根目录，未注册则回退到 vue_project_xxx
     */
    public Path getRoot(Long appId) {
        return workspaces.getOrDefault(appId,
                Paths.get(AppConstant.OUTPUT_DIR, "vue_project_" + appId));
    }

    /**
     * 将相对路径解析为工作空间内的绝对路径；如果传入的已是绝对路径则直接返回
     */
    public Path resolve(Long appId, String relativePath) {
        Path path = Paths.get(relativePath);
        if (path.isAbsolute()) {
            return path;
        }
        return getRoot(appId).resolve(relativePath);
    }
}
