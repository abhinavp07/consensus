package com.abhinavpinisetti.consensus.data.repo;

import android.util.Log;

import com.abhinavpinisetti.consensus.data.model.MemberProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/** users/{userId}: profile and FCM device tokens. */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static UserRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized UserRepository get() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    /** Creates users/{uid} on first sign-in and keeps name/photo fresh afterwards. */
    public Task<Void> saveProfile(FirebaseUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", displayName(user));
        data.put("email", user.getEmail());
        data.put("photoUrl", user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString());
        return db.collection("users").document(user.getUid()).set(data, SetOptions.merge());
    }

    /** The current user's display info, as copied onto trips they create or join. */
    public MemberProfile currentProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return new MemberProfile("Someone", null);
        return new MemberProfile(displayName(user),
                user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString());
    }

    /**
     * Registers this device for push notifications. getToken() is deprecated in recent FCM releases
     * in favour of register()/onRegistered(), but still works and is the documented flow; the
     * messaging service handles both callbacks.
     */
    @SuppressWarnings("deprecation")
    public void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::saveFcmToken)
                .addOnFailureListener(e -> Log.w(TAG, "Could not get FCM token", e));
    }

    public void saveFcmToken(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || token == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("fcmTokens", FieldValue.arrayUnion(token));
        db.collection("users").document(uid).set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Could not save FCM token", e));
    }

    /** Removes this device's token so a signed-out phone stops getting the user's notifications. */
    @SuppressWarnings("deprecation")
    public Task<Void> unregisterFcmToken() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forResult(null);
        return FirebaseMessaging.getInstance().getToken().continueWithTask(task -> {
            if (!task.isSuccessful()) return Tasks.forResult(null);
            return db.collection("users").document(uid)
                    .update("fcmTokens", FieldValue.arrayRemove(task.getResult()));
        });
    }

    private static String displayName(FirebaseUser user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) return user.getDisplayName();
        if (user.getEmail() != null) return user.getEmail().split("@")[0];
        return "Traveler";
    }
}
