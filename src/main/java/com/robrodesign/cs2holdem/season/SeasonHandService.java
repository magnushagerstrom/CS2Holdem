package com.robrodesign.cs2holdem.season;

import com.robrodesign.cs2holdem.collection.CollectionService;
import com.robrodesign.cs2holdem.collection.PlayerCollection;
import com.robrodesign.cs2holdem.hand.BestHandFinder;
import com.robrodesign.cs2holdem.hand.Card;
import com.robrodesign.cs2holdem.hand.Event;
import com.robrodesign.cs2holdem.hand.Hand;
import com.robrodesign.cs2holdem.hand.HandEvaluator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Keeps every player's season hand up to date.
 *
 * - After new cards arrive, the strongest hand is recalculated, unless the player built their own.
 * - A player can build their own hand from their cards, or go back to the automatic one.
 * - Locked seasons never change.
 */
@Service
public class SeasonHandService {

    public static final int LEADERBOARD_SIZE = 100;

    private final SeasonHandRepository handRepository;
    private final SeasonService seasonService;
    private final CollectionService collectionService;

    public SeasonHandService(SeasonHandRepository handRepository, SeasonService seasonService,
                             CollectionService collectionService) {
        this.handRepository = handRepository;
        this.seasonService = seasonService;
        this.collectionService = collectionService;
    }

    /** A season hand together with the cards it is made of. */
    public record HandDetails(SeasonHand stored, Season season, Hand hand, List<HandItem> items) {
    }

    /** One card or kill in a hand, with its database id. */
    public record HandItem(long id, Card card) {
    }

    // ---------------------------------------------------------------- automatic hand

    /** Recalculates the automatic hand. Called after new cards have been imported. */
    @Transactional
    public void refresh(long steamId64, Season season) {
        if (season.isLocked()) {
            return;
        }
        SeasonHand stored = handRepository.findBySteamId64AndSeasonId(steamId64, season.getId())
                .orElse(null);
        PlayerCollection collection = load(steamId64, season);

        if (stored != null && stored.isCustom() && toHand(stored, collection).isPresent()) {
            return; // the player's own hand is still valid, so it stays
        }

        Optional<Hand> best = BestHandFinder.find(collection.events(), collection.roundKills());
        if (best.isEmpty()) {
            if (stored != null) {
                handRepository.delete(stored);
            }
            return;
        }
        if (stored == null) {
            stored = new SeasonHand(steamId64, season.getId());
        }
        save(stored, best.get(), false, collection);
    }

    // ---------------------------------------------------------------- custom hand

    /** Builds the player's own hand for the current season from card ids and kill ids. */
    @Transactional
    public HandDetails buildCustom(long steamId64, List<Long> cardIds, List<Long> killIds) {
        Season season = seasonService.current();
        if (season.isLocked()) {
            throw new SeasonLockedException(season.getName());
        }
        PlayerCollection collection = load(steamId64, season);

        List<Card> cards = new ArrayList<>();
        for (long id : distinct(cardIds)) {
            cards.add(collection.event(id)
                    .orElseThrow(() -> new InvalidHandException("Card " + id + " is not in your collection this season")));
        }
        for (long id : distinct(killIds)) {
            cards.add(collection.roundKill(id)
                    .orElseThrow(() -> new InvalidHandException("Kill " + id + " is not in your collection this season")));
        }
        Hand hand = HandEvaluator.evaluate(cards)
                .orElseThrow(() -> new InvalidHandException("These cards do not form a valid hand"));

        SeasonHand stored = handRepository.findBySteamId64AndSeasonId(steamId64, season.getId())
                .orElseGet(() -> new SeasonHand(steamId64, season.getId()));
        save(stored, hand, true, collection);
        return details(stored, season, collection).orElseThrow();
    }

    /** Drops the player's own hand and goes back to the strongest one. */
    @Transactional
    public Optional<HandDetails> resetToAutomatic(long steamId64) {
        Season season = seasonService.current();
        if (season.isLocked()) {
            throw new SeasonLockedException(season.getName());
        }
        handRepository.findBySteamId64AndSeasonId(steamId64, season.getId())
                .ifPresent(hand -> hand.replace(hand.getHandType(), hand.getScore(), hand.getTotalDamage(),
                        false, List.of()));
        handRepository.flush();
        refresh(steamId64, season);
        return currentHand(steamId64);
    }

    // ---------------------------------------------------------------- new scoring rules

    /**
     * Recalculates every hand that was scored with older rules (see SeasonHand.CURRENT_SCORE_VERSION),
     * so old and new hands compare fairly on the leaderboard. A player's own hand stays their own;
     * only its score changes. Locked seasons are history and keep their scores.
     *
     * @return how many hands were recalculated
     */
    @Transactional
    public int rescoreOutdated() {
        int count = 0;
        for (SeasonHand stored : handRepository.findByScoreVersionLessThan(SeasonHand.CURRENT_SCORE_VERSION)) {
            Season season = seasonService.byId(stored.getSeasonId());
            if (season.isLocked()) {
                continue;
            }
            PlayerCollection collection = load(stored.getSteamId64(), season);
            Optional<Hand> hand = toHand(stored, collection);
            if (stored.isCustom() && hand.isPresent()) {
                save(stored, hand.get(), true, collection); // their own choice: same cards, new score
            } else {
                // Automatic: with new rules another combination may now be the strongest
                refresh(stored.getSteamId64(), season);
            }
            count++;
        }
        return count;
    }

    /**
     * Works out the hand for every registered player who has none this season. A player can have
     * cards without a hand when their matches were read before they signed up, since hands are
     * otherwise only worked out when a new match arrives.
     *
     * @return how many players got a hand
     */
    @Transactional
    public int fillMissingHands() {
        Season season = seasonService.current();
        if (season.isLocked()) {
            return 0;
        }
        int count = 0;
        for (long steamId64 : handRepository.findRegisteredWithoutHand(season.getId())) {
            refresh(steamId64, season);
            if (handRepository.findBySteamId64AndSeasonId(steamId64, season.getId()).isPresent()) {
                count++;
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public Optional<HandDetails> currentHand(long steamId64) {
        return hand(steamId64, seasonService.current());
    }

    @Transactional(readOnly = true)
    public Optional<HandDetails> hand(long steamId64, Season season) {
        return handRepository.findBySteamId64AndSeasonId(steamId64, season.getId())
                .flatMap(stored -> details(stored, season, load(steamId64, season)));
    }

    /** Every season hand the player has ever had, newest season first. */
    @Transactional(readOnly = true)
    public List<HandDetails> allHands(long steamId64) {
        List<HandDetails> result = new ArrayList<>();
        for (SeasonHand stored : handRepository.findBySteamId64OrderBySeasonIdDesc(steamId64)) {
            Season season = seasonService.byId(stored.getSeasonId());
            details(stored, season, load(steamId64, season)).ifPresent(result::add);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<SeasonHand> leaderboard(Season season) {
        return handRepository.findLeaderboard(season.getId(), PageRequest.of(0, LEADERBOARD_SIZE));
    }

    // ---------------------------------------------------------------- helpers

    private PlayerCollection load(long steamId64, Season season) {
        return collectionService.load(steamId64, season.getStartsAt(), season.getEndsAt());
    }

    private void save(SeasonHand stored, Hand hand, boolean custom, PlayerCollection collection) {
        List<SeasonHand.ItemRef> refs = new ArrayList<>();
        for (Card card : hand.cards()) {
            long id = collection.idOf(card);
            refs.add(card instanceof Event ? SeasonHand.ItemRef.card(id) : SeasonHand.ItemRef.kill(id));
        }
        stored.replace(hand.type(), hand.score(), hand.totalDamage(), custom, refs);
        handRepository.save(stored);
    }

    /** Rebuilds the hand from the stored items. Empty if a card or kill no longer exists. */
    private Optional<Hand> toHand(SeasonHand stored, PlayerCollection collection) {
        return toItems(stored, collection).flatMap(items ->
                HandEvaluator.evaluate(items.stream().map(HandItem::card).toList()));
    }

    private Optional<List<HandItem>> toItems(SeasonHand stored, PlayerCollection collection) {
        List<HandItem> items = new ArrayList<>();
        for (SeasonHandItem item : stored.getItems()) {
            Optional<? extends Card> card = item.getCardId() != null
                    ? collection.event(item.getCardId())
                    : collection.roundKill(item.getKillId());
            if (card.isEmpty()) {
                return Optional.empty();
            }
            long id = item.getCardId() != null ? item.getCardId() : item.getKillId();
            items.add(new HandItem(id, card.get()));
        }
        return items.isEmpty() ? Optional.empty() : Optional.of(items);
    }

    private Optional<HandDetails> details(SeasonHand stored, Season season, PlayerCollection collection) {
        return toItems(stored, collection).flatMap(items ->
                HandEvaluator.evaluate(items.stream().map(HandItem::card).toList())
                        .map(hand -> new HandDetails(stored, season, hand, items)));
    }

    private static Set<Long> distinct(List<Long> ids) {
        return ids == null ? Set.of() : new LinkedHashSet<>(ids);
    }
}
