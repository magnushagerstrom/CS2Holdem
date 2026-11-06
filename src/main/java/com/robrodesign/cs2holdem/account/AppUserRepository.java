package com.robrodesign.cs2holdem.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Hämtar de 100 användare som väntat längst på att synkas.
    // Spring Data översätter metodnamnet till:
    // SELECT ... FROM app_users WHERE next_sync_at < ? ORDER BY next_sync_at ASC LIMIT 100
    List<AppUser> findTop100ByNextSyncAtBeforeOrderByNextSyncAtAsc(Instant now);
}
