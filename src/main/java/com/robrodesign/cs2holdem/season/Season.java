package com.robrodesign.cs2holdem.season;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A CS2 season. Matches played between startsAt (inclusive) and endsAt (exclusive) belong to it. */
@Entity
@Table(name = "seasons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 64, nullable = false, unique = true)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    public Season(String name, Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("A season must end after it starts");
        }
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public boolean contains(Instant moment) {
        return !moment.isBefore(startsAt) && moment.isBefore(endsAt);
    }

    public boolean hasEnded(Instant now) {
        return !now.isBefore(endsAt);
    }

    /** Locks the season. After this no season hand in it can change. */
    public void lock() {
        this.locked = true;
    }
}
