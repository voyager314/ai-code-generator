package com.yzy.listener;

import com.yzy.collector.AiModelMetricsCollector;
import com.yzy.monitor.MonitorContext;
import com.yzy.monitor.MonitorContextHolder;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class AiModelMonitorListener implements ChatModelListener {
    /**
     * 记录请求开始时间
     */
    private static final String REQUST_START_TIME = "requets_start_time";

    /**
     * 记录监控上下文（请求和响应不是一个线程）
      */
    private static final String MONITOR_CONTEXT = "monitor_context";

    @Autowired
    private AiModelMetricsCollector metricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        requestContext.attributes().put(REQUST_START_TIME, Instant.now());
        MonitorContext context = MonitorContextHolder.getContext();
        requestContext.attributes().put(MONITOR_CONTEXT, context);
        metricsCollector.recordRequest(context.getUserId(),context.getAppId(),requestContext.modelProvider().name(),"started");
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        MonitorContext context = MonitorContextHolder.getContext();
        errorContext.attributes().put(MONITOR_CONTEXT, context);
        String appId = context.getAppId();
        String userId = context.getUserId();
        String modelName = errorContext.chatRequest().modelName();
        metricsCollector.recordErr(userId, appId, modelName,errorContext.error().getMessage());
        metricsCollector.recordRequest(userId, appId, modelName,"failed");
        //记录响应时间，即使是失败的
        recordResponseTime(errorContext.attributes(),userId,appId,modelName);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Map<Object, Object> attributes = responseContext.attributes();
        //请求响应不在一个线程，从AI context里取参数
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT);
        String userId = context.getUserId();
        String appId = context.getAppId();
        String modelName = responseContext.chatResponse().modelName();
        metricsCollector.recordRequest(userId,appId,modelName,"success");
        recordResponseTime(attributes, userId,appId, modelName);
        recordTokenUsage(responseContext,userId,appId, modelName);
    }

    /**
     * 记录响应时间
     * @param attributes
     * @param userId
     * @param appId
     * @param modelName
     */
    private void recordResponseTime(Map<Object,Object> attributes,String userId,String appId,String modelName) {
        Instant startTime=(Instant)attributes.get(REQUST_START_TIME);
        Duration duration= Duration.between(startTime,Instant.now());
        metricsCollector.recordResponseTime(userId,appId,modelName,duration);
    }

    /**
     * 获取token消耗情况
     * @param responseContext
     * @param userId
     * @param appId
     * @param modelName
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().tokenUsage();
        if (tokenUsage != null) {
            metricsCollector.recordToken(userId,appId,modelName,"input",tokenUsage.inputTokenCount().longValue());
            metricsCollector.recordToken(userId,appId,modelName,"output",tokenUsage.outputTokenCount().longValue());
            metricsCollector.recordToken(userId,appId,modelName,"total",tokenUsage.totalTokenCount().longValue());
        }
    }
}
