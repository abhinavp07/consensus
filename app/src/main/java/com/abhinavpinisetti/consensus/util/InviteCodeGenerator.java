package com.abhinavpinisetti.consensus.util;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

/**
 * Generates 6-character invite codes from an alphabet without look-alike characters
 * (no 0/O, 1/I/L), so codes are easy to read aloud and type.
 */
public final class InviteCodeGenerator {

    public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 6;

    private final Random random;

    public InviteCodeGenerator() {
        this(new SecureRandom());
    }

    public InviteCodeGenerator(Random random) {
        this.random = random;
    }

    public String next() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Upper-cases and strips whitespace/dashes so "k9x-4tp " matches "K9X4TP". */
    public static String normalize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\s-]", "").toUpperCase(Locale.US);
    }

    public static boolean isWellFormed(String code) {
        if (code == null || code.length() != LENGTH) return false;
        for (int i = 0; i < code.length(); i++) {
            if (ALPHABET.indexOf(code.charAt(i)) < 0) return false;
        }
        return true;
    }
}
