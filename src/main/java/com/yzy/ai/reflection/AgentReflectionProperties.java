package com.yzy.ai.reflection;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.reflection")
public class AgentReflectionProperties {
    private boolean enabled = true;
    private int maxRetries = 2;
    private int maxSnapshotChars = 60000;
}
