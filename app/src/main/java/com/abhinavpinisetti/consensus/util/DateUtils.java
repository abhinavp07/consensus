package com.abhinavpinisetti.consensus.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Trip dates are stored as ISO strings ("2026-10-09") so they are timezone-free and sortable. */
public final class DateUtils {

    public static final int MAX_TRIP_DAYS = 30;

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter SHORT_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter DAY_TAB = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US);

    private DateUtils() {}

    public static LocalDate parse(String iso) {
        return LocalDate.parse(iso);
    }

    public static String toIso(LocalDate date) {
        return date.toString();
    }

    /** MaterialDatePicker returns UTC midnight millis. */
    public static LocalDate fromUtcMillis(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    public static long toUtcMillis(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /** Number of days in the trip, inclusive of both ends. */
    public static int tripLength(String startIso, String endIso) {
        return (int) ChronoUnit.DAYS.between(parse(startIso), parse(endIso)) + 1;
    }

    public static List<LocalDate> tripDates(String startIso, String endIso) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate d = parse(startIso);
        LocalDate end = parse(endIso);
        while (!d.isAfter(end)) {
            dates.add(d);
            d = d.plusDays(1);
        }
        return dates;
    }

    /** "Oct 9 – Oct 12, 2026" */
    public static String formatRange(String startIso, String endIso) {
        if (startIso == null || endIso == null) return "";
        LocalDate start = parse(startIso);
        LocalDate end = parse(endIso);
        if (start.equals(end)) return SHORT_YEAR.format(start);
        return SHORT.format(start) + " – " + SHORT_YEAR.format(end);
    }

    public static String formatShort(LocalDate date) {
        return SHORT_YEAR.format(date);
    }

    /** "Day 1 · Fri Oct 9" */
    public static String dayTabLabel(int dayNumber, String iso) {
        return "Day " + dayNumber + " · " + DAY_TAB.format(parse(iso));
    }
}
