package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

/** trips/{tripId}/days/{dayId}/activities/{activityId} */
public class PlannedActivity {
    public static final String SOURCE_AI = "ai";
    public static final String SOURCE_MANUAL = "manual";

    @DocumentId public String id;
    public String title;
    public String description;
    public String time;
    public double estimatedCost;
    public Map<String, Long> votes = new HashMap<>();
    public String source;
    public String placeName;
    public String placeId;
    public String address;
    public GeoPoint location;
    /** True once a Places lookup ran, whether or not it matched, so we never look it up twice. */
    public boolean placeLookupDone;

    public PlannedActivity() {}

    @Exclude
    public int netScore() {
        int score = 0;
        if (votes != null) for (Long v : votes.values()) if (v != null) score += v.intValue();
        return score;
    }

    @Exclude
    public int downVotes() {
        int count = 0;
        if (votes != null) for (Long v : votes.values()) if (v != null && v < 0) count++;
        return count;
    }

    @Exclude
    public int voteOf(String userId) {
        Long v = votes == null ? null : votes.get(userId);
        return v == null ? 0 : v.intValue();
    }

    /** "Needs a swap" when more than half of the trip's members voted down. */
    @Exclude
    public boolean needsSwap(int memberCount) {
        return memberCount > 0 && downVotes() * 2 > memberCount;
    }

    @Exclude
    public boolean isLocationMissing() {
        return placeLookupDone && location == null;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("time", time);
        map.put("estimatedCost", estimatedCost);
        map.put("votes", votes == null ? new HashMap<>() : votes);
        map.put("source", source);
        map.put("placeName", placeName);
        map.put("placeId", placeId);
        map.put("address", address);
        map.put("location", location);
        map.put("placeLookupDone", placeLookupDone);
        return map;
    }
}
