package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.firestore.DocumentId;

/** trips/{tripId}/days/{dayId}. {@code note} is the AI's explanation of the day's balance. */
public class Day {
    @DocumentId public String id;
    public String date;
    public int dayNumber;
    public String note;

    public Day() {}

    public static String idFor(int dayNumber) {
        return String.format(java.util.Locale.US, "day-%02d", dayNumber);
    }
}
