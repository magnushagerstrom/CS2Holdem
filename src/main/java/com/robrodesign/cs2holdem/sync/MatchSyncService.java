package com.robrodesign.cs2holdem.sync;

import com.robrodesign.cs2holdem.account.AccountNotFoundException;
import com.robrodesign.cs2holdem.account.AppUser;
import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.match.Match;
import com.robrodesign.cs2holdem.match.MatchHistoryEntry;
import com.robrodesign.cs2holdem.match.MatchHistoryRepository;
import com.robrodesign.cs2holdem.match.MatchRepository;
import com.robrodesign.cs2holdem.steam.ShareCode;
import com.robrodesign.cs2holdem.steam.SteamMatchHistoryClient;
import com.robrodesign.cs2holdem.steam.SteamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Hämtar nya matcher för en användare från Steam och sparar dem.
 *
 * Ligger i ett eget paket (sync) eftersom den använder flera andra moduler: account, match och steam.
 * Då beror account och match inte på varandra, och vi slipper beroenden i cirkel.
 */
@Service
public class MatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(MatchSyncService.class);

    private static final Duration AFTER_NEW_MATCHES = Duration.ofMinutes(15);
    private static final Duration AFTER_NO_NEW_MATCHES = Duration.ofHours(1);
    private static final Duration MORE_MATCHES_WAITING = Duration.ofMinutes(1);

    // Steam answers 429 Too Many Requests if we ask for share codes back-to-back.
    // A short pause between the calls in one sync keeps us under that limit.
    private static final Duration PAUSE_BETWEEN_CALLS = Duration.ofSeconds(2);

    private final AppUserRepository appUserRepository;
    private final MatchRepository matchRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final SteamMatchHistoryClient steamClient;
    private final SyncProperties properties;

    public MatchSyncService(AppUserRepository appUserRepository,
                            MatchRepository matchRepository,
                            MatchHistoryRepository matchHistoryRepository,
                            SteamMatchHistoryClient steamClient,
                            SyncProperties properties) {
        this.appUserRepository = appUserRepository;
        this.matchRepository = matchRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.steamClient = steamClient;
        this.properties = properties;
    }

    /** @return antal matcher som var nya i användarens historik */
    public int syncUser(long steamId64) {
        AppUser user = appUserRepository.findById(steamId64)
                .orElseThrow(() -> new AccountNotFoundException(steamId64));

        String current = user.getLastKnownShareCode();
        int newMatches = recordInHistory(steamId64, ShareCode.parse(current)) ? 1 : 0;

        // Följ kedjan av share codes framåt i tiden, men max N steg per körning.
        // Det skyddar Steams anropsgräns om en användare har hundratals gamla matcher.
        boolean reachedLatest = false;
        for (int i = 0; i < properties.maxCodesPerRun(); i++) {
            if (i > 0) {
                pause();
            }
            Optional<String> next;
            try {
                next = steamClient.findNextShareCode(steamId64, user.getAuthCode(), current);
            } catch (SteamUnavailableException e) {
                // Keep the codes we already got. Without this the next sync would start from
                // the old code and ask Steam the same questions again.
                if (!current.equals(user.getLastKnownShareCode())) {
                    user.recordNewShareCode(current);
                    appUserRepository.save(user);
                    log.info("Synced player {} partly: {} new match(es) before Steam stopped answering",
                            steamId64, newMatches);
                }
                throw e;
            }
            if (next.isEmpty()) {
                reachedLatest = true;
                break;
            }
            current = next.get();
            if (recordInHistory(steamId64, ShareCode.parse(current))) {
                newMatches++;
            }
        }

        // Bestäm när det är dags nästa gång. Aktiva spelare kollas oftare.
        Duration untilNextSync;
        if (!reachedLatest) {
            untilNextSync = MORE_MATCHES_WAITING;
        } else if (newMatches > 0) {
            untilNextSync = AFTER_NEW_MATCHES;
        } else {
            untilNextSync = AFTER_NO_NEW_MATCHES;
        }

        user.recordNewShareCode(current);
        user.markSynced(Instant.now(), untilNextSync);
        appUserRepository.save(user);

        // Logga SteamID och antal, men ALDRIG auth code.
        log.info("Synced player {}: {} new match(es), next sync in {}", steamId64, newMatches, untilNextSync);
        return newMatches;
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_BETWEEN_CALLS);
        } catch (InterruptedException e) {
            // Someone wants this thread to stop (for example the app is shutting down).
            // Restore the flag so the caller can see it, and stop syncing.
            Thread.currentThread().interrupt();
            throw new SteamUnavailableException("Sync interrupted", e);
        }
    }

    /** Skjuter upp nästa synk, t.ex. när Steam inte svarar. */
    public void postpone(long steamId64, Duration delay) {
        appUserRepository.findById(steamId64).ifPresent(user -> {
            user.markSynced(Instant.now(), delay);
            appUserRepository.save(user);
        });
    }

    /** @return true om matchen var ny i just den här användarens historik */
    private boolean recordInHistory(long steamId64, ShareCode code) {
        ensureMatchExists(code);
        if (matchHistoryRepository.existsBySteamId64AndMatchId(steamId64, code.matchId())) {
            return false;
        }
        matchHistoryRepository.save(new MatchHistoryEntry(steamId64, code.matchId()));
        return true;
    }

    private void ensureMatchExists(ShareCode code) {
        if (matchRepository.existsById(code.matchId())) {
            return;
        }
        try {
            matchRepository.save(new Match(code.matchId(), code.value(), code.reservationId(), code.tvPort()));
        } catch (DataIntegrityViolationException e) {
            // Två lagkamrater synkades samtidigt och båda försökte spara samma match.
            // Den andra hann före, och databasens PRIMARY KEY stoppade dubbletten. Matchen finns nu,
            // vilket är precis vad vi ville, så vi går vidare.
            log.debug("Match {} was created concurrently by another sync", code.matchId());
        }
    }
}
