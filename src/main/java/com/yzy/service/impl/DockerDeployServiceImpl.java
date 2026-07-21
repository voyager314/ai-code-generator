package com.yzy.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.yzy.common.AppConstant;
import com.yzy.entity.App;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import com.yzy.mapper.AppMapper;
import com.yzy.service.DockerDeployService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DockerDeployServiceImpl implements DockerDeployService {

    @Value("${code.deploy-host}")
    private String deployHost;

    @Value("${code.docker.port-range-start:10000}")
    private int portRangeStart;

    @Value("${code.docker.port-range-end:20000}")
    private int portRangeEnd;

    @Value("${code.docker.nginx-conf-dir:#{null}}")
    private String nginxConfDir;

    @Value("${code.docker.container-expire-hours:72}")
    private int containerExpireHours;

    @Autowired
    private AppMapper appMapper;

    private static final String CONTAINER_PREFIX = "wise-app-";

    @Override
    public String deploy(App app) {
        Long appId = app.getId();
        String deployKey = app.getDeployKey();


        File projectDir = resolveProjectDir(appId);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用未生成，请先生成代码！");
        }

        stopAndRemove(appId);

        ProjectType projectType = detectProjectType(projectDir);
        log.info("App {} detected as project type: {}", appId, projectType);

        generateDockerfile(projectDir, projectType);

        int port = allocatePort(appId);

        String imageName = CONTAINER_PREFIX + deployKey;
        String containerName = CONTAINER_PREFIX + deployKey;

        buildImage(projectDir, imageName);
        runContainer(containerName, imageName, port, projectType.getInternalPort());

        if (StrUtil.isNotBlank(nginxConfDir)) {
            generateNginxConf(deployKey, port);
            reloadNginx();
        }

        return String.format("%s/%s/", deployHost, deployKey);
    }

    @Override
    public void stopAndRemove(Long appId) {
        App app = appMapper.selectOneById(appId);
        if (app == null || StrUtil.isBlank(app.getDeployKey())) return;

        String containerName = CONTAINER_PREFIX + app.getDeployKey();
        try {
            runCommand(null, "docker rm -f " + containerName);
        } catch (Exception e) {
            log.debug("Container {} not running, skip removal", containerName);
        }
    }

    @Override
    public void cleanupExpiredContainers() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(containerExpireHours);
        QueryWrapper query = new QueryWrapper()
                .isNotNull("deployPort")
                .le("deployedTime", expireTime)
                .eq("isDelete", 0);

        List<App> expiredApps = appMapper.selectListByQuery(query);
        for (App app : expiredApps) {
            try {
                String containerName = CONTAINER_PREFIX + app.getDeployKey();
                runCommand(null, "docker rm -f " + containerName);

                App update = new App();
                update.setId(app.getId());
                update.setDeployPort(null);
                appMapper.update(update);

                log.info("Cleaned expired container: {}", containerName);
            } catch (Exception e) {
                log.error("Failed to clean container for app {}", app.getId(), e);
            }
        }
    }

    private File resolveProjectDir(Long appId) {
        return new File(AppConstant.OUTPUT_DIR, "agent_" + appId);
    }

    // ==================== 项目类型检测 ====================

    private ProjectType detectProjectType(File projectDir) {
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            return ProjectType.STATIC;
        }

        try {
            String content = FileUtil.readUtf8String(packageJson);
            JSONObject pkg = JSONUtil.parseObj(content);
            JSONObject deps = pkg.getJSONObject("dependencies");
            JSONObject devDeps = pkg.getJSONObject("devDependencies");
            JSONObject scripts = pkg.getJSONObject("scripts");

            boolean hasBackendDir = new File(projectDir, "server").exists()
                    || new File(projectDir, "backend").exists();

            boolean hasBackendDep = deps != null && (
                    deps.containsKey("express")
                            || deps.containsKey("@nestjs/core")
                            || deps.containsKey("fastify")
                            || deps.containsKey("koa")
                            || deps.containsKey("socket.io"));

            boolean hasStartScript = scripts != null && scripts.containsKey("start");

            if (hasBackendDir || hasBackendDep) {
                return ProjectType.FULLSTACK;
            }

            if (hasStartScript && !hasBuildOutput(projectDir, devDeps)) {
                return ProjectType.FULLSTACK;
            }

            return ProjectType.FRONTEND;
        } catch (Exception e) {
            log.warn("Failed to parse package.json for project type detection", e);
            return ProjectType.FRONTEND;
        }
    }

    private boolean hasBuildOutput(File projectDir, JSONObject devDeps) {
        if (devDeps == null) return false;
        return devDeps.containsKey("vite")
                || devDeps.containsKey("next")
                || devDeps.containsKey("@vue/cli-service")
                || devDeps.containsKey("webpack");
    }

    // ==================== Dockerfile 生成 ====================

    private void generateDockerfile(File projectDir, ProjectType type) {
        String template = loadTemplate(type.getTemplateName());
        File dockerfile = new File(projectDir, "Dockerfile");
        FileUtil.writeUtf8String(template, dockerfile);

        if (type == ProjectType.FRONTEND || type == ProjectType.FULLSTACK) {
            generateDockerignore(projectDir);
        }
    }

    private void generateDockerignore(File projectDir) {
        File dockerignore = new File(projectDir, ".dockerignore");
        if (!dockerignore.exists()) {
            FileUtil.writeUtf8String("node_modules\n.git\ndist\nbuild\nout\n", dockerignore);
        }
    }

    private String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("docker-templates/" + templateName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加载Dockerfile模板失败: " + templateName);
        }
    }

    // ==================== 端口分配 ====================

    private int allocatePort(Long appId) {
        App existingApp = appMapper.selectOneById(appId);
        if (existingApp != null && existingApp.getDeployPort() != null) {
            return existingApp.getDeployPort();
        }

        QueryWrapper query = new QueryWrapper()
                .isNotNull("deployPort")
                .eq("isDelete", 0);
        List<App> appsWithPort = appMapper.selectListByQuery(query);

        Set<Integer> usedPorts = new HashSet<>();
        for (App app : appsWithPort) {
            usedPorts.add(app.getDeployPort());
        }

        for (int port = portRangeStart; port <= portRangeEnd; port++) {
            if (!usedPorts.contains(port)) {
                App update = new App();
                update.setId(appId);
                update.setDeployPort(port);
                appMapper.update(update);
                return port;
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无可用端口");
    }

    // ==================== Docker 构建与运行 ====================

    private void buildImage(File projectDir, String imageName) {
        try {
            String output = runCommand(projectDir, "docker build -t " + imageName + " .");
            log.info("Docker build success for {}", imageName);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Docker镜像构建失败: " + e.getMessage());
        }
    }

    private void runContainer(String containerName, String imageName, int hostPort, int containerPort) {
        try {
            String cmd = String.format(
                    "docker run -d --name %s --restart unless-stopped -p %d:%d %s",
                    containerName, hostPort, containerPort, imageName);
            runCommand(null, cmd);
            log.info("Container {} started on port {}", containerName, hostPort);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Docker容器启动失败: " + e.getMessage());
        }
    }

    // ==================== Nginx 配置 ====================

    private void generateNginxConf(String deployKey, int port) {
        String conf = String.format("""
                location /%s/ {
                    proxy_pass http://127.0.0.1:%d/;
                    proxy_set_header Host $host;
                    proxy_set_header X-Real-IP $remote_addr;
                    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                    proxy_set_header X-Forwarded-Proto $scheme;
                    proxy_http_version 1.1;
                    proxy_set_header Upgrade $http_upgrade;
                    proxy_set_header Connection "upgrade";
                }
                """, deployKey, port);

        File destDir=new File(nginxConfDir);
        if(!destDir.exists()){destDir.mkdirs();}
        File confFile = new File(destDir, "wise-app-" + deployKey + ".conf");
        FileUtil.writeUtf8String(conf, confFile);
        log.info("Nginx conf generated: {}", confFile.getAbsolutePath());
    }

    private void reloadNginx() {
        try {
            runCommand(null, "nginx -s reload");
        } catch (Exception e) {
            log.warn("Nginx reload failed, may need manual reload", e);
        }
    }

    // ==================== 命令执行 ====================

    private String runCommand(File workDir, String command) throws Exception {
        ProcessBuilder pb = System.getProperty("os.name").toLowerCase().contains("win")
                ? new ProcessBuilder("cmd", "/c", command)
                : new ProcessBuilder("sh", "-c", command);

        if (workDir != null) pb.directory(workDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
        }

        if (!process.waitFor(600, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new Exception("命令超时: " + command);
        }
        if (process.exitValue() != 0) {
            throw new Exception("命令失败(exit=" + process.exitValue() + "): " + output);
        }
        return output.toString();
    }

    // ==================== 项目类型枚举 ====================

    @Getter
    private enum ProjectType {
        STATIC("Dockerfile.static", 80),
        FRONTEND("Dockerfile.frontend", 80),
        FULLSTACK("Dockerfile.fullstack", 3000);

        private final String templateName;
        private final int internalPort;

        ProjectType(String templateName, int internalPort) {
            this.templateName = templateName;
            this.internalPort = internalPort;
        }

    }
}
