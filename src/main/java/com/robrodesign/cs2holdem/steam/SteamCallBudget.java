package com.robrodesign.cs2holdem.steam;

import com.robrodesign.cs2holdem.sync.SyncProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Counts calls to the Steam Web API per day (UTC) and refuses new calls once the daily budget is
 * used up. Steam allows roughly 100 000 calls a day per API key; going over gets the key blocked.
 *
 * The count lives in memory, so it restarts when the app restarts, and every running copy of the
 * app counts on its own. With several copies on AWS each should get its share of the budget.
 */
@Component
public class SteamCallBudget {

    private final int dailyBudget;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public SteamCallBudget(SyncProperties properties, Clock clock) {
        this.dailyBudget = properties.dailyCallBudget();
        this.clock = clock;
    }

    /** @return true if the call may be made, and counts it */
    public synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        if (used >= dailyBudget) {
            return false;
        }
        used++;
        return true;
    }

    public synchronized int remainingToday() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return today.equals(day) ? Math.max(0, dailyBudget - used) : dailyBudget;
    }
}
