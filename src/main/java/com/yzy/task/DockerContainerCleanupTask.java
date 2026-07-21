package com.yzy.task;

import com.yzy.service.DockerDeployService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DockerContainerCleanupTask {

    @Autowired
    private DockerDeployService dockerDeployService;

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredContainers() {
        log.info("开始清理过期Docker容器...");
        try {
            dockerDeployService.cleanupExpiredContainers();
        } catch (Exception e) {
            log.error("清理过期Docker容器失败", e);
        }
    }
}
