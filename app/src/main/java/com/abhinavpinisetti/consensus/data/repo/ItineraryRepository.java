package com.abhinavpinisetti.consensus.data.repo;

import com.abhinavpinisetti.consensus.ai.GeneratedActivity;
import com.abhinavpinisetti.consensus.ai.GeneratedDay;
import com.abhinavpinisetti.consensus.ai.GeneratedPlan;
import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.QueryLiveData;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.util.DateUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Days and activities, including saving AI-generated plans. */
public class ItineraryRepository {

    private static final int MAX_BATCH_WRITES = 500;
    private static ItineraryRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized ItineraryRepository get() {
        if (instance == null) instance = new ItineraryRepository();
        return instance;
    }

    /** Identifies an activity that still needs a Places lookup. */
    public static class ActivityRef {
        public final String dayId;
        public final String activityId;
        public final String placeName;

        public ActivityRef(String dayId, String activityId, String placeName) {
            this.dayId = dayId;
            this.activityId = activityId;
            this.placeName = placeName;
        }
    }

    private DocumentReference trip(String tripId) {
        return db.collection("trips").document(tripId);
    }

    public CollectionReference activities(String tripId, String dayId) {
        return trip(tripId).collection("days").document(dayId).collection("activities");
    }

    public QueryLiveData<Day> days(String tripId) {
        return new QueryLiveData<>(trip(tripId).collection("days").orderBy("dayNumber"), Day.class);
    }

    public QueryLiveData<PlannedActivity> activitiesLive(String tripId, String dayId) {
        return new QueryLiveData<>(activities(tripId, dayId).orderBy("time"), PlannedActivity.class);
    }

    public Task<List<PlannedActivity>> fetchActivities(String tripId, String dayId) {
        return activities(tripId, dayId).orderBy("time").get()
                .continueWith(t -> t.getResult().toObjects(PlannedActivity.class));
    }

    public Task<PlannedActivity> fetchActivity(String tripId, String dayId, String activityId) {
        return activities(tripId, dayId).document(activityId).get().continueWith(t -> {
            DocumentSnapshot s = t.getResult();
            if (!s.exists()) throw new ErrorMessages.FriendlyException("This activity was deleted.");
            return s.toObject(PlannedActivity.class);
        });
    }

    public Task<DocumentReference> addActivity(String tripId, String dayId, PlannedActivity activity) {
        return activities(tripId, dayId).add(activity.toMap());
    }

    /** Saves edits. Changing the place name clears the old match so it gets looked up again. */
    public Task<Void> updateActivity(String tripId, String dayId, String activityId,
                                     String title, String time, String description, double cost,
                                     String placeName, boolean placeChanged) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("time", time);
        updates.put("description", description);
        updates.put("estimatedCost", cost);
        if (placeChanged) {
            updates.put("placeName", placeName);
            updates.put("placeId", null);
            updates.put("address", null);
            updates.put("location", null);
            updates.put("placeLookupDone", false);
        }
        return activities(tripId, dayId).document(activityId).update(updates);
    }

    public Task<Void> deleteActivity(String tripId, String dayId, String activityId) {
        return activities(tripId, dayId).document(activityId).delete();
    }

    /** Sets the user's vote to 1 or -1, or removes it when {@code value} is 0. */
    public Task<Void> vote(String tripId, String dayId, String activityId, String uid, int value) {
        Object v = value == 0 ? FieldValue.delete() : (Object) (long) value;
        return activities(tripId, dayId).document(activityId).update("votes." + uid, v);
    }

    /** Replaces an activity with an AI alternative and clears its votes. */
    public Task<Void> replaceWithAlternative(String tripId, String dayId, String activityId, GeneratedActivity alt) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", alt.title);
        updates.put("time", alt.time);
        updates.put("description", alt.description);
        updates.put("estimatedCost", alt.estimatedCost);
        updates.put("placeName", alt.placeName);
        updates.put("source", PlannedActivity.SOURCE_AI);
        updates.put("votes", new HashMap<String, Object>());
        updates.put("placeId", null);
        updates.put("address", null);
        updates.put("location", null);
        updates.put("placeLookupDone", false);
        return activities(tripId, dayId).document(activityId).update(updates);
    }

    public Task<Void> savePlaceMatch(String tripId, ActivityRef ref, Map<String, Object> placeFields) {
        return activities(tripId, ref.dayId).document(ref.activityId).update(placeFields);
    }

    /**
     * Replaces every AI-generated activity with the new plan in one atomic batch: old AI activities
     * are deleted, manual ones are kept, day notes are updated and the trip becomes "planned".
     * Resolves to the new activities so their places can be looked up.
     */
    public Task<List<ActivityRef>> saveGeneratedPlan(Trip trip, GeneratedPlan plan) {
        List<Task<QuerySnapshot>> reads = new ArrayList<>();
        for (GeneratedDay day : plan.days) {
            reads.add(activities(trip.id, Day.idFor(day.dayNumber))
                    .whereEqualTo("source", PlannedActivity.SOURCE_AI).get());
        }

        return Tasks.whenAllSuccess(reads).continueWithTask(readTask -> {
            if (!readTask.isSuccessful()) return Tasks.forException(readTask.getException());

            WriteBatch batch = db.batch();
            int writes = 0;
            for (Object result : readTask.getResult()) {
                for (DocumentSnapshot old : ((QuerySnapshot) result).getDocuments()) {
                    batch.delete(old.getReference());
                    writes++;
                }
            }

            List<ActivityRef> created = new ArrayList<>();
            List<java.time.LocalDate> dates = DateUtils.tripDates(trip.startDate, trip.endDate);
            for (GeneratedDay day : plan.days) {
                String dayId = Day.idFor(day.dayNumber);
                DocumentReference dayRef = trip(trip.id).collection("days").document(dayId);
                Map<String, Object> dayFields = new HashMap<>();
                dayFields.put("dayNumber", day.dayNumber);
                dayFields.put("date", DateUtils.toIso(dates.get(day.dayNumber - 1)));
                dayFields.put("note", day.note);
                batch.set(dayRef, dayFields, SetOptions.merge());
                writes++;

                for (GeneratedActivity a : day.activities) {
                    DocumentReference ref = dayRef.collection("activities").document();
                    PlannedActivity activity = new PlannedActivity();
                    activity.title = a.title;
                    activity.time = a.time;
                    activity.description = a.description;
                    activity.estimatedCost = a.estimatedCost;
                    activity.placeName = a.placeName;
                    activity.source = PlannedActivity.SOURCE_AI;
                    batch.set(ref, activity.toMap());
                    created.add(new ActivityRef(dayId, ref.getId(), a.placeName));
                    writes++;
                }
            }
            // generatedAt changes on every (re)generation; the notify Cloud Function watches it.
            batch.update(trip(trip.id), "status", Trip.STATUS_PLANNED,
                    "generatedAt", FieldValue.serverTimestamp());
            writes++;

            if (writes > MAX_BATCH_WRITES) {
                return Tasks.forException(new ErrorMessages.FriendlyException(
                        "This plan is too large to save. Try a shorter trip."));
            }
            return batch.commit().continueWith(commit -> {
                if (!commit.isSuccessful()) throw commit.getException();
                return created;
            });
        });
    }
}
