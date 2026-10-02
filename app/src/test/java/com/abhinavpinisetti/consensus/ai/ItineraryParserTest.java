package com.abhinavpinisetti.consensus.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.List;

public class ItineraryParserTest {

    private static final String ACTIVITY =
            "{\"title\":\"Art Institute\",\"time\":\"10:00\",\"description\":\"Impressionists.\","
                    + "\"estimatedCost\":32,\"placeName\":\"Art Institute of Chicago\"}";

    private static String day(int n, String... activities) {
        return "{\"dayNumber\":" + n + ",\"note\":\"Museum morning for Sam\",\"activities\":["
                + String.join(",", activities) + "]}";
    }

    private static String plan(String... days) {
        return "{\"days\":[" + String.join(",", days) + "]}";
    }

    private static void assertInvalid(String json, int days) {
        try {
            ItineraryParser.parsePlan(json, days);
            fail("Expected InvalidPlanException for " + json);
        } catch (InvalidPlanException expected) {
            // ok
        }
    }

    @Test
    public void parsesValidPlan() throws Exception {
        GeneratedPlan plan = ItineraryParser.parsePlan(plan(day(1, ACTIVITY), day(2, ACTIVITY, ACTIVITY)), 2);
        assertEquals(2, plan.days.size());
        assertEquals(1, (int) plan.days.get(0).dayNumber);
        assertEquals("Museum morning for Sam", plan.days.get(0).note);
        GeneratedActivity a = plan.days.get(1).activities.get(1);
        assertEquals("Art Institute", a.title);
        assertEquals("10:00", a.time);
        assertEquals(32.0, a.estimatedCost, 0.0001);
        assertEquals("Art Institute of Chicago", a.placeName);
    }

    @Test
    public void sortsDaysByDayNumber() throws Exception {
        GeneratedPlan plan = ItineraryParser.parsePlan(plan(day(2, ACTIVITY), day(1, ACTIVITY)), 2);
        assertEquals(1, (int) plan.days.get(0).dayNumber);
        assertEquals(2, (int) plan.days.get(1).dayNumber);
    }

    @Test
    public void fallsBackToListOrderForBadDayNumbers() throws Exception {
        GeneratedPlan plan = ItineraryParser.parsePlan(plan(day(5, ACTIVITY), day(5, ACTIVITY)), 2);
        assertEquals(1, (int) plan.days.get(0).dayNumber);
        assertEquals(2, (int) plan.days.get(1).dayNumber);
    }

    @Test
    public void normalizesTimeAndDefaults() throws Exception {
        String loose = "{\"title\":\" Breakfast \",\"time\":\"8:05\",\"estimatedCost\":0}";
        GeneratedActivity a = ItineraryParser.parsePlan(plan(day(1, loose)), 1).days.get(0).activities.get(0);
        assertEquals("Breakfast", a.title);
        assertEquals("08:05", a.time);
        assertEquals("", a.description);
        assertEquals("Breakfast", a.placeName);
    }

    @Test
    public void stripsMarkdownFences() throws Exception {
        String json = "```json\n" + plan(day(1, ACTIVITY)) + "\n```";
        assertEquals(1, ItineraryParser.parsePlan(json, 1).days.size());
    }

    @Test
    public void rejectsMalformedJson() {
        assertInvalid("{\"days\":[", 1);
        assertInvalid("not json", 1);
        assertInvalid("", 1);
        assertInvalid(null, 1);
    }

    @Test
    public void rejectsIncompletePlans() {
        assertInvalid("{}", 1);
        assertInvalid(plan(), 1);
        assertInvalid(plan(day(1, ACTIVITY)), 2);              // wrong number of days
        assertInvalid(plan(day(1)), 1);                         // empty day
        assertInvalid(plan(day(1, "{\"time\":\"10:00\",\"estimatedCost\":1}")), 1);           // no title
        assertInvalid(plan(day(1, "{\"title\":\"X\",\"time\":\"25:00\",\"estimatedCost\":1}")), 1); // bad time
        assertInvalid(plan(day(1, "{\"title\":\"X\",\"time\":\"10am\",\"estimatedCost\":1}")), 1);
        assertInvalid(plan(day(1, "{\"title\":\"X\",\"time\":\"10:00\"}")), 1);               // no cost
        assertInvalid(plan(day(1, "{\"title\":\"X\",\"time\":\"10:00\",\"estimatedCost\":-5}")), 1);
    }

    @Test
    public void parsesThreeAlternatives() throws Exception {
        String json = "{\"options\":[" + ACTIVITY + "," + ACTIVITY + "," + ACTIVITY + "," + ACTIVITY + "]}";
        List<GeneratedActivity> options = ItineraryParser.parseAlternatives(json);
        assertEquals(3, options.size());
    }

    @Test
    public void rejectsTooFewAlternatives() {
        try {
            ItineraryParser.parseAlternatives("{\"options\":[" + ACTIVITY + "]}");
            fail();
        } catch (InvalidPlanException expected) {
            // ok
        }
    }

    @Test
    public void normalizeTime() {
        assertEquals("09:30", ItineraryParser.normalizeTime("9:30"));
        assertEquals("23:59", ItineraryParser.normalizeTime("23:59"));
        assertNull(ItineraryParser.normalizeTime("24:00"));
        assertNull(ItineraryParser.normalizeTime("12:60"));
        assertNull(ItineraryParser.normalizeTime(null));
    }
}
