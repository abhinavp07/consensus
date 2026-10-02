package com.abhinavpinisetti.consensus.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Formats and parses money stored as integer cents. */
public final class Money {

    private Money() {}

    public static String formatCents(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return String.format(Locale.US, "%s$%d.%02d", sign, abs / 100, abs % 100);
    }

    /** Formats a whole-dollar estimate such as 25.0 as "$25" and 12.5 as "$12.50". */
    public static String formatEstimate(double dollars) {
        if (dollars <= 0) return "Free";
        if (dollars == Math.rint(dollars)) return String.format(Locale.US, "$%.0f", dollars);
        return String.format(Locale.US, "$%.2f", dollars);
    }

    /**
     * Parses user input like "42.5" or "$1,200" into cents.
     * Returns -1 when the input is not a positive amount with at most two decimals.
     */
    public static long parseToCents(String input) {
        if (input == null) return -1;
        String cleaned = input.replace("$", "").replace(",", "").trim();
        if (cleaned.isEmpty()) return -1;
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() <= 0 || value.scale() > 2) return -1;
            return value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }
}
