package com.yzy.ai.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {
    public boolean buildVueProject(String projectPath){
        File file = new File(projectPath);
        if(!file.exists()||!file.isDirectory()){
            log.error("项目目录不存在！");
            return false;
        }
        if(!new File(projectPath,"package.json").exists()){
            log.error("package.json文件不存在！");
            return false;
        }
        boolean finished = runNpmInstall(file);
        if(finished){
            log.info("依赖下载完成");
        }else {
            log.error("依赖下载失败");
            return false;
        }
        finished=runNpmBuild(file);
        if(finished){
            log.info("npm run dev执行成功");
        }else {
            log.error("项目构建失败");
            return false;
        }
        if(!new File(projectPath,"dist").exists()){
            log.error("dist目录未生成，构建失败！");
            return false;
        }
        return true;
    }
    /**
     * 执行命令
     * @param workingDir 工作目录
     * @param cmd 命令
     * @param timeout 超时时间
     * @return 是否执行成功
     */
    private boolean executeCmd(File workingDir,String cmd,int timeout) {
        log.info("在目录{}中执行命令 {}",workingDir.getAbsolutePath(),cmd);
        Process process = RuntimeUtil.exec(null, workingDir, cmd.split("\\s+"));
        boolean finished = false;
        try {
            finished = process.waitFor(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(!finished){
            log.error("命令{}执行超时，终止线程",cmd);
            process.destroy();
            return false;
        }
        int exitValue = process.exitValue();
        if(exitValue==0){
            log.info("命令{}执行成功",cmd);
            return true;
        }else {
            log.error("命令{}执行失败",cmd);
            return false;
        }
    }

    /**
     * 执行npm install命令
     * @param workingDir 工作目录
     * @return 是否执行成功
     */
    private boolean runNpmInstall(File workingDir){
        String cmd = "npm";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows")) {
            cmd = String.format("%s install", cmd + ".cmd");
        }else {
            cmd = String.format("%s install", cmd);
        }
        return executeCmd(workingDir,cmd,300);
    }

    /**
     * 执行npm run dev命令
     * @param workingDir 工作目录
     * @return 是否执行成功
     */
    private boolean runNpmBuild(File workingDir){
        String cmd = "npm";
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows")) {
            cmd = String.format("%s run dev", cmd + ".cmd");
        }else {
            cmd = String.format("%s run dev", cmd);
        }
        return executeCmd(workingDir,cmd,150);
    }
}
