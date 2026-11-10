package com.robrodesign.cs2holdem.sync;

import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import com.robrodesign.cs2holdem.steam.SteamProfileClient;
import com.robrodesign.cs2holdem.steam.SteamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every minute: fetch the Steam name and avatar of up to 100 players we know too little about.
 *
 * New players (seen in a match for the first time) come first, so their names show up within a
 * minute. After that each player is checked again once a day, because people change their names.
 * One call covers 100 players, so this is a small part of Steam's daily call budget.
 *
 * Lives in the sync package because it uses both the player and the steam modules.
 */
@Component
public class PlayerProfileScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlayerProfileScheduler.class);
    private static final Duration CHECK_AGAIN_AFTER = Duration.ofDays(1);

    private final PlayerRepository playerRepository;
    private final SteamProfileClient profileClient;
    private final SyncProperties properties;
    private final Clock clock;

    public PlayerProfileScheduler(PlayerRepository playerRepository, SteamProfileClient profileClient,
                                  SyncProperties properties, Clock clock) {
        this.playerRepository = playerRepository;
        this.profileClient = profileClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT15S")
    @Transactional
    public void refreshProfiles() {
        if (!properties.enabled()) {
            return;
        }
        Instant now = clock.instant();
        List<Player> due = playerRepository.findProfilesToRefresh(
                now.minus(CHECK_AGAIN_AFTER), Limit.of(SteamProfileClient.MAX_PER_CALL));
        if (due.isEmpty()) {
            return;
        }

        List<SteamProfileClient.Profile> profiles;
        try {
            profiles = profileClient.fetch(due.stream().map(Player::getSteamId64).toList());
        } catch (SteamUnavailableException e) {
            log.warn("Could not refresh player names: {}", e.getMessage());
            return; // try again next minute
        }

        Map<Long, SteamProfileClient.Profile> bySteamId = profiles.stream()
                .collect(Collectors.toMap(SteamProfileClient.Profile::steamId64, Function.identity()));
        for (Player player : due) {
            SteamProfileClient.Profile profile = bySteamId.get(player.getSteamId64());
            // Mark players Steam did not know as checked too, or we would ask about them every minute
            player.updateProfile(profile == null ? null : profile.name(),
                    profile == null ? null : profile.avatarUrl(), now);
        }
        // No save() needed: the players were loaded in this transaction, so Hibernate writes the
        // changes when it commits ("dirty checking").
        log.info("Refreshed the Steam profiles of {} player(s)", due.size());
    }
}
