package com.robrodesign.cs2holdem.security;

import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Logs a player in: stores who they are in their HTTP session. */
@Service
public class LoginService {

    private final PlayerRepository playerRepository;
    private final SecurityContextRepository contextRepository;

    public LoginService(PlayerRepository playerRepository, SecurityContextRepository contextRepository) {
        this.playerRepository = playerRepository;
        this.contextRepository = contextRepository;
    }

    @Transactional
    public void logIn(long steamId64, HttpServletRequest request, HttpServletResponse response) {
        if (!playerRepository.existsById(steamId64)) {
            playerRepository.save(new Player(steamId64, null));
        }

        // A new session id at login prevents "session fixation": an attacker cannot hand the
        // victim a session id before login and then use it afterwards.
        request.changeSessionId();

        var authentication = new UsernamePasswordAuthenticationToken(steamId64, null,
                List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
