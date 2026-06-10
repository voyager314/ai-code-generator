package com.yzy.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownLoadService {
    /**
     * 下载项目zip包
     * @param projectPath 文件输出路径
     * @param fileName 文件名
     * @param response 响应对象
     */
    public void downLoadAsZip(String projectPath, String fileName, HttpServletResponse response);
}
