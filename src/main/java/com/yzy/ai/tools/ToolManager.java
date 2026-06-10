package com.yzy.ai.tools;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ToolManager {
    private final Map<String,BaseTool> map = new HashMap<>();
    @Getter
    @Autowired
    private BaseTool[] tools;

    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            map.put(tool.getToolName(), tool);
        }
    }

    /**
     * 根据名称获取工具
     * @param name 工具名称
     * @return 工具实现类
     */
    public BaseTool getToolByName(String name){
        return map.get(name);
    }
}
