package com.fersacare.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool de threads da ingestão de manuais. O upload devolve 202 na hora e o
 * PDF é processado aqui em segundo plano (extração + embeddings pode levar
 * de segundos a alguns minutos). Duas threads bastam: o gargalo é a cota
 * por minuto do Gemini, não a CPU — mais threads só fariam estourar a cota.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ingestaoExecutor")
    public ThreadPoolTaskExecutor ingestaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ingestao-");
        executor.initialize();
        return executor;
    }
}
