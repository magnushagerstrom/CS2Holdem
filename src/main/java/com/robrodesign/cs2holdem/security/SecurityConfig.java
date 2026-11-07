package com.robrodesign.cs2holdem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Who may call what.
 *
 * - Reading hands, collections, seasons and leaderboards is public.
 * - Everything that changes something requires a player logged in through Steam.
 * - /internal/** is for the demo worker and is protected by a shared token (WorkerTokenFilter).
 *
 * The login lives in an HTTP session cookie. CSRF tokens are turned off; instead the cookie is
 * SameSite=Strict (application.yaml), so browsers never send it along with requests that start
 * on another site, which is what a CSRF attack relies on.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository contextRepository,
                                                   SecurityProperties properties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .addFilterBefore(new WorkerTokenFilter(properties.workerToken()),
                        org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/internal/**").hasRole("WORKER")
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers("/api/**").hasRole("PLAYER")
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Nobody logs in with a username and password here: players use Steam and the worker uses its token.
     * An empty user list stops Spring Boot from creating a default user with a generated password.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
