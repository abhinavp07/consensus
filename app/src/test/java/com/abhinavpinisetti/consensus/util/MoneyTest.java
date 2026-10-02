package com.abhinavpinisetti.consensus.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void formatsCents() {
        assertEquals("$42.50", Money.formatCents(4250));
        assertEquals("$0.05", Money.formatCents(5));
        assertEquals("-$12.00", Money.formatCents(-1200));
    }

    @Test
    public void parsesAmounts() {
        assertEquals(4250, Money.parseToCents("42.5"));
        assertEquals(4250, Money.parseToCents("$42.50"));
        assertEquals(120000, Money.parseToCents("1,200"));
        assertEquals(1, Money.parseToCents("0.01"));
    }

    @Test
    public void rejectsInvalidAmounts() {
        assertEquals(-1, Money.parseToCents(""));
        assertEquals(-1, Money.parseToCents("abc"));
        assertEquals(-1, Money.parseToCents("0"));
        assertEquals(-1, Money.parseToCents("-5"));
        assertEquals(-1, Money.parseToCents("1.234"));
        assertEquals(-1, Money.parseToCents(null));
    }

    @Test
    public void formatsEstimates() {
        assertEquals("Free", Money.formatEstimate(0));
        assertEquals("$25", Money.formatEstimate(25));
        assertEquals("$12.50", Money.formatEstimate(12.5));
    }
}
