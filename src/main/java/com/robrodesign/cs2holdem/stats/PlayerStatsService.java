package com.robrodesign.cs2holdem.stats;

import com.robrodesign.cs2holdem.collection.CollectionService;
import com.robrodesign.cs2holdem.collection.PlayerCollection;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.EventType;
import com.robrodesign.cs2holdem.hand.Stamp;
import com.robrodesign.cs2holdem.match.MatchPlayerStatsRepository;
import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import com.robrodesign.cs2holdem.season.Season;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds a player's profile: season statistics, card counts and favourite weapon types. */
@Service
public class PlayerStatsService {

    private final MatchPlayerStatsRepository statsRepository;
    private final PlayerRepository playerRepository;
    private final CollectionService collectionService;

    public PlayerStatsService(MatchPlayerStatsRepository statsRepository, PlayerRepository playerRepository,
                              CollectionService collectionService) {
        this.statsRepository = statsRepository;
        this.playerRepository = playerRepository;
        this.collectionService = collectionService;
    }

    @Transactional(readOnly = true)
    public PlayerProfile profile(long steamId64, Season season) {
        Player player = playerRepository.findById(steamId64).orElse(null);
        SeasonStats stats = SeasonStats.from(
                statsRepository.totals(steamId64, season.getStartsAt(), season.getEndsAt()));
        PlayerCollection collection = collectionService.load(steamId64, season.getStartsAt(), season.getEndsAt());

        // Card types from the strongest (ace) down, and only those the player actually has
        Map<EventType, Long> byType = collection.events().stream()
                .collect(Collectors.groupingBy(Event::type, Collectors.counting()));
        List<PlayerProfile.Count> cards = new ArrayList<>();
        for (int i = EventType.values().length - 1; i >= 0; i--) {
            EventType type = EventType.values()[i];
            if (byType.containsKey(type)) {
                cards.add(new PlayerProfile.Count(type.displayName(), byType.get(type)));
            }
        }
        long clutches = collection.events().stream().filter(e -> e.stamps().contains(Stamp.CLUTCH)).count();

        List<PlayerProfile.Count> weapons = collection.roundKills().stream()
                .collect(Collectors.groupingBy(k -> k.kill().weaponGroup().displayName(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PlayerProfile.Count(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(PlayerProfile.Count::count).reversed())
                .toList();

        return new PlayerProfile(steamId64,
                player == null ? null : player.getDisplayName(),
                player == null ? null : player.getAvatarUrl(),
                season, stats, cards, clutches, weapons);
    }
}
