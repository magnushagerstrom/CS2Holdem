package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.collection.CollectionService;
import com.robrodesign.cs2holdem.collection.PlayerCollection;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.RoundKill;
import com.robrodesign.cs2holdem.season.Season;
import com.robrodesign.cs2holdem.season.SeasonHandService;
import com.robrodesign.cs2holdem.season.SeasonService;
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

import java.util.List;

/**
 * A player's collection and season hands.
 *
 * Reading is public, so friends can look at each other's hands. Changing a hand is only allowed
 * for the logged-in player themselves (see SecurityConfig and CurrentPlayer).
 */
@RestController
@RequestMapping("/api/players/{steamId64}")
public class PlayerController {

    private final CollectionService collectionService;
    private final SeasonService seasonService;
    private final SeasonHandService seasonHandService;
    private final CurrentPlayer currentPlayer;

    public PlayerController(CollectionService collectionService, SeasonService seasonService,
                            SeasonHandService seasonHandService, CurrentPlayer currentPlayer) {
        this.collectionService = collectionService;
        this.seasonService = seasonService;
        this.seasonHandService = seasonHandService;
        this.currentPlayer = currentPlayer;
    }

    // GET /api/players/{id}/collection?seasonId=... : alla kort och kills, nuvarande säsong om inget anges
    @GetMapping("/collection")
    public CollectionView collection(@PathVariable("steamId64") long steamId64,
                                     @RequestParam(name = "seasonId", required = false) Long seasonId) {
        Season season = seasonId == null ? seasonService.current() : seasonService.byId(seasonId);
        PlayerCollection collection = collectionService.load(steamId64, season.getStartsAt(), season.getEndsAt());

        List<CardView> cards = collection.events().stream()
                .map((Event event) -> CardView.from(collection.idOf(event), event))
                .toList();
        List<KillView> kills = collection.roundKills().stream()
                .map((RoundKill kill) -> KillView.from(collection.idOf(kill), kill))
                .toList();
        return new CollectionView(SeasonView.from(season), cards, kills);
    }

    // GET /api/players/{id}/hand : säsongshanden just nu
    @GetMapping("/hand")
    public ResponseEntity<HandView> currentHand(@PathVariable("steamId64") long steamId64) {
        return seasonHandService.currentHand(steamId64)
                .map(HandView::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // GET /api/players/{id}/hands : alla säsongshänder någonsin, nyast först
    @GetMapping("/hands")
    public List<HandView> allHands(@PathVariable("steamId64") long steamId64) {
        return seasonHandService.allHands(steamId64).stream().map(HandView::from).toList();
    }

    // PUT /api/players/{id}/hand : bygg en egen hand
    @PutMapping("/hand")
    public HandView buildHand(@PathVariable("steamId64") long steamId64,
                              @Valid @RequestBody BuildHandRequest request) {
        currentPlayer.requireSelf(steamId64);
        return HandView.from(seasonHandService.buildCustom(steamId64, request.cardIdsAsLong(),
                request.killIdsAsLong()));
    }

    // DELETE /api/players/{id}/hand/custom : tillbaka till den starkaste handen
    @DeleteMapping("/hand/custom")
    public ResponseEntity<HandView> resetHand(@PathVariable("steamId64") long steamId64) {
        currentPlayer.requireSelf(steamId64);
        return seasonHandService.resetToAutomatic(steamId64)
                .map(HandView::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
