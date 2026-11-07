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
        return openIdClient.verify(params, callbackUrl())
                .map(steamId64 -> {
                    loginService.logIn(steamId64, request, response);
                    log.info("Player {} logged in through Steam", steamId64);
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(properties.afterLoginUrl())).<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
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
