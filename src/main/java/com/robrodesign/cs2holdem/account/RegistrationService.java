package com.robrodesign.cs2holdem.account;

import com.robrodesign.cs2holdem.player.Player;
import com.robrodesign.cs2holdem.player.PlayerRepository;
import com.robrodesign.cs2holdem.privacy.PrivacyService;
import com.robrodesign.cs2holdem.steam.ShareCode;
import com.robrodesign.cs2holdem.steam.SteamMatchHistoryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Registrerar en ny användare, men bara om Steam godkänner deras uppgifter. */
@Service
public class RegistrationService {

    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;
    private final SteamMatchHistoryClient steamClient;
    private final TransactionTemplate transactionTemplate;
    private final PrivacyService privacyService;

    // Konstruktor-injektion: Spring skickar in alla beroenden. Fälten kan vara final,
    // och i ett test kan vi enkelt skicka in egna (fejkade) versioner.
    public RegistrationService(AppUserRepository appUserRepository,
                               PlayerRepository playerRepository,
                               SteamMatchHistoryClient steamClient,
                               TransactionTemplate transactionTemplate,
                               PrivacyService privacyService) {
        this.appUserRepository = appUserRepository;
        this.playerRepository = playerRepository;
        this.steamClient = steamClient;
        this.transactionTemplate = transactionTemplate;
        this.privacyService = privacyService;
    }

    public AppUser register(long steamId64, String authCode, String knownShareCode) {
        if (appUserRepository.existsById(steamId64)) {
            throw new AccountAlreadyExistsException(steamId64);
        }

        // Kastar InvalidShareCodeException om formatet är fel. Billigt, så vi gör det före Steam-anropet.
        ShareCode.parse(knownShareCode);

        // Ett riktigt anrop till Steam. Kastar InvalidSteamCredentialsException om Steam säger nej.
        // Det görs UTANFÖR databastransaktionen: vi vill inte hålla en databasanslutning upptagen
        // medan vi väntar på ett externt API som kan ta flera sekunder.
        steamClient.findNextShareCode(steamId64, authCode, knownShareCode);

        // Spelaren och användaren sparas i EN transaktion: antingen sparas båda, eller ingen.
        return transactionTemplate.execute(status -> {
            // Registering is asking to be stored, so an earlier "delete me" no longer applies
            privacyService.optIn(steamId64);
            Player player = playerRepository.findById(steamId64)
                    .orElseGet(() -> playerRepository.save(new Player(steamId64, null)));
            return appUserRepository.save(new AppUser(player, authCode, knownShareCode));
        });
    }
}
