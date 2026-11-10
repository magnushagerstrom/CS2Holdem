package com.robrodesign.cs2holdem.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Hämtar de 100 användare som väntat längst på att synkas.
    // Spring Data översätter metodnamnet till:
    // SELECT ... FROM app_users WHERE next_sync_at < ? ORDER BY next_sync_at ASC LIMIT 100
    List<AppUser> findTop100ByNextSyncAtBeforeOrderByNextSyncAtAsc(Instant now);

    // Which of these players are registered? Selects only the id, so no auth code is loaded
    // (and decrypted) just to answer a yes/no question.
    @Query("SELECT u.steamId64 FROM AppUser u WHERE u.steamId64 IN :ids")
    Set<Long> findRegisteredAmong(@Param("ids") Collection<Long> ids);
}
