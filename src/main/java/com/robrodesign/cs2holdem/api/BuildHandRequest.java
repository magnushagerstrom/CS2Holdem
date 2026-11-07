package com.robrodesign.cs2holdem.api;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The cards and kills a player picked for their own hand. Ids are strings, as everywhere in the API.
 * A hand never has more than five kills, so more than five ids can never be valid.
 */
public record BuildHandRequest(
        @Size(max = 5) List<String> cardIds,
        @Size(max = 5) List<String> killIds
) {
    public List<Long> cardIdsAsLong() {
        return toLongs(cardIds);
    }

    public List<Long> killIdsAsLong() {
        return toLongs(killIds);
    }

    private static List<Long> toLongs(List<String> ids) {
        return ids == null ? List.of() : ids.stream().map(Long::parseLong).toList();
    }
}
