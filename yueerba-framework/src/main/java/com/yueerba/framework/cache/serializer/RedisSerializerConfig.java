package com.yueerba.framework.cache.serializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Description: 配置Redis的序列化和反序列化
 * Author: yueerba
 * Date: 2023/9/12
 */
@Configuration
public class RedisSerializerConfig {

    @Bean
    public GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer() {
        // 创建一个通用的Redis JSON序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        return serializer;
    }

    @Bean
    public ObjectMapper objectMapper() {
        // 创建一个自定义的ObjectMapper对象用于配置序列化和反序列化规则
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置对象的可见性规则，使得所有字段都可以被序列化和反序列化
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 配置在反序列化时忽略未知的属性，防止反序列化失败
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 配置在序列化时忽略空的对象，防止序列化失败
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 启用Jackson的默认类型信息，用于支持多态类型的序列化和反序列化
        objectMapper.activateDefaultTyping(
                // 使用LaissezFaireSubTypeValidator作为子类型验证器，该验证器允许几乎任何类型的序列化和反序列化
                LaissezFaireSubTypeValidator.instance,
                // 指定默认的类型信息处理方式，这里选择NON_FINAL，表示只对非final类使用类型信息，默认的类型信息将被写入非final类的对象中
                ObjectMapper.DefaultTyping.NON_FINAL,
                // 指定类型信息的包装方式，这里选择WRAPPER_ARRAY，表示类型信息将包装在一个JSON数组中
                JsonTypeInfo.As.WRAPPER_ARRAY
        );

        // 设置只包含非空的属性值，不包含null值的属性
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 禁用日期类型的时间戳输出，而是以ISO-8601日期格式输出日期
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 注册JavaTime模块，以支持Java 8中的日期和时间类型
        objectMapper.registerModule(new JavaTimeModule());

        return objectMapper;
    }
}

