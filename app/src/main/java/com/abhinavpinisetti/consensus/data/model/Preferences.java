package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** trips/{tripId}/preferences/{userId} */
public class Preferences {
    public static final List<String> BUDGETS = Arrays.asList("Low", "Medium", "High");
    public static final List<String> PACES = Arrays.asList("Relaxed", "Balanced", "Packed");
    public static final List<String> INTERESTS = Arrays.asList(
            "Food", "Museums", "History", "Nightlife", "Outdoors", "Shopping", "Art", "Sports", "Relaxing");

    @DocumentId public String userId;
    public String budget;
    public List<String> interests = new ArrayList<>();
    public String pace;
    public String mustDos;
    public String avoid;
    @ServerTimestamp public Timestamp updatedAt;

    public Preferences() {}
}
