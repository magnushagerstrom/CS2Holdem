package com.robrodesign.cs2holdem.privacy;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param unregisteredRetention how long we keep data about a player who has not registered,
 *                              counted from their latest match. Written like "60d" in the yaml.
 * @param cleanupEnabled        whether the nightly clean-up runs
 */
@Validated
@ConfigurationProperties(prefix = "cs2holdem.privacy")
public record PrivacyProperties(
        @NotNull Duration unregisteredRetention,
        boolean cleanupEnabled
) {
}
