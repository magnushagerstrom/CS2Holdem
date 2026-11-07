package com.robrodesign.cs2holdem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Lets the demo worker in: a request to /internal/** with the right X-Worker-Token header is
 * treated as logged in with the role WORKER.
 */
public class WorkerTokenFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Worker-Token";

    private final byte[] expected;

    public WorkerTokenFilter(String token) {
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        // MessageDigest.isEqual takes the same time whatever the input, so the token cannot be
        // guessed one character at a time by measuring how fast wrong guesses are rejected.
        if (token != null && MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8))) {
            var worker = new UsernamePasswordAuthenticationToken("worker", null,
                    List.of(new SimpleGrantedAuthority("ROLE_WORKER")));
            SecurityContextHolder.getContext().setAuthentication(worker);
        }
        chain.doFilter(request, response);
    }
}
