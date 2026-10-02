package com.abhinavpinisetti.consensus.data.repo;

import androidx.annotation.Nullable;

import com.abhinavpinisetti.consensus.data.DocumentLiveData;
import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.QueryLiveData;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.data.model.MemberProfile;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.util.DateUtils;
import com.abhinavpinisetti.consensus.util.InviteCodeGenerator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Trips, invite codes, and membership. */
public class TripRepository {

    public static final String NOT_FOUND_MESSAGE = "No trip found with that code";
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static TripRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final InviteCodeGenerator codes = new InviteCodeGenerator();

    public static synchronized TripRepository get() {
        if (instance == null) instance = new TripRepository();
        return instance;
    }

    /** Input for {@link #createTrip}. Destination coordinates are optional (Places may be unavailable). */
    public static class TripDraft {
        public String name;
        public String destination;
        @Nullable public String destinationPlaceId;
        @Nullable public Double destinationLat;
        @Nullable public Double destinationLng;
        public LocalDate startDate;
        public LocalDate endDate;
    }

    public DocumentReference tripRef(String tripId) {
        return db.collection("trips").document(tripId);
    }

    public QueryLiveData<Trip> tripsForUser(String uid) {
        // Sorted client-side by start date to avoid needing a composite index.
        return new QueryLiveData<>(db.collection("trips").whereArrayContains("memberIds", uid), Trip.class);
    }

    public DocumentLiveData<Trip> trip(String tripId) {
        return new DocumentLiveData<>(tripRef(tripId), Trip.class);
    }

    public Task<Trip> fetchTrip(String tripId) {
        return tripRef(tripId).get().continueWith(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (!snapshot.exists()) throw new ErrorMessages.FriendlyException("This trip no longer exists.");
            return snapshot.toObject(Trip.class);
        });
    }

    /**
     * Creates the trip, its invite code, and one day document per date in a single transaction.
     * Retries with a fresh code in the unlikely case the generated one is already taken.
     */
    public Task<String> createTrip(TripDraft draft, String uid, MemberProfile me) {
        return attemptCreate(draft, uid, me, 1);
    }

    private Task<String> attemptCreate(TripDraft draft, String uid, MemberProfile me, int attempt) {
        String code = codes.next();
        DocumentReference tripRef = db.collection("trips").document();
        DocumentReference codeRef = db.collection("inviteCodes").document(code);

        Task<String> transaction = db.runTransaction(tx -> {
            if (tx.get(codeRef).exists()) {
                throw new CodeTakenException();
            }
            Map<String, Object> trip = new HashMap<>();
            trip.put("name", draft.name);
            trip.put("destination", draft.destination);
            trip.put("destinationPlaceId", draft.destinationPlaceId);
            trip.put("destinationLat", draft.destinationLat);
            trip.put("destinationLng", draft.destinationLng);
            trip.put("startDate", DateUtils.toIso(draft.startDate));
            trip.put("endDate", DateUtils.toIso(draft.endDate));
            trip.put("ownerId", uid);
            trip.put("inviteCode", code);
            trip.put("status", Trip.STATUS_COLLECTING);
            trip.put("memberIds", Collections.singletonList(uid));
            Map<String, Object> profiles = new HashMap<>();
            profiles.put(uid, me.toMap());
            trip.put("memberProfiles", profiles);
            trip.put("createdAt", FieldValue.serverTimestamp());
            tx.set(tripRef, trip);

            Map<String, Object> invite = new HashMap<>();
            invite.put("tripId", tripRef.getId());
            invite.put("ownerId", uid);
            tx.set(codeRef, invite);

            List<LocalDate> dates = DateUtils.tripDates(DateUtils.toIso(draft.startDate), DateUtils.toIso(draft.endDate));
            for (int i = 0; i < dates.size(); i++) {
                int dayNumber = i + 1;
                Map<String, Object> day = new HashMap<>();
                day.put("date", DateUtils.toIso(dates.get(i)));
                day.put("dayNumber", dayNumber);
                day.put("note", "");
                tx.set(tripRef.collection("days").document(Day.idFor(dayNumber)), day);
            }
            return tripRef.getId();
        });

        return transaction.continueWithTask(task -> {
            if (!task.isSuccessful()
                    && ErrorMessages.find(task.getException(), CodeTakenException.class) != null
                    && attempt < MAX_CODE_ATTEMPTS) {
                return attemptCreate(draft, uid, me, attempt + 1);
            }
            return task;
        });
    }

    /**
     * Looks up the code and adds the user to the trip. Joining a trip you're already in is a no-op
     * (arrayUnion never duplicates), so it simply resolves to the trip ID.
     */
    public Task<String> joinTrip(String rawCode, String uid, MemberProfile me) {
        String code = InviteCodeGenerator.normalize(rawCode);
        if (!InviteCodeGenerator.isWellFormed(code)) {
            return Tasks.forException(new ErrorMessages.FriendlyException(NOT_FOUND_MESSAGE));
        }
        return db.collection("inviteCodes").document(code).get().continueWithTask(task -> {
            if (!task.isSuccessful()) return Tasks.forException(task.getException());
            DocumentSnapshot invite = task.getResult();
            String tripId = invite.exists() ? invite.getString("tripId") : null;
            if (tripId == null) {
                return Tasks.forException(new ErrorMessages.FriendlyException(NOT_FOUND_MESSAGE));
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("memberIds", FieldValue.arrayUnion(uid));
            updates.put("memberProfiles." + uid, me.toMap());
            return tripRef(tripId).update(updates).continueWith(update -> {
                if (!update.isSuccessful()) {
                    FirebaseFirestoreException fe =
                            ErrorMessages.find(update.getException(), FirebaseFirestoreException.class);
                    // A stale code pointing at a deleted trip fails NOT_FOUND / PERMISSION_DENIED.
                    if (fe != null && (fe.getCode() == FirebaseFirestoreException.Code.NOT_FOUND
                            || fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED)) {
                        throw new ErrorMessages.FriendlyException(NOT_FOUND_MESSAGE, fe);
                    }
                    throw update.getException();
                }
                return tripId;
            });
        });
    }

    /** Removes the user from the trip along with their survey answers. */
    public Task<Void> leaveTrip(String tripId, String uid) {
        WriteBatch batch = db.batch();
        batch.delete(tripRef(tripId).collection("preferences").document(uid));
        Map<String, Object> updates = new HashMap<>();
        updates.put("memberIds", FieldValue.arrayRemove(uid));
        updates.put("memberProfiles." + uid, FieldValue.delete());
        batch.update(tripRef(tripId), updates);
        return batch.commit();
    }

    /**
     * Deletes the trip and its invite code. Subcollections are removed server-side by the
     * onTripDeleted Cloud Function (clients can't recursively delete).
     */
    public Task<Void> deleteTrip(Trip trip) {
        WriteBatch batch = db.batch();
        batch.delete(tripRef(trip.id));
        if (trip.inviteCode != null) batch.delete(db.collection("inviteCodes").document(trip.inviteCode));
        return batch.commit();
    }

    /** Thrown inside the create transaction when a generated invite code already exists. */
    static class CodeTakenException extends RuntimeException {
        CodeTakenException() {
            super("Invite code already in use");
        }
    }
}
