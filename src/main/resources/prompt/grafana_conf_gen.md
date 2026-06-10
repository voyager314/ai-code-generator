帮我根据分析需求、数据上报相关代码、示例从 Prometheus 收集到的数据，来生成 Grafana 看板的 JSON 导入代码，全部汇总到一个看板中。

相关的规范参考：https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/

## 需求
面板要展示总token消耗、成功请求数、错误数、平均响应时间  
在token消耗分析中，还要展示token消耗累计趋势和token类型分布饼图(output/input)

## 数据上报相关代码

```java
package com.yzy.collector;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiModelMetricsCollector {
    @Autowired
    private MeterRegistry registry;
    /**
     * 请求数
     */
    private Map<String, Counter> requestCounterCache=new ConcurrentHashMap<>();
    /**
     * 错误数
     */
    private Map<String, Counter> errCounterCache=new ConcurrentHashMap<>();
    /**
     * token消耗数
     */
    private Map<String,Counter> tokenCounterCache=new ConcurrentHashMap<>();
    /**
     * 响应时间
     */
    private Map<String, Timer> responseTimerCache=new ConcurrentHashMap<>();

    /**
     * 计算请求数
     * @param userId 用户id
     * @param appId 应用id
     * @param modelName 模型
     * @param status 请求状态
     */
    public void recordRequest(String userId,String appId,String modelName,String status){
        String s=String.format("%s_%s_%s_%s",userId,appId,modelName,status);
        Counter counter = requestCounterCache.computeIfAbsent(s, k -> Counter
                .builder("ai_model_requests_total")
                .description("AI模型请求总数")
                .tag("app_id", appId)
                .tag("user_id", userId)
                .tag("model_name", modelName)
                .tag("status", status)
                .register(registry));
        counter.increment();
    }

    /**
     * 计算模型出错数
     * @param userId
     * @param appId
     * @param modelName
     * @param errMsg
     */
    public void recordErr(String userId,String appId,String modelName,String errMsg){
        String s=String.format("%s_%s_%s_%s",userId,appId,modelName,errMsg);
        Counter counter = errCounterCache.computeIfAbsent(s, k -> Counter
                .builder("ai_model_errors_total")
                .description("AI模型错误总数")
                .tag("app_id", appId)
                .tag("user_id", userId)
                .tag("model_name", modelName)
                .tag("error_message", errMsg)
                .register(registry));
        counter.increment();
    }

    /**
     * 计算token消耗
     * @param userId 用户id
     * @param appId 应用id
     * @param modelName 模型
     * @param tokenType token类型
     * @param tokenCount token消耗
     */
    public void recordToken(String userId,String appId,String modelName,String tokenType,long tokenCount){
        String s=String.format("%s_%s_%s_%s",userId,appId,modelName,tokenType);
        Counter counter = tokenCounterCache.computeIfAbsent(s, k -> Counter
                .builder("ai_model_tokens_total")
                .description("AI模型token消耗总数")
                .tag("app_id", appId)
                .tag("user_id", userId)
                .tag("model_name", modelName)
                .tag("token_type", tokenType)
                .register(registry));
        counter.increment(tokenCount);
    }

    public void recordResponseTime(String userId, String appId, String modelName, Duration duration){
        String s=String.format("%s_%s_%s",userId,appId,modelName);
        Timer timer = responseTimerCache.computeIfAbsent(s, k -> Timer
                .builder("ai_model_response_time_seconds")
                .description("AI模型响应时间(秒)")
                .tag("app_id", appId)
                .tag("user_id", userId)
                .tag("model_name", modelName)
                .register(registry));
        timer.record(duration);
    }
}

```
## 示例收集到的数据

HELP ai_model_requests_total AI模型总请求次数
TYPE ai_model_requests_total counter
ai_model_requests_total{app_id="313129227198590976",model_name="deepseek-chat",status="started",user_id="302588523967918080"} 2.0
ai_model_requests_total{app_id="313129227198590976",model_name="deepseek-chat",status="success",user_id="302588523967918080"} 2.0

HELP ai_model_response_duration_seconds AI模型响应时间
TYPE ai_model_response_duration_seconds summary
ai_model_response_duration_seconds_count{app_id="313129227198590976",model_name="deepseek-chat",user_id="302588523967918080"} 2
ai_model_response_duration_seconds_sum{app_id="313129227198590976",model_name="deepseek-chat",user_id="302588523967918080"} 91.285863

HELP ai_model_tokens_total AI模型Token消耗总数
TYPE ai_model_tokens_total counter
ai_model_tokens_total{app_id="313129227198590976",model_name="deepseek-chat",token_type="input",user_id="302588523967918080"} 1321.0
ai_model_tokens_total{app_id="313129227198590976",model_name="deepseek-chat",token_type="output",user_id="302588523967918080"} 519.0
ai_model_tokens_total{app_id="313129227198590976",model_name="deepseek-chat",token_type="total",user_id="302588523967918080"} 1840.0
