package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.collection.CollectionService;
import com.robrodesign.cs2holdem.collection.PlayerCollection;
import com.robrodesign.cs2holdem.hand.Card;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.RoundKill;
import com.robrodesign.cs2holdem.season.Season;
import com.robrodesign.cs2holdem.season.SeasonHandService;
import com.robrodesign.cs2holdem.season.SeasonHandService.HandDetails;
import com.robrodesign.cs2holdem.season.SeasonHandService.HandItem;
import com.robrodesign.cs2holdem.season.SeasonService;
import com.robrodesign.cs2holdem.stats.LifetimeStatsView;
import com.robrodesign.cs2holdem.stats.PlayerStatsService;
import com.robrodesign.cs2holdem.steam.SteamLifetimeStatsClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A player's collection and season hands.
 *
 * Reading is public for registered players, so friends can look at each other's hands.
 * Players who never signed up have no profile here (404), see requirePublicProfile.
 * Changing a hand is only allowed for the logged-in player themselves (see SecurityConfig and CurrentPlayer).
 */
@RestController
@RequestMapping("/api/players/{steamId64}")
public class PlayerController {

    private final CollectionService collectionService;
    private final SeasonService seasonService;
    private final SeasonHandService seasonHandService;
    private final CurrentPlayer currentPlayer;
    private final PlayerStatsService statsService;
    private final SteamLifetimeStatsClient lifetimeClient;
    private final AppUserRepository appUserRepository;
    private final MapNames mapNames;

    public PlayerController(CollectionService collectionService, SeasonService seasonService,
                            SeasonHandService seasonHandService, CurrentPlayer currentPlayer,
                            PlayerStatsService statsService, SteamLifetimeStatsClient lifetimeClient,
                            AppUserRepository appUserRepository, MapNames mapNames) {
        this.collectionService = collectionService;
        this.seasonService = seasonService;
        this.seasonHandService = seasonHandService;
        this.currentPlayer = currentPlayer;
        this.statsService = statsService;
        this.lifetimeClient = lifetimeClient;
        this.appUserRepository = appUserRepository;
        this.mapNames = mapNames;
    }

    /**
     * Profiles are public only for registered players. You can always see your own, which matters
     * in the second between registering and the first sync.
     */
    private void requirePublicProfile(long steamId64) {
        boolean self = currentPlayer.steamId64().filter(id -> id == steamId64).isPresent();
        if (!self && !appUserRepository.existsById(steamId64)) {
            throw new ProfileNotPublicException();
        }
    }

    // GET /api/players/{id}/stats?seasonId=... : profilsidan, nuvarande säsong om inget anges
    @GetMapping("/stats")
    public PlayerStatsView stats(@PathVariable("steamId64") long steamId64,
                                 @RequestParam(name = "seasonId", required = false) Long seasonId) {
        requirePublicProfile(steamId64);
        Season season = seasonId == null ? seasonService.current() : seasonService.byId(seasonId);
        return PlayerStatsView.from(statsService.profile(steamId64, season));
    }

    // GET /api/players/{id}/lifetime : Steams livstidssiffror, om profilen är offentlig
    @GetMapping("/lifetime")
    public LifetimeStatsView lifetime(@PathVariable("steamId64") long steamId64) {
        requirePublicProfile(steamId64);
        return lifetimeClient.fetch(steamId64).map(LifetimeStatsView::from).orElseGet(LifetimeStatsView::unavailable);
    }

    // GET /api/players/{id}/collection?seasonId=... : alla kort och kills, nuvarande säsong om inget anges
    @GetMapping("/collection")
    public CollectionView collection(@PathVariable("steamId64") long steamId64,
                                     @RequestParam(name = "seasonId", required = false) Long seasonId) {
        requirePublicProfile(steamId64);
        Season season = seasonId == null ? seasonService.current() : seasonService.byId(seasonId);
        PlayerCollection collection = collectionService.load(steamId64, season.getStartsAt(), season.getEndsAt());

        List<Card> everything = new ArrayList<>(collection.events());
        everything.addAll(collection.roundKills());
        Map<Long, String> maps = mapNames.of(everything);
        List<CardView> cards = collection.events().stream()
                .map((Event event) -> CardView.from(collection.idOf(event), event, maps))
                .toList();
        List<KillView> kills = collection.roundKills().stream()
                .map((RoundKill kill) -> KillView.from(collection.idOf(kill), kill, maps))
                .toList();
        return new CollectionView(SeasonView.from(season), cards, kills);
    }

    // GET /api/players/{id}/hand : säsongshanden just nu
    @GetMapping("/hand")
    public ResponseEntity<HandView> currentHand(@PathVariable("steamId64") long steamId64) {
        requirePublicProfile(steamId64);
        return seasonHandService.currentHand(steamId64)
                .map(this::handView)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // GET /api/players/{id}/hands : alla säsongshänder någonsin, nyast först
    @GetMapping("/hands")
    public List<HandView> allHands(@PathVariable("steamId64") long steamId64) {
        requirePublicProfile(steamId64);
        return seasonHandService.allHands(steamId64).stream().map(this::handView).toList();
    }

    // PUT /api/players/{id}/hand : bygg en egen hand
    @PutMapping("/hand")
    public HandView buildHand(@PathVariable("steamId64") long steamId64,
                              @Valid @RequestBody BuildHandRequest request) {
        currentPlayer.requireSelf(steamId64);
        return handView(seasonHandService.buildCustom(steamId64, request.cardIdsAsLong(),
                request.killIdsAsLong()));
    }

    // DELETE /api/players/{id}/hand/custom : tillbaka till den starkaste handen
    @DeleteMapping("/hand/custom")
    public ResponseEntity<HandView> resetHand(@PathVariable("steamId64") long steamId64) {
        currentPlayer.requireSelf(steamId64);
        return seasonHandService.resetToAutomatic(steamId64)
                .map(this::handView)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** A hand with the map of every card, looked up in one query. */
    private HandView handView(HandDetails details) {
        List<Card> cards = details.items().stream().map(HandItem::card).toList();
        return HandView.from(details, mapNames.of(cards));
    }
}
