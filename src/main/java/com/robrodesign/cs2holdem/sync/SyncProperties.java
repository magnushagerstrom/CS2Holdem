package com.robrodesign.cs2holdem.sync;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param maxCodesPerRun  max antal nya share codes vi frågar Steam om per användare och körning
 * @param enabled         om den schemalagda synken ska köras
 * @param usersPerRun     max antal användare per körning
 * @param dailyCallBudget max antal anrop till Steam per dygn. Steam tillåter ungefär 100 000.
 */
@Validated
@ConfigurationProperties(prefix = "cs2holdem.sync")
public record SyncProperties(
        @Min(1) int maxCodesPerRun,
        boolean enabled,
        @Min(1) int usersPerRun,
        @Min(1) int dailyCallBudget
) {
}
