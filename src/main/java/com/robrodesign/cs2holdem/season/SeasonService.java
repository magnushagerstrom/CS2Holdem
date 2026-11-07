package com.robrodesign.cs2holdem.season;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SeasonService {

    private static final Logger log = LoggerFactory.getLogger(SeasonService.class);

    private final SeasonRepository seasonRepository;
    private final Clock clock;

    public SeasonService(SeasonRepository seasonRepository, Clock clock) {
        this.seasonRepository = seasonRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Season current() {
        return seasonRepository.findContaining(clock.instant())
                .orElseThrow(() -> new SeasonNotFoundException("No season is running right now"));
    }

    @Transactional(readOnly = true)
    public Optional<Season> containing(Instant moment) {
        return seasonRepository.findContaining(moment);
    }

    @Transactional(readOnly = true)
    public Season byId(long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new SeasonNotFoundException("No season with id " + id));
    }

    @Transactional(readOnly = true)
    public List<Season> all() {
        return seasonRepository.findAllByOrderByStartsAtDesc();
    }

    /** Runs every hour and locks seasons that have ended. Their hands can never change again. */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void lockEndedSeasons() {
        for (Season season : seasonRepository.findByLockedFalseAndEndsAtLessThanEqual(clock.instant())) {
            season.lock();
            log.info("Locked season {}", season.getName());
        }
    }
}
