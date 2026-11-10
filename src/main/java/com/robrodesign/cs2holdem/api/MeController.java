package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final CurrentPlayer currentPlayer;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;

    public MeController(CurrentPlayer currentPlayer, AppUserRepository appUserRepository,
                        PlayerRepository playerRepository) {
        this.currentPlayer = currentPlayer;
        this.appUserRepository = appUserRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * @param registered  true once the player has connected their match history (auth code)
     * @param displayName the Steam name, or null until it has been fetched (within a minute)
     */
    public record MeView(String steamId64, boolean registered, String displayName, String avatarUrl) {
    }

    // GET /api/me : vem är inloggad?
    @GetMapping("/api/me")
    public MeView me() {
        long steamId64 = currentPlayer.steamId64().orElseThrow(NotYourAccountException::new);
        Player player = playerRepository.findById(steamId64).orElse(null);
        return new MeView(String.valueOf(steamId64), appUserRepository.existsById(steamId64),
                player == null ? null : player.getDisplayName(),
                player == null ? null : player.getAvatarUrl());
    }
}
