package com.paymentplatform.config;

import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
@ImportAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    RedissonAutoConfigurationV2.class
})
public class LocalRedisAutoConfigExclusion {
}
