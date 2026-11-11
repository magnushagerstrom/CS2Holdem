package com.robrodesign.cs2holdem.season;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * When the app has started: recalculate the hands that were scored with older rules.
 *
 * Usually there are none and this is one quick query. After a change to the scoring rules
 * (and a raised SeasonHand.CURRENT_SCORE_VERSION) every stored hand is recalculated once,
 * because the leaderboard sorts on the stored score and would otherwise mix two scales.
 */
@Component
public class ScoreVersionUpgrade {

    private static final Logger log = LoggerFactory.getLogger(ScoreVersionUpgrade.class);

    private final SeasonHandService seasonHandService;

    public ScoreVersionUpgrade(SeasonHandService seasonHandService) {
        this.seasonHandService = seasonHandService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void upgrade() {
        int count = seasonHandService.rescoreOutdated();
        if (count > 0) {
            log.info("Recalculated {} season hands with the scoring rules of version {}",
                    count, SeasonHand.CURRENT_SCORE_VERSION);
        }
    }
}
