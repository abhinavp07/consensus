package com.abhinavpinisetti.consensus.data.repo;

import com.abhinavpinisetti.consensus.data.QueryLiveData;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** trips/{tripId}/preferences/{userId} */
public class PreferencesRepository {

    private static PreferencesRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized PreferencesRepository get() {
        if (instance == null) instance = new PreferencesRepository();
        return instance;
    }

    private CollectionReference collection(String tripId) {
        return db.collection("trips").document(tripId).collection("preferences");
    }

    public QueryLiveData<Preferences> preferences(String tripId) {
        return new QueryLiveData<>(collection(tripId), Preferences.class);
    }

    /** Resolves to null when the user hasn't answered yet. */
    public Task<Preferences> fetchMine(String tripId, String uid) {
        return collection(tripId).document(uid).get()
                .continueWith(task -> task.getResult().toObject(Preferences.class));
    }

    public Task<List<Preferences>> fetchAll(String tripId) {
        return collection(tripId).get()
                .continueWith(task -> task.getResult().toObjects(Preferences.class));
    }

    /** Overwrites any previous answers. */
    public Task<Void> save(String tripId, String uid, Preferences prefs) {
        Map<String, Object> data = new HashMap<>();
        data.put("budget", prefs.budget);
        data.put("interests", prefs.interests);
        data.put("pace", prefs.pace);
        data.put("mustDos", prefs.mustDos);
        data.put("avoid", prefs.avoid);
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        return collection(tripId).document(uid).set(data);
    }
}
