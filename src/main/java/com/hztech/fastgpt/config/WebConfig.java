package com.hztech.fastgpt.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.hztech.model.properties.FluentMybatisProperties;
import com.hztech.util.HzDateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * WebConfig
 *
 * @author: boboo
 * @Date: 2024/8/31 9:59
 **/
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({FluentMybatisProperties.class})
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // 设置默认的时间格式化格式
            builder.timeZone(TimeZone.getDefault());
            builder.simpleDateFormat(HzDateUtils.Formats.DATETIME);
            builder.serializers(
                    new LocalTimeSerializer(DateTimeFormatter.ofPattern(HzDateUtils.Formats.TIME)),
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(HzDateUtils.Formats.DATE)),
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(HzDateUtils.Formats.DATETIME))
            );
            builder.deserializers(
                    new LocalTimeDeserializer(DateTimeFormatter.ofPattern(HzDateUtils.Formats.TIME)),
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern(HzDateUtils.Formats.DATE)),
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(HzDateUtils.Formats.DATETIME))
            );
        };
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Add handlers to serve static resources such as images, js, and, css
     * files from specific locations under web application root, the classpath,
     * and others.
     *
     * @see ResourceHandlerRegistry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/fstore/**", "/fstore/preview/**")
                .addResourceLocations("file:/fstore/", "file:/fstore/preview/");
    }
}
