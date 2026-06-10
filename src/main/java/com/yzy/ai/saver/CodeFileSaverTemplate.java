package com.yzy.ai.saver;

import com.yzy.common.AppConstant;

import java.io.File;

public abstract class CodeFileSaverTemplate<T> {
    protected static final String baseDir= AppConstant.OUTPUT_DIR;
    public abstract File saveCode(T t,Long appId);
}
