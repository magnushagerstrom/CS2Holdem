package com.robrodesign.cs2holdem.steam;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A player's lifetime CS2 numbers from Steam (GetUserStatsForGame).
 *
 * These are totals since the account was created, over every game mode, so they are shown as a
 * side note next to our own season statistics. Steam only answers for profiles whose game
 * details are public; for others the result is empty.
 *
 * Answers are kept for 12 hours, so opening a profile page again costs no Steam call.
 */
@Component
public class SteamLifetimeStatsClient {

    private static final int CS2_APP_ID = 730;
    private static final Duration KEEP = Duration.ofHours(12);

    private final RestClient restClient;
    private final SteamProperties properties;
    private final SteamCallBudget budget;
    private final Clock clock;
    // ConcurrentHashMap: several web requests may read and write it at the same time
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Instant fetchedAt, Optional<Map<String, Long>> stats) {
    }

    public SteamLifetimeStatsClient(RestClient.Builder builder, SteamProperties properties,
                                    SteamCallBudget budget, Clock clock) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.budget = budget;
        this.clock = clock;
    }

    /** @return Steam's counters by name, e.g. "total_kills", or empty if the profile is private */
    public Optional<Map<String, Long>> fetch(long steamId64) {
        Instant now = clock.instant();
        Cached cached = cache.get(steamId64);
        if (cached != null && cached.fetchedAt().plus(KEEP).isAfter(now)) {
            return cached.stats();
        }
        if (!budget.tryAcquire()) {
            return cached == null ? Optional.empty() : cached.stats(); // an old answer beats none
        }
        Optional<Map<String, Long>> stats;
        try {
            StatsResponse response = restClient.get()
                    .uri(uri -> uri
                            .path("/ISteamUserStats/GetUserStatsForGame/v2/")
                            .queryParam("key", properties.apiKey())
                            .queryParam("steamid", steamId64)
                            .queryParam("appid", CS2_APP_ID)
                            .build())
                    .retrieve()
                    .body(StatsResponse.class);
            stats = Optional.ofNullable(response)
                    .map(StatsResponse::playerstats)
                    .map(StatsResponse.PlayerStats::stats)
                    .map(list -> list.stream().collect(Collectors.toMap(
                            StatsResponse.Stat::name, StatsResponse.Stat::value, (a, b) -> a)));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Steam answers 403 or 500 for private profiles
            stats = Optional.empty();
        } catch (RestClientException e) {
            return Optional.empty(); // network trouble: do not remember this, try again next time
        }
        cache.put(steamId64, new Cached(now, stats));
        return stats;
    }

    // Mirrors Steam's JSON: { "playerstats": { "stats": [ { "name": "total_kills", "value": 1234 } ] } }
    record StatsResponse(PlayerStats playerstats) {
        record PlayerStats(List<Stat> stats) {
        }

        record Stat(String name, long value) {
        }
    }
}
