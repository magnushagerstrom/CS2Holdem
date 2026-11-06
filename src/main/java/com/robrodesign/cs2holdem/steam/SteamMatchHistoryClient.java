package com.robrodesign.cs2holdem.steam;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Pratar med Steams Web API om en spelares matchhistorik.
 * All Steam-specifik kunskap (URL:er, parameternamn, felkoder) finns här och ingen annanstans.
 */
@Component
public class SteamMatchHistoryClient {

    private static final String NO_MORE_MATCHES = "n/a";
    private static final int PRECONDITION_FAILED = 412;

    private final RestClient restClient;
    private final SteamProperties properties;

    // Spring Boot ger oss en färdigkonfigurerad RestClient.Builder. Vi bygger en klient som pekar på Steam.
    public SteamMatchHistoryClient(RestClient.Builder builder, SteamProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    /**
     * Frågar Steam: "vilken match spelade den här användaren EFTER knownShareCode?"
     *
     * @return nästa share code, eller Optional.empty() om knownShareCode redan är den senaste
     */
    public Optional<String> findNextShareCode(long steamId64, String authCode, String knownShareCode) {
        NextCodeResponse response;
        try {
            response = restClient.get()
                    .uri(uri -> uri
                            .path("/ICSGOPlayers_730/GetNextMatchSharingCode/v1/")
                            .queryParam("key", properties.apiKey())
                            .queryParam("steamid", steamId64)
                            .queryParam("steamidkey", authCode)
                            .queryParam("knowncode", knownShareCode)
                            .build())
                    .retrieve()
                    .body(NextCodeResponse.class);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new InvalidSteamCredentialsException();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == PRECONDITION_FAILED) {
                throw new InvalidShareCodeException("Steam does not recognise share code " + knownShareCode
                        + " for this player");
            }
            throw new SteamUnavailableException("Steam returned " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            // Nätverksfel, timeout, 5xx-svar m.m.
            throw new SteamUnavailableException("Could not reach Steam", e);
        }

        if (response == null || response.result() == null || response.result().nextcode() == null) {
            throw new SteamUnavailableException("Steam returned an unexpected response", null);
        }

        String nextCode = response.result().nextcode();
        return NO_MORE_MATCHES.equals(nextCode) ? Optional.empty() : Optional.of(nextCode);
    }

    // Speglar Steams JSON: { "result": { "nextcode": "CSGO-..." } }
    record NextCodeResponse(Result result) {
        record Result(String nextcode) {
        }
    }
}
