package com.yzy.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(RedisProperties prop){
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + prop.getHost() + ":" + prop.getPort())
                .setPassword(prop.getPassword())
                .setUsername(prop.getUser())
                .setRetryAttempts(3)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2)
                .setConnectTimeout(5000);
        return Redisson.create(config);
    }
}
