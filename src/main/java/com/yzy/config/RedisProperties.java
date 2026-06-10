package com.yzy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisProperties {

    private String host;

    private int port;

    private String user;

    private String password;

    private long ttl;
}
