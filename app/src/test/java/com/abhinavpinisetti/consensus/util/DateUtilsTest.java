package com.abhinavpinisetti.consensus.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

public class DateUtilsTest {

    @Test
    public void tripLengthIsInclusive() {
        assertEquals(1, DateUtils.tripLength("2026-10-09", "2026-10-09"));
        assertEquals(4, DateUtils.tripLength("2026-10-09", "2026-10-12"));
        assertEquals(3, DateUtils.tripLength("2026-12-30", "2027-01-01"));
    }

    @Test
    public void tripDatesListsEveryDay() {
        assertEquals(LocalDate.of(2026, 10, 11), DateUtils.tripDates("2026-10-09", "2026-10-12").get(2));
    }

    @Test
    public void dayTabLabel() {
        assertEquals("Day 1 · Fri Oct 9", DateUtils.dayTabLabel(1, "2026-10-09"));
    }

    @Test
    public void utcMillisRoundTrip() {
        LocalDate d = LocalDate.of(2026, 10, 9);
        assertEquals(d, DateUtils.fromUtcMillis(DateUtils.toUtcMillis(d)));
    }
}
