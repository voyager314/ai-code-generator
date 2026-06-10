package com.yzy.ai.WorkFlow.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yzy.ai.WorkFlow.model.ImageCategoryEnum;
import com.yzy.ai.WorkFlow.model.ImageResource;
import com.yzy.exception.ToolExecutionException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ImageSearchTool {
    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";
    @Value("${pexels.api-key}")
    private String pexelsApiKey;

    @Tool("内容图片和页面插画搜索工具，用于内容展示与页面美化")
    public List<ImageResource> searchImages(@P("搜索关键字") String query){
        int cnt=20;
        List<ImageResource> res=new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                    .header("Authorization", pexelsApiKey)
                    .form("query", query)
                    .form("per_page", cnt)
                    .form("page", 1)
                    .execute()) {
                if (response.isOk()) {
                    JSONObject jsonObject = JSONUtil.parseObj(response.body());
                    JSONArray photos = jsonObject.getJSONArray("photos");
                    for (int j = 0; j < photos.size(); j++) {
                        JSONObject photo = photos.getJSONObject(j);
                        JSONObject src = photo.getJSONObject("src");
                        res.add(ImageResource.builder()
                                .category(ImageCategoryEnum.CONTENT)
                                .description(photo.getStr("alt", query))
                                .url(src.getStr("medium"))
                                .build());
                    }
                    return res;
                } else if (response.getStatus() == 401) {
                    log.error("Pexels API Key无效");
                    throw new ToolExecutionException(
                        "图片搜索失败：Pexels API Key无效。请在application-local.yml中配置pexels.api-key，" +
                        "获取地址：https://www.pexels.com/api/"
                    );
                }
                log.warn("图片搜索失败，状态码: {}, 重试 {}/3", response.getStatus(), i + 1);
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                log.error("图片搜索异常: {}, 重试 {}/3", e.getMessage(), i + 1);
                if (i == 2) {
                    throw new ToolExecutionException("图片搜索失败: " + e.getMessage());
                }
            }
        }
        throw new ToolExecutionException("图片搜索失败：重试3次后仍无法连接Pexels API");
    }

}
