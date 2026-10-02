package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** trips/{tripId}. Dates are ISO strings ("2026-10-09"). */
public class Trip {
    public static final String STATUS_COLLECTING = "collecting_preferences";
    public static final String STATUS_PLANNED = "planned";

    @DocumentId public String id;
    public String name;
    public String destination;
    public String destinationPlaceId;
    public Double destinationLat;
    public Double destinationLng;
    public String startDate;
    public String endDate;
    public String ownerId;
    public String inviteCode;
    public String status;
    public List<String> memberIds = new ArrayList<>();
    public Map<String, MemberProfile> memberProfiles = new HashMap<>();
    @ServerTimestamp public Timestamp createdAt;
    /** Set on every AI (re)generation; the notification Cloud Function watches it. */
    public Timestamp generatedAt;

    public Trip() {}

    @Exclude
    public boolean isOwnedBy(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    /** Member's display name, falling back to "Someone" for missing profiles. */
    @Exclude
    public String nameOf(String userId) {
        MemberProfile p = memberProfiles == null ? null : memberProfiles.get(userId);
        return p == null || p.name == null || p.name.isEmpty() ? "Someone" : p.name;
    }

    @Exclude
    public String firstNameOf(String userId) {
        String name = nameOf(userId);
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    @Exclude
    public String photoOf(String userId) {
        MemberProfile p = memberProfiles == null ? null : memberProfiles.get(userId);
        return p == null ? null : p.photoUrl;
    }

    @Exclude
    public boolean hasDestinationLocation() {
        return destinationLat != null && destinationLng != null;
    }
}
