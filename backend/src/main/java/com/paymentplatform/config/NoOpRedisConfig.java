package com.paymentplatform.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Configuration
@ConditionalOnProperty(prefix = "feature.cache", name = "enabled", havingValue = "false")
public class NoOpRedisConfig {

    @Bean
    public RedissonClient redisson() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                Class<?> returnType = method.getReturnType();
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) return false;
                    if (returnType == byte.class) return (byte) 0;
                    if (returnType == short.class) return (short) 0;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == float.class) return 0f;
                    if (returnType == double.class) return 0d;
                    if (returnType == char.class) return '\0';
                }
                return null;
            }
        };

        RedissonClient client = (RedissonClient) Proxy.newProxyInstance(
            RedissonClient.class.getClassLoader(),
            new Class<?>[]{RedissonClient.class},
            handler
        );
        return client;
    }

    private RedisConnectionFactory noOpRedisConnectionFactory() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                Class<?> returnType = method.getReturnType();
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) return false;
                    if (returnType == byte.class) return (byte) 0;
                    if (returnType == short.class) return (short) 0;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == float.class) return 0f;
                    if (returnType == double.class) return 0d;
                    if (returnType == char.class) return '\0';
                }
                return null;
            }
        };

        return (RedisConnectionFactory) Proxy.newProxyInstance(
            RedisConnectionFactory.class.getClassLoader(),
            new Class<?>[]{RedisConnectionFactory.class},
            handler
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(noOpRedisConnectionFactory());
        return template;
    }
}
