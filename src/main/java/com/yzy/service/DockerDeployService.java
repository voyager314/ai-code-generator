package com.yzy.service;

import com.yzy.entity.App;

public interface DockerDeployService {

    /**
     * Docker部署应用，返回访问URL
     */
    String deploy(App app);

    /**
     * 停止并移除指定应用的容器
     */
    void stopAndRemove(Long appId);

    /**
     * 清理过期容器（由定时任务调用）
     */
    void cleanupExpiredContainers();
}
