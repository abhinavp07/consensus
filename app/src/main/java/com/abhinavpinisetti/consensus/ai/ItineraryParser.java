package com.abhinavpinisetti.consensus.ai;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates Gemini's structured JSON. Validation is strict: a plan is either complete
 * and usable, or an {@link InvalidPlanException} is thrown and nothing gets written.
 */
public final class ItineraryParser {

    public static final int ALTERNATIVE_COUNT = 3;

    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
    private static final Gson GSON = new Gson();

    private ItineraryParser() {}

    public static GeneratedPlan parsePlan(String json, int expectedDays) throws InvalidPlanException {
        GeneratedPlan plan = fromJson(json, GeneratedPlan.class);
        if (plan == null || plan.days == null || plan.days.isEmpty()) {
            throw new InvalidPlanException("Response has no days");
        }
        if (plan.days.size() != expectedDays) {
            throw new InvalidPlanException("Expected " + expectedDays + " days but got " + plan.days.size());
        }

        // Trust dayNumber only if it is a clean 1..N permutation; otherwise fall back to list order.
        boolean numbersValid = true;
        boolean[] seen = new boolean[expectedDays + 1];
        for (GeneratedDay day : plan.days) {
            if (day == null) throw new InvalidPlanException("Null day");
            Integer n = day.dayNumber;
            if (n == null || n < 1 || n > expectedDays || seen[n]) {
                numbersValid = false;
                break;
            }
            seen[n] = true;
        }
        List<GeneratedDay> ordered = new ArrayList<>(plan.days);
        if (numbersValid) {
            Collections.sort(ordered, (a, b) -> Integer.compare(a.dayNumber, b.dayNumber));
        } else {
            for (int i = 0; i < ordered.size(); i++) ordered.get(i).dayNumber = i + 1;
        }

        for (GeneratedDay day : ordered) {
            if (day.activities == null || day.activities.isEmpty()) {
                throw new InvalidPlanException("Day " + day.dayNumber + " has no activities");
            }
            for (GeneratedActivity a : day.activities) {
                validateActivity(a);
            }
            day.note = day.note == null ? "" : day.note.trim();
        }
        plan.days = ordered;
        return plan;
    }

    public static List<GeneratedActivity> parseAlternatives(String json) throws InvalidPlanException {
        AlternativesResponse response = fromJson(json, AlternativesResponse.class);
        if (response == null || response.options == null || response.options.size() < ALTERNATIVE_COUNT) {
            throw new InvalidPlanException("Expected " + ALTERNATIVE_COUNT + " alternatives");
        }
        List<GeneratedActivity> options = new ArrayList<>(response.options.subList(0, ALTERNATIVE_COUNT));
        for (GeneratedActivity a : options) {
            validateActivity(a);
        }
        return options;
    }

    /** Validates required fields and normalizes time to zero-padded HH:mm. */
    static void validateActivity(GeneratedActivity a) throws InvalidPlanException {
        if (a == null) throw new InvalidPlanException("Null activity");
        if (isBlank(a.title)) throw new InvalidPlanException("Activity missing title");
        a.title = a.title.trim();
        a.time = normalizeTime(a.time);
        if (a.time == null) throw new InvalidPlanException("Activity '" + a.title + "' has invalid time");
        if (a.estimatedCost == null || a.estimatedCost < 0 || a.estimatedCost.isNaN()) {
            throw new InvalidPlanException("Activity '" + a.title + "' has invalid cost");
        }
        a.description = a.description == null ? "" : a.description.trim();
        a.placeName = isBlank(a.placeName) ? a.title : a.placeName.trim();
    }

    /** Returns "HH:mm" or null if the input isn't a valid 24-hour time. */
    public static String normalizeTime(String time) {
        if (time == null) return null;
        Matcher m = TIME.matcher(time.trim());
        if (!m.matches()) return null;
        int h = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        if (h > 23 || min > 59) return null;
        return String.format(java.util.Locale.US, "%02d:%02d", h, min);
    }

    private static <T> T fromJson(String json, Class<T> type) throws InvalidPlanException {
        if (isBlank(json)) throw new InvalidPlanException("Empty response");
        try {
            return GSON.fromJson(stripFences(json), type);
        } catch (JsonParseException e) {
            throw new InvalidPlanException("Malformed JSON", e);
        }
    }

    /** Structured output shouldn't include markdown fences, but strip them defensively. */
    static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                t = t.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    static class AlternativesResponse {
        List<GeneratedActivity> options;
    }
}
