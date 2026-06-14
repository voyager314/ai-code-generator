package com.yzy.common;

import java.io.File;

public interface AppConstant {
    /**
     * 应用源码输出目录
     */
    String OUTPUT_DIR=System.getProperty("user.dir")+ File.separator+"temp"+File.separator+"output";

    /**
     * 应用源码部署目录
     */
    String DEPLOY_DIR=System.getProperty("user.dir")+ File.separator+"temp"+File.separator+"deploy";

    /**
     * 图片临时目录
     */
    String IMG_DIR=System.getProperty("user.dir")+ File.separator+"temp"+File.separator+"img";

    /**
     * 工具输出归档子目录名（位于各工作区根目录下）
     */
    String LOG_SUBDIR = "log";
}
