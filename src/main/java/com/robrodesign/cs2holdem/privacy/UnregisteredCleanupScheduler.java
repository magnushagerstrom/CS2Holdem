package com.robrodesign.cs2holdem.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Every night: forget players who never registered and have not been seen for a while
 * (60 days by default, see PrivacyProperties).
 *
 * Why: we store everyone in a match so that a friend who registers later gets their old cards.
 * But a stranger who happened to be in one match should not stay in our database forever.
 *
 * Deletes in batches of 500, each in its own transaction, so a big clean-up never locks the
 * tables for long. The loop stops when a batch comes back smaller than 500: then there is nothing left.
 */
@Component
public class UnregisteredCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(UnregisteredCleanupScheduler.class);
    private static final int BATCH_SIZE = 500;

    private final PrivacyService privacyService;
    private final PrivacyProperties properties;
    private final Clock clock;

    public UnregisteredCleanupScheduler(PrivacyService privacyService, PrivacyProperties properties, Clock clock) {
        this.privacyService = privacyService;
        this.properties = properties;
        this.clock = clock;
    }

    // Cron: second minute hour day month weekday. 04:17 every night, when nobody is playing.
    @Scheduled(cron = "0 17 4 * * *")
    public void deleteExpired() {
        if (!properties.cleanupEnabled()) {
            return;
        }
        Instant cutoff = clock.instant().minus(properties.unregisteredRetention());
        int total = 0;
        int deleted;
        do {
            deleted = privacyService.deleteExpiredUnregistered(cutoff, BATCH_SIZE);
            total += deleted;
        } while (deleted == BATCH_SIZE);
        if (total > 0) {
            log.info("Deleted {} unregistered player(s) not seen since {}", total, cutoff);
        }
    }
}
