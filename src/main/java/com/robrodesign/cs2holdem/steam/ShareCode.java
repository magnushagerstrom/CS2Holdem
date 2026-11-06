package com.robrodesign.cs2holdem.steam;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * En CS2 share code, t.ex. "CSGO-GADqf-jjyJ8-cSP2r-smZRo-TO2xK", avkodad till sina delar.
 *
 * En share code är inte krypterad, bara ett stort tal skrivet i ett eget 57-teckens alfabet.
 * Talet innehåller 18 bytes: match-ID (8), reservation-ID (8) och TV-port (2).
 * Vi behöver alla tre senare för att be CS2:s Game Coordinator om demo-filen.
 */
public record ShareCode(String value, long matchId, long reservationId, int tvPort) {

    // Valves alfabet. Saknar medvetet tecken som är lätta att förväxla: I, l, g, 0 och 1.
    private static final String DICTIONARY = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefhijkmnopqrstuvwxyz23456789";
    private static final BigInteger BASE = BigInteger.valueOf(DICTIONARY.length());
    private static final Pattern FORMAT = Pattern.compile("^CSGO(-[A-Za-z0-9]{5}){5}$");
    private static final int TOTAL_BYTES = 18;

    public static ShareCode parse(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidShareCodeException("Not a valid share code: " + value);
        }

        String characters = value.substring("CSGO".length()).replace("-", "");

        // Steg 1: läs tecknen baklänges och bygg upp ett stort tal (som att läsa ett tal i bas 57).
        // BigInteger behövs eftersom talet är 144 bitar, långt större än en long (64 bitar).
        BigInteger number = BigInteger.ZERO;
        for (int i = characters.length() - 1; i >= 0; i--) {
            int digit = DICTIONARY.indexOf(characters.charAt(i));
            if (digit < 0) {
                throw new InvalidShareCodeException("Share code contains an invalid character: " + value);
            }
            number = number.multiply(BASE).add(BigInteger.valueOf(digit));
        }

        // Steg 2: gör om talet till exakt 18 bytes (fyll på med nollor framifrån om det är kortare).
        byte[] bytes = toFixedLength(number.toByteArray());

        // Steg 3: plocka ut fälten. Varje fält ligger lagrat "little-endian" (minst signifikanta byte först).
        long matchId = readLittleEndian(bytes, 0, 8).longValueExact();
        long reservationId = readLittleEndian(bytes, 8, 8).longValueExact();
        int tvPort = readLittleEndian(bytes, 16, 2).intValueExact();

        return new ShareCode(value, matchId, reservationId, tvPort);
    }

    private static byte[] toFixedLength(byte[] raw) {
        byte[] result = new byte[TOTAL_BYTES];
        int length = Math.min(raw.length, TOTAL_BYTES);
        System.arraycopy(raw, raw.length - length, result, TOTAL_BYTES - length, length);
        return result;
    }

    private static BigInteger readLittleEndian(byte[] bytes, int offset, int length) {
        BigInteger result = BigInteger.ZERO;
        for (int i = offset + length - 1; i >= offset; i--) {
            result = result.shiftLeft(8).or(BigInteger.valueOf(bytes[i] & 0xFF)); // & 0xFF: byte är signerad i Java
        }
        return result;
    }
}
