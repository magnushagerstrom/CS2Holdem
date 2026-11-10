package com.robrodesign.cs2holdem.steam;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fetches players' public Steam profiles: their current name (persona name) and avatar.
 *
 * GetPlayerSummaries takes up to 100 SteamIDs in one call, so a whole match's players, or a
 * hundred players at a time, cost one call from the daily budget.
 */
@Component
public class SteamProfileClient {

    public static final int MAX_PER_CALL = 100;

    private final RestClient restClient;
    private final SteamProperties properties;
    private final SteamCallBudget budget;

    public SteamProfileClient(RestClient.Builder builder, SteamProperties properties, SteamCallBudget budget) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.budget = budget;
    }

    /** What Steam tells us about one player. avatarUrl is the 64 x 64 pixel picture. */
    public record Profile(long steamId64, String name, String avatarUrl) {
    }

    /**
     * @return the profiles Steam knows. Players missing from the answer (deleted accounts) are
     *         simply not in the list.
     */
    public List<Profile> fetch(Collection<Long> steamIds) {
        if (steamIds.isEmpty()) {
            return List.of();
        }
        if (steamIds.size() > MAX_PER_CALL) {
            throw new IllegalArgumentException("At most " + MAX_PER_CALL + " players per call");
        }
        if (!budget.tryAcquire()) {
            throw new SteamUnavailableException("Daily Steam API budget is used up", null);
        }
        String ids = steamIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        SummariesResponse response;
        try {
            response = restClient.get()
                    .uri(uri -> uri
                            .path("/ISteamUser/GetPlayerSummaries/v2/")
                            .queryParam("key", properties.apiKey())
                            .queryParam("steamids", ids)
                            .build())
                    .retrieve()
                    .body(SummariesResponse.class);
        } catch (RestClientException e) {
            throw new SteamUnavailableException("Could not fetch player profiles", e);
        }
        if (response == null || response.response() == null || response.response().players() == null) {
            return List.of();
        }
        return response.response().players().stream()
                .map(p -> new Profile(Long.parseLong(p.steamid()), p.personaname(), p.avatarmedium()))
                .toList();
    }

    // Mirrors Steam's JSON: { "response": { "players": [ { "steamid": "...", "personaname": "...", ... } ] } }
    record SummariesResponse(Players response) {
        record Players(List<Player> players) {
        }

        record Player(String steamid, String personaname, String avatarmedium) {
        }
    }
}
