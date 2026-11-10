package com.robrodesign.cs2holdem.pipeline;

import com.robrodesign.cs2holdem.detection.ParsedMatch;
import com.robrodesign.cs2holdem.match.Match;
import com.robrodesign.cs2holdem.match.MatchHistoryRepository;
import com.robrodesign.cs2holdem.match.MatchNotFoundException;
import com.robrodesign.cs2holdem.match.MatchRepository;
import com.robrodesign.cs2holdem.match.MatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * The demo pipeline, seen from the server:
 *
 * DISCOVERED --locate--> DEMO_LOCATED --parse--> PARSED
 *     |                                 ^
 *     +--(player uploads the demo)--> DOWNLOADED
 *
 * The heavy work (talking to Valve, reading 100 MB demo files) is done by a separate worker
 * program. It asks this service for jobs and reports back. The server never blocks on it.
 */
@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    // A worker that has not reported back within this time is assumed dead; the job is handed out again.
    static final Duration LOCATE_TIME_LIMIT = Duration.ofMinutes(5);
    static final Duration PARSE_TIME_LIMIT = Duration.ofMinutes(30);
    static final int MAX_JOBS = 10;

    private final PipelineMatchRepository pipelineRepository;
    private final MatchRepository matchRepository;
    private final MatchHistoryRepository historyRepository;
    private final MatchImportService importService;
    private final DemoStorage demoStorage;
    private final Clock clock;

    public PipelineService(PipelineMatchRepository pipelineRepository, MatchRepository matchRepository,
                           MatchHistoryRepository historyRepository, MatchImportService importService,
                           DemoStorage demoStorage, Clock clock) {
        this.pipelineRepository = pipelineRepository;
        this.matchRepository = matchRepository;
        this.historyRepository = historyRepository;
        this.importService = importService;
        this.demoStorage = demoStorage;
        this.clock = clock;
    }

    // ---- Jobs for the worker ----

    @Transactional
    public List<LocateJob> claimLocateJobs(int limit) {
        return claim(List.of(MatchStatus.DISCOVERED), LOCATE_TIME_LIMIT, limit).stream()
                .map(m -> new LocateJob(String.valueOf(m.getMatchId()), m.getShareCode(),
                        String.valueOf(m.getReservationId()), m.getTvPort() == null ? 0 : m.getTvPort()))
                .toList();
    }

    @Transactional
    public List<ParseJob> claimParseJobs(int limit) {
        return claim(List.of(MatchStatus.DEMO_LOCATED, MatchStatus.DOWNLOADED), PARSE_TIME_LIMIT, limit).stream()
                .map(m -> new ParseJob(String.valueOf(m.getMatchId()),
                        m.getStatus() == MatchStatus.DOWNLOADED ? null : m.getDemoUrl()))
                .toList();
    }

    private List<Match> claim(List<MatchStatus> statuses, Duration timeLimit, int limit) {
        int size = Math.clamp(limit, 1, MAX_JOBS);
        List<String> names = statuses.stream().map(Enum::name).toList();
        List<Match> matches = pipelineRepository.lockClaimable(names, clock.instant().minus(timeLimit), size);
        // The rows are locked, so setting claimed_at cannot collide with another worker.
        // Hibernate writes the change when the transaction commits.
        matches.forEach(m -> m.claim(clock.instant()));
        return matches;
    }

    // ---- Reports from the worker ----

    @Transactional
    public void demoLocated(long matchId, String demoUrl) {
        if (demoUrl == null || !demoUrl.startsWith("http")) {
            throw new DemoRejectedException("The demo URL must be an http(s) link");
        }
        find(matchId).markDemoLocated(demoUrl);
    }

    /** The worker has read the demo. Stores the cards and removes our copy of the demo. */
    @Transactional
    public ImportResult parsed(long matchId, ParsedMatch parsed) {
        if (parsed.matchId() != matchId) {
            throw new DemoRejectedException("The parsed match id does not match the URL");
        }
        Match match = find(matchId);
        String demoKey = match.getDemoKey();
        if (parsed.playedAt() == null) {
            // A demo file does not say when it was played. We found the match within minutes of it
            // ending (the sync runs often), so the discovery time is a good stand-in for the season.
            parsed = parsed.withPlayedAt(match.getCreatedAt());
        }
        ImportResult result = importService.importMatch(parsed);
        if (demoKey != null) {
            deleteQuietly(demoKey);
        }
        return result;
    }

    /**
     * The worker could not finish a job.
     *
     * @param expired true when Valve no longer has the demo (they keep them for a few weeks);
     *                retrying is pointless then.
     */
    @Transactional
    public void failed(long matchId, String error, boolean expired) {
        Match match = find(matchId);
        if (expired) {
            match.markExpired();
        } else {
            match.recordFailure(error);
        }
        log.warn("Match {} failed (expired={}): {}", matchId, expired, error);
    }

    /** Opens the uploaded demo of a match, for the worker to download. */
    @Transactional(readOnly = true)
    public InputStream openDemo(long matchId) throws IOException {
        String key = find(matchId).getDemoKey();
        if (key == null) {
            throw new MatchNotFoundException(matchId);
        }
        return demoStorage.open(key);
    }

    // ---- Uploads from players ----

    /**
     * A player uploads the demo of one of their matches themselves, for instance when Valve's copy
     * has expired. Only players who have the match in their history may do it.
     */
    @Transactional
    public void uploadDemo(long steamId64, long matchId, InputStream content) throws IOException {
        Match match = find(matchId);
        if (!historyRepository.existsBySteamId64AndMatchId(steamId64, matchId)) {
            throw new DemoRejectedException("This match is not in your match history");
        }
        if (match.getStatus() == MatchStatus.PARSED) {
            throw new DemoRejectedException("This match has already been read");
        }
        InputStream buffered = new BufferedInputStream(content);
        if (!DemoFormat.looksLikeDemo(buffered)) {
            throw new DemoRejectedException("The file is not a CS2 demo (.dem or .dem.bz2)");
        }
        String key = demoStorage.store(matchId, buffered);
        match.markDownloaded(key);
        log.info("Player {} uploaded the demo of match {}", steamId64, matchId);
    }

    private Match find(long matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    private void deleteQuietly(String key) {
        try {
            demoStorage.delete(key);
        } catch (IOException e) {
            log.warn("Could not delete demo {}: {}", key, e.getMessage());
        }
    }
}
