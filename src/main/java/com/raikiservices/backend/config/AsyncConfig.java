package com.raikiservices.backend.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool dédié aux traitements hors requête : envois d'emails, notifications push.
 *
 * <p>Sans lui, l'appel HTTP au fournisseur d'emails allongerait le temps de réponse de
 * l'endpoint appelant — et un fournisseur lent ferait attendre le visiteur qui vient
 * d'envoyer le formulaire de contact.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("raiki-async-");
        executor.initialize();
        return executor;
    }
}
