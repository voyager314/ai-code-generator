package com.yzy.ai.WorkFlow.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.yzy.ai.WorkFlow.model.ImageCategoryEnum;
import com.yzy.ai.WorkFlow.model.ImageResource;
import com.yzy.common.AppConstant;
import com.yzy.exception.ToolExecutionException;
import com.yzy.manager.AliOSSManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "dashscope")
@Component
@Data
@Slf4j
public class LogoGenerateTool {
    @Autowired
    private AliOSSManager aliOSSManager;
    private String apiKey;
    private String imageModel;

    @Tool("根据描述生成应用Logo")
    public List<ImageResource> getLogo(@P("Logo的设计描述") String description){
        List<ImageResource> list = new ArrayList<>();
        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(apiKey)
                .model(imageModel)
                .prompt(description)
                .n(1)
                .size("512*512")
                .build();
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        ImageSynthesisResult result;
        try {
            result = imageSynthesis.call(param);
        } catch (NoApiKeyException e) {
            log.error("DashScope API Key未配置");
            throw new ToolExecutionException(
                "Logo生成失败：DashScope API Key未配置。请在application-local.yml中配置dashscope.api-key，" +
                "获取地址：https://dashscope.console.aliyun.com/apiKey"
            );
        } catch (Exception e) {
            log.error("Logo生成失败: {}", e.getMessage());
            throw new ToolExecutionException("Logo生成失败: " + e.getMessage());
        }

        if(result != null && result.getOutput() != null) {
            List<Map<String, String>> maps = result.getOutput().getResults();
            if (maps !=null) {
                for (Map<String, String> map : maps) {
                    String s = map.get("url");
                    if (s != null&&!s.isEmpty()) {
                        try {
                            String imgLocalPath=AppConstant.IMG_DIR+File.separator+ RandomUtil.randomString(8)+".png";
                            HttpUtil.downloadFile(s,imgLocalPath);
                            String ossUrl = aliOSSManager.upload(imgLocalPath);
                            FileUtil.del(imgLocalPath);
                            list.add(ImageResource.builder()
                                    .category(ImageCategoryEnum.LOGO)
                                    .description(description)
                                    .url(ossUrl)
                                    .build());
                        } catch (Exception e) {
                            log.error("Logo下载或上传失败: {}", e.getMessage());
                            throw new ToolExecutionException("Logo下载或上传失败: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return list;
    }
}
