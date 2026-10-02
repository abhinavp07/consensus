package com.abhinavpinisetti.consensus.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Display info copied onto the trip (trips/{tripId}.memberProfiles.{userId}) when a member joins,
 * because security rules only let users read their own users/{userId} document.
 */
public class MemberProfile {
    public String name;
    public String photoUrl;

    public MemberProfile() {}

    public MemberProfile(String name, String photoUrl) {
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("photoUrl", photoUrl);
        return map;
    }
}
