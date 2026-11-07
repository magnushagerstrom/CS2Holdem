package com.robrodesign.cs2holdem.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Who is logged in, and the rule that players may only change their own things. */
@Component
public class CurrentPlayer {

    public Optional<Long> steamId64() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long steamId64) {
            return Optional.of(steamId64);
        }
        return Optional.empty();
    }

    /** Throws NotYourAccountException unless the logged-in player is steamId64. */
    public void requireSelf(long steamId64) {
        if (steamId64().filter(id -> id == steamId64).isEmpty()) {
            throw new NotYourAccountException();
        }
    }
}
