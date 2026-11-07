package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.account.AppUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final CurrentPlayer currentPlayer;
    private final AppUserRepository appUserRepository;

    public MeController(CurrentPlayer currentPlayer, AppUserRepository appUserRepository) {
        this.currentPlayer = currentPlayer;
        this.appUserRepository = appUserRepository;
    }

    /** @param registered true once the player has connected their match history (auth code) */
    public record MeView(String steamId64, boolean registered) {
    }

    // GET /api/me : vem är inloggad?
    @GetMapping("/api/me")
    public MeView me() {
        long steamId64 = currentPlayer.steamId64().orElseThrow(NotYourAccountException::new);
        return new MeView(String.valueOf(steamId64), appUserRepository.existsById(steamId64));
    }
}
