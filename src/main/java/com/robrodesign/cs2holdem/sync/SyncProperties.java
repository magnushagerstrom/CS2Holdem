package com.robrodesign.cs2holdem.sync;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cs2holdem.sync")
public record SyncProperties(@Min(1) int maxCodesPerRun) {
}
