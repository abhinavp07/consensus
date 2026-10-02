package com.abhinavpinisetti.consensus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class InviteCodeGeneratorTest {

    @Test
    public void codesAreSixCharactersFromTheAlphabet() {
        InviteCodeGenerator generator = new InviteCodeGenerator(new Random(42));
        for (int i = 0; i < 1000; i++) {
            String code = generator.next();
            assertEquals(6, code.length());
            assertTrue(code, InviteCodeGenerator.isWellFormed(code));
        }
    }

    @Test
    public void alphabetHasNoLookAlikes() {
        for (char c : "0O1IL".toCharArray()) {
            assertFalse("Alphabet contains " + c, InviteCodeGenerator.ALPHABET.indexOf(c) >= 0);
        }
    }

    @Test
    public void alphabetHasNoDuplicates() {
        Set<Character> seen = new HashSet<>();
        for (char c : InviteCodeGenerator.ALPHABET.toCharArray()) assertTrue(seen.add(c));
    }

    @Test
    public void codesAreVaried() {
        InviteCodeGenerator generator = new InviteCodeGenerator(new Random(7));
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) codes.add(generator.next());
        assertTrue(codes.size() > 990);
    }

    @Test
    public void normalizeIsCaseInsensitiveAndIgnoresSpacesAndDashes() {
        assertEquals("K9X4TP", InviteCodeGenerator.normalize(" k9x-4tp "));
        assertEquals("K9X4TP", InviteCodeGenerator.normalize("K9X 4TP"));
        assertEquals("", InviteCodeGenerator.normalize(null));
    }

    @Test
    public void rejectsMalformedCodes() {
        assertFalse(InviteCodeGenerator.isWellFormed("K9X4T"));
        assertFalse(InviteCodeGenerator.isWellFormed("K9X4TPP"));
        assertFalse(InviteCodeGenerator.isWellFormed("K9X4T0")); // zero isn't in the alphabet
        assertFalse(InviteCodeGenerator.isWellFormed("k9x4tp")); // must be normalized first
        assertFalse(InviteCodeGenerator.isWellFormed(null));
    }
}
