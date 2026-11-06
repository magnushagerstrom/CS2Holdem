package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.account.AccountNotFoundException;
import com.robrodesign.cs2holdem.account.AppUserRepository;
import com.robrodesign.cs2holdem.account.RegistrationService;
import com.robrodesign.cs2holdem.match.MatchRepository;
import com.robrodesign.cs2holdem.sync.MatchSyncService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP-lagret. Controllern ska vara tunn: ta emot anropet, låta services göra jobbet, svara.
 * Ingen affärslogik här, så kan samma logik senare anropas från t.ex. ett schemalagt jobb.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final RegistrationService registrationService;
    private final MatchSyncService matchSyncService;
    private final AppUserRepository appUserRepository;
    private final MatchRepository matchRepository;

    public AccountController(RegistrationService registrationService,
                             MatchSyncService matchSyncService,
                             AppUserRepository appUserRepository,
                             MatchRepository matchRepository) {
        this.registrationService = registrationService;
        this.matchSyncService = matchSyncService;
        this.appUserRepository = appUserRepository;
        this.matchRepository = matchRepository;
    }

    // POST /api/accounts : registrera och hämta historiken direkt
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        long steamId64 = Long.parseLong(request.steamId64());
        registrationService.register(steamId64, request.authCode(), request.knownShareCode());
        int matchesFound = matchSyncService.syncUser(steamId64);
        return new RegisterResponse(request.steamId64(), matchesFound);
    }

    // POST /api/accounts/{steamId64}/sync : synka manuellt (bra för att testa efter en ny match)
    @PostMapping("/{steamId64}/sync")
    public SyncResponse sync(@PathVariable("steamId64") long steamId64) {
        int newMatches = matchSyncService.syncUser(steamId64);
        return new SyncResponse(String.valueOf(steamId64), newMatches);
    }

    // GET /api/accounts/{steamId64}/matches : alla matcher i användarens historik
    @GetMapping("/{steamId64}/matches")
    public List<MatchSummary> matches(@PathVariable("steamId64") long steamId64) {
        if (!appUserRepository.existsById(steamId64)) {
            throw new AccountNotFoundException(steamId64);
        }
        return matchRepository.findMatchesInHistoryOf(steamId64).stream()
                .map(MatchSummary::from)
                .toList();
    }
}
