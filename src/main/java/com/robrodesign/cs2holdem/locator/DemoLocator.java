package com.robrodesign.cs2holdem.locator;

import com.robrodesign.cs2holdem.pipeline.LocateJob;
import com.robrodesign.cs2holdem.pipeline.PipelineService;
import com.robrodesign.cs2holdem.steam.ShareCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Every ten seconds: take one discovered match and ask the Game Coordinator where its demo is.
 *
 * DISCOVERED --(this class)--> DEMO_LOCATED --(the Go worker downloads and reads it)--> PARSED
 *
 * One match at a time, with a pause in between, because the GC limits how often a client may ask.
 * A bot that asks too fast gets ignored for a while, just like the Steam Web API's 429.
 *
 * The matches are claimed through the same queue as the external worker uses
 * (FOR UPDATE SKIP LOCKED), so both can run at the same time without doing the same work twice.
 */
@Component
@ConditionalOnProperty(prefix = "cs2holdem.locator", name = "enabled", havingValue = "true")
public class DemoLocator {

    private static final Logger log = LoggerFactory.getLogger(DemoLocator.class);

    private final GameCoordinatorClient gameCoordinator;
    private final PipelineService pipeline;

    public DemoLocator(GameCoordinatorClient gameCoordinator, PipelineService pipeline) {
        this.gameCoordinator = gameCoordinator;
        this.pipeline = pipeline;
    }

    @Scheduled(fixedDelayString = "PT10S", initialDelayString = "PT20S")
    public void locateNext() {
        // Do not claim a match we cannot work on. Claiming would only make it wait 5 minutes.
        if (!gameCoordinator.isReady()) {
            return;
        }
        List<LocateJob> jobs = pipeline.claimLocateJobs(1);
        if (jobs.isEmpty()) {
            return;
        }
        locate(jobs.getFirst());
    }

    private void locate(LocateJob job) {
        long matchId = Long.parseLong(job.matchId());
        // Read the three numbers from the share code itself, not from the job's separate fields.
        // Matches saved by an early version of the app have no reservation id in the database
        // (it is null there), but every match has its share code, and the numbers are in it.
        ShareCode code;
        try {
            code = ShareCode.parse(job.shareCode());
        } catch (RuntimeException e) {
            pipeline.failed(matchId, "Unreadable share code: " + e.getMessage(), true);
            return;
        }
        try {
            Optional<String> demoUrl = gameCoordinator.findDemoUrl(
                    code.matchId(), code.reservationId(), code.tvPort());
            if (demoUrl.isPresent()) {
                pipeline.demoLocated(matchId, demoUrl.get());
                log.info("Found the demo of match {}", matchId);
            } else {
                // Retrying is pointless; the player can still upload the demo themselves.
                pipeline.failed(matchId, "Valve no longer has the demo of this match", true);
            }
        } catch (TimeoutException e) {
            // Counts as one failed attempt; the match is tried again later.
            pipeline.failed(matchId, "No answer from the Game Coordinator: " + e.getMessage(), false);
        }
    }
}
