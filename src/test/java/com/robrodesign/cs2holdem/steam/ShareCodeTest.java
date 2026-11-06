package com.robrodesign.cs2holdem.steam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Ett vanligt enhetstest: ingen Spring, ingen databas. Körs på millisekunder.
class ShareCodeTest {

    @Test
    void decodesKnownShareCode() {
        // Exemplet kommer från biblioteket csgo-sharecode, som används brett i communityn.
        ShareCode code = ShareCode.parse("CSGO-GADqf-jjyJ8-cSP2r-smZRo-TO2xK");

        assertThat(code.matchId()).isEqualTo(3230642215713767580L);
        assertThat(code.reservationId()).isEqualTo(3230647599455273103L);
        assertThat(code.tvPort()).isEqualTo(55788);
    }

    @Test
    void rejectsWrongFormat() {
        assertThatThrownBy(() -> ShareCode.parse("not-a-share-code"))
                .isInstanceOf(InvalidShareCodeException.class);
    }

    @Test
    void rejectsCharactersOutsideValvesAlphabet() {
        // "l" (litet L) finns inte i Valves alfabet
        assertThatThrownBy(() -> ShareCode.parse("CSGO-lADqf-jjyJ8-cSP2r-smZRo-TO2xK"))
                .isInstanceOf(InvalidShareCodeException.class);
    }
}
