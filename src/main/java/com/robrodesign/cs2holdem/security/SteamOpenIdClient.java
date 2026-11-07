package com.robrodesign.cs2holdem.security;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Sign in through Steam". Steam uses OpenID 2.0:
 *
 * 1. We send the browser to Steam's login page together with the URL Steam should come back to.
 * 2. The player logs in on Steam's own site. We never see their password.
 * 3. Steam sends the browser back with signed parameters, including the player's SteamID.
 * 4. We ask Steam directly whether those parameters are genuine (check_authentication).
 *    Without this step anyone could fake a login by editing the URL.
 */
@Component
public class SteamOpenIdClient {

    static final String STEAM_LOGIN = "https://steamcommunity.com/openid/login";
    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    private static final Pattern CLAIMED_ID = Pattern.compile("^https://steamcommunity\\.com/openid/id/(7656119\\d{10})$");

    private final RestClient restClient;

    public SteamOpenIdClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /** The URL of Steam's login page. */
    public String loginUrl(String returnTo, String realm) {
        return UriComponentsBuilder.fromUriString(STEAM_LOGIN)
                .queryParam("openid.ns", OPENID_NS)
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", returnTo)
                .queryParam("openid.realm", realm)
                .queryParam("openid.identity", IDENTIFIER_SELECT)
                .queryParam("openid.claimed_id", IDENTIFIER_SELECT)
                .encode()
                .toUriString();
    }

    /**
     * Checks the parameters Steam sent back.
     *
     * @return the player's SteamID64 if Steam confirms the login, otherwise empty
     */
    public Optional<Long> verify(Map<String, String> params, String expectedReturnTo) {
        if (!"id_res".equals(params.get("openid.mode"))) {
            return Optional.empty();
        }
        String returnTo = params.get("openid.return_to");
        if (returnTo == null || !returnTo.startsWith(expectedReturnTo)) {
            return Optional.empty(); // the login was meant for another site
        }
        Matcher matcher = CLAIMED_ID.matcher(params.getOrDefault("openid.claimed_id", ""));
        if (!matcher.matches()) {
            return Optional.empty();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach((name, value) -> {
            if (name.startsWith("openid.")) {
                form.add(name, value);
            }
        });
        form.set("openid.mode", "check_authentication");

        String body;
        try {
            body = restClient.post()
                    .uri(STEAM_LOGIN)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            return Optional.empty();
        }
        if (body == null || !body.contains("is_valid:true")) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(1)));
    }
}
