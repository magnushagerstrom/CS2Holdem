package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import com.robrodesign.cs2holdem.season.Season;
import com.robrodesign.cs2holdem.season.SeasonHand;
import com.robrodesign.cs2holdem.season.SeasonHandService;
import com.robrodesign.cs2holdem.season.SeasonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    private final SeasonService seasonService;
    private final SeasonHandService seasonHandService;
    private final PlayerRepository playerRepository;

    public SeasonController(SeasonService seasonService, SeasonHandService seasonHandService,
                            PlayerRepository playerRepository) {
        this.seasonService = seasonService;
        this.seasonHandService = seasonHandService;
        this.playerRepository = playerRepository;
    }

    // GET /api/seasons : alla säsonger, nyast först
    @GetMapping
    public List<SeasonView> all() {
        return seasonService.all().stream().map(SeasonView::from).toList();
    }

    // GET /api/seasons/current
    @GetMapping("/current")
    public SeasonView current() {
        return SeasonView.from(seasonService.current());
    }

    // GET /api/seasons/current/leaderboard
    @GetMapping("/current/leaderboard")
    public List<LeaderboardEntryView> currentLeaderboard() {
        return leaderboard(seasonService.current());
    }

    // GET /api/seasons/{seasonId}/leaderboard : även för avslutade säsonger (hall of fame)
    @GetMapping("/{seasonId}/leaderboard")
    public List<LeaderboardEntryView> leaderboard(@PathVariable("seasonId") long seasonId) {
        return leaderboard(seasonService.byId(seasonId));
    }

    private List<LeaderboardEntryView> leaderboard(Season season) {
        List<SeasonHand> hands = seasonHandService.leaderboard(season);
        Map<Long, Player> players = playerRepository.findAllById(hands.stream().map(SeasonHand::getSteamId64).toList())
                .stream().collect(Collectors.toMap(Player::getSteamId64, Function.identity()));

        List<LeaderboardEntryView> entries = new ArrayList<>();
        int rank = 1;
        for (SeasonHand hand : hands) {
            Player player = players.get(hand.getSteamId64());
            entries.add(new LeaderboardEntryView(
                    rank++,
                    String.valueOf(hand.getSteamId64()),
                    player == null ? null : player.getDisplayName(),
                    player == null ? null : player.getAvatarUrl(),
                    hand.getHandType().name(),
                    hand.getHandType().displayName(),
                    hand.getScore(),
                    hand.getTotalDamage(),
                    hand.isCustom()));
        }
        return entries;
    }
}
