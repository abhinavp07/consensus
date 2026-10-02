package com.abhinavpinisetti.consensus.ai;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class PromptBuilderTest {

    @Test
    public void itineraryPromptIncludesTripAndEveryMember() {
        String prompt = PromptBuilder.itineraryPrompt("Chicago", "2026-10-09", "2026-10-11", 3, 4, Arrays.asList(
                new MemberAnswers("Sam", "Medium", Arrays.asList("Museums", "Art"), "Relaxed", "Art Institute", ""),
                new MemberAnswers("Jordan", "Low", Collections.singletonList("Food"), "Packed", "", "No pork")));
        assertTrue(prompt.contains("Destination: Chicago"));
        assertTrue(prompt.contains("(3 days)"));
        assertTrue(prompt.contains("Group size: 4 people, 2 of whom answered"));
        assertTrue(prompt.contains("Sam: budget Medium, pace Relaxed, interests Museums, Art, must-dos: Art Institute"));
        assertTrue(prompt.contains("Jordan: budget Low, pace Packed, interests Food, avoid: No pork"));
        assertTrue(prompt.contains("Return exactly 3 days"));
    }

    @Test
    public void alternativesPromptIncludesTargetAndRestOfDay() {
        String prompt = PromptBuilder.alternativesPrompt("Chicago", "2026-10-09", "Navy Pier", "14:00", "Ferris wheel",
                Arrays.asList("10:00 Art Institute", "19:00 Deep dish"), Collections.emptyList());
        assertTrue(prompt.contains("14:00 Navy Pier — Ferris wheel"));
        assertTrue(prompt.contains("- 19:00 Deep dish"));
        assertTrue(prompt.contains("exactly 3 different options"));
    }
}
