package com.robrodesign.cs2holdem.season;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    @Query("select s from Season s where s.startsAt <= :moment and s.endsAt > :moment")
    Optional<Season> findContaining(@Param("moment") Instant moment);

    List<Season> findAllByOrderByStartsAtDesc();

    List<Season> findByLockedFalseAndEndsAtLessThanEqual(Instant now);
}
