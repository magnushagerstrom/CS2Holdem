package com.robrodesign.cs2holdem.sync;

import com.robrodesign.cs2holdem.account.AppUser;
import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.steam.InvalidShareCodeException;
import com.robrodesign.cs2holdem.steam.InvalidSteamCredentialsException;
import com.robrodesign.cs2holdem.steam.SteamCallBudget;
import com.robrodesign.cs2holdem.steam.SteamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Every minute: sync the users whose turn it is (next_sync_at has passed), oldest first.
 *
 * Each user decides their own pace through next_sync_at (see MatchSyncService), so active players
 * are checked often and inactive ones rarely. That is what keeps thousands of users within
 * Steam's daily limit. The SteamCallBudget is the safety net on top.
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);
    private static final Duration AFTER_STEAM_ERROR = Duration.ofMinutes(30);
    private static final Duration AFTER_BAD_CREDENTIALS = Duration.ofDays(1);

    private final AppUserRepository appUserRepository;
    private final MatchSyncService syncService;
    private final SteamCallBudget budget;
    private final SyncProperties properties;
    private final Clock clock;

    public SyncScheduler(AppUserRepository appUserRepository, MatchSyncService syncService,
                         SteamCallBudget budget, SyncProperties properties, Clock clock) {
        this.appUserRepository = appUserRepository;
        this.syncService = syncService;
        this.budget = budget;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void syncDueUsers() {
        if (!properties.enabled()) {
            return;
        }
        List<AppUser> due = appUserRepository.findTop100ByNextSyncAtBeforeOrderByNextSyncAtAsc(clock.instant());
        int synced = 0;
        for (AppUser user : due) {
            if (synced >= properties.usersPerRun() || budget.remainingToday() <= properties.maxCodesPerRun()) {
                break;
            }
            long steamId64 = user.getSteamId64();
            try {
                syncService.syncUser(steamId64);
                synced++;
            } catch (InvalidSteamCredentialsException | InvalidShareCodeException e) {
                // The user changed or revoked their auth code. Try again tomorrow.
                log.warn("Sync of player {} rejected by Steam: {}", steamId64, e.getMessage());
                syncService.postpone(steamId64, AFTER_BAD_CREDENTIALS);
            } catch (SteamUnavailableException e) {
                log.warn("Steam unavailable while syncing player {}: {}", steamId64, e.getMessage());
                syncService.postpone(steamId64, AFTER_STEAM_ERROR);
                break; // Steam is down or the budget is used up; no point trying the others now
            } catch (RuntimeException e) {
                log.error("Unexpected error while syncing player {}", steamId64, e);
                syncService.postpone(steamId64, AFTER_STEAM_ERROR);
            }
        }
        if (synced > 0) {
            log.info("Scheduled sync: {} player(s), {} Steam calls left today", synced, budget.remainingToday());
        }
    }
}
