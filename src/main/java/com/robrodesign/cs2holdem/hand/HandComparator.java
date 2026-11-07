package com.robrodesign.cs2holdem.hand;

import java.util.Comparator;

/**
 * Orders hands from weakest to strongest: by score, then total damage, then hand type.
 * The strongest of several hands is {@code Collections.max(hands, HandComparator.BY_STRENGTH)}.
 */
public final class HandComparator {

    public static final Comparator<Hand> BY_STRENGTH = Comparator
            .comparingInt(Hand::score)
            .thenComparingInt(Hand::totalDamage)
            .thenComparing(Hand::type);

    private HandComparator() {
    }
}
