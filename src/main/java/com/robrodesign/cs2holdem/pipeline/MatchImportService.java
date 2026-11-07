package com.robrodesign.cs2holdem.pipeline;

import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.collection.StoredCard;
import com.robrodesign.cs2holdem.collection.StoredCardRepository;
import com.robrodesign.cs2holdem.collection.StoredKill;
import com.robrodesign.cs2holdem.collection.StoredKillRepository;
import com.robrodesign.cs2holdem.detection.DetectionResult;
import com.robrodesign.cs2holdem.detection.EventDetector;
import com.robrodesign.cs2holdem.detection.ParsedMatch;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.Kill;
import com.robrodesign.cs2holdem.hand.RoundKill;
import com.robrodesign.cs2holdem.match.Match;
import com.robrodesign.cs2holdem.match.MatchNotFoundException;
import com.robrodesign.cs2holdem.match.MatchRepository;
import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import com.robrodesign.cs2holdem.season.Season;
import com.robrodesign.cs2holdem.season.SeasonHandService;
import com.robrodesign.cs2holdem.season.SeasonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Takes what the demo parser found in a match and stores it as cards and kills,
 * then updates the season hands of every registered player who played in it.
 *
 * Importing the same match twice is safe: everything from the match is replaced.
 */
@Service
public class MatchImportService {

    private static final Logger log = LoggerFactory.getLogger(MatchImportService.class);

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final StoredCardRepository cardRepository;
    private final StoredKillRepository killRepository;
    private final SeasonService seasonService;
    private final SeasonHandService seasonHandService;

    public MatchImportService(MatchRepository matchRepository, PlayerRepository playerRepository,
                              AppUserRepository appUserRepository, StoredCardRepository cardRepository,
                              StoredKillRepository killRepository, SeasonService seasonService,
                              SeasonHandService seasonHandService) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.appUserRepository = appUserRepository;
        this.cardRepository = cardRepository;
        this.killRepository = killRepository;
        this.seasonService = seasonService;
        this.seasonHandService = seasonHandService;
    }

    @Transactional
    public ImportResult importMatch(ParsedMatch parsed) {
        long matchId = parsed.matchId();
        if (!matchRepository.existsById(matchId)) {
            throw new MatchNotFoundException(matchId);
        }

        // 1. Remove anything from an earlier import of this match. Kills first: they point at cards.
        killRepository.deleteForMatch(matchId);
        cardRepository.deleteForMatch(matchId);

        // 2. Store the match facts. Loaded after the deletes, which clear Hibernate's memory.
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        match.markParsed(parsed.mapName(), parsed.playedAt(), parsed.durationSeconds(),
                parsed.team1Score(), parsed.team2Score());

        // 3. Find every player's cards and store them
        int cards = 0;
        int kills = 0;
        Iterable<DetectionResult> results = EventDetector.detectAll(parsed);
        for (DetectionResult result : results) {
            ensurePlayer(result.steamId64());
            cards += result.events().size();
            kills += store(result);
        }

        // 4. Update the season hands of registered players
        Optional<Season> season = parsed.playedAt() == null
                ? Optional.empty() : seasonService.containing(parsed.playedAt());
        int players = 0;
        for (DetectionResult result : results) {
            players++;
            if (season.isPresent() && appUserRepository.existsById(result.steamId64())) {
                seasonHandService.refresh(result.steamId64(), season.get());
            }
        }

        log.info("Imported match {}: {} players, {} cards, {} kills", matchId, players, cards, kills);
        return new ImportResult(matchId, players, cards, kills);
    }

    /** Stores one player's cards and kills. Returns the number of kills stored. */
    private int store(DetectionResult result) {
        long steamId64 = result.steamId64();

        // The cards first, so the kills can point at them
        Map<Integer, Long> cardIdByRound = new HashMap<>();
        Map<Integer, Integer> eventKillsByRound = new HashMap<>();
        for (Event event : result.events()) {
            StoredCard card = cardRepository.save(new StoredCard(steamId64, event.matchId(), event.round(),
                    event.type(), event.stamps()));
            cardIdByRound.put(event.round(), card.getId());
            eventKillsByRound.put(event.round(), event.kills().size());
        }

        // An event is made of the first kills of its round, in tick order (see EventDetector)
        Map<Integer, Integer> usedByRound = new HashMap<>();
        for (RoundKill roundKill : result.roundKills()) {
            Long cardId = null;
            Integer eventKills = eventKillsByRound.get(roundKill.round());
            if (eventKills != null) {
                int used = usedByRound.merge(roundKill.round(), 1, Integer::sum);
                if (used <= eventKills) {
                    cardId = cardIdByRound.get(roundKill.round());
                }
            }
            Kill kill = roundKill.kill();
            killRepository.save(new StoredKill(steamId64, roundKill.matchId(), roundKill.round(), roundKill.tick(),
                    kill.weaponGroup(), kill.difficulty(), kill.damage(), kill.mapArea(), kill.stamps(), cardId));
        }
        return result.roundKills().size();
    }

    private void ensurePlayer(long steamId64) {
        if (!playerRepository.existsById(steamId64)) {
            playerRepository.save(new Player(steamId64, null));
        }
    }
}
