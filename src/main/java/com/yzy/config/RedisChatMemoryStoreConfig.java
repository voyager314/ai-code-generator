package com.yzy.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisChatMemoryStoreConfig {

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore(RedisProperties props) {
        return RedisChatMemoryStore.builder()
                .host(props.getHost())
                .port(props.getPort())
                .user(props.getUser())
                .password(props.getPassword())
                .ttl(props.getTtl())
                .build();
    }
}

