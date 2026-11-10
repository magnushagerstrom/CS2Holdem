package com.robrodesign.cs2holdem.locator;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * The settings under "cs2holdem.locator" in application.yaml.
 *
 * @param enabled   whether the app logs in to Steam and locates demos by itself
 * @param username  the bot account's Steam login name (never your own account)
 * @param password  the bot account's password
 * @param tokenFile where the refresh token is kept after the first login, so the bot does not
 *                  need the password or a Steam Guard code every time it starts
 * @param clientVersion the CS2 client version we tell the Game Coordinator we run; it refuses
 *                      a hello without one
 */
@ConfigurationProperties(prefix = "cs2holdem.locator")
public record LocatorProperties(
        boolean enabled,
        String username,
        String password,
        Path tokenFile,
        int clientVersion
) {
    // Never print the password, not even by accident in a log.
    @Override
    public String toString() {
        return "LocatorProperties[enabled=" + enabled + ", username=" + username
                + ", password=***, tokenFile=" + tokenFile + "]";
    }
}
