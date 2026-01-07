package com.sep490.backendclubmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Bật cơ chế @Async cho toàn ứng dụng
public class AsyncConfig {

    // Thread pool dành riêng cho upload ảnh
    @Bean(name = "uploadExecutor")
    public Executor uploadExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(6);      // số luồng tối thiểu (tùy CPU/IO)
        ex.setMaxPoolSize(10);       // số luồng tối đa
        ex.setQueueCapacity(200);   // hàng đợi tác vụ khi đầy core pool
        ex.setThreadNamePrefix("upload-"); // dễ đọc log
        ex.initialize();
        return ex;
    }
}
