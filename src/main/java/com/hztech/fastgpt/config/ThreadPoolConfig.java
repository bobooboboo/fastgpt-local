package com.hztech.fastgpt.config;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.RejectPolicy;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author boboo
 * @version 1.0
 * @date 2023/3/21 16:38
 * 线程池配置类
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    private static final int CPU_NUM = Runtime.getRuntime().availableProcessors();

    @Bean(name = "law-pool")
    public ThreadPoolExecutor threadPoolExecutor() {
        log.info("law-pool 核心线程数：{}", CPU_NUM);
        return ExecutorBuilder.create().setCorePoolSize(CPU_NUM)
                .setMaxPoolSize(CPU_NUM * 2)
                .setKeepAliveTime(120, TimeUnit.SECONDS)
                .setWorkQueue(new LinkedBlockingQueue<>(1000))
                .setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("ThreadPoolExecutor-pool").build())
                .setHandler(RejectPolicy.CALLER_RUNS.getValue()).build();
    }

}