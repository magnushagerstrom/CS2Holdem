package com.robrodesign.cs2holdem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Application-wide beans.
 *
 * - @EnableScheduling turns on @Scheduled methods (season locking, sync, the demo pipeline).
 * - A Clock bean instead of Instant.now() everywhere: tests can then use a fixed clock.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
