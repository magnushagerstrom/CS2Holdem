package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import com.robrodesign.cs2holdem.privacy.PrivacyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final CurrentPlayer currentPlayer;
    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final PrivacyService privacyService;

    public MeController(CurrentPlayer currentPlayer, AppUserRepository appUserRepository,
                        PlayerRepository playerRepository, PrivacyService privacyService) {
        this.currentPlayer = currentPlayer;
        this.appUserRepository = appUserRepository;
        this.playerRepository = playerRepository;
        this.privacyService = privacyService;
    }

    /**
     * DELETE /api/me : deletes everything about the signed-in player and signs them out.
     * Anyone who signs in through Steam can do this, registered or not, since matches store
     * data about every player in them.
     */
    @DeleteMapping("/api/me")
    public ResponseEntity<Void> deleteMe(HttpServletRequest request, HttpServletResponse response) {
        long steamId64 = currentPlayer.steamId64().orElseThrow(NotYourAccountException::new);
        privacyService.deleteEverything(steamId64);
        // Ends the session and forgets the login, the same as signing out
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
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
