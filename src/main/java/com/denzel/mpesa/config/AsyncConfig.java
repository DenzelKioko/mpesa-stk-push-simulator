package com.denzel.mpesa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async thread pool configuration.
 *
 * The @Async("simulatorExecutor") in PaymentService uses this executor,
 * keeping callback simulation separate from the main HTTP thread pool.
 * This mirrors the non-blocking pattern used in real banking API integrations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "simulatorExecutor")
    public Executor simulatorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mpesa-sim-");
        executor.initialize();
        return executor;
    }
}
