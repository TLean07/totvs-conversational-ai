package com.tlean07.fiap.challenge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Executor das analises assincronas.
 *
 * O numero de threads precisa ser menor que o pool de conexoes Oracle (5 por padrao): com 4 threads
 * e um pool de 3, uma importacao grande esgotava o pool, o dashboard ficava sem conexao e as
 * transcricoes travavam em PROCESSANDO. Duas threads deixam sempre conexao sobrando para a API.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor(@Value("${analysis.threads:2}") int threads,
                                 @Value("${analysis.queue-capacity:10000}") int queueCapacity) {
        int size = Math.max(1, threads);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(size);
        executor.setMaxPoolSize(size);
        executor.setQueueCapacity(Math.max(100, queueCapacity));
        executor.setThreadNamePrefix("transcription-analysis-");
        executor.initialize();
        return executor;
    }
}
