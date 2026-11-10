package com.robrodesign.cs2holdem.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String CALLBACK_PATH = "/auth/steam/callback";
    // Windows on the web page (see js/app.js) that explain a sign-in that did not go through
    static final String SIGN_IN_CANCELLED = "/#/signin-cancelled";
    static final String SIGN_IN_FAILED = "/#/signin-failed";

    private final SteamOpenIdClient openIdClient;
    private final LoginService loginService;
    private final SecurityProperties properties;

    public AuthController(SteamOpenIdClient openIdClient, LoginService loginService, SecurityProperties properties) {
        this.openIdClient = openIdClient;
        this.loginService = loginService;
        this.properties = properties;
    }

    // GET /auth/steam : skickar webbläsaren till Steams inloggningssida
    @GetMapping("/steam")
    public ResponseEntity<Void> login() {
        String url = openIdClient.loginUrl(callbackUrl(), properties.publicBaseUrl());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    // GET /auth/steam/callback : Steam skickar tillbaka webbläsaren hit efter inloggningen
    @GetMapping("/steam/callback")
    public ResponseEntity<Void> callback(@RequestParam Map<String, String> params,
                                         HttpServletRequest request, HttpServletResponse response) {
        // The player pressed "Cancel" on Steam's page. Not an error, just back to the start.
        if ("cancel".equals(params.get("openid.mode"))) {
            return redirect(SIGN_IN_CANCELLED);
        }
        return openIdClient.verify(params, callbackUrl())
                .map(steamId64 -> {
                    loginService.logIn(steamId64, request, response);
                    log.info("Player {} logged in through Steam", steamId64);
                    return redirect(properties.afterLoginUrl());
                })
                .orElseGet(() -> {
                    // Steam did not confirm the login: Steam was down, or the address was tampered with.
                    // The browser goes back to our page, which explains it, instead of a bare 401.
                    log.warn("A Steam login could not be verified");
                    return redirect(SIGN_IN_FAILED);
                });
    }

    private static ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    // GET /auth/dev-login?steamId64=... : bara för lokal utveckling, avstängd som standard
    @GetMapping("/dev-login")
    public ResponseEntity<Void> devLogin(@RequestParam("steamId64") long steamId64,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (!properties.devLoginEnabled()) {
            return ResponseEntity.notFound().build();
        }
        log.warn("DEV LOGIN used for player {}. This must never be enabled in production.", steamId64);
        loginService.logIn(steamId64, request, response);
        return ResponseEntity.noContent().build();
    }

    private String callbackUrl() {
        return properties.publicBaseUrl() + CALLBACK_PATH;
    }
}
